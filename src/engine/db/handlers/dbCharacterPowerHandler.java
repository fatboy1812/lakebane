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
import engine.objects.CharacterPower;
import engine.objects.PlayerCharacter;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

public class dbCharacterPowerHandler extends dbHandlerBase {

	public dbCharacterPowerHandler() {
		this.localClass = CharacterPower.class;
		this.localObjectType = Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
	}

	public CharacterPower ADD_CHARACTER_POWER(CharacterPower toAdd) {

		CharacterPower characterPower = null;

		if (CharacterPower.getOwner(toAdd) == null || toAdd.getPower() == null) {
			Logger.error("dbCharacterSkillHandler.ADD_Power", toAdd.getObjectUUID() + " missing owner or powersBase");
			return null;
		}

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_character_power` (`CharacterID`, `powersBaseToken`, `trains`) VALUES (?, ?, ?);")) {

			preparedStatement.setLong(1, CharacterPower.getOwner(toAdd).getObjectUUID());
			preparedStatement.setInt(2, toAdd.getPower().getToken());
			preparedStatement.setInt(3, toAdd.getTrains());

			preparedStatement.executeUpdate();
			ResultSet rs = preparedStatement.getGeneratedKeys();

			if (rs.next())
				characterPower = GET_CHARACTER_POWER(rs.getInt(1));

		} catch (SQLException e) {
			Logger.error(e);
			return null;
		}
		return characterPower;
	}

	public int DELETE_CHARACTER_POWER(final int objectUUID) {

		int rowCount = 0;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_character_power` WHERE `UID` = ?")) {

			preparedStatement.setLong(1, objectUUID);
			rowCount = preparedStatement.executeUpdate();

		} catch (SQLException e) {
			Logger.error(e);
		}

		return rowCount;
	}

	public CharacterPower GET_CHARACTER_POWER(int objectUUID) {

		CharacterPower characterPower = (CharacterPower) DbManager.getFromCache(Enum.GameObjectType.CharacterPower, objectUUID);

		if (characterPower != null)
			return characterPower;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_character_power` WHERE `UID` = ?")) {

			preparedStatement.setLong(1, objectUUID);
			ResultSet rs = preparedStatement.executeQuery();
			characterPower = (CharacterPower) getObjectFromRs(rs);

		} catch (SQLException e) {
			Logger.error(e);
		}

		return characterPower;
	}

	public ConcurrentHashMap<Integer, CharacterPower> GET_POWERS_FOR_CHARACTER(PlayerCharacter pc) {

		ConcurrentHashMap<Integer, CharacterPower> powers = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
		int objectUUID = pc.getObjectUUID();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_character_power` WHERE CharacterID = ?")) {

			preparedStatement.setLong(1, (long) objectUUID);
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {
				CharacterPower cp = new CharacterPower(rs, pc);
				if (cp.getPower() != null)
					powers.put(cp.getPower().getToken(), cp);
			}

		} catch (SQLException e) {
			Logger.error(e);
		}

		return powers;
	}

	public void UPDATE_TRAINS(final CharacterPower characterPower) {

		//skip update if nothing changed

		if (!characterPower.isTrained())
			return;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_character_power` SET `trains`=? WHERE `UID`=?")) {

			preparedStatement.setShort(1, (short) characterPower.getTrains());
			preparedStatement.setInt(2, characterPower.getObjectUUID());

			preparedStatement.execute();

		} catch (SQLException e) {
			Logger.error(e);
		}

		characterPower.setTrained(false);
	}

	public void updateDatabase(final CharacterPower pow) {

		if (pow.getPower() == null) {
			Logger.error("Failed to find powersBase for Power " + pow.getObjectUUID());
			return;
		}

		if (CharacterPower.getOwner(pow) == null) {
			Logger.error("Failed to find owner for Power " + pow.getObjectUUID());
			return;
		}

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_character_power` SET `PowersBaseToken`=?, `CharacterID`=?, `trains`=? WHERE `UID`=?")) {

			preparedStatement.setInt(1, pow.getPower().getToken());
			preparedStatement.setInt(2, CharacterPower.getOwner(pow).getObjectUUID());
			preparedStatement.setShort(3, (short) pow.getTrains());
			preparedStatement.setInt(4, pow.getObjectUUID());
			preparedStatement.execute();

		} catch (SQLException e) {
			Logger.error(e);
		}

		pow.setTrained(false);
	}
}
