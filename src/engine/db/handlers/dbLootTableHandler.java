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
import engine.loot.GenTableRow;
import engine.loot.ItemTableRow;
import engine.loot.ModTableRow;
import engine.loot.ModTypeTableRow;
import engine.objects.Item;
import engine.objects.LootTable;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class dbLootTableHandler extends dbHandlerBase {

    public dbLootTableHandler() {

    }

    public void populateLootGroups() {

        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `groupID`, `minRoll`, `maxRoll`, `lootTableID`, `pModTableID`, `sModTableID` FROM `static_gentables`")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                recordsRead++;
                LootTable lootTable = LootTable.getLootGroup(rs.getInt("groupID"));
                lootTable.addRow(rs.getFloat("minRoll"), rs.getFloat("maxRoll"), rs.getInt("lootTableID"), rs.getInt("pModTableID"), rs.getInt("sModTableID"), "");
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + LootTable.getLootGroups().size());
    }

    public void populateLootTables() {

        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `lootTable`, `minRoll`, `maxRoll`, `itemBaseUUID`, `minSpawn`, `maxSpawn` FROM `static_itemtables`")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                recordsRead++;
                LootTable lootTable = LootTable.getLootTable(rs.getInt("lootTable"));
                lootTable.addRow(rs.getFloat("minRoll"), rs.getFloat("maxRoll"), rs.getInt("itemBaseUUID"), rs.getInt("minSpawn"), rs.getInt("maxSpawn"), "");
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + LootTable.getLootTables().size());
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

    public void populateModGroups() {

        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `modGroup`,`minRoll`,`maxRoll`,`subTableID` FROM `static_modgroups`")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                recordsRead++;
                LootTable lootTable = LootTable.getModGroup(rs.getInt("modGroup"));
                lootTable.addRow(rs.getFloat("minRoll"), rs.getFloat("maxRoll"), rs.getInt("subTableID"), 0, 0, "");
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + LootTable.getModGroups().size());
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
                LootManager.AddGenTableRow(rs.getInt("groupID"), row);
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
                LootManager.AddItemTableRow(rs.getInt("lootTable"), row);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead);
    }

    public void LOAD_ALL_MODGROUPS() {
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_modgroups")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                recordsRead++;
                ModTypeTableRow mttr = new ModTypeTableRow(rs);
                LootManager.AddModTypeTableRow(rs.getInt("modGroup"), mttr);
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
