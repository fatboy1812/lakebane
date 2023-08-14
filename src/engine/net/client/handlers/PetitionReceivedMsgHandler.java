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

        Petition petition = new Petition(msg, origin);

        if (petition == null)
            return false;

        try {
            // Write petition to database

            DbManager.PetitionQueries.WRITE_PETITION_TO_TABLE(petition);

            // Close the petition window

            petitionReceivedMsg.petition = 1;
            Dispatch dispatch = Dispatch.borrow(playerCharacter, msg);
            DispatchMessage.dispatchMsgDispatch(dispatch, Enum.DispatchChannel.SECONDARY);

        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
