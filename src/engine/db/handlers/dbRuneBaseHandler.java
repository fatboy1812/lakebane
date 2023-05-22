// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.gameManager.DbManager;
import engine.objects.RuneBase;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class dbRuneBaseHandler extends dbHandlerBase {

	public dbRuneBaseHandler() {
		this.localClass = RuneBase.class;
		this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
	}

	public void GET_RUNE_REQS(final RuneBase rb) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_rune_runereq` WHERE `runeID` = ?")) {

			preparedStatement.setInt(1, rb.getObjectUUID());

			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {
				int type = rs.getInt("type");

				switch (type) {
					case 1:
						rb.getRace().put(rs.getInt("requiredRuneID"), rs.getBoolean("isAllowed"));
						break;
					case 2:
						rb.getBaseClass().put(rs.getInt("requiredRuneID"), rs.getBoolean("isAllowed"));
						break;
					case 3:
						rb.getPromotionClass().put(rs.getInt("requiredRuneID"), rs.getBoolean("isAllowed"));
						break;
					case 4:
						rb.getDiscipline().put(rs.getInt("requiredRuneID"), rs.getBoolean("isAllowed"));
						break;
					case 5:
						rb.getOverwrite().add(rs.getInt("requiredRuneID"));
						break;
					case 6:
						rb.setLevelRequired(rs.getInt("requiredRuneID"));
						break;
				}
			}

		} catch (SQLException e) {
			Logger.error(e);
		}

	}

	public RuneBase GET_RUNEBASE(final int id) {

		RuneBase runeBase = null;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_rune_runebase` WHERE `ID` = ?")) {

			preparedStatement.setInt(1, id);

			ResultSet rs = preparedStatement.executeQuery();
			runeBase = (RuneBase) getObjectFromRs(rs);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return runeBase;
	}

	public ArrayList<RuneBase> LOAD_ALL_RUNEBASES() {

		ArrayList<RuneBase> runeBasesList = new ArrayList<>();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_rune_runebase`;")) {

			ResultSet rs = preparedStatement.executeQuery();
			runeBasesList = getObjectsFromRs(rs, 1000);

		} catch (SQLException e) {
			Logger.error(e);
		}

		return runeBasesList;
	}

	public HashMap<Integer, ArrayList<Integer>> LOAD_ALLOWED_STARTING_RUNES_FOR_BASECLASS() {

		HashMap<Integer, ArrayList<Integer>> runeSets = new HashMap<>();

		int recordsRead = 0;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_rune_baseclassrune")) {

			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {

				recordsRead++;

				int baseClassID = rs.getInt("BaseClassesID");
				int runeBaseID = rs.getInt("RuneBaseID");

				if (runeSets.get(baseClassID) == null) {
					ArrayList<Integer> runeList = new ArrayList<>();
					runeList.add(runeBaseID);
					runeSets.put(baseClassID, runeList);
				} else {
					ArrayList<Integer> runeList = runeSets.get(baseClassID);
					runeList.add(runeBaseID);
					runeSets.put(baseClassID, runeList);
				}
			}

		} catch (SQLException e) {
			Logger.error(e);
		}

		Logger.info("read: " + recordsRead + " cached: " + runeSets.size());

		return runeSets;
	}

	public HashMap<Integer, ArrayList<Integer>> LOAD_ALLOWED_STARTING_RUNES_FOR_RACE() {

		HashMap<Integer, ArrayList<Integer>> runeSets;

		runeSets = new HashMap<>();
		int recordsRead = 0;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_rune_racerune")) {

			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {

				recordsRead++;

				int raceID = rs.getInt("RaceID");
				int runeBaseID = rs.getInt("RuneBaseID");

				if (runeSets.get(raceID) == null) {
					ArrayList<Integer> runeList = new ArrayList<>();
					runeList.add(runeBaseID);
					runeSets.put(raceID, runeList);
				} else {
					ArrayList<Integer> runeList = runeSets.get(raceID);
					runeList.add(runeBaseID);
					runeSets.put(raceID, runeList);
				}
			}
		} catch (SQLException e) {
			Logger.error(e);
		}

		Logger.info("read: " + recordsRead + " cached: " + runeSets.size());
		return runeSets;
	}
}
