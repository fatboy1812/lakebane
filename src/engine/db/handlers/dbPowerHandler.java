// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum;
import engine.gameManager.DbManager;
import engine.gameManager.PowersManager;
import engine.objects.Mob;
import engine.objects.PreparedStatementShared;
import engine.powers.EffectsBase;
import engine.powers.MobPowerEntry;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class dbPowerHandler extends dbHandlerBase {

    public dbPowerHandler() {
        this.localClass = Mob.class;
        this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public static void addAllSourceTypes() {
        PreparedStatementShared ps = null;
        try {
            ps = new PreparedStatementShared("SELECT * FROM static_power_sourcetype");
            ResultSet rs = ps.executeQuery();
            String IDString, source;
            while (rs.next()) {
                IDString = rs.getString("IDString");
                int token = DbManager.hasher.SBStringHash(IDString);


                source = rs.getString("source").replace("-", "").trim();
                Enum.EffectSourceType effectSourceType = Enum.EffectSourceType.GetEffectSourceType(source);

                if (EffectsBase.effectSourceTypeMap.containsKey(token) == false)
                    EffectsBase.effectSourceTypeMap.put(token, new HashSet<>());

                EffectsBase.effectSourceTypeMap.get(token).add(effectSourceType);
            }
            rs.close();
        } catch (Exception e) {
            Logger.error(e);
        } finally {
            ps.release();
        }
    }

    public static void addAllAnimationOverrides() {
        PreparedStatementShared ps = null;
        try {
            ps = new PreparedStatementShared("SELECT * FROM static_power_animation_override");
            ResultSet rs = ps.executeQuery();
            String IDString;
            int animation;
            while (rs.next()) {
                IDString = rs.getString("IDString");

                EffectsBase eb = PowersManager.getEffectByIDString(IDString);
                if (eb != null)
                    IDString = eb.getIDString();

                animation = rs.getInt("animation");
                PowersManager.AnimationOverrides.put(IDString, animation);

            }
            rs.close();
        } catch (Exception e) {
            Logger.error(e);
        } finally {
            ps.release();
        }
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

                mobbaseID = rs.getInt("mobbaseUUID");
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
