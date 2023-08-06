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

    private static final ConcurrentHashMap<Integer, LootTable> lootGroups = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    private static final ConcurrentHashMap<Integer, LootTable> lootTables = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    private static final ConcurrentHashMap<Integer, LootTable> modTables = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    private static final ConcurrentHashMap<Integer, LootTable> modGroups = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
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

        if (lootGroups.containsKey(UUID))
            return lootGroups.get(UUID);

        LootTable lootGroup = new LootTable(UUID);
        lootGroups.put(UUID, lootGroup);
        return lootGroup;
    }

    public static LootTable getLootTable(int UUID) {

        if (lootTables.containsKey(UUID))
            return lootTables.get(UUID);

        LootTable lootTable = new LootTable(UUID);
        lootTables.put(UUID, lootTable);

        return lootTable;
    }

    /**
     * @return the lootGroups
     */
    public static ConcurrentHashMap<Integer, LootTable> getLootGroups() {
        return lootGroups;
    }

    /**
     * @return the lootTables
     */
    public static ConcurrentHashMap<Integer, LootTable> getLootTables() {
        return lootTables;
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
    public static ConcurrentHashMap<Integer, LootTable> getModGroups() {
        return modGroups;
    }

    public static LootTable getModGroup(int UUID) {

        if (modGroups.containsKey(UUID))
            return modGroups.get(UUID);

        LootTable modTable = new LootTable(UUID);
        modGroups.put(UUID, modTable);

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

        //preset chances for rune drops
        populateStatRuneChances();
    }

    public static int gaussianLevel(int level) {

        int ret = -76;

        while (ret < -75 || ret > 75) {
            ret = (int) (ThreadLocalRandom.current().nextGaussian() * 75);
        }

        return (level * 5) + ret;

    }

    //This set's the drop chances for stat runes.
    public static void populateStatRuneChances() {

        //+3, Increased
        statRuneChances.put(250018, 60);
        statRuneChances.put(250009, 60);
        statRuneChances.put(250027, 60);
        statRuneChances.put(250036, 60);
        statRuneChances.put(250000, 60);

        //+5, Enhanced
        statRuneChances.put(250019, 60);
        statRuneChances.put(250010, 60);
        statRuneChances.put(250028, 60);
        statRuneChances.put(250037, 60);
        statRuneChances.put(250001, 60);

        //+10 Exceptional
        statRuneChances.put(250020, 60);
        statRuneChances.put(250011, 60);
        statRuneChances.put(250029, 60);
        statRuneChances.put(250038, 60);
        statRuneChances.put(250002, 60);

        //+15, Amazing
        statRuneChances.put(250021, 60);
        statRuneChances.put(250012, 60);
        statRuneChances.put(250030, 60);
        statRuneChances.put(250039, 60);
        statRuneChances.put(250003, 60);

        //+20, Incredible
        statRuneChances.put(250022, 60);
        statRuneChances.put(250013, 60);
        statRuneChances.put(250031, 60);
        statRuneChances.put(250040, 60);
        statRuneChances.put(250004, 60);

        //+25, Great
        statRuneChances.put(250023, 60);
        statRuneChances.put(250014, 60);
        statRuneChances.put(250032, 60);
        statRuneChances.put(250041, 60);
        statRuneChances.put(250005, 60);

        //+30, Heroic
        statRuneChances.put(250024, 60);
        statRuneChances.put(250015, 60);
        statRuneChances.put(250033, 60);
        statRuneChances.put(250042, 60);
        statRuneChances.put(250006, 60);

        //+35, Legendary
        statRuneChances.put(250025, 60);
        statRuneChances.put(250016, 60);
        statRuneChances.put(250034, 60);
        statRuneChances.put(250043, 60);
        statRuneChances.put(250007, 60);

        //+40, of the Gods
        statRuneChances.put(250026, 60);
        statRuneChances.put(250017, 60);
        statRuneChances.put(250035, 60);
        statRuneChances.put(250044, 60);
        statRuneChances.put(250008, 60);
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

        LootTable lootGroup = LootTable.lootGroups.get(groupID);

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

        lootTable = LootTable.lootTables.get(groupRow.getValueOne());
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
            modGroup = LootTable.modGroups.get(groupRow.getValueTwo());

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
            modGroup = LootTable.modGroups.get(groupRow.getValueThree());

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
            modGroup = LootTable.modGroups.get(groupRow.getValueTwo());

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
            modGroup = LootTable.modGroups.get(groupRow.getValueThree());

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
