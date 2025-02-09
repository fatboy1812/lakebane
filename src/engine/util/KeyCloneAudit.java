package engine.util;

import engine.gameManager.ChatManager;
import engine.gameManager.ConfigManager;
import engine.gameManager.DbManager;
import engine.gameManager.SessionManager;
import engine.net.client.ClientConnection;
import engine.net.client.Protocol;
import engine.net.client.msg.ClientNetMsg;
import engine.net.client.msg.TargetObjectMsg;
import engine.objects.Group;
import engine.objects.PlayerCharacter;
import org.pmw.tinylog.Logger;

public enum KeyCloneAudit {
    KEYCLONEAUDIT;

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

    public static boolean auditNetMsg(ClientNetMsg msg) {
        boolean valid = true;

        if (msg.getProtocolMsg().equals(Protocol.KEEPALIVESERVERCLIENT))
            return true;

        ClientConnection origin = (ClientConnection) msg.getOrigin();
        long now = System.currentTimeMillis();
        PlayerCharacter pc = SessionManager.getSession(origin).getPlayerCharacter();

        if (msg.getProtocolMsg().equals(Protocol.SETSELECTEDOBECT)) {
            TargetObjectMsg tarMsg = (TargetObjectMsg) msg;

            // Calculate time since last target switch
            long timeSinceLastTarget = now - origin.lastTargetSwitchTime;
            origin.lastTargetSwitchTime = now;

            // Check if the target has changed
            if (tarMsg.getTargetID() != origin.lastTargetID) {
                origin.lastTargetID = tarMsg.getTargetID();
                origin.targetSwitchCount++;

                // If switching too fast, flag as bot-like behavior
                if (timeSinceLastTarget < 300) { // Adjust this threshold if needed
                    origin.fastTargetSwitchCount++;
                } else {
                    origin.fastTargetSwitchCount = Math.max(0, origin.fastTargetSwitchCount - 1);
                }

                if (origin.fastTargetSwitchCount > 5) {
                    ChatManager.chatSystemInfo(pc, "Possible bot detected: Targeting too quickly.");
                    valid = false;
                }
            }
        }

        return valid;
    }

}
