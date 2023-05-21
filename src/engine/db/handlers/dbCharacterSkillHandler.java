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
import engine.objects.AbstractCharacter;
import engine.objects.CharacterSkill;
import engine.objects.PlayerCharacter;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

public class dbCharacterSkillHandler extends dbHandlerBase {

	public dbCharacterSkillHandler() {
		this.localClass = CharacterSkill.class;
		this.localObjectType = Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
	}

	public CharacterSkill ADD_SKILL(CharacterSkill toAdd) {

		CharacterSkill characterSkill = null;

		if (CharacterSkill.GetOwner(toAdd) == null || toAdd.getSkillsBase() == null) {
			Logger.error("dbCharacterSkillHandler.ADD_SKILL", toAdd.getObjectUUID() + " missing owner or skillsBase");
			return null;
		}

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_character_skill` (`CharacterID`, `skillsBaseID`, `trains`) VALUES (?, ?, ?);")) {

			preparedStatement.setLong(1, CharacterSkill.GetOwner(toAdd).getObjectUUID());
			preparedStatement.setInt(2, toAdd.getSkillsBase().getObjectUUID());
			preparedStatement.setInt(3, toAdd.getNumTrains());

			preparedStatement.executeUpdate();
			ResultSet rs = preparedStatement.getGeneratedKeys();

			if (rs.next())
				characterSkill = GET_SKILL(rs.getInt(1));

		} catch (SQLException e) {
			Logger.error(e);
		}
		return characterSkill;
	}

	public boolean DELETE_SKILL(final int objectUUID) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_character_skill` WHERE `UID` = ?")) {

			preparedStatement.setLong(1, objectUUID);

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
			return false;
		}
	}

	public CharacterSkill GET_SKILL(final int objectUUID) {

		CharacterSkill characterSkill = (CharacterSkill) DbManager.getFromCache(Enum.GameObjectType.CharacterSkill, objectUUID);

		if (characterSkill != null)
			return characterSkill;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_character_skill` WHERE `UID` = ?")) {

			preparedStatement.setInt(1, objectUUID);
			ResultSet rs = preparedStatement.executeQuery();

			characterSkill = (CharacterSkill) getObjectFromRs(rs);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return characterSkill;
	}

	public ConcurrentHashMap<String, CharacterSkill> GET_SKILLS_FOR_CHARACTER(final AbstractCharacter ac) {

		ConcurrentHashMap<String, CharacterSkill> characterSkills = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);

		if (ac == null || (!(ac.getObjectType().equals(Enum.GameObjectType.PlayerCharacter))))
			return characterSkills;

		PlayerCharacter playerCharacter = (PlayerCharacter) ac;
		int characterId = playerCharacter.getObjectUUID();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_character_skill` WHERE `CharacterID` = ?")) {

			preparedStatement.setInt(1, characterId);

			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {
				CharacterSkill cs = new CharacterSkill(rs, playerCharacter);
				if (cs.getSkillsBase() != null)
					characterSkills.put(cs.getSkillsBase().getName(), cs);
			}

		} catch (SQLException e) {
			Logger.error(e);
		}

		return characterSkills;
	}


	public void UPDATE_TRAINS(final CharacterSkill characterSkill) {

		if (!characterSkill.isTrained())
			return;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_character_skill` SET `trains`=? WHERE `UID` = ?")) {

			preparedStatement.setShort(1, (short) characterSkill.getNumTrains());
			preparedStatement.setLong(2, (long) characterSkill.getObjectUUID());

			if (preparedStatement.executeUpdate() != 0)
				characterSkill.syncTrains();

		} catch (SQLException e) {
			Logger.error(e);
		}
	}

	public void updateDatabase(final CharacterSkill characterSkill) {

		if (characterSkill.getSkillsBase() == null) {
			Logger.error("Failed to find skillsBase for Skill " + characterSkill.getObjectUUID());
			return;
		}

		if (CharacterSkill.GetOwner(characterSkill) == null) {
			Logger.error("Failed to find owner for Skill " + characterSkill.getObjectUUID());
			return;
		}

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_character_skill` SET `skillsBaseID`=?, `CharacterID`=?, `trains`=? WHERE `UID`=?")) {

			preparedStatement.setInt(1, characterSkill.getSkillsBase().getObjectUUID());
			preparedStatement.setInt(2, CharacterSkill.GetOwner(characterSkill).getObjectUUID());
			preparedStatement.setShort(3, (short) characterSkill.getNumTrains());
			preparedStatement.setLong(4, (long) characterSkill.getObjectUUID());

			if (preparedStatement.executeUpdate() != 0)
				characterSkill.syncTrains();

		} catch (SQLException e) {
			Logger.error(e);
		}

	}

}
