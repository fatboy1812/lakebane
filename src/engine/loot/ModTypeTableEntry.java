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

public class ModTypeTableEntry {
    public int minRoll;
    public int maxRoll;
    public int modTableID;

    public ModTypeTableEntry(ResultSet rs) throws SQLException {
        this.minRoll = rs.getInt("minRoll");
        this.maxRoll = rs.getInt("maxRoll");
        this.modTableID = rs.getInt("subTableID");
    }

    public static ModTypeTableEntry rollTable(int modTablwe, int roll) {

        ModTypeTableEntry modTypeTableEntry = null;
        List<ModTypeTableEntry> modTypeTableEntryList;

        modTypeTableEntryList = LootManager._modTypeTables.get(modTablwe);

        for (ModTypeTableEntry iteration : modTypeTableEntryList)
            if (roll >= iteration.minRoll && roll <= iteration.maxRoll)
                modTypeTableEntry = iteration;

        return modTypeTableEntry;
    }
}
