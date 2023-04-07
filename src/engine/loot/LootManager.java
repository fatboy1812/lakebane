// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com

package engine.loot;

import engine.Enum;
import engine.gameManager.ConfigManager;
import engine.gameManager.DbManager;
import engine.gameManager.ZoneManager;
import engine.net.DispatchMessage;
import engine.net.client.msg.chat.ChatSystemMsg;
import engine.objects.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Class contains static methods for data from Magicbane's loot tables
 */
public class LootManager {

    //new tables
    private static final HashMap<Integer,GenTable> generalItemTables = new HashMap<>();
    private static final HashMap<Integer,ItemTable> itemTables = new HashMap<>();
    private static final HashMap<Integer,ModTypeTable> modTypeTables = new HashMap<>();
    private static final HashMap<Integer,ModTable> modTables = new HashMap<>();

    private LootManager() {}
    // Bootstrap routine to load loot data from database
    public static void loadLootData() {
        DbManager.LootQueries.LOAD_ALL_LOOTGROUPS();
        DbManager.LootQueries.LOAD_ALL_LOOTTABLES();
        DbManager.LootQueries.LOAD_ALL_MODGROUPS();
        DbManager.LootQueries.LOAD_ALL_MODTABLES();
    }
    public static void GenerateMobLoot(Mob mob){
        //determine if mob is in hotzone
        boolean inHotzone = ZoneManager.inHotZone(mob.getLoc());
        //get multiplier form config manager
        float multiplier = Float.parseFloat(ConfigManager.MB_NORMAL_DROP_RATE.getValue());
        if (inHotzone) {
            //if mob is inside hotzone, use the hotzone gold multiplier form the config instead
            multiplier = Float.parseFloat(ConfigManager.MB_HOTZONE_DROP_RATE.getValue());
        }
        //iterate the booty sets
        for(BootySetEntry bse : mob.getMobBase().bootySets) {
            //check if chance roll is good
            if (ThreadLocalRandom.current().nextInt(100) <= (bse.dropChance * multiplier)) {
                //early exit, failed to hit minimum chance roll
                continue;
            }
            if (bse.bootyType.equals("GOLD")) {
                //determine and add gold to mob inventory
                int gold = new Random().nextInt(bse.highGold - bse.lowGold) + bse.lowGold;
                if (gold > 0) {
                    MobLoot goldAmount = new MobLoot(mob, (int) (gold * multiplier));
                    mob.getCharItemManager().addItemToInventory(goldAmount);
                }
            } else if (bse.bootyType.equals("BOOTYTABLE")) {
                //iterate the booty tables and add items to mob inventory
                Item toAdd = getGenTableItem(bse.lootTable, mob);
                if(toAdd != null) {
                    mob.getCharItemManager().addItemToInventory(toAdd);
                }
                if (inHotzone) {
                    Item toAddHZ = getGenTableItem(bse.lootTable + 1, mob);
                    mob.getCharItemManager().addItemToInventory(toAddHZ);
                }
            }
        }
        //lastly, check mobs inventory for godly or disc runes to send a server announcement
        for (Item it : mob.getInventory()) {
            ItemBase ib = it.getItemBase();
            if (ib.isDiscRune() || ib.getName().toLowerCase().contains("of the gods")) {
                ChatSystemMsg chatMsg = new ChatSystemMsg(null, mob.getName() + " in " + mob.getParentZone().getName() + " has found the " + ib.getName() + ". Are you tough enough to take it?");
                chatMsg.setMessageType(10);
                chatMsg.setChannel(Enum.ChatChannelType.SYSTEM.getChannelID());
                DispatchMessage.dispatchMsgToAll(chatMsg);
            }
        }
    }
    public static Item getGenTableItem(int genTableID, Mob mob){
        Item outItem;
        int minRollRange = mob.getParentZone().minLvl + mob.getLevel();
        int maxRollRange = (mob.getParentZone().minLvl + mob.getLevel() + mob.getParentZone().maxLvl) * 2;
        GenTableRow selectedRow = generalItemTables.get(genTableID).getRowForRange(new Random().nextInt(100));
        int itemUUID = itemTables.get(selectedRow.itemTableID).getRowForRange(new Random().nextInt(maxRollRange) + minRollRange).cacheID;
        if(itemUUID == 0){
            return null;
        }
        ModTypeTable prefixTable = modTypeTables.get(selectedRow.pModTable);
        ModTypeTable suffixTable = modTypeTables.get(selectedRow.sModTable);
        ModTable prefixModTable = modTables.get(prefixTable.getRowForRange(100).modTableID);
        ModTable suffixModTable = modTables.get(suffixTable.getRowForRange(100).modTableID);
        ModTableRow prefixMod = prefixModTable.getRowForRange(new Random().nextInt(maxRollRange) + minRollRange);
        ModTableRow suffixMod = suffixModTable.getRowForRange(new Random().nextInt(maxRollRange) + minRollRange);
        outItem = Item.getItem(itemUUID);
        if(prefixMod.action.length() > 0){
            outItem.addPermanentEnchantment(prefixMod.action, prefixMod.level);
        }
        if(suffixMod.action.length() > 0){
            outItem.addPermanentEnchantment(suffixMod.action, suffixMod.level);
        }
        return outItem;
    }
    public static void AddGenTableRow(int tableID, GenTable genTable, GenTableRow row){
        if(!generalItemTables.containsKey(tableID)){
            //create the new table
            generalItemTables.put(tableID,genTable);
            //add row to new table
            generalItemTables.get(tableID).rows.add(row);
        } else{
            //add row to existing table
            GenTable toAdd = generalItemTables.get(tableID);
            toAdd.rows.add(row);
        }
    }
    public static void AddItemTableRow(int tableID,ItemTable itemTable, ItemTableRow row){
        if(!itemTables.containsKey(tableID)){
            //create the new table
            itemTables.put(tableID,itemTable);
            //add new row to table
            itemTables.get(tableID).rows.add(row);
        } else{
            //add row to existing table
            ItemTable toAdd = itemTables.get(tableID);
            toAdd.rows.add(row);
        }
    }
    public static void AddModTypeTableRow(int tableID,ModTypeTable modtypeTable, ModTypeTableRow row){
        if(!modTypeTables.containsKey(tableID)){
            //create the new table
            modTypeTables.put(tableID,modtypeTable);
            //add row to new table
            modTypeTables.get(tableID).rows.add(row);
        } else{
            //add row to existing table
            ModTypeTable toAdd = modTypeTables.get(tableID);
            toAdd.rows.add(row);
        }
    }
    public static void AddModTableRow(int tableID, ModTable modTable, ModTableRow row){
        if(!itemTables.containsKey(tableID)){
            //create the new table
            modTables.put(tableID,modTable);
            //add row to new table
            modTables.get(tableID).rows.add(row);
        } else{
            //add row to existing table
            ModTable toAdd = modTables.get(tableID);
            toAdd.rows.add(row);
        }
    }
    public static class GenTable{
        public ArrayList<GenTableRow> rows = new ArrayList<GenTableRow>();
        public GenTableRow getRowForRange(int roll){
            GenTableRow outRow = null;
            for(GenTableRow iteration : this.rows){
                if(iteration.minRoll >= roll && iteration.maxRoll <= roll){
                    outRow = iteration;
                }
            }
            return outRow;
        }
    }
    public static class ItemTable{
        public ArrayList<ItemTableRow> rows = new ArrayList<ItemTableRow>();
        public ItemTableRow getRowForRange(int roll){
            if(roll > 320){
                roll = 320;
            }
            ItemTableRow outRow = null;
            for(ItemTableRow iteration : this.rows){
                if(iteration.minRoll >= roll && iteration.maxRoll <= roll){
                    outRow = iteration;
                }
            }
            return outRow;
        }
    }
    public static class ModTypeTable{
        public ArrayList<ModTypeTableRow> rows = new ArrayList<ModTypeTableRow>();
        public ModTypeTableRow getRowForRange(int roll){
            ModTypeTableRow outRow = null;
            for(ModTypeTableRow iteration : this.rows){
                if(iteration.minRoll >= roll && iteration.maxRoll <= roll){
                    outRow = iteration;
                }
            }
            return outRow;
        }
    }
    public static class ModTable{
        public ArrayList<ModTableRow> rows = new ArrayList<ModTableRow>();
        public ModTableRow getRowForRange(int roll){
            if(roll > 320){
                roll = 320;
            }
            ModTableRow outRow = null;
            for(ModTableRow iteration : this.rows){
                if(iteration.minRoll >= roll && iteration.maxRoll <= roll){
                    outRow = iteration;
                }
            }
            return outRow;
        }
    }
    public static class GenTableRow{
        public int minRoll;
        public int maxRoll;
        public int itemTableID;
        public int pModTable;
        public int sModTable;
        public GenTableRow(ResultSet rs) throws SQLException {
            this.minRoll = rs.getInt("minRoll");
            this.maxRoll = rs.getInt("maxRoll");
            this.itemTableID = rs.getInt("lootTableID");
            this.pModTable = rs.getInt("pModTableID");
            this.sModTable = rs.getInt("sModTableID");
        }
    }
    public static class ItemTableRow{
        public int minRoll;
        public int maxRoll;
        public int cacheID;
        public ItemTableRow(ResultSet rs) throws SQLException {
            this.minRoll = rs.getInt("minRoll");
            this.maxRoll = rs.getInt("maxRoll");
            this.cacheID = rs.getInt("itemBaseUUID");

        }
    }
    public static class ModTypeTableRow{
        public int minRoll;
        public int maxRoll;
        public int modTableID;
        public ModTypeTableRow(ResultSet rs) throws SQLException {
            this.minRoll = rs.getInt("minRoll");
            this.maxRoll = rs.getInt("maxRoll");
            this.modTableID = rs.getInt("subTableID");

        }
    }
    public static class ModTableRow{
        public int minRoll;
        public int maxRoll;
        public String action;
        public int level;
        public ModTableRow(ResultSet rs) throws SQLException {
            this.minRoll = rs.getInt("minRoll");
            this.maxRoll = rs.getInt("maxRoll");
            this.action = rs.getString("action");
            this.level = rs.getInt("level");

        }
    }
}
