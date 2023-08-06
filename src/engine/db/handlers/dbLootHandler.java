// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.gameManager.DbManager;
import engine.gameManager.LootManager;
import engine.loot.*;
import engine.objects.Item;
import engine.objects.LootTable;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class dbLootHandler extends dbHandlerBase {

    public dbLootHandler() {

    }

    public HashMap<Integer, ArrayList<GenTableEntry>> LOAD_GEN_ITEM_TABLES() {

        HashMap<Integer, ArrayList<GenTableEntry>> genTables = new HashMap<>();
        GenTableEntry genTableEntry;

        int genTableID;
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `genTable`, `minRoll`, `maxRoll`, `itemTableID`, `pModTableID`, `sModTableID` FROM `static_gentables`")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                recordsRead++;

                genTableID = rs.getInt("bootySet");
                genTableEntry = new GenTableEntry(rs);

                if (genTables.get(genTableID) == null) {
                    ArrayList<GenTableEntry> genItemList = new ArrayList<>();
                    genItemList.add(genTableEntry);
                    genTables.put(genTableID, genItemList);
                } else {
                    ArrayList<GenTableEntry> genItemList = genTables.get(genTableID);
                    genItemList.add(genTableEntry);
                    genTables.put(genTableID, genItemList);
                }
            }
        } catch (SQLException e) {
            Logger.error(e);
            return genTables;
        }

        Logger.info("read: " + recordsRead + " cached: " + genTables.size());
        return genTables;
    }

    public HashMap<Integer, ArrayList<BootySetEntry>> LOAD_BOOTY_TABLES() {

        HashMap<Integer, ArrayList<BootySetEntry>> bootySets = new HashMap<>();
        BootySetEntry bootySetEntry;
        int bootySetID;
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_npc_bootySet")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                recordsRead++;

                bootySetID = rs.getInt("bootySet");
                bootySetEntry = new BootySetEntry(rs);

                if (bootySets.get(bootySetID) == null) {
                    ArrayList<BootySetEntry> bootyList = new ArrayList<>();
                    bootyList.add(bootySetEntry);
                    bootySets.put(bootySetID, bootyList);
                } else {
                    ArrayList<BootySetEntry> bootyList = bootySets.get(bootySetID);
                    bootyList.add(bootySetEntry);
                    bootySets.put(bootySetID, bootyList);
                }
            }
        } catch (SQLException e) {
            Logger.error(e);
            return bootySets;
        }

        Logger.info("read: " + recordsRead + " cached: " + bootySets.size());
        return bootySets;
    }

    public void populateGenTables() {

        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `genTable`, `minRoll`, `maxRoll`, `itemTableID`, `pModTableID`, `sModTableID` FROM `static_gentables`")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                recordsRead++;
                LootTable lootTable = LootTable.getGenTable(rs.getInt("genTable"));
                lootTable.addRow(rs.getFloat("minRoll"), rs.getFloat("maxRoll"), rs.getInt("itemTableID"), rs.getInt("pModTableID"), rs.getInt("sModTableID"), "");
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + LootTable.getGenTables().size());
    }

    public void populateItemTables() {

        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `itemTable`, `minRoll`, `maxRoll`, `itemBaseUUID`, `minSpawn`, `maxSpawn` FROM `static_itemtables`")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                recordsRead++;
                LootTable lootTable = LootTable.getItemTable(rs.getInt("itemTable"));
                lootTable.addRow(rs.getFloat("minRoll"), rs.getFloat("maxRoll"), rs.getInt("itemBaseUUID"), rs.getInt("minSpawn"), rs.getInt("maxSpawn"), "");
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + LootTable.getItemTables().size());
    }

    public void populateModTables() {

        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `modTable`,`minRoll`,`maxRoll`,`value`,`action` FROM `static_modtables`")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                recordsRead++;
                LootTable lootTable = LootTable.getModTable(rs.getInt("modTable"));
                lootTable.addRow(rs.getFloat("minRoll"), rs.getFloat("maxRoll"), rs.getInt("value"), 0, 0, rs.getString("action"));
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + LootTable.getModTables().size());
    }

    public void populateModTypeTables() {

        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `modType`,`minRoll`,`maxRoll`,`subTableID` FROM `static_modtypetables`")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                recordsRead++;
                LootTable lootTable = LootTable.getModTypeTable(rs.getInt("modType"));
                lootTable.addRow(rs.getFloat("minRoll"), rs.getFloat("maxRoll"), rs.getInt("subTableID"), 0, 0, "");
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + LootTable.getModTypeTables().size());
    }

    public void LOAD_ENCHANT_VALUES() {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `IDString`, `minMod` FROM `static_power_effectmod` WHERE `modType` = ?")) {

            preparedStatement.setString(1, "Value");
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next())
                Item.addEnchantValue(rs.getString("IDString"), rs.getInt("minMod"));

        } catch (SQLException e) {
            Logger.error(e);
        }
    }

    public void LOAD_ALL_GENTABLES() {

        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_gentables")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                GenTableRow row = new GenTableRow(rs);
                LootManager.AddGenTableRow(rs.getInt("gentable"), row);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead);
    }

    public void LOAD_ALL_ITEMTABLES() {

        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_itemtables")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                recordsRead++;
                ItemTableRow row = new ItemTableRow(rs);
                LootManager.AddItemTableRow(rs.getInt("itemTable"), row);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead);
    }

    public void LOAD_ALL_MODTYPES() {
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_modtypetables")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                recordsRead++;
                ModTypeTableRow mttr = new ModTypeTableRow(rs);
                LootManager.AddModTypeTableRow(rs.getInt("modType"), mttr);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }
        Logger.info("read: " + recordsRead);
    }

    public void LOAD_ALL_MODTABLES() {
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_modtables")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                recordsRead++;
                ModTableRow mtr = new ModTableRow(rs);
                LootManager.AddModTableRow(rs.getInt("modTable"), mtr);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }
        Logger.info("read: " + recordsRead);
    }
}
