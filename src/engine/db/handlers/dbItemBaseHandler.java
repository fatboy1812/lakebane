// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.gameManager.DbManager;
import engine.objects.ItemBase;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class dbItemBaseHandler extends dbHandlerBase {

    public dbItemBaseHandler() {

    }

    public void LOAD_BAKEDINSTATS(ItemBase itemBase) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_item_bakedinstat` WHERE `itemID` = ?")) {

            preparedStatement.setInt(1, itemBase.getUUID());
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                if (rs.getBoolean("fromUse"))
                    itemBase.getUsedStats().put(rs.getInt("token"), rs.getInt("numTrains"));
                else
                    itemBase.getBakedInStats().put(rs.getInt("token"), rs.getInt("numTrains"));
            }
        } catch (SQLException e) {
            Logger.error(e);
        }
    }

    public void LOAD_ANIMATIONS(ItemBase itemBase) {

        ArrayList<Integer> tempList = new ArrayList<>();
        ArrayList<Integer> tempListOff = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_itembase_animations` WHERE `itemBaseUUID` = ?")) {

            preparedStatement.setInt(1, itemBase.getUUID());
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                int animation = rs.getInt("animation");
                boolean rightHand = rs.getBoolean("rightHand");

                if (rightHand)
                    tempList.add(animation);
                else
                    tempListOff.add(animation);
            }
        } catch (SQLException e) {
            Logger.error(e);
        }

        itemBase.setAnimations(tempList);
        itemBase.setOffHandAnimations(tempListOff);
    }

    public void LOAD_ALL_ITEMBASES() {

        ItemBase itemBase;
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_itembase")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                recordsRead++;
                itemBase = new ItemBase(rs);
                ItemBase.addToCache(itemBase);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + ItemBase.getUUIDCache().size());
    }

    public HashMap<Integer, ArrayList<Integer>> LOAD_RUNES_FOR_NPC_AND_MOBS() {

        HashMap<Integer, ArrayList<Integer>> runeSets = new HashMap<>();
        int runeSetID;
        int runeBaseID;
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_npc_runeSet")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                recordsRead++;

                runeSetID = rs.getInt("runeSet");
                runeBaseID = rs.getInt("runeBase");

                if (runeSets.get(runeSetID) == null) {
                    ArrayList<Integer> runeList = new ArrayList<>();
                    runeList.add(runeBaseID);
                    runeSets.put(runeSetID, runeList);
                } else {
                    ArrayList<Integer> runeList = runeSets.get(runeSetID);
                    runeList.add(runeSetID);
                    runeSets.put(runeSetID, runeList);
                }
            }

        } catch (SQLException e) {
            Logger.error(e);
            return runeSets;
        }

        Logger.info("read: " + recordsRead + " cached: " + runeSets.size());
        return runeSets;
    }

}
