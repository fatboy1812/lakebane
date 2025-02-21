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

        if(pc.selectedUUID == 0)
            return false;

        int id = pc.selectedUUID;
        String value = String.valueOf(id);
        if(message.contains(value)) {
            //targeting software detected

            Group g = GroupManager.getGroup(pc);

            if (g == null) {
                try {
                    Logger.error("TARGET SOFTWARE DETECTED ON ACCOUNT: " + pc.getAccount().getUname());
                    DbManager.AccountQueries.SET_TRASH(pc.getAccount().getUname(), "TARGET");
                    pc.getClientConnection().forceDisconnect();
                }catch(Exception e){

                }
            }else {
                for (PlayerCharacter member : g.members) {
                    try {
                        Logger.error("TARGET SOFTWARE DETECTED ON ACCOUNT: " + member.getAccount().getUname());
                        DbManager.AccountQueries.SET_TRASH(member.getAccount().getUname(), "TARGET");
                        member.getClientConnection().forceDisconnect();
                    } catch (Exception e) {

                    }
                }
            }
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
            DbManager.AccountQueries.SET_TRASH(machineID,"MEMBERLIMIT");
        }

    }

    public static void auditTargetMsg(ClientNetMsg msg) {
        try {
            TargetObjectMsg tarMsg = (TargetObjectMsg) msg;
            ClientConnection origin = (ClientConnection) msg.getOrigin();

            long now = System.currentTimeMillis();

            if (PlayerCharacter.getPlayerCharacter(tarMsg.getTargetID()) == null)
                return;

            PlayerCharacter pc = origin.getPlayerCharacter();
            pc.selectedUUID = tarMsg.getTargetID();


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
            if (origin.finalStrikes > 3) {
                origin.forceDisconnect();
                DbManager.AccountQueries.SET_TRASH(pc.getAccount().getUname(), "TABSPEED");
                Logger.error("TAB SPEED DETECTED ON ACCOUNT: " + pc.getAccount().getUname());
            }
        } catch (Exception e) {

        }
    }

}
