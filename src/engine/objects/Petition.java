// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import engine.gameManager.DbManager;
import engine.math.Vector3fImmutable;
import engine.net.client.ClientConnection;
import engine.net.client.msg.ClientNetMsg;
import engine.net.client.msg.PetitionReceivedMsg;
import org.joda.time.DateTime;
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
    public DateTime reportTime;
    public String message;

    public Petition(ClientNetMsg msg, ClientConnection origin){
        this.primaryType = ((PetitionReceivedMsg)msg).getType();
        this.subType = ((PetitionReceivedMsg)msg).getSubType();
        this.reportAccount = origin.getAccount();
        this.reportPlayer = origin.getPlayerCharacter();
        this.playerLocation = origin.getPlayerCharacter().getLoc();
        this.reportTime = DateTime.now();
        this.message = ((PetitionReceivedMsg) msg).getMessage();
    }
    public static String getPrimaryTypeString(int value){
        String primaryReportType;
        switch(value){
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
                break;
        }
        return primaryReportType;
    }
    public static String getSecondaryTypeString(int value){
        String subType;
        switch (value) {
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
        return subType;
    }
    public static String getLocationString(Vector3fImmutable loc){
        return loc.x + "," + loc.y + "," + loc.z;
    }

    @Override
    public void updateDatabase() {
        try (Connection connection = DbManager.getConnection();

             //check that table exists


             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_petition` (`time`=?, `primaryType`=?, `subType`=?, `account`=?, "
                     + "`character`=?, `location`=?, `message`=?, `ID`=?)")) {

            preparedStatement.setString(1, this.reportTime.toString());
            preparedStatement.setString(2, getPrimaryTypeString(this.primaryType));
            preparedStatement.setString(3, getSecondaryTypeString(this.subType));
            preparedStatement.setString(4, this.reportAccount.getUname());
            preparedStatement.setString(5, this.reportPlayer.getFirstName());
            preparedStatement.setString(6, getLocationString(this.playerLocation));
            preparedStatement.setString(7, this.message);
            preparedStatement.setInt(8, this.getObjectUUID());

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            Logger.error("CREATE PETITION FAILED: " + e);
        }
    }
}
