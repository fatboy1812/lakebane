// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com

package engine.loot;

import engine.gameManager.LootManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class GenTableEntry {
    public int minRoll;
    public int maxRoll;
    public int itemTableID;
    public int pModTable;
    public int sModTable;

    public GenTableEntry(ResultSet rs) throws SQLException {
        this.minRoll = rs.getInt("minRoll");
        this.maxRoll = rs.getInt("maxRoll");
        this.itemTableID = rs.getInt("itemTableID");
        this.pModTable = rs.getInt("pModTableID");
        this.sModTable = rs.getInt("sModTableID");
    }

    public static GenTableEntry rollTable(int genTable, int roll, float dropRate) {

        GenTableEntry genTableEntry = null;
        List<GenTableEntry> genTableEntryList;

        genTableEntryList = LootManager._genTables.get(genTable);

        for (GenTableEntry iteration : genTableEntryList)
            if (roll >= iteration.minRoll && roll <= iteration.maxRoll)
                genTableEntry = iteration;

        return genTableEntry;
    }
}
