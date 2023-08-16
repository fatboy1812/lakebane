// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com

package engine.objects;
import engine.math.Vector3fImmutable;
import engine.net.client.ClientConnection;
import engine.net.client.msg.PetitionReceivedMsg;

public class Petition {
    public int primaryType;
    public int subType;
    public Account reportAccount;
    public PlayerCharacter reportPlayer;
    public Vector3fImmutable playerLocation;
    public String message;

    public Petition(PetitionReceivedMsg petitionReceivedMsg, ClientConnection origin) {
        this.primaryType = petitionReceivedMsg.type;
        this.subType = petitionReceivedMsg.subType;
        this.reportAccount = origin.getAccount();
        this.reportPlayer = origin.getPlayerCharacter();
        this.playerLocation = origin.getPlayerCharacter().getLoc();
        this.message = petitionReceivedMsg.message;
    }

}
