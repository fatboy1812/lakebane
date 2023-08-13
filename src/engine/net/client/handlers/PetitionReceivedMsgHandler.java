package engine.net.client.handlers;

import engine.exception.MsgSendException;
import engine.net.client.ClientConnection;
import engine.net.client.msg.ClientNetMsg;
import engine.net.client.msg.PetitionReceivedMsg;
import engine.objects.Petition;

public class PetitionReceivedMsgHandler extends AbstractClientMsgHandler {

    public PetitionReceivedMsgHandler() {
        super(PetitionReceivedMsg.class);
    }

    @Override
    protected boolean _handleNetMsg(ClientNetMsg msg, ClientConnection origin) throws MsgSendException {
        if(msg == null)
            return false;

        if(origin == null)
            return false;

        Petition report = new Petition(msg,origin);
        if(report == null){
            return false;
        }

        try {
            report.updateDatabase();
        }
        catch(Exception e){
            return false;
        }
        return true;
    }
}
