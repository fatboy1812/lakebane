package engine.util;

import engine.gameManager.ConfigManager;
import engine.gameManager.DbManager;
import engine.gameManager.GroupManager;
import engine.gameManager.SessionManager;
import engine.net.client.ClientConnection;
import engine.net.client.Protocol;
import engine.net.client.msg.ClientNetMsg;
import engine.net.client.msg.TargetObjectMsg;
import engine.objects.Group;
import engine.objects.PlayerCharacter;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

public enum KeyCloneAudit {
    KEYCLONEAUDIT;

    public static boolean auditChatMsg(PlayerCharacter pc, String message) {

        if(pc.combatTarget != null && message.contains(String.valueOf(pc.combatTarget.getObjectUUID()))) {
            //targeting software detected

            Group g = GroupManager.getGroup(pc);

            if (g == null)
                pc.getClientConnection().forceDisconnect();
             else
                for (PlayerCharacter member : g.members)
                    member.getClientConnection().forceDisconnect();

            return true;
        }

        return false;
    }

    public void audit(PlayerCharacter player, Group group) {

        int machineCount = 0;
        String machineID;

        machineID = player.getClientConnection().machineID;

        for (PlayerCharacter member : group.getMembers())
            if (machineID.equals(member.getClientConnection().machineID))
                machineCount = machineCount + 1;

        if (machineCount > Integer.parseInt(ConfigManager.MB_WORLD_KEYCLONE_MAX.getValue())) {
            Logger.error("Keyclone detected from: " + player.getAccount().getUname() +
                    " with machine count of: " + machineCount);
            DbManager.AccountQueries.SET_TRASH(machineID);
        }

    }

    public static void auditTargetMsg(ClientNetMsg msg) {
        try {
            TargetObjectMsg tarMsg = (TargetObjectMsg) msg;
            ClientConnection origin = (ClientConnection) msg.getOrigin();
            long now = System.currentTimeMillis();

            if (tarMsg.getTargetType() != MBServerStatics.MASK_PLAYER)
                return;

            if (System.currentTimeMillis() > origin.finalStrikeRefresh) {
                origin.lastStrike = System.currentTimeMillis();
                origin.strikes = 0;
                origin.finalStrikes = 0;
                origin.finalStrikeRefresh = System.currentTimeMillis();
            }
            // Calculate time since last target switch
            long timeSinceLastTarget = now - origin.lastTargetSwitchTime;
            origin.lastTargetSwitchTime = now;
            if (timeSinceLastTarget < 150) {
                origin.strikes++;
                origin.finalStrikeRefresh = System.currentTimeMillis() + 1000L;
            }
            if (origin.strikes > 20) {
                origin.finalStrikes++;
            }
            if (origin.finalStrikes > 3) {origin.forceDisconnect();
                DbManager.AccountQueries.SET_TRASH(origin.machineID);
            }
        } catch (Exception e) {

        }
    }

}
