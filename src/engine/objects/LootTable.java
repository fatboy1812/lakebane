// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import engine.server.MBServerStatics;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

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

    public LootRow getLootRow(int probability) {

        if (lootTable.containsKey(probability))
            return lootTable.get(probability);

        return null;
    }

}
