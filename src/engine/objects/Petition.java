// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import engine.Enum;
import engine.gameManager.DbManager;
import engine.math.Vector3fImmutable;
import engine.net.client.ClientConnection;
import engine.net.client.msg.ClientNetMsg;
import engine.net.client.msg.PetitionReceivedMsg;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Petition extends AbstractGameObject {
    public int primaryType;
    public int subType;
    public Account reportAccount;
    public PlayerCharacter reportPlayer;
    public Vector3fImmutable playerLocation;
    public String message;

    public Petition(ClientNetMsg msg, ClientConnection origin){
        this.primaryType = ((PetitionReceivedMsg)msg).getType();
        this.subType = ((PetitionReceivedMsg)msg).getSubType();
        this.reportAccount = origin.getAccount();
        this.reportPlayer = origin.getPlayerCharacter();
        this.playerLocation = origin.getPlayerCharacter().getLoc();
        this.message = ((PetitionReceivedMsg) msg).getMessage();
    }

    public static String getLocationString(Vector3fImmutable loc){
        return loc.x + "," + loc.y + "," + loc.z;
    }

    @Override
    public void updateDatabase() {

        try (Connection connection = DbManager.getConnection();

             //check that table exists

             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_petition` (`time`=?, `primaryType`=?, `subType`=?, `accountID`=?, `account`=?, "
                     + "`characterID`=?, `character`=?, `location`=?, `message`=?")) {

            preparedStatement.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
            preparedStatement.setString(2, Enum.PetitionType.values()[this.primaryType].name());
            preparedStatement.setString(3, Enum.PetitionSubType.values()[this.subType].name());
            preparedStatement.setInt(4, this.reportAccount.getObjectUUID());
            preparedStatement.setString(5, this.reportAccount.getUname());
            preparedStatement.setInt(6, this.reportPlayer.getObjectUUID());
            preparedStatement.setString(7, this.reportPlayer.getFirstName());
            preparedStatement.setString(8, getLocationString(this.playerLocation));
            preparedStatement.setString(9, this.message);

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            Logger.error("CREATE PETITION FAILED: " + e);
        }
    }
}
