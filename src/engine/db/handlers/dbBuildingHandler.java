// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum;
import engine.Enum.DbObjectType;
import engine.Enum.ProtectionState;
import engine.Enum.TaxType;
import engine.gameManager.DbManager;
import engine.math.Vector3fImmutable;
import engine.objects.*;
import org.joda.time.DateTime;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;

public class dbBuildingHandler extends dbHandlerBase {

    public dbBuildingHandler() {
        this.localClass = Building.class;
        this.localObjectType = Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public Building CREATE_BUILDING(int parentZoneID, int OwnerUUID, String name, int meshUUID,
                                    Vector3fImmutable location, float meshScale, int currentHP,
                                    ProtectionState protectionState, int currentGold, int rank,
                                    DateTime upgradeDate, int blueprintUUID, float w, float rotY) {

        Building building = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("CALL `building_CREATE`(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,? ,? ,?, ?);")) {

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

            ResultSet rs = preparedStatement.executeQuery();

            int objectUUID = (int) rs.getLong("UID");

            if (objectUUID > 0)
                building = GET_BUILDINGBYUUID(objectUUID);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return building;
    }

    public boolean DELETE_FROM_DATABASE(final Building b) {

        return removeFromBuildings(b);
    }

    public ArrayList<Building> GET_ALL_BUILDINGS_FOR_ZONE(Zone zone) {

        ArrayList<Building> buildings = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_building`.*, `object`.`parent` FROM `object` INNER JOIN `obj_building` ON `obj_building`.`UID` = `object`.`UID` WHERE `object`.`parent` = ?;")) {

            preparedStatement.setLong(1, zone.getObjectUUID());

            ResultSet rs = preparedStatement.executeQuery();
            buildings = getObjectsFromRs(rs, 1000);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return buildings;
    }

    public Building GET_BUILDINGBYUUID(int uuid) {

        if (uuid == 0)
            return null;

        Building building = (Building) DbManager.getFromCache(Enum.GameObjectType.Building, uuid);

        if (building != null)
            return building;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_building`.*, `object`.`parent` FROM `object` INNER JOIN `obj_building` ON `obj_building`.`UID` = `object`.`UID` WHERE `object`.`UID` = ?;")) {

            preparedStatement.setLong(1, uuid);

            ResultSet rs = preparedStatement.executeQuery();
            building = (Building) getObjectFromRs(rs);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return building;
    }

    public Building GET_BUILDING_BY_MESH(final int meshID) {

        Building building = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("CALL building_GETBYMESH(?)")) {

            preparedStatement.setInt(1, meshID);

            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next())
                building = new Building(rs);

        } catch (SQLException e) {
            Logger.error("Building", e);
        }

        return building;
    }

    public String SET_PROPERTY(final Building b, String name, Object new_value) {

        String result = "";

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("CALL building_SETPROP(?,?,?)")) {

            preparedStatement.setLong(1, b.getObjectUUID());
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, String.valueOf(new_value));

            ResultSet rs = preparedStatement.executeQuery();
            result = rs.getString("result");


        } catch (SQLException e) {
            Logger.error(e);
        }

        return result;
    }

    public int MOVE_BUILDING(long buildingID, long parentID, float locX, float locY, float locZ) {

        int rowCount;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `object` INNER JOIN `obj_building` On `object`.`UID` = `obj_building`.`UID` SET `object`.`parent`=?, `obj_building`.`locationX`=?, `obj_building`.`locationY`=?, `obj_building`.`locationZ`=? WHERE `obj_building`.`UID`=?;")) {

            preparedStatement.setLong(1, parentID);
            preparedStatement.setFloat(2, locX);
            preparedStatement.setFloat(3, locY);
            preparedStatement.setFloat(4, locZ);
            preparedStatement.setLong(5, buildingID);

            rowCount = preparedStatement.executeUpdate();

        } catch (SQLException e) {
            Logger.error(e);
            return 0;
        }

        return rowCount;
    }

    private boolean removeFromBuildings(final Building b) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `object` WHERE `UID` = ?")) {

            preparedStatement.setLong(1, b.getObjectUUID());
            preparedStatement.execute();

            return true;
        } catch (SQLException e) {
            Logger.error(e);
        }

        return false;
    }

    public boolean CHANGE_NAME(Building b, String newName) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_building` SET `name`=? WHERE `UID`=?")) {

            preparedStatement.setString(1, newName);
            preparedStatement.setLong(2, b.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean SET_RESERVE(Building b, int reserveAmount) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_building` SET `reserve`=? WHERE `UID`=?")) {

            preparedStatement.setInt(1, reserveAmount);
            preparedStatement.setLong(2, b.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return false;
    }

    //CAS update to rank
    public boolean CHANGE_RANK(final long buildingID, int newRank) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_building` SET `rank`=? WHERE `UID`=?")) {

            preparedStatement.setInt(1, newRank);
            preparedStatement.setLong(2, buildingID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean UPDATE_BUILDING_HEALTH(final long buildingID, int NewHealth) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_building` SET `currentHP`=? WHERE `UID`=?")) {

            preparedStatement.setInt(1, NewHealth);
            preparedStatement.setLong(2, buildingID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean UPDATE_BUILDING_ALTITUDE(final long buildingID, float newAltitude) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_building` SET `locationY`=? WHERE `UID`=?")) {

            preparedStatement.setFloat(1, newAltitude);
            preparedStatement.setLong(2, buildingID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean UPDATE_PROTECTIONSTATE(final long buildingUUID, ProtectionState protectionState) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_building` SET `protectionState`=? WHERE `UID`=?")) {

            preparedStatement.setString(1, protectionState.name());
            preparedStatement.setLong(2, buildingUUID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean UPDATE_DOOR_LOCK(final int buildingUUID, int doorFlags) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE obj_building SET doorState = ? WHERE UID = ?")) {

            preparedStatement.setInt(1, doorFlags);
            preparedStatement.setInt(2, buildingUUID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean ADD_TO_FRIENDS_LIST(final long buildingID, final long friendID, final long guildID, final int friendType) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_building_friends` (`buildingUID`, `playerUID`,`guildUID`, `friendType`) VALUES (?,?,?,?)")) {

            preparedStatement.setLong(1, buildingID);
            preparedStatement.setLong(2, friendID);
            preparedStatement.setLong(3, guildID);
            preparedStatement.setInt(4, friendType);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean REMOVE_FROM_FRIENDS_LIST(final long buildingID, long friendID, long guildID, int friendType) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_building_friends` WHERE `buildingUID`=? AND `playerUID`=? AND `guildUID` =? AND `friendType` = ?")) {

            preparedStatement.setLong(1, buildingID);
            preparedStatement.setLong(2, friendID);
            preparedStatement.setLong(3, guildID);
            preparedStatement.setInt(4, friendType);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean REMOVE_FROM_CONDEMNED_LIST(final long buildingID, long friendID, long guildID, int friendType) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_building_condemned` WHERE `buildingUID`=? AND `playerUID`=? AND `guildUID` =? AND `friendType` = ?")) {

            preparedStatement.setLong(1, buildingID);
            preparedStatement.setLong(2, friendID);
            preparedStatement.setLong(3, guildID);
            preparedStatement.setInt(4, friendType);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public void CLEAR_FRIENDS_LIST(final long buildingID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_building_friends` WHERE `buildingUID`=?")) {

            preparedStatement.setLong(1, buildingID);
            preparedStatement.execute();

        } catch (SQLException e) {
            Logger.error(e);
        }

    }

    public void CLEAR_CONDEMNED_LIST(final long buildingID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_building_condemned` WHERE `buildingUID`=?")) {

            preparedStatement.setLong(1, buildingID);
            preparedStatement.execute();

        } catch (SQLException e) {
            Logger.error(e);
        }

    }

    public boolean CLEAR_PATROL(final long buildingID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_building_patrol_points` WHERE `buildingUID`=?")) {

            preparedStatement.setLong(1, buildingID);
            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public void LOAD_ALL_FRIENDS_FOR_BUILDING(Building building) {

        if (building == null)
            return;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_building_friends` WHERE `buildingUID` = ?")) {

            preparedStatement.setInt(1, building.getObjectUUID());
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                BuildingFriends friend = new BuildingFriends(rs);
                switch (friend.getFriendType()) {
                    case 7:
                        building.getFriends().put(friend.getPlayerUID(), friend);
                        break;
                    case 8:
                    case 9:
                        building.getFriends().put(friend.getGuildUID(), friend);
                        break;
                }
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

    }

    public void LOAD_ALL_CONDEMNED_FOR_BUILDING(Building building) {

        if (building == null)
            return;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_building_condemned` WHERE `buildingUID` = ?")) {

            preparedStatement.setInt(1, building.getObjectUUID());
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                Condemned condemned = new Condemned(rs);
                switch (condemned.getFriendType()) {
                    case 2:
                        building.getCondemned().put(condemned.getPlayerUID(), condemned);
                        break;
                    case 4:
                    case 5:
                        building.getCondemned().put(condemned.getGuildUID(), condemned);
                        break;
                }
            }

        } catch (SQLException e) {
            Logger.error(e);
        }
    }

    public ArrayList<Vector3fImmutable> LOAD_PATROL_POINTS(Building building) {

        if (building == null)
            return null;

        ArrayList<Vector3fImmutable> patrolPoints = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_building_patrol_points` WHERE `buildingUID` = ?")) {

            preparedStatement.setInt(1, building.getObjectUUID());


            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                float x1 = rs.getFloat("patrolX");
                float y1 = rs.getFloat("patrolY");
                float z1 = rs.getFloat("patrolZ");
                Vector3fImmutable patrolPoint = new Vector3fImmutable(x1, y1, z1);
                patrolPoints.add(patrolPoint);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        return patrolPoints;

    }

    public boolean ADD_TO_CONDEMNLIST(final long parentUID, final long playerUID, final long guildID, final int friendType) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_building_condemned` (`buildingUID`, `playerUID`,`guildUID`, `friendType`) VALUES (?,?,?,?)")) {

            preparedStatement.setLong(1, parentUID);
            preparedStatement.setLong(2, playerUID);
            preparedStatement.setLong(3, guildID);
            preparedStatement.setInt(4, friendType);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean ADD_TO_PATROL(final long parentUID, final Vector3fImmutable patrolPoint) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_building_patrol_points` (`buildingUID`, `patrolX`,`patrolY`, `patrolZ`) VALUES (?,?,?,?)")) {

            preparedStatement.setLong(1, parentUID);
            preparedStatement.setFloat(2, (int) patrolPoint.x);
            preparedStatement.setFloat(3, (int) patrolPoint.y);
            preparedStatement.setFloat(4, (int) patrolPoint.z);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public HashMap<Integer, ArrayList<BuildingRegions>> LOAD_BUILDING_REGIONS() {

        HashMap<Integer, ArrayList<BuildingRegions>> regionList = new HashMap<>();
        BuildingRegions buildingRegions;
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_building_regions")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                recordsRead++;
                buildingRegions = new BuildingRegions(rs);

                if (regionList.get(buildingRegions.getBuildingID()) == null) {
                    ArrayList<BuildingRegions> regionsList = new ArrayList<>();
                    regionsList.add(buildingRegions);
                    regionList.put(buildingRegions.getBuildingID(), regionsList);
                } else {
                    ArrayList<BuildingRegions> regionsList = regionList.get(buildingRegions.getBuildingID());
                    regionsList.add(buildingRegions);
                    regionList.put(buildingRegions.getBuildingID(), regionsList);
                }
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + regionList.size());
        return regionList;
    }

    public HashMap<Integer, MeshBounds> LOAD_MESH_BOUNDS() {

        HashMap<Integer, MeshBounds> meshBoundsMap = new HashMap<>();
        MeshBounds meshBounds;
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_mesh_bounds")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                recordsRead++;
                meshBounds = new MeshBounds(rs);
                meshBoundsMap.put(meshBounds.meshID, meshBounds);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + meshBoundsMap.size());
        return meshBoundsMap;
    }

    public HashMap<Integer, ArrayList<StaticColliders>> LOAD_ALL_STATIC_COLLIDERS() {

        HashMap<Integer, ArrayList<StaticColliders>> colliders = new HashMap<>();
        StaticColliders thisColliders;
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_building_colliders")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                recordsRead++;
                thisColliders = new StaticColliders(rs);

                if (colliders.get(thisColliders.getMeshID()) == null) {
                    ArrayList<StaticColliders> colliderList = new ArrayList<>();
                    colliderList.add(thisColliders);
                    colliders.put(thisColliders.getMeshID(), colliderList);
                } else {
                    ArrayList<StaticColliders> colliderList = colliders.get(thisColliders.getMeshID());
                    colliderList.add(thisColliders);
                    colliders.put(thisColliders.getMeshID(), colliderList);
                }
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + colliders.size());
        return colliders;
    }

    // This public class inserted here as it's a generic utility function
    // with no other good place for it.  If you find a good home for it
    // feel free to move it.  -

    public final DbObjectType GET_UID_ENUM(long object_UID) {

        DbObjectType storedEnum = DbObjectType.INVALID;
        String objectType = "INVALID";

        if (object_UID == 0)
            return storedEnum;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `type` FROM `object` WHERE `object`.`UID` = ? LIMIT 1;")) {

            preparedStatement.setLong(1, object_UID);

            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {
                objectType = rs.getString("type").toUpperCase();
                storedEnum = DbObjectType.valueOf(objectType);
            }

        } catch (SQLException e) {
            Logger.error(e);
            return DbObjectType.INVALID;
        }

        return storedEnum;
    }

    public boolean updateBuildingRank(final Building b, int Rank) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_building` SET `rank`=?,"
                     + "`upgradeDate`=?, `meshUUID`=?, `currentHP`=? "
                     + "WHERE `UID` = ?")) {

            preparedStatement.setInt(1, Rank);
            preparedStatement.setNull(2, java.sql.Types.DATE);
            preparedStatement.setInt(3, b.getBlueprint().getMeshForRank(Rank));
            preparedStatement.setInt(4, b.getBlueprint().getMaxHealth(Rank));
            preparedStatement.setInt(5, b.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean updateReverseKOS(final Building b, boolean reverse) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_building` SET `reverseKOS`=? WHERE `UID` = ?")) {

            preparedStatement.setBoolean(1, reverse);
            preparedStatement.setInt(2, b.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean updateActiveCondemn(final Condemned condemn, boolean active) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_building_condemned` SET `active`=? "
                     + "WHERE`buildingUID` = ? AND `playerUID` = ? AND `guildUID` = ? AND `friendType` = ?")) {

            preparedStatement.setBoolean(1, active);
            preparedStatement.setInt(2, condemn.getParent());
            preparedStatement.setInt(3, condemn.getPlayerUID());
            preparedStatement.setInt(4, condemn.getGuildUID());
            preparedStatement.setInt(5, condemn.getFriendType());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean updateBuildingOwner(final Building building, int ownerUUID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_building` SET `ownerUUID`=? "
                     + " WHERE `UID` = ?")) {

            preparedStatement.setInt(1, ownerUUID);
            preparedStatement.setInt(2, building.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean updateBuildingUpgradeTime(LocalDateTime upgradeDateTime, Building toUpgrade, int costToUpgrade) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE obj_building SET upgradeDate=?, currentGold=? "
                     + "WHERE UID = ?")) {

            if (upgradeDateTime == null)
                preparedStatement.setNull(1, java.sql.Types.DATE);
            else
                preparedStatement.setTimestamp(1, new java.sql.Timestamp(upgradeDateTime.atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()));

            preparedStatement.setInt(2, toUpgrade.getStrongboxValue() - costToUpgrade);
            preparedStatement.setInt(3, toUpgrade.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean updateMaintDate(Building building) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE obj_building SET maintDate=? "
                     + "WHERE UID = ?")) {

            if (building.maintDateTime == null)
                preparedStatement.setNull(1, java.sql.Types.DATE);
            else
                preparedStatement.setTimestamp(1, new java.sql.Timestamp(building.maintDateTime.atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()));
            preparedStatement.setInt(2, building.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean addTaxes(Building building, TaxType taxType, int amount, boolean enforceKOS) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE obj_building SET taxType=?, taxAmount = ?, enforceKOS = ? "
                     + "WHERE UID = ?")) {

            preparedStatement.setString(1, taxType.name());
            preparedStatement.setInt(2, amount);
            preparedStatement.setBoolean(3, enforceKOS);
            preparedStatement.setInt(4, building.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean removeTaxes(Building building) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE obj_building SET taxType=?, taxAmount = ?, enforceKOS = ?, taxDate = ? "
                     + "WHERE UID = ?")) {

            preparedStatement.setString(1, TaxType.NONE.name());
            preparedStatement.setInt(2, 0);
            preparedStatement.setBoolean(3, false);
            preparedStatement.setNull(4, java.sql.Types.DATE);
            preparedStatement.setInt(5, building.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean acceptTaxes(Building building) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE obj_building SET taxDate=? "
                     + "WHERE UID = ?")) {

            preparedStatement.setTimestamp(1, new java.sql.Timestamp(DateTime.now().plusDays(7).getMillis()));
            preparedStatement.setInt(2, building.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

}
