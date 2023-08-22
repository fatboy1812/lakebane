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
import engine.net.client.msg.ErrorPopupMsg;
import engine.net.client.msg.chat.ChatSystemMsg;
import engine.objects.*;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashMap;
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
    public static HashMap<Integer, ArrayList<BootySetEntry>> _bootySetMap = new HashMap<>();

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

    public static void GenerateMobLoot(Mob mob) {

        //determine if mob is in hotzone
        boolean inHotzone = ZoneManager.inHotZone(mob.getLoc());

        //iterate the booty sets

        if (mob.getMobBase().bootySet != 0 && _bootySetMap.containsKey(mob.getMobBase().bootySet) == true)
            RunBootySet(_bootySetMap.get(mob.getMobBase().bootySet), mob, inHotzone);

        if (mob.bootySet != 0 && _bootySetMap.containsKey(mob.bootySet) == true)
            RunBootySet(_bootySetMap.get(mob.bootySet), mob, inHotzone);

        //lastly, check mobs inventory for godly or disc runes to send a server announcement
            for (Item it : mob.getInventory()) {

                ItemBase ib = it.getItemBase();
                if(ib == null)
                    break;
                if (ib.isDiscRune() || ib.getName().toLowerCase().contains("of the gods")) {
                    ChatSystemMsg chatMsg = new ChatSystemMsg(null, mob.getName() + " in " + mob.getParentZone().getName() + " has found the " + ib.getName() + ". Are you tough enough to take it?");
                    chatMsg.setMessageType(10);
                    chatMsg.setChannel(Enum.ChatChannelType.SYSTEM.getChannelID());
                    DispatchMessage.dispatchMsgToAll(chatMsg);
                }
            }

    }

    private static void RunBootySet(ArrayList<BootySetEntry> entries, Mob mob, boolean inHotzone) {

        boolean hotzoneWasRan = false;
        float dropRate = 1.0f;

        // Iterate all entries in this bootySet and process accordingly

        for (BootySetEntry bse : entries) {
            switch (bse.bootyType) {
                case "GOLD":
                    GenerateGoldDrop(mob, bse, inHotzone);
                    break;
                case "LOOT":

                    if (mob.getSafeZone() == false)
                        dropRate = LootManager.NORMAL_DROP_RATE;

                    if (inHotzone == true)
                        dropRate = LootManager.HOTZONE_DROP_RATE;

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

    public static MobLoot getGenTableItem(int genTableID, AbstractCharacter mob, Boolean inHotzone) {

        if (mob == null || _genTables.containsKey(genTableID) == false)
            return null;

        MobLoot outItem;

        int genRoll = ThreadLocalRandom.current().nextInt(1,100 + 1);

        GenTableEntry selectedRow = GenTableEntry.rollTable(genTableID, genRoll, 1.0f);

        if (selectedRow == null)
            return null;

        int itemTableId = selectedRow.itemTableID;

        if (_itemTables.containsKey(itemTableId) == false)
            return null;

        //gets the 1-320 roll for this mob
        int itemTableRoll = 0;
        int objectType = mob.getObjectType().ordinal();
        if(mob.getObjectType().ordinal() == 52) { //52 = player character
            itemTableRoll = ThreadLocalRandom.current().nextInt(1,320 + 1);
        } else{
            itemTableRoll = TableRoll(mob.level, inHotzone);
        }
        ItemTableEntry tableRow = ItemTableEntry.rollTable(itemTableId, itemTableRoll);
        if (tableRow == null)
            return null;

        int itemUUID = tableRow.cacheID;

        if (itemUUID == 0)
            return null;

        if (ItemBase.getItemBase(itemUUID).getType().ordinal() == Enum.ItemType.RESOURCE.ordinal()) {
            int amount = ThreadLocalRandom.current().nextInt(tableRow.minSpawn, tableRow.maxSpawn + 1);
            return new MobLoot(mob, ItemBase.getItemBase(itemUUID), amount, false);
        }

        outItem = new MobLoot(mob, ItemBase.getItemBase(itemUUID), false);
        Enum.ItemType outType = outItem.getItemBase().getType();


        if(selectedRow.pModTable != 0){
            try {
                outItem = GeneratePrefix(mob, outItem, genTableID, genRoll, inHotzone);
                outItem.setIsID(false);
            } catch (Exception e) {
                Logger.error("Failed to GeneratePrefix for item: " + outItem.getName());
            }
        }
        if(selectedRow.sModTable != 0){
            try {
                outItem = GenerateSuffix(mob, outItem, genTableID, genRoll, inHotzone);
                outItem.setIsID(false);
            } catch (Exception e) {
                Logger.error("Failed to GenerateSuffix for item: " + outItem.getName());
            }
        }
        return outItem;
    }

    private static MobLoot GeneratePrefix(AbstractCharacter mob, MobLoot inItem, int genTableID, int genRoll, Boolean inHotzone) {

        GenTableEntry selectedRow = GenTableEntry.rollTable(genTableID, genRoll, 1.0f);

        if (selectedRow == null)
            return inItem;

        int prefixroll = ThreadLocalRandom.current().nextInt(1, 100 + 1);

        ModTypeTableEntry prefixTable = ModTypeTableEntry.rollTable(selectedRow.pModTable, prefixroll);

        if (prefixTable == null)
            return inItem;
        int prefixTableRoll = 0;
        if(mob.getObjectType().ordinal() == 52) {
            prefixTableRoll = ThreadLocalRandom.current().nextInt(1,320 + 1);
        } else{
            prefixTableRoll = TableRoll(mob.level, inHotzone);
        }
        ModTableEntry prefixMod = ModTableEntry.rollTable(prefixTable.modTableID, prefixTableRoll);

        if (prefixMod == null)
            return inItem;

        if (prefixMod.action.length() > 0) {
            inItem.setPrefix(prefixMod.action);
            inItem.addPermanentEnchantment(prefixMod.action, 0, prefixMod.level, true);
        }

        return inItem;
    }

    private static MobLoot GenerateSuffix(AbstractCharacter mob, MobLoot inItem, int genTableID, int genRoll, Boolean inHotzone) {

        GenTableEntry selectedRow = GenTableEntry.rollTable(genTableID, genRoll, 1.0f);

        if (selectedRow == null)
            return inItem;

        int suffixRoll = ThreadLocalRandom.current().nextInt(1, 100 + 1);

        ModTypeTableEntry suffixTable = ModTypeTableEntry.rollTable(selectedRow.sModTable, suffixRoll);

        if (suffixTable == null)
            return inItem;
        int suffixTableRoll = 0;
        if(mob.getObjectType().ordinal() == 52) {
            suffixTableRoll = ThreadLocalRandom.current().nextInt(1,320 + 1);
        } else{
            suffixTableRoll = TableRoll(mob.level, inHotzone);
        }
        ModTableEntry suffixMod = ModTableEntry.rollTable(suffixTable.modTableID, suffixTableRoll);

        if (suffixMod == null)
            return inItem;

        if (suffixMod.action.length() > 0) {
            inItem.setSuffix(suffixMod.action);
            inItem.addPermanentEnchantment(suffixMod.action, 0, suffixMod.level, false);
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

        int roll = ThreadLocalRandom.current().nextInt(min, max + 1);

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
        int gold = ThreadLocalRandom.current().nextInt(low, high + 1);

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
        int dropCount = 0;
        if (mob.getEquip() != null)
            for (MobEquipment me : mob.getEquip().values()) {

                if (me.getDropChance() == 0)
                    continue;

                float equipmentRoll = ThreadLocalRandom.current().nextInt(1, 100 + 1);
                float dropChance = me.getDropChance() * 100;

                if (equipmentRoll > dropChance)
                    continue;

                MobLoot ml = new MobLoot(mob, me.getItemBase(), false);

                if (ml != null && dropCount < 1) {
                    ml.setIsID(true);
                    ml.setDurabilityCurrent((short) (ml.getDurabilityCurrent() - ThreadLocalRandom.current().nextInt(5) + 1));
                    mob.getCharItemManager().addItemToInventory(ml);
                    dropCount = 1;
                    //break; // Exit on first successful roll.
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

    public static void peddleFate(PlayerCharacter playerCharacter, Item gift) {

        //get table ID for the itembase ID

        int tableID = 0;

        if (_bootySetMap.get(gift.getItemBaseID()) != null)
            tableID = _bootySetMap.get(gift.getItemBaseID()).get(ThreadLocalRandom.current().nextInt(_bootySetMap.get(gift.getItemBaseID()).size())).genTable;

        if (tableID == 0)
            return;

        //get the character item manager

        CharacterItemManager itemMan = playerCharacter.getCharItemManager();

        if (itemMan == null)
            return;

        //check if player owns the gift he is trying to open

        if (itemMan.doesCharOwnThisItem(gift.getObjectUUID()) == false)
            return;

        //roll 1-100 for the gen table selection

        int genRoll = ThreadLocalRandom.current().nextInt(1, 100 + 1);
        GenTableEntry selectedRow = GenTableEntry.rollTable(tableID, genRoll, LootManager.NORMAL_DROP_RATE);

        if(selectedRow == null)
            return;

        //roll 220-320 for the item table selection

        int itemRoll = ThreadLocalRandom.current().nextInt(220, 320 + 1);
        ItemTableEntry selectedItem = ItemTableEntry.rollTable(selectedRow.itemTableID, itemRoll);

        if (selectedItem == null)
            return;

        //create the item from the table, quantity is always 1

        MobLoot winnings = new MobLoot(playerCharacter, ItemBase.getItemBase(selectedItem.cacheID), 1, false);

        if (winnings == null)
            return;

        //early exit if the inventory of the player will not old the item

        if (itemMan.hasRoomInventory(winnings.getItemBase().getWeight()) == false) {
            ErrorPopupMsg.sendErrorPopup(playerCharacter, 21);
            return;
        }

        //determine if the winning item needs a prefix

        if(selectedRow.pModTable != 0){
            int prefixRoll = ThreadLocalRandom.current().nextInt(220,320 + 1);
            ModTableEntry prefix = ModTableEntry.rollTable(selectedRow.pModTable, prefixRoll);
            if(prefix != null)
                winnings.addPermanentEnchantment(prefix.action, 0, prefix.level, true);
        }

        //determine if the winning item needs a suffix

        if(selectedRow.sModTable != 0){
            int suffixRoll = ThreadLocalRandom.current().nextInt(220,320 + 1);
            ModTableEntry suffix = ModTableEntry.rollTable(selectedRow.sModTable, suffixRoll);
            if (suffix != null)
                winnings.addPermanentEnchantment(suffix.action, 0, suffix.level, true);
        }
        winnings.setIsID(true);

        //remove gift from inventory

        itemMan.consume(gift);

        //add winnings to player inventory

        Item playerWinnings = winnings.promoteToItem(playerCharacter);
        itemMan.addItemToInventory(playerWinnings);
        itemMan.updateInventory();
    }
}
