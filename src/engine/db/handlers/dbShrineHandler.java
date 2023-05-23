// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum.ProtectionState;
import engine.gameManager.DbManager;
import engine.math.Vector3fImmutable;
import engine.objects.AbstractGameObject;
import engine.objects.Building;
import engine.objects.Shrine;
import org.joda.time.DateTime;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class dbShrineHandler extends dbHandlerBase {

    public dbShrineHandler() {
        this.localClass = Shrine.class;
        this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public static void addObject(ArrayList<AbstractGameObject> list, ResultSet rs) throws SQLException {

        String type = rs.getString("type");

        switch (type) {
            case "building":
                Building building = new Building(rs);
                DbManager.addToCache(building);
                list.add(building);
                break;
            case "shrine":
                Shrine shrine = new Shrine(rs);
                DbManager.addToCache(shrine);
                list.add(shrine);
                break;
        }
    }

    public ArrayList<AbstractGameObject> CREATE_SHRINE(int parentZoneID, int OwnerUUID, String name, int meshUUID,
                                                       Vector3fImmutable location, float meshScale, int currentHP,
                                                       ProtectionState protectionState, int currentGold, int rank,
                                                       DateTime upgradeDate, int blueprintUUID, float w, float rotY, String shrineType) {

        ArrayList<AbstractGameObject> shrineList = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("CALL `shrine_CREATE`(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,? ,? ,?, ?,?);")) {

            preparedStatement.setInt(1, parentZoneID);
            preparedStatement.setInt(2, OwnerUUID);
            preparedStatement.setString(3, name);
            preparedStatement.setInt(4, meshUUID);
            preparedStatement.setFloat(5, location.x);
            preparedStatement.setFloat(6, location.y);
            preparedStatement.setFloat(7, location.z);
            preparedStatement.setFloat(8, meshScale);
            preparedStatement.setInt(9, currentHP);
            preparedStatement.setString(10, protectionState.name());
            preparedStatement.setInt(11, currentGold);
            preparedStatement.setInt(12, rank);

            if (upgradeDate != null)
                preparedStatement.setTimestamp(13, new java.sql.Timestamp(upgradeDate.getMillis()));
            else
                preparedStatement.setNull(13, java.sql.Types.DATE);

            preparedStatement.setInt(14, blueprintUUID);
            preparedStatement.setFloat(15, w);
            preparedStatement.setFloat(16, rotY);
            preparedStatement.setString(17, shrineType);

            preparedStatement.execute();

            ResultSet rs = preparedStatement.getResultSet();

            while (rs.next())
                addObject(shrineList, rs);

            while (preparedStatement.getMoreResults()) {
                rs = preparedStatement.getResultSet();

                while (rs.next())
                    addObject(shrineList, rs);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        return shrineList;
    }

    public boolean updateFavors(Shrine shrine, int amount, int oldAmount) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_shrine` SET `shrine_favors`=? WHERE `UID` = ? AND `shrine_favors` = ?")) {

            preparedStatement.setInt(1, amount);
            preparedStatement.setLong(2, shrine.getObjectUUID());
            preparedStatement.setInt(3, oldAmount);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }

    }

    public void LOAD_ALL_SHRINES() {

        Shrine shrine;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_shrine`.*, `object`.`parent`, `object`.`type` FROM `object` LEFT JOIN `obj_shrine` ON `object`.`UID` = `obj_shrine`.`UID` WHERE `object`.`type` = 'shrine';")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                shrine = new Shrine(rs);
                shrine.getShrineType().addShrineToServerList(shrine);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }
    }

}
