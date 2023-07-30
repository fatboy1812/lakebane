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
import engine.gameManager.NPCManager;
import engine.gameManager.ZoneManager;
import engine.net.DispatchMessage;
import engine.net.client.msg.chat.ChatSystemMsg;
import engine.objects.*;
import org.pmw.tinylog.Logger;

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
    private static final HashMap<Integer, GenTable> generalItemTables = new HashMap<>();
    private static final HashMap<Integer, ItemTable> itemTables = new HashMap<>();
    private static final HashMap<Integer, ModTypeTable> modTypeTables = new HashMap<>();
    private static final HashMap<Integer, ModTable> modTables = new HashMap<>();

    private LootManager() {
    }

    // Bootstrap routine to load loot data from database
    public static void loadLootData() {
        DbManager.LootQueries.LOAD_ALL_LOOTGROUPS();
        DbManager.LootQueries.LOAD_ALL_LOOTTABLES();
        DbManager.LootQueries.LOAD_ALL_MODGROUPS();
        DbManager.LootQueries.LOAD_ALL_MODTABLES();
    }

    public static void GenerateMobLoot(Mob mob, boolean fromDeath) {
        //determine if mob is in hotzone
        boolean inHotzone = ZoneManager.inHotZone(mob.getLoc());
        //get multiplier form config manager
        float multiplier = Float.parseFloat(ConfigManager.MB_NORMAL_DROP_RATE.getValue());
        if (inHotzone) {
            //if mob is inside hotzone, use the hotzone multiplier from the config instead
            multiplier = Float.parseFloat(ConfigManager.MB_HOTZONE_DROP_RATE.getValue());
        }
        //iterate the booty sets
        if (mob.getMobBase().bootySet != 0 && NPCManager._bootySetMap.containsKey(mob.getMobBase().bootySet) == true) {
                RunBootySet(NPCManager._bootySetMap.get(mob.getMobBase().bootySet), mob, multiplier, inHotzone, fromDeath);
        }
        if (mob.bootySet != 0 && NPCManager._bootySetMap.containsKey(mob.bootySet) == true) {
            RunBootySet(NPCManager._bootySetMap.get(mob.bootySet), mob, multiplier, inHotzone, fromDeath);
        }
        //lastly, check mobs inventory for godly or disc runes to send a server announcement
        if (!fromDeath) {
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
    }
    private static void RunBootySet(ArrayList<BootySetEntry> entries, Mob mob, float multiplier, boolean inHotzone, boolean fromDeath) {
            if (fromDeath) {
                DropEquipment(mob,multiplier);
                if (inHotzone) {
                    //all mobs in HZ get to roll for glass
                    RollForGlass(mob);
                }
                return;
            }
            for (BootySetEntry bse : entries) {
                switch (bse.bootyType) {
                    case "GOLD":
                        GenerateGoldDrop(mob,bse,multiplier);
                        break;
                    case "LOOT":
                        GenerateNormalLootDrop(mob,bse,multiplier);
                        if (inHotzone && mob.level < 80) {
                            if (generalItemTables.containsKey(bse.lootTable + 1)) {
                                GenerateHotzoneLootDrop(mob, bse, multiplier);
                            }
                            RollForGlass(mob);
                        }
                        if(mob.level > 80){
                            RollForGlass(mob);
                        }
                        break;
                    case "ITEM":
                        GenerateItemLootDrop(mob,bse);
                        break;
                }
            }
    }
    public static MobLoot getGenTableItem(int genTableID, Mob mob) {
            if (genTableID == 0 || mob == null || generalItemTables.containsKey(genTableID) == false) {
                return null;
            }
            MobLoot outItem;
            int genRoll = new Random().nextInt(101);
            GenTableRow selectedRow = generalItemTables.get(genTableID).getRowForRange(genRoll);
            if (selectedRow == null) {
                return null;
            }
            int itemTableId = selectedRow.itemTableID;
            //gets the 1-320 roll for this mob
            int roll2 = TableRoll(mob.level);
            ItemTableRow tableRow = itemTables.get(itemTableId).getRowForRange(roll2);
            if (tableRow == null) {
                return null;
            }
            int itemUUID = tableRow.cacheID;
            if (itemUUID == 0) {
                return null;
            }
            if (ItemBase.getItemBase(itemUUID).getType().ordinal() == Enum.ItemType.RESOURCE.ordinal()) {
                int amount = ThreadLocalRandom.current().nextInt(tableRow.maxSpawn - tableRow.minSpawn) + tableRow.minSpawn;
                return new MobLoot(mob, ItemBase.getItemBase(itemUUID), amount, false);
            }
            outItem = new MobLoot(mob, ItemBase.getItemBase(itemUUID), false);
            Enum.ItemType outType = outItem.getItemBase().getType();
            if (outType.ordinal() == Enum.ItemType.WEAPON.ordinal() || outType.ordinal() == Enum.ItemType.ARMOR.ordinal() || outType.ordinal() == Enum.ItemType.JEWELRY.ordinal()) {
                if (outItem.getItemBase().isGlass() == false) {
                    int prefixChanceRoll = ThreadLocalRandom.current().nextInt(101);
                    double prefixChance = 2.057 * mob.level - 28.67;
                    if (prefixChanceRoll < prefixChance) {
                        ModTypeTable prefixTable = modTypeTables.get(selectedRow.pModTable);

                        int prefixroll = ThreadLocalRandom.current().nextInt(101);
                        if (modTables.get(prefixTable.getRowForRange(prefixroll).modTableID) != null) {
                            ModTable prefixModTable = modTables.get(prefixTable.getRowForRange(prefixroll).modTableID);
                            ModTableRow prefixMod = prefixModTable.getRowForRange(TableRoll(mob.level));
                            if (prefixMod != null && prefixMod.action.length() > 0) {
                                outItem.setPrefix(prefixMod.action);
                                outItem.addPermanentEnchantment(prefixMod.action, 0, prefixMod.level, true);
                            }
                        }
                    }
                    int suffixChanceRoll = ThreadLocalRandom.current().nextInt(101);
                    double suffixChance = 2.057 * mob.level - 28.67;
                    if (suffixChanceRoll < suffixChance) {
                        int suffixroll = ThreadLocalRandom.current().nextInt(101);
                        ModTypeTable suffixTable = modTypeTables.get(selectedRow.sModTable);
                        if (modTables.get(suffixTable.getRowForRange(suffixroll).modTableID) != null) {
                            ModTable suffixModTable = modTables.get(suffixTable.getRowForRange(suffixroll).modTableID);
                            ModTableRow suffixMod = suffixModTable.getRowForRange(TableRoll(mob.level));
                            if (suffixMod != null && suffixMod.action.length() > 0) {
                                outItem.setSuffix(suffixMod.action);
                                outItem.addPermanentEnchantment(suffixMod.action, 0, suffixMod.level, false);
                            }
                        }
                    }
                }
            }
            return outItem;
    }
    private static int TableRoll(int mobLevel){
        if(mobLevel > 65){
            mobLevel = 65;
        }
        int max = (int)(5.882 * mobLevel + 127.0);
        if(max > 320){
            max = 320;
        }
        int min = (int)(4.469 * mobLevel - 3.469);
        int roll = ThreadLocalRandom.current().nextInt(max-min) + min;
        return roll;
    }
    public static void GenerateGoldDrop(Mob mob, BootySetEntry bse, float multiplier){
        int chanceRoll = ThreadLocalRandom.current().nextInt(100) + 1;
        if (chanceRoll > bse.dropChance) {
            //early exit, failed to hit minimum chance roll OR booty was generated from mob's death
            return;
        }
        //determine and add gold to mob inventory
        int high = (int)(bse.highGold * multiplier);
        int low = (int)(bse.lowGold * multiplier);
        int gold = ThreadLocalRandom.current().nextInt(high - low) + low;
        if (gold > 0) {
            MobLoot goldAmount = new MobLoot(mob, (int) (gold * multiplier));
            mob.getCharItemManager().addItemToInventory(goldAmount);
        }
    }
    public static void GenerateNormalLootDrop(Mob mob, BootySetEntry bse,float multiplier){
        try{
        int chanceRoll = ThreadLocalRandom.current().nextInt(100) + 1;
        if (chanceRoll > bse.dropChance * multiplier) {
            //early exit, failed to hit minimum chance roll
            return;
        }
        //iterate the booty tables and add items to mob inventory
        MobLoot toAdd = getGenTableItem(bse.lootTable, mob);
        if (toAdd != null) {
            if(toAdd.getPrefix() == null && toAdd.getSuffix() == null){
                toAdd.setIsID(true);
            }
            mob.getCharItemManager().addItemToInventory(toAdd);
        }
        }
        catch(Exception e){
            //TODO chase down loot generation error, affects roughly 2% of drops
            int i = 0;
        }
    }
    public static void GenerateHotzoneLootDrop(Mob mob, BootySetEntry bse, float multiplier){
            int lootTableID = bse.lootTable + 1;
            int chanceRoll = ThreadLocalRandom.current().nextInt(100) + 1;
            if (chanceRoll > bse.dropChance * multiplier) {
                //early exit, failed to hit minimum chance roll
                return;
            }
            MobLoot toAdd = getGenTableItem(lootTableID, mob);
            if (toAdd != null) {
                if (toAdd.getPrefix() != null && toAdd.getPrefix().isEmpty() == true && toAdd.getSuffix() != null && toAdd.getSuffix().isEmpty() == true) {
                    toAdd.setIsID(true);
                }
                mob.getCharItemManager().addItemToInventory(toAdd);
            }
        }
    public static void RollForGlass(Mob mob){
        int glassRoll = ThreadLocalRandom.current().nextInt(100) + 1;
        if (glassRoll >= 99 - mob.getRank()){
            int roll2 = TableRoll(mob.level);
            if (itemTables.get(126).getRowForRange(roll2) == null) {
                return;
            }
            ItemTableRow tableRow = itemTables.get(126).getRowForRange(roll2);
            if (tableRow == null) {
                return;
            }
            int itemUUID = tableRow.cacheID;
            if (itemUUID == 0) {
                return;
            }
            MobLoot toAddHZ = new MobLoot(mob, ItemBase.getItemBase(itemUUID), false);
            if (toAddHZ != null)
                mob.getCharItemManager().addItemToInventory(toAddHZ);
        }
    }
    public static void DropEquipment(Mob mob, float multiplier){
        //do equipment here
        if (mob.getEquip() != null) {
            for (MobEquipment me : mob.getEquip().values()) {
                if (me.getDropChance() == 0)
                    continue;
                float equipmentRoll = ThreadLocalRandom.current().nextInt(101);
                float dropChance = me.getDropChance() * 100;
                if (equipmentRoll <= (dropChance * multiplier)) {
                    MobLoot ml = new MobLoot(mob, me.getItemBase(), false);
                    if (ml.getPrefix().isEmpty() == true && ml.getSuffix().isEmpty() == true) {
                        ml.setIsID(true);
                    }
                    mob.getCharItemManager().addItemToInventory(ml);
                }
            }
        }
        return;
    }
    public static void GenerateItemLootDrop(Mob mob, BootySetEntry bse){
        MobLoot disc = new MobLoot(mob, ItemBase.getItemBase(bse.itemBase), true);
        if (disc != null)
            mob.getCharItemManager().addItemToInventory(disc);
    }
    public static void AddGenTableRow(int tableID, GenTableRow row) {
        if (!generalItemTables.containsKey(tableID)) {
            //create the new table
            GenTable gt = new GenTable();
            gt.rows.add(row);
            generalItemTables.put(tableID, gt);
        } else {
            //add row to existing table
            GenTable toAdd = generalItemTables.get(tableID);
            toAdd.rows.add(row);
        }
    }
    public static void AddItemTableRow(int tableID, ItemTableRow row) {
        if (!itemTables.containsKey(tableID)) {
            //create the new table
            ItemTable it = new ItemTable();
            it.rows.add(row);
            itemTables.put(tableID, it);
        } else {
            //add row to existing table
            ItemTable toAdd = itemTables.get(tableID);
            toAdd.rows.add(row);
        }
    }
    public static void AddModTypeTableRow(int tableID, ModTypeTableRow row) {
        if (!modTypeTables.containsKey(tableID)) {
            //create the new table
            ModTypeTable mtt = new ModTypeTable();
            mtt.rows.add(row);
            modTypeTables.put(tableID, mtt);
        } else {
            //add row to existing table
            ModTypeTable toAdd = modTypeTables.get(tableID);
            toAdd.rows.add(row);
        }
    }
    public static void AddModTableRow(int tableID, ModTableRow row) {
        if (!modTables.containsKey(tableID)) {
            //create the new table
            ModTable mt = new ModTable();
            mt.rows.add(row);
            modTables.put(tableID, mt);
        } else {
            //add row to existing table
            ModTable toAdd = modTables.get(tableID);
            toAdd.rows.add(row);
        }
    }
    public static class GenTable {
        public ArrayList<GenTableRow> rows = new ArrayList<GenTableRow>();

        public GenTableRow getRowForRange(int roll) {
            GenTableRow outRow = null;
            for (GenTableRow iteration : this.rows) {
                if (roll >= iteration.minRoll && roll <= iteration.maxRoll) {
                    outRow = iteration;
                }
            }
            return outRow;
        }
    }
    public static class ItemTable {
        public ArrayList<ItemTableRow> rows = new ArrayList<ItemTableRow>();

        public ItemTableRow getRowForRange(int roll) {
            if (roll > 320) {
                roll = 320;
            }
            ItemTableRow outRow = null;
            for (ItemTableRow iteration : this.rows) {
                if (roll >= iteration.minRoll && roll <= iteration.maxRoll) {
                    outRow = iteration;
                }
            }
            return outRow;
        }
    }
    public static class ModTypeTable {
        public ArrayList<ModTypeTableRow> rows = new ArrayList<ModTypeTableRow>();

        public ModTypeTableRow getRowForRange(int roll) {
            ModTypeTableRow outRow = null;
            for (ModTypeTableRow iteration : this.rows) {
                if (roll >= iteration.minRoll && roll <= iteration.maxRoll) {
                    return iteration;
                }
            }
            return outRow;
        }
    }
    public static class ModTable {
        public ArrayList<ModTableRow> rows = new ArrayList<ModTableRow>();

        public ModTableRow getRowForRange(int roll) {
            if (roll > 320) {
                roll = 320;
            }
            ModTableRow outRow = null;
            for (ModTableRow iteration : this.rows) {
                if (roll >= iteration.minRoll && roll <= iteration.maxRoll) {
                    outRow = iteration;
                }
            }
            return outRow;
        }
    }
    public static class GenTableRow {
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
    public static class ItemTableRow {
        public int minRoll;
        public int maxRoll;
        public int cacheID;
        public int minSpawn;
        public int maxSpawn;

        public ItemTableRow(ResultSet rs) throws SQLException {
            this.minRoll = rs.getInt("minRoll");
            this.maxRoll = rs.getInt("maxRoll");
            this.cacheID = rs.getInt("itemBaseUUID");
            this.minSpawn = rs.getInt("minSpawn");
            this.maxSpawn = rs.getInt("maxSpawn");

        }
    }
    public static class ModTypeTableRow {
        public int minRoll;
        public int maxRoll;
        public int modTableID;

        public ModTypeTableRow(ResultSet rs) throws SQLException {
            this.minRoll = rs.getInt("minRoll");
            this.maxRoll = rs.getInt("maxRoll");
            this.modTableID = rs.getInt("subTableID");

        }
    }
    public static class ModTableRow {
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