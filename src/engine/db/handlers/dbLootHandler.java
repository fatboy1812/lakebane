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
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_gentables`")) {

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

    public HashMap<Integer, ArrayList<ItemTableEntry>> LOAD_ITEM_TABLES() {

        HashMap<Integer, ArrayList<ItemTableEntry>> itemTables = new HashMap<>();
        ItemTableEntry itemTableEntry;

        int itemTableID;
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_itemTables`")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                recordsRead++;

                itemTableID = rs.getInt("itemTable");
                itemTableEntry = new ItemTableEntry(rs);

                if (itemTables.get(itemTableID) == null) {
                    ArrayList<ItemTableEntry> itemTableList = new ArrayList<>();
                    itemTableList.add(itemTableEntry);
                    itemTables.put(itemTableID, itemTableList);
                } else {
                    ArrayList<ItemTableEntry> itemTableList = itemTables.get(itemTableID);
                    itemTableList.add(itemTableEntry);
                    itemTables.put(itemTableID, itemTableList);
                }
            }
        } catch (SQLException e) {
            Logger.error(e);
            return itemTables;
        }

        Logger.info("read: " + recordsRead + " cached: " + itemTables.size());
        return itemTables;
    }

    public HashMap<Integer, ArrayList<ModTableEntry>> LOAD_MOD_TABLES() {

        HashMap<Integer, ArrayList<ModTableEntry>> modTables = new HashMap<>();
        ModTableEntry modTableEntry;

        int modTableID;
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_modTables`")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                recordsRead++;

                modTableID = rs.getInt("itemTable");
                modTableEntry = new ModTableEntry(rs);

                if (modTables.get(modTableID) == null) {
                    ArrayList<ModTableEntry> modTableList = new ArrayList<>();
                    modTableList.add(modTableEntry);
                    modTables.put(modTableID, modTableList);
                } else {
                    ArrayList<ModTableEntry> modTableList = modTables.get(modTableID);
                    modTableList.add(modTableEntry);
                    modTables.put(modTableID, modTableList);
                }
            }
        } catch (SQLException e) {
            Logger.error(e);
            return modTables;
        }

        Logger.info("read: " + recordsRead + " cached: " + modTables.size());
        return modTables;
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
