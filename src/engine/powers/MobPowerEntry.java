package engine.powers;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MobPowerEntry {

    public int token;
    public int rank;


    public MobPowerEntry(ResultSet rs) throws SQLException {
        this.token = rs.getInt("token");
        this.rank = rs.getInt("rank");
    }

}
