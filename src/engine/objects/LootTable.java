// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import engine.Enum.ItemContainerType;
import engine.Enum.ItemType;
import engine.Enum.OwnerType;
import engine.gameManager.DbManager;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class LootTable {

    private static final ConcurrentHashMap<Integer, LootTable> genTables = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    private static final ConcurrentHashMap<Integer, LootTable> itemTables = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    private static final ConcurrentHashMap<Integer, LootTable> modTables = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    private static final ConcurrentHashMap<Integer, LootTable> modTypeTables = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    private static final ConcurrentHashMap<Integer, Integer> statRuneChances = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    public static boolean initialized = false;
    public static HashMap<ItemBase, Integer> itemsDroppedMap = new HashMap<>();
    public static HashMap<ItemBase, Integer> resourceDroppedMap = new HashMap<>();
    public static HashMap<ItemBase, Integer> runeDroppedMap = new HashMap<>();
    public static HashMap<ItemBase, Integer> contractDroppedMap = new HashMap<>();
    public static HashMap<ItemBase, Integer> glassDroppedMap = new HashMap<>();
    public static int rollCount = 0;
    public static int dropCount = 0;
    public static int runeCount = 0;
    public static int contractCount = 0;
    public static int resourceCount = 0;
    public static int glassCount = 0;
    private final ConcurrentHashMap<Integer, LootRow> lootTable = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    public float minRoll = 320;
    public float maxRoll = 1;
    public int lootTableID = 0;


    /**
     * Generic Constructor
     */
    public LootTable(int lootTableID) {
        this.lootTableID = lootTableID;
    }

    public static LootTable getGenTable(int UUID) {

        if (genTables.containsKey(UUID))
            return genTables.get(UUID);

        LootTable lootGroup = new LootTable(UUID);
        genTables.put(UUID, lootGroup);
        return lootGroup;
    }

    public static LootTable getItemTable(int UUID) {

        if (itemTables.containsKey(UUID))
            return itemTables.get(UUID);

        LootTable lootTable = new LootTable(UUID);
        itemTables.put(UUID, lootTable);

        return lootTable;
    }

    /**
     * @return the lootGroups
     */
    public static ConcurrentHashMap<Integer, LootTable> getGenTables() {
        return genTables;
    }

    /**
     * @return the lootTables
     */
    public static ConcurrentHashMap<Integer, LootTable> getItemTables() {
        return itemTables;
    }

    /**
     * @return the modTables
     */
    public static ConcurrentHashMap<Integer, LootTable> getModTables() {
        return modTables;
    }

    /**
     * @return the modGroups
     */
    public static ConcurrentHashMap<Integer, LootTable> getModTypeTables() {
        return modTypeTables;
    }

    public static LootTable getModTypeTable(int UUID) {

        if (modTypeTables.containsKey(UUID))
            return modTypeTables.get(UUID);

        LootTable modTable = new LootTable(UUID);
        modTypeTables.put(UUID, modTable);

        return modTable;
    }

    public static LootTable getModTable(int UUID) {

        if (modTables.containsKey(UUID))
            return modTables.get(UUID);

        LootTable modTypeTable = new LootTable(UUID);
        modTables.put(UUID, modTypeTable);

        return modTypeTable;
    }

    //call this on server startup to populate the tables
    public static void populateLootTables() {

        DbManager.LootQueries.populateGenTables();
        DbManager.LootQueries.populateItemTables();
        DbManager.LootQueries.populateModTables();
        DbManager.LootQueries.populateModTypeTables();

    }

    public static int gaussianLevel(int level) {

        int ret = -76;

        while (ret < -75 || ret > 75) {
            ret = (int) (ThreadLocalRandom.current().nextGaussian() * 75);
        }

        return (level * 5) + ret;

    }

    public static Item CreateGamblerItem(Item item, PlayerCharacter gambler) {

        if (item == null)
            return null;

        int groupID = 0;

        switch (item.getItemBase().getUUID()) {
            case 971050: //Wrapped Axe
                groupID = 3000;
                break;
            case 971051://Wrapped Great Axe
                groupID = 3005;
                break;
            case 971052://Wrapped Throwing Axe
                groupID = 3010;
                break;
            case 971053://	Wrapped Bow
                groupID = 3015;
                break;
            case 971054://Wrapped Crossbow
                groupID = 3020;
                break;
            case 971055:    //Wrapped Dagger
                groupID = 3025;
                break;
            case 971056:    //	Wrapped Throwing Dagger
                groupID = 3030;
                break;
            case 971057:    //	Wrapped Hammer
                groupID = 3035;
                break;
            case 971058://			Wrapped Great Hammer
                groupID = 3040;
                break;
            case 971059://			Wrapped Throwing Hammer
                groupID = 3045;
                break;
            case 971060://			Wrapped Polearm
                groupID = 3050;
                break;
            case 971061://			Wrapped Spear
                groupID = 3055;
                break;
            case 971062://			Wrapped Staff
                groupID = 3060;
                break;
            case 971063://			Wrapped Sword
                groupID = 3065;
                break;
            case 971064://			Wrapped Great Sword
                groupID = 3070;
                break;
            case 971065://			Wrapped Unarmed Weapon
                groupID = 3075;
                break;
            case 971066://			Wrapped Cloth Armor
                groupID = 3100;
                break;
            case 971067://			Wrapped Light Armor
                groupID = 3105;
                break;
            case 971068://			Wrapped Medium Armor
                groupID = 3110;
                break;
            case 971069://			Wrapped Heavy Armor
                groupID = 3115;
                break;
            case 971070://			Wrapped Rune
                groupID = 3200;
                break;
            case 971071://			Wrapped City Improvement
                groupID = 3210;
                break;
        }
        //couldnt find group

        if (groupID == 0)
            return null;

        LootTable lootGroup = LootTable.genTables.get(groupID);

        if (lootGroup == null)
            return null;

        float calculatedMobLevel;
        int minSpawn;
        int maxSpawn;
        int spawnQuanity = 0;
        int subTableID;
        String modifierPrefix = "";
        String modifierSuffix = "";

        // Lookup Table Variables

        LootTable lootTable;
        LootRow lootRow;
        LootRow groupRow = null;
        LootTable modTable;
        LootTable modGroup;
        LootRow modRow = null;

        // Used for actual generation of items

        int itemBaseUUID;
        ItemBase itemBase = null;

        int roll = ThreadLocalRandom.current().nextInt(100) + 1; //Does not return Max, but does return min?

        groupRow = lootGroup.getLootRow(roll);

        lootTable = LootTable.itemTables.get(groupRow.getValueOne());
        roll = ThreadLocalRandom.current().nextInt(100) + 1;
        lootRow = lootTable.getLootRow(roll + 220); //get the item row from the bell's curve of level +-15

        if (lootRow == null)
            return null; //no item found for roll

        itemBaseUUID = lootRow.getValueOne();

        if (lootRow.getValueOne() == 0)
            return null;

        //handle quantities > 1 for resource drops

        minSpawn = lootRow.getValueTwo();
        maxSpawn = lootRow.getValueThree();

        // spawnQuanity between minspawn (inclusive) and maxspawn (inclusive)

        if (maxSpawn > 1)
            spawnQuanity = ThreadLocalRandom.current().nextInt((maxSpawn + 1 - minSpawn)) + minSpawn;

        //get modifierPrefix

        calculatedMobLevel = 49;

        int chanceMod = ThreadLocalRandom.current().nextInt(100) + 1;

        if (chanceMod < 25) {
            modGroup = LootTable.modTypeTables.get(groupRow.getValueTwo());

            if (modGroup != null) {

                for (int a = 0; a < 10; a++) {
                    roll = ThreadLocalRandom.current().nextInt(100) + 1;
                    modRow = modGroup.getLootRow(roll);
                    if (modRow != null)
                        break;
                }

                if (modRow != null) {
                    subTableID = modRow.getValueOne();

                    if (LootTable.modTables.containsKey(subTableID)) {

                        modTable = LootTable.modTables.get(subTableID);

                        roll = gaussianLevel((int) calculatedMobLevel);

                        if (roll < modTable.minRoll)
                            roll = (int) modTable.minRoll;

                        if (roll > modTable.maxRoll)
                            roll = (int) modTable.maxRoll;

                        modRow = modTable.getLootRow(roll);

                        if (modRow != null)
                            modifierPrefix = modRow.getAction();
                    }
                }
            }
        } else if (chanceMod < 50) {
            modGroup = LootTable.modTypeTables.get(groupRow.getValueThree());

            if (modGroup != null) {

                for (int a = 0; a < 10; a++) {
                    roll = ThreadLocalRandom.current().nextInt(100) + 1;
                    modRow = modGroup.getLootRow(roll);
                    if (modRow != null)
                        break;
                }

                if (modRow != null) {

                    subTableID = modRow.getValueOne();

                    if (LootTable.modTables.containsKey(subTableID)) {

                        modTable = LootTable.modTables.get(subTableID);
                        roll = gaussianLevel((int) calculatedMobLevel);

                        if (roll < modTable.minRoll)
                            roll = (int) modTable.minRoll;

                        if (roll > modTable.maxRoll)
                            roll = (int) modTable.maxRoll;

                        modRow = modTable.getLootRow(roll);

                        if (modRow == null)
                            modRow = modTable.getLootRow((int) ((modTable.minRoll + modTable.maxRoll) * .05f));

                        if (modRow != null)
                            modifierSuffix = modRow.getAction();
                    }
                }
            }
        } else {
            modGroup = LootTable.modTypeTables.get(groupRow.getValueTwo());

            if (modGroup != null) {

                for (int a = 0; a < 10; a++) {
                    roll = ThreadLocalRandom.current().nextInt(100) + 1;
                    modRow = modGroup.getLootRow(roll);
                    if (modRow != null)
                        break;
                }

                if (modRow != null) {

                    subTableID = modRow.getValueOne();

                    if (LootTable.modTables.containsKey(subTableID)) {

                        modTable = LootTable.modTables.get(subTableID);
                        roll = gaussianLevel((int) calculatedMobLevel);

                        if (roll < modTable.minRoll)
                            roll = (int) modTable.minRoll;

                        if (roll > modTable.maxRoll)
                            roll = (int) modTable.maxRoll;

                        modRow = modTable.getLootRow(roll);

                        if (modRow == null)
                            modRow = modTable.getLootRow((int) ((modTable.minRoll + modTable.maxRoll) * .05f));

                        if (modRow != null)
                            modifierPrefix = modRow.getAction();
                    }
                }
            }

            //get modifierSuffix
            modGroup = LootTable.modTypeTables.get(groupRow.getValueThree());

            if (modGroup != null) {

                for (int a = 0; a < 10; a++) {
                    roll = ThreadLocalRandom.current().nextInt(100) + 1;
                    modRow = modGroup.getLootRow(roll);
                    if (modRow != null)
                        break;
                }

                if (modRow != null) {

                    subTableID = modRow.getValueOne();

                    if (LootTable.modTables.containsKey(subTableID)) {

                        modTable = LootTable.modTables.get(subTableID);
                        roll = gaussianLevel((int) calculatedMobLevel);

                        if (roll < modTable.minRoll)
                            roll = (int) modTable.minRoll;

                        if (roll > modTable.maxRoll)
                            roll = (int) modTable.maxRoll;

                        modRow = modTable.getLootRow(roll);

                        if (modRow == null)
                            modRow = modTable.getLootRow((int) ((modTable.minRoll + modTable.maxRoll) * .05f));

                        if (modRow != null)
                            modifierSuffix = modRow.getAction();
                    }
                }
            }
        }

        itemBase = ItemBase.getItemBase(itemBaseUUID);
        byte charges = (byte) itemBase.getNumCharges();
        short dur = (short) itemBase.getDurability();

        short weight = itemBase.getWeight();

        if (!gambler.getCharItemManager().hasRoomInventory(weight))
            return null;

        Item gambledItem = new Item(itemBase, gambler.getObjectUUID(),
                OwnerType.PlayerCharacter, charges, charges, dur, dur,
                true, false, ItemContainerType.INVENTORY, (byte) 0,
                new ArrayList<>(), "");

        if (spawnQuanity == 0 && itemBase.getType().equals(ItemType.RESOURCE))
            spawnQuanity = 1;

        if (spawnQuanity > 0)
            item.setNumOfItems(spawnQuanity);

        try {
            gambledItem = DbManager.ItemQueries.ADD_ITEM(gambledItem);
        } catch (Exception e) {
            Logger.error(e);
        }

        if (gambledItem == null)
            return null;

        if (!modifierPrefix.isEmpty())
            gambledItem.addPermanentEnchantment(modifierPrefix, 0);

        if (!modifierSuffix.isEmpty())
            gambledItem.addPermanentEnchantment(modifierSuffix, 0);

        //add item to inventory

        gambler.getCharItemManager().addItemToInventory(gambledItem);
        gambler.getCharItemManager().updateInventory();

        return gambledItem;
    }

    public void addRow(float min, float max, int valueOne, int valueTwo, int valueThree, String action) {

        //hackey way to set the minimum roll for SHIAT!

        if (min < this.minRoll)
            this.minRoll = min;

        if (max > this.maxRoll)
            this.maxRoll = max;

        int minInt = (int) min;
        int maxInt = (int) max;

        //Round up min

        if (minInt != min)
            min = minInt + 1;

        //Round down max;

        if (maxInt != max)
            max = maxInt;

        LootRow lootRow = new LootRow(valueOne, valueTwo, valueThree, action);

        for (int i = (int) min; i <= max; i++)
            lootTable.put(i, lootRow);
    }

    public LootRow getLootRow(int probability) {

        if (lootTable.containsKey(probability))
            return lootTable.get(probability);

        return null;
    }

}
