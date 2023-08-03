package engine.loot;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ModTableRow {
    public int minRoll;
    public int maxRoll;
    public String action;
    public int level;

    public ModTableRow(ResultSet rs) throws SQLException {
        this.minRoll = rs.getInt("minRoll");
        this.maxRoll = rs.getInt("maxRoll");
        this.action = rs.getString("action");
        this.level = rs.getInt("level");
    }
}
