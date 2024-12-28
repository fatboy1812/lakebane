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
    public static final ArrayList<Integer> racial_guard_uuids = new ArrayList<>(Arrays.asList(841,951,952,1050,1052,1180,1182,1250,1252,1350,1352,1450,1452,1500,1502,1525,1527,1550,1552,1575,1577,1600,1602,1650,1652,1700,980100,980102));

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

        //iterate the booty sets

        if(mob.mobBase == null || mob.getMobBaseID() == 253003){
            int i = 0;
        }

        if (mob.getMobBase().bootySet != 0 && _bootySetMap.containsKey(mob.getMobBase().bootySet))
            RunBootySet(_bootySetMap.get(mob.getMobBase().bootySet), mob, inHotzone);

        if (mob.bootySet != 0 && _bootySetMap.containsKey(mob.bootySet)) {
            RunBootySet(_bootySetMap.get(mob.bootySet), mob, inHotzone);
        }else if(mob.bootySet != 0 && ItemBase.getItemBase(mob.bootySet) != null){
            MobLoot specialDrop = null;
            specialDrop = new MobLoot(mob,ItemBase.getItemBase(mob.bootySet),true);
            if(specialDrop != null) {
                ChatSystemMsg chatMsg = new ChatSystemMsg(null, mob.getName() + " in " + mob.getParentZone().getName() + " has found the " + specialDrop.getName() + ". Are you tough enough to take it?");
                chatMsg.setMessageType(10);
                chatMsg.setChannel(Enum.ChatChannelType.SYSTEM.getChannelID());
                DispatchMessage.dispatchMsgToAll(chatMsg);
                mob.getCharItemManager().addItemToInventory(specialDrop);
                mob.setResists(new Resists("Dropper"));
                if(!Mob.discDroppers.contains(mob))
                    Mob.AddDiscDropper(mob);
            }

        }

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

        if (!mob.getSafeZone()) {
            int baseBound = 100000;
            int levelPenalty = (int) (Math.max(0, Math.abs(50 - mob.level)) * 0.01 * 100000);
            int totalRange = baseBound + levelPenalty;

            // Roll within the adjusted range
            int specialCaseRoll = ThreadLocalRandom.current().nextInt(1, totalRange + 1);

            // Special Case Contract Drop
            if (specialCaseRoll <= 400) { // 0.4% of the range
                SpecialCaseResourceDrop(mob, entries);
            } else if (specialCaseRoll <= 800) { // Next 0.4% of the range
                SpecialCaseContractDrop(mob, entries);
            } else if (specialCaseRoll <= 1200) { // Next 0.4% of the range
                SpecialCaseRuneDrop(mob, entries);
            } else if (specialCaseRoll <= 1210) { // Next 0.01% of the range
                int glassID = rollRandomItem(126);
                ItemBase glassItem = ItemBase.getItemBase(glassID);
                if (glassItem != null) {
                    MobLoot toAddGlass = new MobLoot(mob, glassItem, false);
                    if (toAddGlass != null)
                        mob.getCharItemManager().addItemToInventory(toAddGlass);
                }
            } else if (specialCaseRoll <= 1220) { // Next 0.01% of the range
                int guardContractID = racial_guard_uuids.get(new java.util.Random().nextInt(racial_guard_uuids.size()));
                ItemBase guardContract = ItemBase.getItemBase(guardContractID);
                if (guardContract != null) {
                    MobLoot toAddContract = new MobLoot(mob, guardContract, false);
                    if (toAddContract != null)
                        mob.getCharItemManager().addItemToInventory(toAddContract);
                }
            }
        }

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

    public static void SpecialCaseContractDrop(Mob mob,ArrayList<BootySetEntry> entries){

        int lootTableID = 0;
        for(BootySetEntry entry : entries){
            if(entry.bootyType.equals("LOOT")){
                lootTableID = entry.genTable;
                break;
            }
        }

        if(lootTableID == 0)
            return;

        int ContractTableID = 0;
        for(GenTableEntry entry : _genTables.get(lootTableID)){
            try {
                if (ItemBase.getItemBase(_itemTables.get(entry.itemTableID).get(0).cacheID).getType().equals(Enum.ItemType.CONTRACT)) {
                    ContractTableID = entry.itemTableID;
                    break;
                }
            }catch(Exception e){

            }
        }

        if(ContractTableID == 0)
            return;

        ItemBase ib = ItemBase.getItemBase(rollRandomItem(ContractTableID));
        if(ib != null){
            MobLoot toAdd = new MobLoot(mob,ib,false);
            mob.getCharItemManager().addItemToInventory(toAdd);
        }
    }

    public static void SpecialCaseRuneDrop(Mob mob,ArrayList<BootySetEntry> entries){
        int lootTableID = 0;
        for(BootySetEntry entry : entries){
            if(entry.bootyType.equals("LOOT")){
                lootTableID = entry.genTable;
                break;
            }
        }

        if(lootTableID == 0)
            return;

        int RuneTableID = 0;
        for(GenTableEntry entry : _genTables.get(lootTableID)){
            try {
                if (ItemBase.getItemBase(_itemTables.get(entry.itemTableID).get(0).cacheID).getType().equals(Enum.ItemType.RUNE)) {
                    RuneTableID = entry.itemTableID;
                    break;
                }
            }catch(Exception e){

            }
        }

        if(RuneTableID == 0)
            return;

        ItemBase ib = ItemBase.getItemBase(rollRandomItem(RuneTableID));
        if(ib != null){
            MobLoot toAdd = new MobLoot(mob,ib,false);
            mob.getCharItemManager().addItemToInventory(toAdd);
        }
    }

    public static void SpecialCaseResourceDrop(Mob mob,ArrayList<BootySetEntry> entries){
        int lootTableID = 0;
        for(BootySetEntry entry : entries){
            if(entry.bootyType.equals("LOOT")){
                lootTableID = entry.genTable;
                break;
            }
        }

        if(lootTableID == 0)
            return;

        int ResourceTableID = 0;
        for(GenTableEntry entry : _genTables.get(lootTableID)){
            try {
                if (ItemBase.getItemBase(_itemTables.get(entry.itemTableID).get(0).cacheID).getType().equals(Enum.ItemType.RESOURCE)) {
                    ResourceTableID = entry.itemTableID;
                    break;
                }
            }catch(Exception e){

            }
        }

        if(ResourceTableID == 0)
            return;

        ItemBase ib = ItemBase.getItemBase(rollRandomItem(ResourceTableID));
        if(ib != null){
            MobLoot toAdd = new MobLoot(mob,ib,false);
            mob.getCharItemManager().addItemToInventory(toAdd);
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
        if(mob.getObjectType().ordinal() == 52) { //52 = player character
            itemTableRoll = ThreadLocalRandom.current().nextInt(1,320 + 1);
        } else{
            itemTableRoll = TableRoll(mob.level);
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
            prefixTableRoll = TableRoll(mob.level);
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
            suffixTableRoll = TableRoll(mob.level);
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

    public static int TableRoll(int mobLevel) {

        int rank = (int)(mobLevel * 0.1f);
        int min = 50;
        int max = 100;
        switch(rank){
            case 1:
                min = 200;
                max = 250;
                break;
            case 2:
                min = 210;
                max = 275;
                break;
            case 3:
                min = 220;
                max = 300;
                break;
            case 4:
                min = 230;
                max = 320;
                break;
            case 5:
            case 6:
            case 7:
            case 8:
                min = 240;
                max = 320;
                break;
        }

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
        if(toAdd != null){
            ItemBase ib = toAdd.getItemBase();
            switch(ib.getType()){
                case CONTRACT:
                case RUNE:
                case RESOURCE:
                    return;
            }
            toAdd.setIsID(true);
            mob.getCharItemManager().addItemToInventory(toAdd);
        }
    }


    public static void GenerateEquipmentDrop(Mob mob) {

        if (mob == null || mob.getSafeZone())
            return; // no equipment to drop in safezones

        if(mob.StrongholdGuardian || mob.StrongholdCommander || mob.StrongholdEpic)
            return; // stronghold mobs don't drop equipment

        //do equipment here
        if (mob.getEquip() != null) {
            boolean isVorg = false;
            for (MobEquipment me : mob.getEquip().values()) {

                if (me.getDropChance() == 0)
                    continue;

                String name = me.getItemBase().getName().toLowerCase();
                if (name.contains("vorgrim legionnaire's") || name.contains("vorgrim auxiliary's") ||name.contains("bellugh nuathal") || name.contains("crimson circle"))
                    isVorg = true;

                if(isVorg && !mob.isDropper){
                    continue;
                }

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

        ItemBase present = ItemBase.getItemBase(random);
        if (present != null) {
            MobLoot toAdd = new MobLoot(mob, present, true);

            if (toAdd != null)
                mob.getCharItemManager().addItemToInventory(toAdd);
        }
    }

    public static void GenerateStrongholdLoot(Mob mob, boolean commander, boolean epic) {

        mob.getCharItemManager().clearInventory();

        int multiplier = 1;
        if (commander)
            multiplier = 2;
        if(epic)
            multiplier = 10;

        int high = 125000;
        int low = 50000;
        int gold = ThreadLocalRandom.current().nextInt(low, high + 1) * multiplier;

        if (gold > 0) {
            MobLoot goldAmount = new MobLoot(mob, gold);
            mob.getCharItemManager().addItemToInventory(goldAmount);
        }

        //present drop chance for all
        if (ThreadLocalRandom.current().nextInt(100) < 35)
            DropPresent(mob);

        //random contract drop chance for all
        if (ThreadLocalRandom.current().nextInt(100) < 40) {
            int contractTableID = 250;
            contractTableID += ThreadLocalRandom.current().nextInt(0, 11);
            if (contractTableID > 259)
                contractTableID = 659;

                int id = rollRandomItem(contractTableID);
                ItemBase ib = ItemBase.getItemBase(id);
                if (ib != null) {
                    MobLoot contract = new MobLoot(mob, ib, true);

                    if (contract != null)
                        mob.getCharItemManager().addItemToInventory(contract);
                }
        }

        //special commander drop chances
        if (commander)
            GenerateCommanderLoot(mob,false);

        //special epic drop chances
        if (epic) {
            GenerateCommanderLoot(mob, true);
            GenerateCommanderLoot(mob,false);
        }
    }

    public static void GenerateCommanderLoot(Mob mob, boolean epic){
        //present chance
        if (ThreadLocalRandom.current().nextInt(100) < 25)
            DropPresent(mob);

        //present chance
        if (ThreadLocalRandom.current().nextInt(100) < 25)
            DropPresent(mob);

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
        if(epic){
            int contractTableID = 250;
            contractTableID += ThreadLocalRandom.current().nextInt(0, 11);
            if (contractTableID > 259)
                contractTableID = 659;

            int id = rollRandomItem(contractTableID);
            ItemBase ib = ItemBase.getItemBase(id);
            if (ib != null) {
                MobLoot contract = new MobLoot(mob, ib, true);

                if (contract != null)
                    mob.getCharItemManager().addItemToInventory(contract);
            }
        }
    }
}
