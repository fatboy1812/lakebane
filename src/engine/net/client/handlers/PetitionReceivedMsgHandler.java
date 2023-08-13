package engine.net.client.handlers;

import engine.exception.MsgSendException;
import engine.gameManager.DbManager;
import engine.math.Vector3fImmutable;
import engine.net.client.ClientConnection;
import engine.net.client.msg.ClientNetMsg;
import engine.net.client.msg.PetitionReceivedMsg;
import org.pmw.tinylog.Logger;

public class PetitionReceivedMsgHandler extends AbstractClientMsgHandler {

    public PetitionReceivedMsgHandler() {
        super(PetitionReceivedMsg.class);
    }

    @Override
    protected boolean _handleNetMsg(ClientNetMsg msg, ClientConnection origin) throws MsgSendException {
        String originAccount = origin.getAccount().getUname();
        String originCharacter = origin.getPlayerCharacter().getName();
        Vector3fImmutable playerLocation = origin.getPlayerCharacter().getLoc();
        String primaryReportType;
        switch(((PetitionReceivedMsg) msg).getType()){
            case 1: // TYPE_GENERAL_HELP
                primaryReportType = "GENERAL";
                break;
            case 2: // TYPE_FEEDBACK
                primaryReportType = "FEEDBACK";
                break;
            case 3: // TYPE_STUCK
                primaryReportType = "STUCK";
                break;
            case 4: // TYPE_HARASSMENT
                primaryReportType = "HARASSMENT";
                break;
            case 5: // TYPE_EXPLOIT
                primaryReportType = "EXPLOIT";
                break;
            case 6: // TYPE_BUG
                primaryReportType = "BUG";
                break;
            case 7: // TYPE_GAME_STOPPER
                primaryReportType = "GAME STOPPER";
                break;
            case 8: // TYPE_TECH_SUPPORT
                primaryReportType = "TECH SUPPORT";
                break;
            default: // INVALID_TYPE cannot process this
                primaryReportType = "NONE";
                return false;
        }
        String subType;
            switch (((PetitionReceivedMsg)msg).getSubType()) {
                case 1: // SUBTYPE_EXPLOIT_DUPE
                    subType = "DUPE";
                    break;
                case 2: // SUBTYPE_EXPLOIT_LEVELING
                    subType = "LEVELLING";
                    break;
                case 3: // SUBTYPE_EXPLOIT_SKILL_GAIN
                    subType = "SKILL GAIN";
                    break;
                case 4: // SUBTYPE_EXPLOIT_KILLING
                    subType = "KILLING";
                    break;
                case 5: // SUBTYPE_EXPLOIT_POLICY
                    subType = "POLICY";
                    break;
                case 6: // SUBTYPE_EXPLOIT_OTHER
                    subType = "OTHER";
                    break;
                case 7: // SUBTYPE_TECH_VIDEO
                    subType = "VIDEO";
                    break;
                case 8: // SUBTYPE_TECH_SOUND
                    subType = "SOUND";
                    break;
                case 9: // SUBTYPE_TECH_NETWORK
                    subType = "NETWORK";
                    break;
                case 10: // SUBTYPE_TECH_OTHER
                    subType = "OTHER";
                    break;
                default: // INVALID_SUB_TYPE
                    subType = "NONE";
                    break;
            }
            String report = "ACCOUNT: " + originAccount + " CHARACTER: " + originCharacter + " LOCATION: " + playerLocation + " PRIMARY TYPE: " + primaryReportType + " SUB TYPE: " + subType + " MESSAGE: " + ((PetitionReceivedMsg) msg).getMessage();
            Logger.info(report);
        return true;
    }
    private void logBugReport(PetitionReceivedMsg msg){

    }
}
