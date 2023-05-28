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
import engine.objects.AbstractGameObject;
import engine.objects.Building;
import engine.objects.City;
import engine.objects.Zone;
import org.pmw.tinylog.Logger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class dbCityHandler extends dbHandlerBase {

    public dbCityHandler() {
        this.localClass = City.class;
        this.localObjectType = Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public static void addObject(ArrayList<AbstractGameObject> list, ResultSet rs) throws SQLException {
        String type = rs.getString("type");
        switch (type) {
            case "zone":
                Zone zone = new Zone(rs);
                DbManager.addToCache(zone);
                list.add(zone);
                break;
            case "building":
                Building building = new Building(rs);
                DbManager.addToCache(building);
                list.add(building);
                break;
            case "city":
                City city = new City(rs);
                DbManager.addToCache(city);
                list.add(city);
                break;
        }
    }

    public ArrayList<AbstractGameObject> CREATE_CITY(int ownerID, int parentZoneID, int realmID, float xCoord, float yCoord, float zCoord, float rotation, float W, String name, LocalDateTime established) {

        LocalDateTime upgradeTime = LocalDateTime.now().plusHours(2);
        ArrayList<AbstractGameObject> objectList = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("CALL `city_CREATE`(?, ?, ?, ?, ?, ?, ?, ?, ?,?,?)")) {

            preparedStatement.setLong(1, ownerID); //objectUUID of owning player
            preparedStatement.setLong(2, parentZoneID); //objectUUID of parent (continent) zone
            preparedStatement.setLong(3, realmID); //objectUUID of realm city belongs in
            preparedStatement.setFloat(4, xCoord); //xOffset from parentZone center
            preparedStatement.setFloat(5, yCoord); //yOffset from parentZone center
            preparedStatement.setFloat(6, zCoord); //zOffset from parentZone center
            preparedStatement.setString(7, name); //city name
            preparedStatement.setTimestamp(8, Timestamp.valueOf(established));
            preparedStatement.setFloat(9, rotation);
            preparedStatement.setFloat(10, W);
            preparedStatement.setTimestamp(11, Timestamp.valueOf(upgradeTime));

            boolean work = preparedStatement.execute();

            if (work) {

                ResultSet rs = preparedStatement.getResultSet();

                while (rs.next())
                    addObject(objectList, rs);
                rs.close();
            } else {
                Logger.info("City Placement Failed: " + preparedStatement);
                return objectList; //city creation failure
            }
            while (preparedStatement.getMoreResults()) {
                ResultSet rs = preparedStatement.getResultSet();
                while (rs.next()) {
                    addObject(objectList, rs);
                }
                rs.close();
            }
        } catch (SQLException e) {
            Logger.error(e);
        }

        return objectList;
    }

    public ArrayList<City> GET_CITIES_BY_ZONE(final int objectUUID) {

        ArrayList<City> cityList = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_city`.*, `object`.`parent` FROM `obj_city` INNER JOIN `object` ON `object`.`UID` = `obj_city`.`UID` WHERE `object`.`parent`=?;")) {

            preparedStatement.setLong(1, objectUUID);

            ResultSet rs = preparedStatement.executeQuery();
            cityList = getObjectsFromRs(rs, 100);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return cityList;
    }

    public City GET_CITY(final int cityId) {

        City city = (City) DbManager.getFromCache(Enum.GameObjectType.City, cityId);

        if (city != null)
            return city;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_city`.*, `object`.`parent` FROM `obj_city` INNER JOIN `object` ON `object`.`UID` = `obj_city`.`UID` WHERE `object`.`UID`=?;")) {

            preparedStatement.setLong(1, cityId);

            ResultSet rs = preparedStatement.executeQuery();
            city = (City) getObjectFromRs(rs);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return city;
    }

    public boolean updateforceRename(City city, boolean value) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_city` SET `forceRename`=?"
                     + " WHERE `UID` = ?")) {

            preparedStatement.setByte(1, (value == true) ? (byte) 1 : (byte) 0);
            preparedStatement.setInt(2, city.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public boolean updateOpenCity(City city, boolean value) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_city` SET `open`=?"
                     + " WHERE `UID` = ?")) {

            preparedStatement.setByte(1, (value == true) ? (byte) 1 : (byte) 0);
            preparedStatement.setInt(2, city.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public boolean updateTOL(City city, int tolID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_city` SET `treeOfLifeUUID`=?"
                     + " WHERE `UID` = ?")) {

            preparedStatement.setInt(1, tolID);
            preparedStatement.setInt(2, city.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }

    }

    public boolean renameCity(City city, String name) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_city` SET `name`=?"
                     + " WHERE `UID` = ?")) {

            preparedStatement.setString(1, name);
            preparedStatement.setInt(2, city.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }

    }

    public boolean updateSiegesWithstood(City city, int value) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_city` SET `name`=?"
                     + " WHERE `UID` = ?")) {

            preparedStatement.setInt(1, value);
            preparedStatement.setInt(2, city.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }

    }

    public boolean updateRealmTaxDate(City city, LocalDateTime localDateTime) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_city` SET `realmTaxDate` =?"
                     + " WHERE `UID` = ?")) {

            preparedStatement.setTimestamp(1, Timestamp.valueOf(localDateTime));
            preparedStatement.setInt(2, city.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

}
