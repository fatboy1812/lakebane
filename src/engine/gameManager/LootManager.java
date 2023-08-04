// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com

package engine.gameManager;

import engine.Enum;
import engine.loot.*;
import engine.net.DispatchMessage;
import engine.net.client.msg.chat.ChatSystemMsg;
import engine.objects.*;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Class contains static methods for data from Magicbane's loot tables
 */
public enum LootManager {

    LOOTMANAGER;

    //new tables
    private static final HashMap<Integer, GenTable> generalItemTables = new HashMap<>();
    private static final HashMap<Integer, ItemTable> itemTables = new HashMap<>();
    private static final HashMap<Integer, ModTypeTable> modTypeTables = new HashMap<>();
    private static final HashMap<Integer, ModTable> modTables = new HashMap<>();

    // Drop Rates

    public static float NORMAL_DROP_RATE;
    public static float NORMAL_EXP_RATE;
    public static float NORMAL_GOLD_RATE;
    public static float HOTZONE_DROP_RATE;
    public static float HOTZONE_EXP_RATE;
    public static float HOTZONE_GOLD_RATE;

    // Bootstrap routine to initialize the Loot Manager

    public static void init() {

        // Load loot tables from database.

        DbManager.LootQueries.LOAD_ALL_LOOTGROUPS();
        DbManager.LootQueries.LOAD_ALL_LOOTTABLES();
        DbManager.LootQueries.LOAD_ALL_MODGROUPS();
        DbManager.LootQueries.LOAD_ALL_MODTABLES();

        // Cache drop rate values from Config manager.

        NORMAL_DROP_RATE = Float.parseFloat(ConfigManager.MB_NORMAL_DROP_RATE.getValue());
        NORMAL_EXP_RATE = Float.parseFloat(ConfigManager.MB_NORMAL_EXP_RATE.getValue());
        NORMAL_GOLD_RATE = Float.parseFloat(ConfigManager.MB_NORMAL_GOLD_RATE.getValue());
        HOTZONE_DROP_RATE = Float.parseFloat(ConfigManager.MB_HOTZONE_DROP_RATE.getValue());
        HOTZONE_EXP_RATE = Float.parseFloat(ConfigManager.MB_HOTZONE_EXP_RATE.getValue());
        HOTZONE_GOLD_RATE = Float.parseFloat(ConfigManager.MB_HOTZONE_GOLD_RATE.getValue());

    }

    public static void GenerateMobLoot(Mob mob, boolean fromDeath) {

        //determine if mob is in hotzone

        boolean inHotzone = ZoneManager.inHotZone(mob.getLoc());

        //get multiplier form config manager

        float multiplier = NORMAL_DROP_RATE;

        //if mob is inside hotzone, use the hotzone multiplier from the config instead

        if (inHotzone)
            multiplier = HOTZONE_DROP_RATE;

        //iterate the booty sets

        if (mob.getMobBase().bootySet != 0 && NPCManager._bootySetMap.containsKey(mob.getMobBase().bootySet) == true)
            RunBootySet(NPCManager._bootySetMap.get(mob.getMobBase().bootySet), mob, multiplier, inHotzone, fromDeath);

        if (mob.bootySet != 0 && NPCManager._bootySetMap.containsKey(mob.bootySet) == true)
            RunBootySet(NPCManager._bootySetMap.get(mob.bootySet), mob, multiplier, inHotzone, fromDeath);

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
                if (it.getEnchants().isEmpty() == true)
                    it.setIsID(true);
            }
        }
    }

    private static void RunBootySet(ArrayList<BootySetEntry> entries, Mob mob, float multiplier, boolean inHotzone, boolean fromDeath) {

        if (fromDeath)
            DropEquipment(mob, multiplier);
        else {
            for (BootySetEntry bse : entries) {
                switch (bse.bootyType) {
                    case "GOLD":
                        GenerateGoldDrop(mob, bse, inHotzone);
                        break;
                    case "LOOT":
                        //always run base table loot drop
                        GenerateLootDrop(mob, bse.lootTable, bse.dropChance, multiplier);  //generate normal loot drop

                        if (inHotzone)
                            //run another iteration for the hotzone table if in hotzone
                            if (generalItemTables.containsKey(bse.lootTable + 1))
                                GenerateLootDrop(mob, bse.lootTable + 1, bse.dropChance, multiplier);  //generate loot drop from hotzone table
                        break;
                    case "ITEM":
                        GenerateItemLootDrop(mob, bse, multiplier);
                        break;
                }
            }
        }
    }

    public static MobLoot getGenTableItem(int genTableID, Mob mob) {

        if (mob == null || generalItemTables.containsKey(genTableID) == false)
            return null;

        MobLoot outItem;

        int genRoll = new Random().nextInt(99) + 1;

        GenTableRow selectedRow = generalItemTables.get(genTableID).getRowForRange(genRoll);
        if (selectedRow == null)
            return null;

        int itemTableId = selectedRow.itemTableID;

        if(itemTables.containsKey(itemTableId) == false)
            return null;

        //gets the 1-320 roll for this mob

        int roll2 = TableRoll(mob.level);

        ItemTableRow tableRow = itemTables.get(itemTableId).getRowForRange(roll2);

        if (tableRow == null)
            return null;

        int itemUUID = tableRow.cacheID;

        if (itemUUID == 0)
            return null;

        if (ItemBase.getItemBase(itemUUID).getType().ordinal() == Enum.ItemType.RESOURCE.ordinal()) {
            int amount = ThreadLocalRandom.current().nextInt(tableRow.maxSpawn - tableRow.minSpawn) + tableRow.minSpawn;
            return new MobLoot(mob, ItemBase.getItemBase(itemUUID), amount, false);
        }

        outItem = new MobLoot(mob, ItemBase.getItemBase(itemUUID), false);
        Enum.ItemType outType = outItem.getItemBase().getType();

        if (outType.ordinal() == Enum.ItemType.WEAPON.ordinal() || outType.ordinal() == Enum.ItemType.ARMOR.ordinal() || outType.ordinal() == Enum.ItemType.JEWELRY.ordinal()) {
            if (outItem.getItemBase().isGlass() == false) {
                try {
                    outItem = GeneratePrefix(mob, outItem, genTableID, genRoll);
                } catch (Exception e) {
                    Logger.error("Failed to GeneratePrefix for item: " + outItem.getName());
                }
                try {
                    outItem = GenerateSuffix(mob, outItem, genTableID, genRoll);
                } catch (Exception e) {
                    Logger.error("Failed to GenerateSuffix for item: " + outItem.getName());
                }
            }
        }
        return outItem;
    }

    private static MobLoot GeneratePrefix(Mob mob, MobLoot inItem, int genTableID, int genRoll) {

        int prefixChanceRoll = ThreadLocalRandom.current().nextInt(99) + 1;
        double prefixChance = 2.057 * mob.level - 28.67;

        if (prefixChanceRoll < prefixChance) {

            GenTableRow selectedRow = generalItemTables.get(genTableID).getRowForRange(genRoll);
            if(selectedRow == null)
                return inItem;

            ModTypeTable prefixTable = modTypeTables.get(selectedRow.pModTable);
            if(prefixTable == null)
                return inItem;

            int prefixroll = ThreadLocalRandom.current().nextInt(99) + 1;

            if (modTables.get(prefixTable.getRowForRange(prefixroll).modTableID) != null) {

                ModTable prefixModTable = modTables.get(prefixTable.getRowForRange(prefixroll).modTableID);
                if(prefixModTable == null)
                    return inItem;

                ModTableRow prefixMod = prefixModTable.getRowForRange(TableRoll(mob.level));
                if(prefixMod == null)
                    return inItem;

                if (prefixMod != null && prefixMod.action.length() > 0) {
                    inItem.setPrefix(prefixMod.action);
                    inItem.addPermanentEnchantment(prefixMod.action, 0, prefixMod.level, true);
                }
            }
        }
        return inItem;
    }

    private static MobLoot GenerateSuffix(Mob mob, MobLoot inItem, int genTableID, int genRoll) {

        int suffixChanceRoll = ThreadLocalRandom.current().nextInt(99) + 1;
        double suffixChance = 2.057 * mob.level - 28.67;

        if (suffixChanceRoll < suffixChance) {

            GenTableRow selectedRow = generalItemTables.get(genTableID).getRowForRange(genRoll);
            if(selectedRow == null)
                return inItem;

            int suffixroll = ThreadLocalRandom.current().nextInt(99) + 1;

            ModTypeTable suffixTable = modTypeTables.get(selectedRow.sModTable);
            if(suffixTable == null)
                return inItem;

            if (modTables.get(suffixTable.getRowForRange(suffixroll).modTableID) != null) {

                ModTable suffixModTable = modTables.get(suffixTable.getRowForRange(suffixroll).modTableID);
                if(suffixModTable == null)
                    return inItem;

                ModTableRow suffixMod = suffixModTable.getRowForRange(TableRoll(mob.level));
                if(suffixMod == null)
                    return inItem;

                if (suffixMod != null && suffixMod.action.length() > 0) {
                    inItem.setSuffix(suffixMod.action);
                    inItem.addPermanentEnchantment(suffixMod.action, 0, suffixMod.level, false);
                }
            }
        }
        return inItem;
    }

    private static int TableRoll(int mobLevel) {

        if (mobLevel > 65)
            mobLevel = 65;

        int max = (int) (4.882 * mobLevel + 127.0);

        if (max > 319)
            max = 319;

        int min = (int) (2.089 * mobLevel + 22.14);

        int roll = ThreadLocalRandom.current().nextInt(max - min) + min;

        return roll;
    }

    public static void GenerateGoldDrop(Mob mob, BootySetEntry bse, Boolean inHotzone) {

        int chanceRoll = ThreadLocalRandom.current().nextInt(99) + 1;

        //early exit, failed to hit minimum chance roll

        if (chanceRoll > bse.dropChance)
            return;

        //determine and add gold to mob inventory

        int high = bse.highGold;
        int low = bse.lowGold;
        int gold = ThreadLocalRandom.current().nextInt(high - low) + low;

        if (inHotzone == true)
            gold = (int) (gold * HOTZONE_GOLD_RATE);
        else
            gold = (int) (gold * NORMAL_GOLD_RATE);

        if (gold > 0) {
            MobLoot goldAmount = new MobLoot(mob, gold);
            mob.getCharItemManager().addItemToInventory(goldAmount);
        }

    }

    public static void GenerateLootDrop(Mob mob, int tableID, float dropChance, float multiplier) {

        try {
            int chanceRoll = ThreadLocalRandom.current().nextInt(99) + 1;

            //early exit, failed to hit minimum chance roll

            if (chanceRoll > dropChance * multiplier)
                return;

            MobLoot toAdd = getGenTableItem(tableID, mob);

            if (toAdd != null) {
                mob.getCharItemManager().addItemToInventory(toAdd);
            }
        } catch (Exception e) {
            //TODO chase down loot generation error, affects roughly 2% of drops
            int i = 0;
        }
    }

    public static void DropEquipment(Mob mob, float multiplier) {

        //do equipment here

        if (mob.getEquip() != null) {
            for (MobEquipment me : mob.getEquip().values()) {

                if (me.getDropChance() == 0)
                    continue;

                float equipmentRoll = ThreadLocalRandom.current().nextInt(99) + 1;
                float dropChance = me.getDropChance() * 100;

                if (equipmentRoll > (dropChance * multiplier))
                    continue;
                MobLoot ml = new MobLoot(mob, me.getItemBase(), false);
                if (ml != null)
                    mob.getCharItemManager().addItemToInventory(ml);
            }
        }
    }

    public static void GenerateItemLootDrop(Mob mob, BootySetEntry bse, float multiplier) {

        int chanceRoll = ThreadLocalRandom.current().nextInt(99) + 1;

        //early exit, failed to hit minimum chance roll

        if (chanceRoll > bse.dropChance * multiplier)
            return;

        MobLoot lootItem = new MobLoot(mob, ItemBase.getItemBase(bse.itemBase), true);

        if (lootItem != null)
            mob.getCharItemManager().addItemToInventory(lootItem);
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

}