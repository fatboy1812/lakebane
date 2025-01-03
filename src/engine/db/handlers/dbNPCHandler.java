// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum.ProfitType;
import engine.gameManager.DbManager;
import engine.math.Vector3fImmutable;
import engine.objects.NPC;
import engine.objects.NPCProfits;
import engine.objects.ProducedItem;
import engine.objects.Zone;
import org.joda.time.DateTime;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class dbNPCHandler extends dbHandlerBase {

    public dbNPCHandler() {
        this.localClass = NPC.class;
        this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public NPC PERSIST(NPC toAdd) {

        NPC npc = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("CALL `npc_CREATE`(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {

            Vector3fImmutable bindLocation;

            if (toAdd.buildingUUID == 0)
                bindLocation = toAdd.bindLoc;
            else
                bindLocation = Vector3fImmutable.ZERO;

            preparedStatement.setLong(1, toAdd.parentZoneUUID);
            preparedStatement.setString(2, toAdd.getName());
            preparedStatement.setInt(3, toAdd.contractUUID);
            preparedStatement.setInt(4, toAdd.guildUUID);
            preparedStatement.setFloat(5, bindLocation.x);
            preparedStatement.setFloat(6, bindLocation.y);
            preparedStatement.setFloat(7, bindLocation.z);
            preparedStatement.setInt(8, toAdd.level);
            preparedStatement.setFloat(9, toAdd.buyPercent);
            preparedStatement.setFloat(10, toAdd.sellPercent);

            preparedStatement.setInt(11, Math.max(toAdd.buildingUUID, 0));

            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {
                int objectUUID = (int) rs.getLong("UID");

                if (objectUUID > 0)
                    npc = GET_NPC(objectUUID);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return npc;
    }

    public int DELETE_NPC(final NPC npc) {

        int row_count = 0;

        npc.removeFromZone();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `object` WHERE `UID` = ?")) {

            preparedStatement.setLong(1, npc.getDBID());
            row_count = preparedStatement.executeUpdate();

        } catch (SQLException e) {
            Logger.error(e);

        }
        return row_count;
    }

    public ArrayList<NPC> GET_ALL_NPCS_FOR_ZONE(Zone zone) {

        ArrayList<NPC> npcList = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_npc`.*, `object`.`parent` FROM `object` INNER JOIN `obj_npc` ON `obj_npc`.`UID` = `object`.`UID` WHERE `object`.`parent` = ?;")) {

            preparedStatement.setLong(1, zone.getObjectUUID());

            ResultSet rs = preparedStatement.executeQuery();
            npcList = getObjectsFromRs(rs, 1000);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return npcList;
    }

    public NPC GET_NPC(final int objectUUID) {

        NPC npc = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_npc`.*, `object`.`parent` FROM `object` INNER JOIN `obj_npc` ON `obj_npc`.`UID` = `object`.`UID` WHERE `object`.`UID` = ?;")) {

            preparedStatement.setLong(1, objectUUID);

            ResultSet rs = preparedStatement.executeQuery();
            npc = (NPC) getObjectFromRs(rs);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return npc;
    }

    public int BANE_COMMANDER_EXISTS(final int objectUUID) {

        int uid = 0;

        String query = "SELECT `UID` FROM `obj_npc` WHERE `npc_buildingID` = ? LIMIT 1;";

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, objectUUID);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    // Retrieve the UID column value
                    uid = rs.getInt("UID");
                }
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        return uid;
    }


    public int MOVE_NPC(long npcID, long parentID, float locX, float locY, float locZ) {

        int rowCount;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `object` INNER JOIN `obj_npc` On `object`.`UID` = `obj_npc`.`UID` SET `object`.`parent`=?, `obj_npc`.`npc_spawnX`=?, `obj_npc`.`npc_spawnY`=?, `obj_npc`.`npc_spawnZ`=? WHERE `obj_npc`.`UID`=?;")) {

            preparedStatement.setLong(1, parentID);
            preparedStatement.setFloat(2, locX);
            preparedStatement.setFloat(3, locY);
            preparedStatement.setFloat(4, locZ);
            preparedStatement.setLong(5, npcID);

            rowCount = preparedStatement.executeUpdate();

        } catch (SQLException e) {
            Logger.error(e);
            return 0;
        }
        return rowCount;
    }


    public String SET_PROPERTY(final NPC n, String name, Object new_value) {

        String result = "";

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("CALL npc_SETPROP(?,?,?)")) {

            preparedStatement.setLong(1, n.getDBID());
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, String.valueOf(new_value));

            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next())
                result = rs.getString("result");

        } catch (SQLException e) {
            Logger.error(e);
        }
        return result;
    }

    public void updateDatabase(final NPC npc) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE obj_npc SET npc_name=?, npc_contractID=?, npc_typeID=?, npc_guildID=?,"
                     + " npc_spawnX=?, npc_spawnY=?, npc_spawnZ=?, npc_level=? ,"
                     + " npc_buyPercent=?, npc_sellPercent=?, npc_buildingID=?, specialPrice=? WHERE UID = ?")) {

            preparedStatement.setString(1, npc.getName());
            preparedStatement.setInt(2, (npc.getContract() != null) ? npc.getContract().getObjectUUID() : 0);
            preparedStatement.setInt(3, 0);
            preparedStatement.setInt(4, (npc.getGuild() != null) ? npc.getGuild().getObjectUUID() : 0);
            preparedStatement.setFloat(5, npc.getBindLoc().x);
            preparedStatement.setFloat(6, npc.getBindLoc().y);
            preparedStatement.setFloat(7, npc.getBindLoc().z);
            preparedStatement.setShort(8, npc.getLevel());
            preparedStatement.setFloat(9, npc.getBuyPercent());
            preparedStatement.setFloat(10, npc.getSellPercent());
            preparedStatement.setInt(11, (npc.getBuilding() != null) ? npc.getBuilding().getObjectUUID() : 0);
            preparedStatement.setInt(12, npc.getDBID());
            preparedStatement.setInt(13, npc.getSpecialPrice());

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            Logger.error(e);
        }
    }

    public boolean updateUpgradeTime(NPC npc, DateTime upgradeDateTime) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE obj_npc SET upgradeDate=? "
                     + "WHERE UID = ?")) {

            if (upgradeDateTime == null)
                preparedStatement.setNull(1, java.sql.Types.DATE);
            else
                preparedStatement.setTimestamp(1, new java.sql.Timestamp(upgradeDateTime.getMillis()));

            preparedStatement.setInt(2, npc.getObjectUUID());

            preparedStatement.execute();
            return true;

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean UPDATE_MOBBASE(NPC npc, int mobBaseID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_npc` SET `npc_raceID`=? WHERE `UID`=?")) {

            preparedStatement.setLong(1, mobBaseID);
            preparedStatement.setLong(2, npc.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public boolean UPDATE_EQUIPSET(NPC npc, int equipSetID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_npc` SET `equipsetID`=? WHERE `UID`=?")) {

            preparedStatement.setInt(1, equipSetID);
            preparedStatement.setLong(2, npc.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public boolean UPDATE_NAME(NPC npc, String name) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_npc` SET `npc_name`=? WHERE `UID`=?")) {

            preparedStatement.setString(1, name);
            preparedStatement.setLong(2, npc.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public void LOAD_PIRATE_NAMES() {

        String pirateName;
        int mobBase;
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_piratenames")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                recordsRead++;
                mobBase = rs.getInt("mobbase");
                pirateName = rs.getString("first_name");

                // Handle new mobbbase entries

                if (NPC._pirateNames.get(mobBase) == null)
                    NPC._pirateNames.putIfAbsent(mobBase, new ArrayList<>());

                // Insert name into proper arraylist

                NPC._pirateNames.get(mobBase).add(pirateName);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("names read: " + recordsRead + " for "
                + NPC._pirateNames.size() + " mobBases");
    }

    public boolean ADD_TO_PRODUCTION_LIST(final long ID, final long npcUID, final long itemBaseID, DateTime dateTime, String prefix, String suffix, String name, boolean isRandom, int playerID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_npc_production` (`ID`,`npcUID`, `itemBaseID`,`dateToUpgrade`, `isRandom`, `prefix`, `suffix`, `name`,`playerID`) VALUES (?,?,?,?,?,?,?,?,?)")) {

            preparedStatement.setLong(1, ID);
            preparedStatement.setLong(2, npcUID);
            preparedStatement.setLong(3, itemBaseID);
            preparedStatement.setTimestamp(4, new java.sql.Timestamp(dateTime.getMillis()));
            preparedStatement.setBoolean(5, isRandom);
            preparedStatement.setString(6, prefix);
            preparedStatement.setString(7, suffix);
            preparedStatement.setString(8, name);
            preparedStatement.setInt(9, playerID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public boolean REMOVE_FROM_PRODUCTION_LIST(final long ID, final long npcUID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_npc_production` WHERE `ID`=? AND `npcUID`=?;")) {

            preparedStatement.setLong(1, ID);
            preparedStatement.setLong(2, npcUID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public boolean UPDATE_ITEM_TO_INVENTORY(final long ID, final long npcUID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_npc_production` SET `inForge`=? WHERE `ID`=? AND `npcUID`=?;")) {

            preparedStatement.setByte(1, (byte) 0);
            preparedStatement.setLong(2, ID);
            preparedStatement.setLong(3, npcUID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public boolean UPDATE_ITEM_PRICE(final long ID, final long npcUID, int value) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_npc_production` SET `value`=? WHERE `ID`=? AND `npcUID`=?;")) {

            preparedStatement.setInt(1, value);
            preparedStatement.setLong(2, ID);
            preparedStatement.setLong(3, npcUID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public boolean UPDATE_ITEM_ID(final long ID, final long npcUID, final long value) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_npc_production` SET `ID`=? WHERE `ID`=? AND `npcUID`=? LIMIT 1;")) {

            preparedStatement.setLong(1, value);
            preparedStatement.setLong(2, ID);
            preparedStatement.setLong(3, npcUID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public void LOAD_ALL_ITEMS_TO_PRODUCE(NPC npc) {

        if (npc == null)
            return;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_npc_production` WHERE `npcUID` = ?")) {

            preparedStatement.setInt(1, npc.getObjectUUID());
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                ProducedItem producedItem = new ProducedItem(rs);
                npc.forgedItems.add(producedItem);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }
    }

    public boolean UPDATE_PROFITS(NPC npc, ProfitType profitType, float value) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_npc_profits` SET `" + profitType.dbField + "` = ? WHERE `npcUID`=?")) {

            preparedStatement.setFloat(1, value);
            preparedStatement.setInt(2, npc.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public void LOAD_NPC_PROFITS() {

        NPCProfits npcProfit;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM dyn_npc_profits")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                npcProfit = new NPCProfits(rs);
                NPCProfits.ProfitCache.put(npcProfit.npcUID, npcProfit);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }
    }

    public boolean CREATE_PROFITS(NPC npc) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_npc_profits` (`npcUID`) VALUES (?)")) {

            preparedStatement.setLong(1, npc.getObjectUUID());
            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }
}
