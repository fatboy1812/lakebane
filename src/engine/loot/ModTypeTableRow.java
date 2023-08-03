package engine.loot;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ModTypeTableRow {
    public int minRoll;
    public int maxRoll;
    public int modTableID;

    public ModTypeTableRow(ResultSet rs) throws SQLException {
        this.minRoll = rs.getInt("minRoll");
        this.maxRoll = rs.getInt("maxRoll");
        this.modTableID = rs.getInt("subTableID");
    }
}
