package engine.net.client.handlers;

import engine.exception.MsgSendException;
import engine.net.client.ClientConnection;
import engine.net.client.msg.ClientNetMsg;
import engine.net.client.msg.PetitionReceivedMsg;

public class PetitionReceivedMsgHandler extends AbstractClientMsgHandler {

    public PetitionReceivedMsgHandler() {
        super(PetitionReceivedMsg.class);
    }

    @Override
    protected boolean _handleNetMsg(ClientNetMsg msg, ClientConnection origin) throws MsgSendException {
        switch(((PetitionReceivedMsg) msg).getType()){
            case 1: // TYPE_GENERAL_HELP

                break;
            case 2: // TYPE_FEEDBACK

                break;
            case 3: // TYPE_STUCK

                break;
            case 4: // TYPE_HARASSMENT

                break;
            case 5: // TYPE_EXPLOIT

                break;
            case 6: // TYPE_BUG

                break;
            case 7: // TYPE_GAME_STOPPER

                break;
            case 8: // TYPE_TECH_SUPPORT

                break;
            default: // INVALID_TYPE cannot process this
                return false;
        }
            switch (((PetitionReceivedMsg)msg).getSubType()) {
                case 1: // SUBTYPE_EXPLOIT_DUPE

                    break;
                case 2: // SUBTYPE_EXPLOIT_LEVELING

                    break;
                case 3: // SUBTYPE_EXPLOIT_SKILL_GAIN

                    break;
                case 4: // SUBTYPE_EXPLOIT_KILLING

                    break;
                case 5: // SUBTYPE_EXPLOIT_POLICY

                    break;
                case 6: // SUBTYPE_EXPLOIT_OTHER

                    break;
                case 7: // SUBTYPE_TECH_VIDEO

                    break;
                case 8: // SUBTYPE_TECH_SOUND

                    break;
                case 9: // SUBTYPE_TECH_NETWORK

                    break;
                case 10: // SUBTYPE_TECH_OTHER

                    break;
                default: // INVALID_SUB_TYPE

                    break;
            }
        return true;
    }
}
