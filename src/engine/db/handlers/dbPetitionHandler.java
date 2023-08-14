// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum;
import engine.gameManager.DbManager;
import engine.objects.Petition;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class dbPetitionHandler extends dbHandlerBase {

    public dbPetitionHandler() {

    }

    public void CREATE_PETITION_TABLE() {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS dyn_petition \n" +
                     "   petitionNumber INT AUTO_INCREMENT NOT NULL," +
                     "   petitionTime DATETIME," +
                     "   primaryType VARCHAR(50)," +
                     "   subType VARCHAR(50)," +
                     "   message VARCHAR(255)," +
                     "   accountID INT," +
                     "   account VARCHAR(255)," +
                     "   characterID INT," +
                     "   character` VARCHAR(255)," +
                     "   location VARCHAR(255), " +
                     "  PRIMARY KEY (petitionNumber))" +
                     " ENGINE = innodb ROW_FORMAT = DEFAULT;")) {

            preparedStatement.executeQuery();

        } catch (SQLException e) {
            Logger.error(e);
        }
    }

    public void WRITE_PETITION_TO_TABLE(Petition petition) {

        try (Connection connection = DbManager.getConnection();

             //check that table exists

             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_petition` (`petitionTime`, `primaryType`, `subType`, `accountID`, `account`, `characterID`, `character`, `location`, `message`)" +
                     " VALUES (?,?,?,?,?,?,?,?,?);")) {

            preparedStatement.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
            preparedStatement.setString(2, Enum.PetitionType.values()[petition.primaryType].name());
            preparedStatement.setString(3, Enum.PetitionSubType.values()[petition.subType].name());
            preparedStatement.setInt(4, petition.reportAccount.getObjectUUID());
            preparedStatement.setString(5, petition.reportAccount.getUname());
            preparedStatement.setInt(6, petition.reportPlayer.getObjectUUID());
            preparedStatement.setString(7, petition.reportPlayer.getFirstName());
            preparedStatement.setString(8, petition.playerLocation.toString());
            preparedStatement.setString(9, petition.message);

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            Logger.error("CREATE PETITION FAILED: " + e);
        }
    }

}
