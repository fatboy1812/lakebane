// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import java.util.HashMap;

public enum MineProduction {

    LUMBER("Lumber Camp", new HashMap<>(), 1618637196, 1663491950),
    ORE("Ore Mine", new HashMap<>(), 518103023, -788976428),
    MAGIC("Magic Mine", new HashMap<>(), 504746863, -1753567069),
    GOLDMINE("Gold Mine", new HashMap<>(), -662193002, -1227205358);

    public final String name;
    public final HashMap<Integer, Resource> resources;
    public final int hash;
    public final int xpacHash;

    MineProduction(String name, HashMap<Integer, Resource> resources,int hash, int xpacHash) {
        this.name = name;
        this.resources = resources;
        this.hash = hash;
        this.xpacHash = xpacHash;
    }

    public static void addResources() {
        if (MineProduction.LUMBER.resources.size() == 0) {
            MineProduction.LUMBER.resources.put(7, Resource.GOLD);
            MineProduction.LUMBER.resources.put(1580004, Resource.LUMBER);
            MineProduction.LUMBER.resources.put(1580005, Resource.OAK);
            MineProduction.LUMBER.resources.put(1580006, Resource.BRONZEWOOD);
            MineProduction.LUMBER.resources.put(1580007, Resource.MANDRAKE);
            MineProduction.LUMBER.resources.put(1580018, Resource.WORMWOOD);
        }
        if (MineProduction.ORE.resources.size() == 0) {
            MineProduction.ORE.resources.put(7, Resource.GOLD);
            MineProduction.ORE.resources.put(1580000, Resource.STONE);
            MineProduction.ORE.resources.put(1580001, Resource.TRUESTEEL);
            MineProduction.ORE.resources.put(1580002, Resource.IRON);
            MineProduction.ORE.resources.put(1580003, Resource.ADAMANT);
            MineProduction.ORE.resources.put(1580019, Resource.OBSIDIAN);
        }
        if (MineProduction.GOLDMINE.resources.size() == 0) {
            MineProduction.GOLDMINE.resources.put(7, Resource.GOLD);
            MineProduction.GOLDMINE.resources.put(1580000, Resource.STONE);
            MineProduction.GOLDMINE.resources.put(1580008, Resource.COAL);
            MineProduction.GOLDMINE.resources.put(1580009, Resource.AGATE);
            MineProduction.GOLDMINE.resources.put(1580010, Resource.DIAMOND);
            MineProduction.GOLDMINE.resources.put(1580011, Resource.ONYX);
            MineProduction.GOLDMINE.resources.put(1580017, Resource.GALVOR);
        }
        if (MineProduction.MAGIC.resources.size() == 0) {
            MineProduction.MAGIC.resources.put(7, Resource.GOLD);
            MineProduction.MAGIC.resources.put(1580012, Resource.AZOTH);
            MineProduction.MAGIC.resources.put(1580013, Resource.ORICHALK);
            MineProduction.MAGIC.resources.put(1580014, Resource.ANTIMONY);
            MineProduction.MAGIC.resources.put(1580015, Resource.SULFUR);
            MineProduction.MAGIC.resources.put(1580016, Resource.QUICKSILVER);
            MineProduction.MAGIC.resources.put(1580020, Resource.BLOODSTONE);
        }
    }

    public static MineProduction getByName(String name) {
        if (name.equalsIgnoreCase("lumber"))
            return MineProduction.LUMBER;
        else if (name.equalsIgnoreCase("ore"))
            return MineProduction.ORE;
        else if (name.equalsIgnoreCase("gold"))
            return MineProduction.GOLDMINE;
        else
            return MineProduction.MAGIC;
    }

    public boolean validForMine(Resource r) {
        if (r == null)
            return false;
        return this.resources.containsKey(r.UUID);
    }


//Name			Xpac		Resources
//Lumber Camp	Wormwood	Gold, Lumber, Oak, Bronzewood, Mandrake
//Ore Mine		Obsidian	Gold, Stone, Truesteal, Iron, Adamant
//Gold Mine		Galvor		Gold, Coal, Agate, Diamond, Onyx
//Magic Mine	Bloodstone	Gold, Orichalk, Azoth, Antimony, Quicksilver, Sulfer
}
