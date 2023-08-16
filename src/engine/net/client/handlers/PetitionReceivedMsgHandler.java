package engine.net.client.handlers;

import engine.Enum;
import engine.exception.MsgSendException;
import engine.gameManager.DbManager;
import engine.net.Dispatch;
import engine.net.DispatchMessage;
import engine.net.client.ClientConnection;
import engine.net.client.msg.ClientNetMsg;
import engine.net.client.msg.PetitionReceivedMsg;
import engine.objects.Petition;
import engine.objects.PlayerCharacter;

public class PetitionReceivedMsgHandler extends AbstractClientMsgHandler {

    public static final int PETITION_NEW = 1;
    public static final int PETITION_CANCEL = 2;
    public static final int PETITION_CLOSE = 4;

    public PetitionReceivedMsgHandler() {
        super(PetitionReceivedMsg.class);
    }

    @Override
    protected boolean _handleNetMsg(ClientNetMsg msg, ClientConnection origin) throws MsgSendException {

        if (msg == null)
            return true;

        PetitionReceivedMsg petitionReceivedMsg = (PetitionReceivedMsg) msg;

        if (origin == null)
            return true;

        PlayerCharacter playerCharacter = origin.getPlayerCharacter();

        if (playerCharacter == null)
            return true;

        Petition petition = new Petition(petitionReceivedMsg, origin);

            // Write petition to database

            if (petitionReceivedMsg.petition == PETITION_NEW)
                DbManager.PetitionQueries.WRITE_PETITION_TO_TABLE(petition);

            // Close the petition window

            if (petitionReceivedMsg.petition == PETITION_NEW)
                petitionReceivedMsg.petition = PETITION_CLOSE;

            petitionReceivedMsg.unknownByte01 = 0;
            petitionReceivedMsg.unknown04 = 0;

            Dispatch dispatch = Dispatch.borrow(playerCharacter, petitionReceivedMsg);
            DispatchMessage.dispatchMsgDispatch(dispatch, Enum.DispatchChannel.SECONDARY);

        return true;
    }
}
