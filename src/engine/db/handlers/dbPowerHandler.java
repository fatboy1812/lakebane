// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.gameManager.DbManager;
import engine.objects.Mob;
import engine.powers.MobPowerEntry;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class dbPowerHandler extends dbHandlerBase {

    public dbPowerHandler() {
        this.localClass = Mob.class;
        this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public HashMap<Integer, ArrayList<MobPowerEntry>> LOAD_MOB_POWERS() {

        HashMap<Integer, ArrayList<MobPowerEntry>> mobPowers = new HashMap<>();
        MobPowerEntry mobPowerEntry;

        int mobbaseID;
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_npc_mobbase_powers")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                recordsRead++;

                mobbaseID = rs.getInt("mobbaseID");
                mobPowerEntry = new MobPowerEntry(rs);

                if (mobPowers.get(mobbaseID) == null) {
                    ArrayList<MobPowerEntry> powerList = new ArrayList<>();
                    powerList.add(mobPowerEntry);
                    mobPowers.put(mobbaseID, powerList);
                } else {
                    ArrayList<MobPowerEntry> powerList = mobPowers.get(mobbaseID);
                    powerList.add(mobPowerEntry);
                    mobPowers.put(mobbaseID, powerList);
                }
            }
        } catch (SQLException e) {
            Logger.error(e);
            return mobPowers;
        }

        Logger.info("read: " + recordsRead + " cached: " + mobPowers.size());
        return mobPowers;
    }

}
