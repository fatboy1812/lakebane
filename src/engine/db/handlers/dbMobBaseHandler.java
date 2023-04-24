// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum.GameObjectType;
import engine.gameManager.DbManager;
import engine.objects.*;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class dbMobBaseHandler extends dbHandlerBase {

	public dbMobBaseHandler() {
		this.localClass = MobBase.class;
		this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
	}

    public MobBase GET_MOBBASE(int id, boolean forceDB) {


		if (id == 0)
			return null;

		MobBase mobBase = (MobBase) DbManager.getFromCache(GameObjectType.MobBase, id);

		if ( mobBase != null)
			return mobBase;

		prepareCallable("SELECT * FROM `static_npc_mobbase` WHERE `ID`=?");
		setInt(1, id);
		return (MobBase) getObjectSingle(id, forceDB, true);
	}

	public ArrayList<MobBase> GET_ALL_MOBBASES() {
		prepareCallable("SELECT * FROM `static_npc_mobbase`;");
		return  getObjectList();
	}

	public void SET_AI_DEFAULTS() {
		prepareCallable("SELECT * FROM `static_ai_defaults`");
		try {
			ResultSet rs = executeQuery();
			while (rs.next()) {
				MBServerStatics.AI_BASE_AGGRO_RANGE = rs.getInt("aggro_range");
				MBServerStatics.AI_PATROL_DIVISOR = rs.getInt("patrol_chance");
				MBServerStatics.AI_DROP_AGGRO_RANGE = rs.getInt("drop_aggro_range");
				MBServerStatics.AI_POWER_DIVISOR = rs.getInt("cast_chance");
				MBServerStatics.AI_RECALL_RANGE = rs.getInt("recall_range");
				MBServerStatics.AI_PET_HEEL_DISTANCE = rs.getInt("pet_heel_distance");
			}
			rs.close();
		} catch (SQLException e) {
			Logger.error( e.getMessage());
		} finally {
			closeCallable();
		}

	}

	public boolean UPDATE_AI_DEFAULTS() {
		prepareCallable("UPDATE `static_ai_defaults` SET `aggro_range` = ?,`patrol_chance`= ?,`drop_aggro_range`= ?,`cast_chance`= ?,`recall_range`= ? WHERE `ID` = 1");
		setInt(1, MBServerStatics.AI_BASE_AGGRO_RANGE);
		setInt(2, MBServerStatics.AI_PATROL_DIVISOR);
		setInt(3, MBServerStatics.AI_DROP_AGGRO_RANGE);
		setInt(4, MBServerStatics.AI_POWER_DIVISOR);
		setInt(5, MBServerStatics.AI_RECALL_RANGE);
		return (executeUpdate() > 0);

	}

	public HashMap<Integer, Integer> LOAD_STATIC_POWERS(int mobBaseUUID) {
		HashMap<Integer, Integer> powersList = new HashMap<>();
		prepareCallable("SELECT * FROM `static_npc_mobbase_powers` WHERE `mobbaseUUID`=?");
		setInt(1, mobBaseUUID);
		try {
			ResultSet rs = executeQuery();
			while (rs.next()) {

				powersList.put(rs.getInt("token"), rs.getInt("rank"));
			}
			rs.close();
		} catch (SQLException e) {
			Logger.error( e.getMessage());
		} finally {
			closeCallable();
		}
		return powersList;

	}

	public ArrayList<MobBaseEffects> GET_RUNEBASE_EFFECTS(int runeID) {
		ArrayList<MobBaseEffects> effectsList = new ArrayList<>();
		prepareCallable("SELECT * FROM `static_npc_mobbase_effects` WHERE `mobbaseUUID` = ?");
		setInt(1, runeID);

		try {
			ResultSet rs = executeQuery();
			while (rs.next()) {

				MobBaseEffects mbs = new MobBaseEffects(rs);
				effectsList.add(mbs);
			}
			rs.close();
		} catch (SQLException e) {
			Logger.error (e.getMessage());
		} finally {
			closeCallable();
		}

		return effectsList;

	}

	public MobBaseStats LOAD_STATS(int mobBaseUUID) {
		MobBaseStats mbs = MobBaseStats.GetGenericStats();

		prepareCallable("SELECT * FROM `static_npc_mobbase_stats` WHERE `mobbaseUUID` = ?");
		setInt(1, mobBaseUUID);
		try {
			ResultSet rs = executeQuery();
			while (rs.next()) {

				mbs = new MobBaseStats(rs);
			}

		} catch (SQLException e) {
			Logger.error(e.getErrorCode() + ' ' + e.getMessage(), e);
		} finally {
			closeCallable();
		}
		return mbs;

	}

	public boolean RENAME_MOBBASE(int ID, String newName) {
		prepareCallable("UPDATE `static_npc_mobbase` SET `name`=? WHERE `ID`=?;");
		setString(1, newName);
		setInt(2, ID);
		return (executeUpdate() > 0);
	}

	public void LOAD_ALL_MOBBASE_SPEEDS(MobBase mobBase) {

		if (mobBase.getLoadID() == 0)
			return;
		ArrayList<MobLootBase> mobLootList = new ArrayList<>();
		prepareCallable("SELECT * FROM `static_npc_mobbase_race` WHERE `mobbaseID` = ?");
		setInt(1, mobBase.getLoadID());

		try {
			ResultSet rs = executeQuery();

			//shrines cached in rs for easy cache on creation.
			while (rs.next()) {
				float walk = rs.getFloat("walkStandard");
				float walkCombat = rs.getFloat("walkCombat");
				float run = rs.getFloat("runStandard");
				float runCombat = rs.getFloat("runCombat");
				mobBase.updateSpeeds(walk, walkCombat, run, runCombat);
			}


		} catch (SQLException e) {
			Logger.error(e.getErrorCode() + ' ' + e.getMessage(), e);
		} finally {
			closeCallable();
		}
	}
}
