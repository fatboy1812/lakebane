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

public class ModTableEntry {
    public int minRoll;
    public int maxRoll;
    public String action;
    public int level;

    public ModTableEntry(ResultSet rs) throws SQLException {
        this.minRoll = rs.getInt("minRoll");
        this.maxRoll = rs.getInt("maxRoll");
        this.action = rs.getString("action");
        this.level = rs.getInt("level");
    }

    public static ModTableEntry rollTable(int modTablwe, int roll) {

        ModTableEntry modTableEntry = null;
        List<ModTableEntry> itemTableEntryList;

        itemTableEntryList = LootManager._modTables.get(modTablwe);

        for (ModTableEntry iteration : itemTableEntryList)
            if (roll >= iteration.minRoll && roll <= iteration.maxRoll)
                modTableEntry = iteration;

        return modTableEntry;
    }
}
