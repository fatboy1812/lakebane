package engine.loot;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ItemTableRow {
    public int minRoll;
    public int maxRoll;
    public int cacheID;
    public int minSpawn;
    public int maxSpawn;

    public ItemTableRow(ResultSet rs) throws SQLException {
        this.minRoll = rs.getInt("minRoll");
        this.maxRoll = rs.getInt("maxRoll");
        this.cacheID = rs.getInt("itemBaseUUID");
        this.minSpawn = rs.getInt("minSpawn");
        this.maxSpawn = rs.getInt("maxSpawn");
    }
}
