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
import engine.objects.MobBase;
import engine.objects.MobBaseEffects;
import engine.objects.MobBaseStats;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class dbMobBaseHandler extends dbHandlerBase {

	public dbMobBaseHandler() {
		this.localClass = MobBase.class;
		this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
	}

	public MobBase GET_MOBBASE(int id) {

		if (id == 0)
			return null;

		MobBase mobBase = (MobBase) DbManager.getFromCache(GameObjectType.MobBase, id);

		if (mobBase != null)
			return mobBase;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_npc_mobbase` WHERE `ID`=?")) {

			preparedStatement.setInt(1, id);

			ResultSet rs = preparedStatement.executeQuery();
			mobBase = (MobBase) getObjectFromRs(rs);

		} catch (SQLException e) {
			Logger.error(e);
		}

		return mobBase;
	}


	public ArrayList<MobBase> GET_ALL_MOBBASES() {

		ArrayList<MobBase> mobbaseList = new ArrayList<>();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_npc_mobbase`;")) {

			ResultSet rs = preparedStatement.executeQuery();
			mobbaseList = getObjectsFromRs(rs, 1000);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return mobbaseList;
	}

	public void SET_AI_DEFAULTS() {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_ai_defaults`")) {

			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {
				MBServerStatics.AI_BASE_AGGRO_RANGE = rs.getInt("aggro_range");
				MBServerStatics.AI_PATROL_DIVISOR = rs.getInt("patrol_chance");
				MBServerStatics.AI_DROP_AGGRO_RANGE = rs.getInt("drop_aggro_range");
				MBServerStatics.AI_POWER_DIVISOR = rs.getInt("cast_chance");
				MBServerStatics.AI_RECALL_RANGE = rs.getInt("recall_range");
				MBServerStatics.AI_PET_HEEL_DISTANCE = rs.getInt("pet_heel_distance");
			}
		} catch (SQLException e) {
			Logger.error(e);
		}
	}

	public boolean UPDATE_AI_DEFAULTS() {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `static_ai_defaults` SET `aggro_range` = ?,`patrol_chance`= ?,`drop_aggro_range`= ?,`cast_chance`= ?,`recall_range`= ? WHERE `ID` = 1")) {

			preparedStatement.setInt(1, MBServerStatics.AI_BASE_AGGRO_RANGE);
			preparedStatement.setInt(2, MBServerStatics.AI_PATROL_DIVISOR);
			preparedStatement.setInt(3, MBServerStatics.AI_DROP_AGGRO_RANGE);
			preparedStatement.setInt(4, MBServerStatics.AI_POWER_DIVISOR);
			preparedStatement.setInt(5, MBServerStatics.AI_RECALL_RANGE);

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public HashMap<Integer, Integer> LOAD_STATIC_POWERS(int mobBaseUUID) {

		HashMap<Integer, Integer> powersList = new HashMap<>();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_npc_mobbase_powers` WHERE `mobbaseUUID`=?")) {

			preparedStatement.setInt(1, mobBaseUUID);
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next())
				powersList.put(rs.getInt("token"), rs.getInt("rank"));

		} catch (SQLException e) {
			Logger.error(e);
		}
		return powersList;
	}

	public ArrayList<MobBaseEffects> GET_RUNEBASE_EFFECTS(int runeID) {

		ArrayList<MobBaseEffects> effectsList = new ArrayList<>();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_npc_mobbase_effects` WHERE `mobbaseUUID` = ?")) {

			preparedStatement.setInt(1, runeID);
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {
				MobBaseEffects mbs = new MobBaseEffects(rs);
				effectsList.add(mbs);
			}

		} catch (SQLException e) {
			Logger.error(e);
		}

		return effectsList;
	}

	public MobBaseStats LOAD_STATS(int mobBaseUUID) {

		MobBaseStats mobBaseStats = MobBaseStats.GetGenericStats();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_npc_mobbase_stats` WHERE `mobbaseUUID` = ?")) {

			preparedStatement.setInt(1, mobBaseUUID);
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next())
				mobBaseStats = new MobBaseStats(rs);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return mobBaseStats;
	}

	public void LOAD_ALL_MOBBASE_SPEEDS(MobBase mobBase) {

		if (mobBase.getLoadID() == 0)
			return;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_npc_mobbase_race` WHERE `mobbaseID` = ?")) {

			preparedStatement.setInt(1, mobBase.getLoadID());
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {
				float walk = rs.getFloat("walkStandard");
				float walkCombat = rs.getFloat("walkCombat");
				float run = rs.getFloat("runStandard");
				float runCombat = rs.getFloat("runCombat");
				mobBase.updateSpeeds(walk, walkCombat, run, runCombat);
			}

		} catch (SQLException e) {
			Logger.error(e);
		}
	}
}
