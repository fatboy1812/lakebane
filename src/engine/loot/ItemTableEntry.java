// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com

package engine.loot;

import engine.gameManager.LootManager;
import org.pmw.tinylog.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ItemTableEntry {
    public int minRoll;
    public int maxRoll;
    public int cacheID;
    public int minSpawn;
    public int maxSpawn;

    public ItemTableEntry(ResultSet rs) throws SQLException {
        this.minRoll = rs.getInt("minRoll");
        this.maxRoll = rs.getInt("maxRoll");
        this.cacheID = rs.getInt("itemBaseUUID");
        this.minSpawn = rs.getInt("minSpawn");
        this.maxSpawn = rs.getInt("maxSpawn");
    }

    public static ItemTableEntry rollTable(int itemTable, int roll) {

        ItemTableEntry itemTableEntry = null;
        List<ItemTableEntry> itemTableEntryList;

        itemTableEntryList = LootManager._itemTables.get(itemTable);

        for (ItemTableEntry iteration : itemTableEntryList)
            if (roll >= iteration.minRoll && roll <= iteration.maxRoll)
                itemTableEntry = iteration;

        return itemTableEntry;
    }

    public static Integer getRandomItem(int itemTable) {
        int id = 0;
        List<ItemTableEntry> itemTableEntryList;

        itemTableEntryList = LootManager._itemTables.get(itemTable);

        if(itemTableEntryList != null && itemTableEntryList.size() > 0){
            id =  itemTableEntryList.get(ThreadLocalRandom.current().nextInt(1,itemTableEntryList.size() - 1)).cacheID;
        }
        return id;
    }
}
