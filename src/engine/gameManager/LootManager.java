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

    // Newer tables

    public static HashMap<Integer, ArrayList<GenTableEntry>> _genTables = new HashMap<>();
    public static HashMap<Integer, ArrayList<ItemTableEntry>> _itemTables = new HashMap<>();
    public static HashMap<Integer, ArrayList<ModTableEntry>> _modTables = new HashMap<>();
    public static HashMap<Integer, ArrayList<ModTypeTableEntry>> _modTypeTables = new HashMap<>();

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

        _genTables = DbManager.LootQueries.LOAD_GEN_ITEM_TABLES();
        _itemTables = DbManager.LootQueries.LOAD_ITEM_TABLES();
        _modTables = DbManager.LootQueries.LOAD_MOD_TABLES();
        _modTypeTables = DbManager.LootQueries.LOAD_MOD_TYPE_TABLES();

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

        //iterate the booty sets

        if (mob.getMobBase().bootySet != 0 && NPCManager._bootySetMap.containsKey(mob.getMobBase().bootySet) == true)
            RunBootySet(NPCManager._bootySetMap.get(mob.getMobBase().bootySet), mob, inHotzone, fromDeath);

        if (mob.bootySet != 0 && NPCManager._bootySetMap.containsKey(mob.bootySet) == true)
            RunBootySet(NPCManager._bootySetMap.get(mob.bootySet), mob, inHotzone, fromDeath);

        //lastly, check mobs inventory for godly or disc runes to send a server announcement

        if (!fromDeath)
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

    private static void RunBootySet(ArrayList<BootySetEntry> entries, Mob mob, boolean inHotzone, boolean fromDeath) {

        boolean hotzoneWasRan = false;
        float dropRate = LootManager.NORMAL_DROP_RATE;

        if (fromDeath) {
            GenerateEquipmentDrop(mob);
            return;
        }

        // Iterate all entries in this bootySet and process accordingly

        for (BootySetEntry bse : entries) {
            switch (bse.bootyType) {
                case "GOLD":
                    GenerateGoldDrop(mob, bse, inHotzone);
                    break;
                case "LOOT":

                    if (inHotzone == true)
                        dropRate = LootManager.HOTZONE_DROP_RATE;
                    else
                        dropRate = LootManager.NORMAL_DROP_RATE;

                    if (ThreadLocalRandom.current().nextInt(1, 100 + 1) < (bse.dropChance * dropRate))
                        GenerateLootDrop(mob, bse.genTable, false);  //generate normal loot drop

                    // Generate hotzone loot if in hotzone
                    // Only one bite at the hotzone apple per bootyset.

                    if (inHotzone == true && hotzoneWasRan == false)
                        if (_genTables.containsKey(bse.genTable + 1) && ThreadLocalRandom.current().nextInt(1, 100 + 1) < (bse.dropChance * dropRate)) {
                            GenerateLootDrop(mob, bse.genTable + 1, true);  //generate loot drop from hotzone table
                            hotzoneWasRan = true;
                        }

                    break;
                case "ITEM":
                    GenerateInventoryDrop(mob, bse);
                    break;
            }
        }
    }

    public static MobLoot getGenTableItem(int genTableID, Mob mob, Boolean inHotzone) {

        if (mob == null || _genTables.containsKey(genTableID) == false)
            return null;

        MobLoot outItem;

        int genRoll = new Random().nextInt(99) + 1;

        GenTableEntry selectedRow = GenTableEntry.rollTable(genTableID, genRoll);

        if (selectedRow == null)
            return null;

        int itemTableId = selectedRow.itemTableID;

        if (_itemTables.containsKey(itemTableId) == false)
            return null;

        //gets the 1-320 roll for this mob

        int itemTableRoll = TableRoll(mob.level, inHotzone);

        ItemTableEntry tableRow = ItemTableEntry.rollTable(itemTableId, itemTableRoll);

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
                    outItem = GeneratePrefix(mob, outItem, genTableID, genRoll, inHotzone);
                } catch (Exception e) {
                    Logger.error("Failed to GeneratePrefix for item: " + outItem.getName());
                }

                try {
                    outItem = GenerateSuffix(mob, outItem, genTableID, genRoll, inHotzone);
                } catch (Exception e) {
                    Logger.error("Failed to GenerateSuffix for item: " + outItem.getName());
                }
            }
        }

        if (outItem.getPrefix() != null && outItem.getPrefix().isEmpty() == false)
            outItem.setIsID(false);

        if (outItem.getSuffix() != null && outItem.getSuffix().isEmpty() == false)
            outItem.setIsID(false);

        return outItem;
    }

    private static MobLoot GeneratePrefix(Mob mob, MobLoot inItem, int genTableID, int genRoll, Boolean inHotzone) {

        GenTableEntry selectedRow = GenTableEntry.rollTable(genTableID, genRoll);

        if (selectedRow == null)
            return inItem;

        int prefixroll = ThreadLocalRandom.current().nextInt(1, 100 + 1);

        ModTypeTableEntry prefixTable = ModTypeTableEntry.rollTable(selectedRow.pModTable, prefixroll);

        if (prefixTable == null)
            return inItem;

        ModTableEntry prefixMod = ModTableEntry.rollTable(prefixTable.modTableID, TableRoll(mob.level, inHotzone));

        if (prefixMod == null)
            return inItem;

        if (prefixMod.action.length() > 0) {
            inItem.setPrefix(prefixMod.action);
            inItem.addPermanentEnchantment(prefixMod.action, 0, prefixMod.level, true);
        }

        return inItem;
    }

    private static MobLoot GenerateSuffix(Mob mob, MobLoot inItem, int genTableID, int genRoll, Boolean inHotzone) {

        GenTableEntry selectedRow = GenTableEntry.rollTable(genTableID, genRoll);

        if (selectedRow == null)
            return inItem;

        int suffixRoll = ThreadLocalRandom.current().nextInt(1, 100 + 1);

        ModTypeTableEntry suffixTable = ModTypeTableEntry.rollTable(selectedRow.sModTable, suffixRoll);

        if (suffixTable == null)
            return inItem;

        ModTableEntry suffixMod = ModTableEntry.rollTable(suffixTable.modTableID, TableRoll(mob.level, inHotzone));

        if (suffixMod == null)
            return inItem;

        if (suffixMod.action.length() > 0) {
            inItem.setPrefix(suffixMod.action);
            inItem.addPermanentEnchantment(suffixMod.action, 0, suffixMod.level, true);
        }

        return inItem;
    }

    public static int TableRoll(int mobLevel, Boolean inHotzone) {

        if (mobLevel > 65)
            mobLevel = 65;

        int max = (int) (4.882 * mobLevel + 127.0);

        if (max > 319)
            max = 319;

        int min = (int) (4.469 * mobLevel - 3.469);

        if (min < 70)
            min = 70;

        if (inHotzone)
            min += mobLevel;

        int roll = ThreadLocalRandom.current().nextInt(max - min) + min;

        return roll;
    }

    public static void GenerateGoldDrop(Mob mob, BootySetEntry bse, Boolean inHotzone) {

        int chanceRoll = ThreadLocalRandom.current().nextInt(1, 100 + 1);

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

    public static void GenerateLootDrop(Mob mob, int tableID, Boolean inHotzone) {

        try {

            MobLoot toAdd = getGenTableItem(tableID, mob, inHotzone);

            if (toAdd != null)
                mob.getCharItemManager().addItemToInventory(toAdd);

        } catch (Exception e) {
            //TODO chase down loot generation error, affects roughly 2% of drops
            int i = 0;
        }
    }

    public static void GenerateEquipmentDrop(Mob mob) {

        //do equipment here

        if (mob.getEquip() != null)
            for (MobEquipment me : mob.getEquip().values()) {

                if (me.getDropChance() == 0)
                    continue;

                float equipmentRoll = ThreadLocalRandom.current().nextInt(1, 100 + 1);
                float dropChance = me.getDropChance() * 100;

                if (equipmentRoll > dropChance)
                    continue;

                MobLoot ml = new MobLoot(mob, me.getItemBase(), false);

                if (ml != null) {
                    ml.setIsID(true);
                    ml.setDurabilityCurrent((short) (ml.getDurabilityCurrent() - ThreadLocalRandom.current().nextInt(5) + 1));
                    mob.getCharItemManager().addItemToInventory(ml);
                }
            }
    }

    public static void GenerateInventoryDrop(Mob mob, BootySetEntry bse) {

        int chanceRoll = ThreadLocalRandom.current().nextInt(1, 100 + 1);

        //early exit, failed to hit minimum chance roll

        if (chanceRoll > bse.dropChance)
            return;

        MobLoot lootItem = new MobLoot(mob, ItemBase.getItemBase(bse.itemBase), true);

        if (lootItem != null)
            mob.getCharItemManager().addItemToInventory(lootItem);
    }

}
