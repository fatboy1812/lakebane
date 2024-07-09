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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

    public static final ArrayList<Integer> vorg_ha_uuids = new ArrayList<>(Arrays.asList(27580, 27590, 188500, 188510, 188520, 188530, 188540, 188550, 189510));
    public static final ArrayList<Integer> vorg_ma_uuids = new ArrayList<>(Arrays.asList(27570,188900,188910,188920,188930,188940,188950,189500));
    public static final ArrayList<Integer> vorg_la_uuids = new ArrayList<>(Arrays.asList(27550,27560,189100,189110,189120,189130,189140,189150));
    public static final ArrayList<Integer> vorg_cloth_uuids = new ArrayList<>(Arrays.asList(27600,188700,188720,189550,189560));

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
        boolean inHotzone = false;

        //special blood rune droppers
        MobLoot specialDrop = null;
        switch(mob.getObjectUUID()) {
            case 22595://elf 1
                specialDrop = new MobLoot(mob,ItemBase.getItemBase(252134),true);
                mob.setFirstName("Melandrach The Blood-Mage");
                break;
            case 22432: //elf 2
                specialDrop = new MobLoot(mob,ItemBase.getItemBase(252135),true);
                mob.setFirstName("Kyrtaar The Blood-Mage");
                break;
            case 22537: //elf 3
                specialDrop = new MobLoot(mob,ItemBase.getItemBase(252136),true);
                mob.setFirstName("Vamir The Blood-Mage");
                break;
            case 16387: //human 4 DONE
                specialDrop = new MobLoot(mob,ItemBase.getItemBase(252129),true);
                mob.setFirstName("Alatar The Blood-Mage");
                break;
            case 32724:// human 5 GOOD
                specialDrop = new MobLoot(mob,ItemBase.getItemBase(252130),true);
                mob.setFirstName("Elphaba The Blood-Mage");
                break;
            case 23379: //human 1 GOOD
                specialDrop = new MobLoot(mob,ItemBase.getItemBase(252131),true);
                mob.setFirstName("Bavmorda The Blood-Mage");
                break;
            case 10826: //human 2 REDO
                specialDrop = new MobLoot(mob,ItemBase.getItemBase(252132),true);
                mob.setFirstName("Draco The Blood-Mage");
                break;
            case 15929: //human 3 GOOD
                specialDrop = new MobLoot(mob,ItemBase.getItemBase(252133),true);
                mob.setFirstName("Atlantes The Blood-Mage");
                break;
        }
        if(specialDrop != null) {
            mob.setLevel((short) 65);
            mob.setSpawnTime(10800);
            mob.healthMax = (7500);
            mob.setHealth(7500);
            ChatSystemMsg chatMsg = new ChatSystemMsg(null, mob.getName() + " in " + mob.getParentZone().getName() + " has found the " + specialDrop.getName() + ". Are you tough enough to take it?");
            chatMsg.setMessageType(10);
            chatMsg.setChannel(Enum.ChatChannelType.SYSTEM.getChannelID());
            DispatchMessage.dispatchMsgToAll(chatMsg);
            mob.getCharItemManager().addItemToInventory(specialDrop);
            mob.setResists(new Resists("Dropper"));
            if(!Mob.discDroppers.contains(mob))
                Mob.AddDiscDropper(mob);
        }

        //iterate the booty sets

        if (mob.getMobBase().bootySet != 0 && _bootySetMap.containsKey(mob.getMobBase().bootySet))
            RunBootySet(_bootySetMap.get(mob.getMobBase().bootySet), mob, inHotzone);

        if (mob.bootySet != 0 && _bootySetMap.containsKey(mob.bootySet))
            RunBootySet(_bootySetMap.get(mob.bootySet), mob, inHotzone);

        //lastly, check mobs inventory for godly or disc runes to send a server announcement
        for (Item it : mob.getInventory()) {

            ItemBase ib = it.getItemBase();
            if (ib == null)
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
        float dropRate;

        //1 in 1,000 chance to drop glass
        if(ThreadLocalRandom.current().nextInt(1,1000) == 500){
            int glassID = rollRandomItem(126);
            ItemBase glassItem = ItemBase.getItemBase(glassID);
            if(glassItem != null) {
                MobLoot toAdd = new MobLoot(mob, glassItem, false);

                if (toAdd != null)
                    mob.getCharItemManager().addItemToInventory(toAdd);
            }
        }

        //check for special gifts 1/100 to drop present
        if(ThreadLocalRandom.current().nextInt(1,25) == 15)
            DropPresent(mob);

        // Iterate all entries in this bootySet and process accordingly
        for (BootySetEntry bse : entries) {
            switch (bse.bootyType) {
                case "GOLD":
                    GenerateGoldDrop(mob, bse, inHotzone);
                    break;
                case "LOOT":

                    if (mob.getSafeZone())
                        return; // no loot to drop in safezones

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
            if(ThreadLocalRandom.current().nextInt(1,101) < 91)
                return null; // cut down world drops rates of resources by 90%
            int amount = ThreadLocalRandom.current().nextInt(tableRow.minSpawn, tableRow.maxSpawn + 1);
            return new MobLoot(mob, ItemBase.getItemBase(itemUUID), amount, false);
        }
        if(ItemBase.getItemBase(itemUUID).getType().equals(Enum.ItemType.RUNE)){
            int randomRune = rollRandomItem(itemTableId);
            if(randomRune != 0) {
                itemUUID = randomRune;
            }
        } else if(ItemBase.getItemBase(itemUUID).getType().equals(Enum.ItemType.CONTRACT)){
            int randomContract = rollRandomItem(itemTableId);
            if(randomContract != 0) {
                itemUUID = randomContract;
            }
        }
        outItem = new MobLoot(mob, ItemBase.getItemBase(itemUUID), false);

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

        if(outItem.getItemBase().getType().equals(Enum.ItemType.CONTRACT) || outItem.getItemBase().getType().equals(Enum.ItemType.RUNE)){
            if(ThreadLocalRandom.current().nextInt(1,101) < 66)
                return null; // cut down world drops rates of resources by 65%
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
        int gold = (int) (ThreadLocalRandom.current().nextInt(low, high + 1) * NORMAL_GOLD_RATE);

        if (gold > 0) {
            MobLoot goldAmount = new MobLoot(mob, gold);
            mob.getCharItemManager().addItemToInventory(goldAmount);
        }

    }

    public static void GenerateLootDrop(Mob mob, int tableID, Boolean inHotzone) {

        MobLoot toAdd = getGenTableItem(tableID, mob, inHotzone);

        if (toAdd != null) {
            toAdd.setIsID(true);
            mob.getCharItemManager().addItemToInventory(toAdd);
        }
    }


    public static void GenerateEquipmentDrop(Mob mob) {

        if (mob == null || mob.getSafeZone())
            return; // no equipment to drop in safezones

        //do equipment here
        if (mob.getEquip() != null) {
            boolean isVorg = false;
            for (MobEquipment me : mob.getEquip().values()) {

                if (me.getDropChance() == 0)
                    continue;

                String name = me.getItemBase().getName().toLowerCase();
                if (name.contains("vorgrim legionnaire's") || name.contains("vorgrim auxiliary's") ||name.contains("bellugh nuathal") || name.contains("crimson circle"))
                    isVorg = true;

                float equipmentRoll = ThreadLocalRandom.current().nextInt(1, 100 + 1);
                float dropChance = me.getDropChance() * 100;
                ItemBase itemBase = me.getItemBase();
                if(isVorg) {
                    mob.spawnTime = ThreadLocalRandom.current().nextInt(300, 2700);
                    dropChance = 10;
                    itemBase = getRandomVorg(itemBase);
                }
                if (equipmentRoll > dropChance)
                    continue;

                MobLoot ml = new MobLoot(mob, itemBase, false);

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

        if (lootItem != null) {
            mob.getCharItemManager().addItemToInventory(lootItem);
            if(lootItem.getItemBase().isDiscRune() && !Mob.discDroppers.contains(mob))
                Mob.AddDiscDropper(mob);
        }
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

        if (!itemMan.doesCharOwnThisItem(gift.getObjectUUID()))
            return;

        //roll 1-100 for the gen table selection

        int genRoll = ThreadLocalRandom.current().nextInt(94, 100) + 1;
        GenTableEntry selectedRow = GenTableEntry.rollTable(tableID, genRoll, LootManager.NORMAL_DROP_RATE);

        if(selectedRow == null)
            return;

        //roll 220-320 for the item table selection

        int itemRoll = ThreadLocalRandom.current().nextInt(220, 320 + 1);
        ItemTableEntry selectedItem = ItemTableEntry.rollTable(selectedRow.itemTableID, itemRoll);

        if (selectedItem == null)
            return;

        //create the item from the table, quantity is always 1

        ItemBase ib = ItemBase.getItemBase(selectedItem.cacheID);
        if(ib.getUUID() == Warehouse.coalIB.getUUID()){
            //no more coal, give gold instead
            if (itemMan.getGoldInventory().getNumOfItems() + 250000 > 10000000) {
                ErrorPopupMsg.sendErrorPopup(playerCharacter, 21);
                return;
            }
            itemMan.addGoldToInventory(250000,false);
            itemMan.updateInventory();
        }else {
            MobLoot winnings = new MobLoot(playerCharacter, ib, 1, false);

            if (winnings == null)
                return;

            //early exit if the inventory of the player will not hold the item

            if (itemMan.hasRoomInventory(winnings.getItemBase().getWeight()) == false) {
                ErrorPopupMsg.sendErrorPopup(playerCharacter, 21);
                return;
            }

            //determine if the winning item needs a prefix

            if (selectedRow.pModTable != 0) {
                int prefixRoll = ThreadLocalRandom.current().nextInt(220, 320 + 1);
                ModTableEntry prefix = ModTableEntry.rollTable(selectedRow.pModTable, prefixRoll);
                if (prefix != null)
                    winnings.addPermanentEnchantment(prefix.action, 0, prefix.level, true);
            }

            //determine if the winning item needs a suffix

            if (selectedRow.sModTable != 0) {
                int suffixRoll = ThreadLocalRandom.current().nextInt(220, 320 + 1);
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

    public static int rollRandomItem(int itemTable){
        int returnedID = ItemTableEntry.getRandomItem(itemTable);
        return returnedID;
    }

    public static ItemBase getRandomVorg(ItemBase itemBase){
        int roll = 0;
        if(vorg_ha_uuids.contains(itemBase.getUUID())) {
            roll = ThreadLocalRandom.current().nextInt(0, 10);
            switch (roll) {
                case 1:
                    return ItemBase.getItemBase(vorg_ha_uuids.get(0));
                case 2:
                    return ItemBase.getItemBase(vorg_ha_uuids.get(1));
                case 3:
                    return ItemBase.getItemBase(vorg_ha_uuids.get(2));
                case 4:
                    return ItemBase.getItemBase(vorg_ha_uuids.get(3));
                case 5:
                    return ItemBase.getItemBase(vorg_ha_uuids.get(4));
                case 6:
                    return ItemBase.getItemBase(vorg_ha_uuids.get(5));
                case 7:
                    return ItemBase.getItemBase(vorg_ha_uuids.get(6));
                case 8:
                    return ItemBase.getItemBase(vorg_ha_uuids.get(7));
                default:
                    return ItemBase.getItemBase(vorg_ha_uuids.get(8));
            }
        }

        if(vorg_ma_uuids.contains(itemBase.getUUID())) {
            roll = ThreadLocalRandom.current().nextInt(0, 10);
            switch (roll) {
                case 1:
                    return ItemBase.getItemBase(vorg_ma_uuids.get(0));
                case 2:
                    return ItemBase.getItemBase(vorg_ma_uuids.get(1));
                case 3:
                    return ItemBase.getItemBase(vorg_ma_uuids.get(2));
                case 4:
                    return ItemBase.getItemBase(vorg_ma_uuids.get(3));
                case 5:
                    return ItemBase.getItemBase(vorg_ma_uuids.get(4));
                case 6:
                    return ItemBase.getItemBase(vorg_ma_uuids.get(5));
                case 7:
                    return ItemBase.getItemBase(vorg_ma_uuids.get(6));
                default:
                    return ItemBase.getItemBase(vorg_ma_uuids.get(7));
            }
        }

        if(vorg_la_uuids.contains(itemBase.getUUID())) {
            roll = ThreadLocalRandom.current().nextInt(0, 10);
            switch (roll) {
                case 1:
                    return ItemBase.getItemBase(vorg_la_uuids.get(0));
                case 2:
                    return ItemBase.getItemBase(vorg_la_uuids.get(1));
                case 3:
                    return ItemBase.getItemBase(vorg_la_uuids.get(2));
                case 4:
                    return ItemBase.getItemBase(vorg_la_uuids.get(3));
                case 5:
                    return ItemBase.getItemBase(vorg_la_uuids.get(4));
                case 6:
                    return ItemBase.getItemBase(vorg_la_uuids.get(5));
                case 7:
                    return ItemBase.getItemBase(vorg_la_uuids.get(6));
                default:
                    return ItemBase.getItemBase(vorg_la_uuids.get(7));
            }
        }

        if(vorg_cloth_uuids.contains(itemBase.getUUID())) {
            roll = ThreadLocalRandom.current().nextInt(0, 10);
            switch (roll) {
                case 1:
                    return ItemBase.getItemBase(vorg_cloth_uuids.get(0));
                case 2:
                    return ItemBase.getItemBase(vorg_cloth_uuids.get(1));
                case 3:
                    return ItemBase.getItemBase(vorg_cloth_uuids.get(2));
                case 4:
                    return ItemBase.getItemBase(vorg_cloth_uuids.get(3));
                default:
                    return ItemBase.getItemBase(vorg_cloth_uuids.get(4));
            }
        }

        return null;
    }

    public static void DropPresent(Mob mob){
        int random = 971049 + ThreadLocalRandom.current().nextInt(24);
        if (random > 971071)
            random = 971071;

        int baseLoot = rollRandomItem(random);
        ItemBase contract = ItemBase.getItemBase(baseLoot);
        if (contract != null) {
            MobLoot toAdd = new MobLoot(mob, contract, true);

            if (toAdd != null)
                mob.getCharItemManager().addItemToInventory(toAdd);
        }
    }

    public static void GenerateStrongholdLoot(Mob mob, boolean commander) {

        mob.getCharItemManager().clearInventory();

        int multiplier = 1;
        if (commander)
            multiplier = 2;

        int high = 250000;
        int low = 65000;
        int gold = ThreadLocalRandom.current().nextInt(low, high + 1) * multiplier;

        if (gold > 0) {
            MobLoot goldAmount = new MobLoot(mob, gold);
            mob.getCharItemManager().addItemToInventory(goldAmount);
        }
        if (ThreadLocalRandom.current().nextInt(100) < 75)
            DropPresent(mob);

        if (commander) {

            //chance for glass
            if (ThreadLocalRandom.current().nextInt(100) < 75) {
                int glassID = rollRandomItem(126);
                ItemBase glassItem = ItemBase.getItemBase(glassID);
                if (glassItem != null) {
                    MobLoot toAdd2 = new MobLoot(mob, glassItem, true);

                    if (toAdd2 != null)
                        mob.getCharItemManager().addItemToInventory(toAdd2);
                }
            }

            //chance for disc
            if (ThreadLocalRandom.current().nextInt(100) < 75) {
                int discID = rollRandomItem(3202);
                ItemBase discItem = ItemBase.getItemBase(discID);
                if (discItem != null) {
                    MobLoot toAdd3 = new MobLoot(mob, discItem, true);

                    if (toAdd3 != null)
                        mob.getCharItemManager().addItemToInventory(toAdd3);
                }
            }

            //chance for stat rune
            if (ThreadLocalRandom.current().nextInt(100) < 75) {
                int runeID = rollRandomItem(3201);
                ItemBase runeItem = ItemBase.getItemBase(runeID);
                if (runeItem != null) {
                    MobLoot toAdd4 = new MobLoot(mob, runeItem, true);

                    if (toAdd4 != null)
                        mob.getCharItemManager().addItemToInventory(toAdd4);
                }
            }
        }
    }
}
