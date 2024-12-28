// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.gameManager.DbManager;
import engine.gameManager.ZoneManager;
import engine.math.Vector3fImmutable;
import engine.objects.*;
import org.joda.time.DateTime;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class dbBaneHandler extends dbHandlerBase {

    public dbBaneHandler() {

    }

    public boolean CREATE_BANE(City city, PlayerCharacter owner, Building stone) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_banes` (`cityUUID`, `ownerUUID`, `stoneUUID`, `placementDate`) VALUES(?,?,?,?)")) {

            preparedStatement.setLong(1, city.getObjectUUID());
            preparedStatement.setLong(2, owner.getObjectUUID());
            preparedStatement.setLong(3, stone.getObjectUUID());
            preparedStatement.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));

            preparedStatement.execute();

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }

        return true;
    }

    public Bane LOAD_BANE(int cityUUID) {

        Bane bane = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * from dyn_banes WHERE `dyn_banes`.`cityUUID` = ?")) {

            preparedStatement.setLong(1, cityUUID);
            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {
                bane = new Bane(rs);
                Bane.addBane(bane);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        return bane;
    }


    public boolean SET_BANE_TIME(DateTime toSet, int cityUUID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_banes` SET `liveDate`=? WHERE `cityUUID`=?")) {

            preparedStatement.setTimestamp(1, new java.sql.Timestamp(toSet.getMillis()));
            preparedStatement.setLong(2, cityUUID);

            preparedStatement.execute();

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }

        return true;
    }

    public boolean REMOVE_BANE(Bane bane) {

        if (bane == null)
            return false;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_banes` WHERE `cityUUID` = ?")) {

            preparedStatement.setLong(1, bane.getCity().getObjectUUID());
            preparedStatement.execute();

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }

        return true;
    }
}
