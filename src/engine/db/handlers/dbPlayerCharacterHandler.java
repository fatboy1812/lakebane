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
import engine.objects.AbstractWorldObject;
import engine.objects.Heraldry;
import engine.objects.PlayerCharacter;
import engine.objects.PlayerFriends;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class dbPlayerCharacterHandler extends dbHandlerBase {

	public dbPlayerCharacterHandler() {
		this.localClass = PlayerCharacter.class;
		this.localObjectType = Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
	}

	public PlayerCharacter ADD_PLAYER_CHARACTER(final PlayerCharacter toAdd) {

		PlayerCharacter playerCharacter = null;

		if (toAdd.getAccount() == null)
			return null;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("CALL `character_CREATE`(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {

			preparedStatement.setLong(1, toAdd.getAccount().getObjectUUID());
			preparedStatement.setString(2, toAdd.getFirstName());
			preparedStatement.setString(3, toAdd.getLastName());
			preparedStatement.setInt(4, toAdd.getRace().getRaceRuneID());
			preparedStatement.setInt(5, toAdd.getBaseClass().getObjectUUID());
			preparedStatement.setInt(6, toAdd.getStrMod());
			preparedStatement.setInt(7, toAdd.getDexMod());
			preparedStatement.setInt(8, toAdd.getConMod());
			preparedStatement.setInt(9, toAdd.getIntMod());
			preparedStatement.setInt(10, toAdd.getSpiMod());
			preparedStatement.setInt(11, toAdd.getExp());
			preparedStatement.setInt(12, toAdd.getSkinColor());
			preparedStatement.setInt(13, toAdd.getHairColor());
			preparedStatement.setByte(14, toAdd.getHairStyle());
			preparedStatement.setInt(15, toAdd.getBeardColor());
			preparedStatement.setByte(16, toAdd.getBeardStyle());

			ResultSet rs = preparedStatement.executeQuery();

			int objectUUID = (int) rs.getLong("UID");

			if (objectUUID > 0)
				playerCharacter = GET_PLAYER_CHARACTER(objectUUID);

		} catch (SQLException e) {
			Logger.error(e);
		}

		return playerCharacter;
	}

	public boolean SET_IGNORE_LIST(int sourceID, int targetID, boolean toIgnore, String charName) {

		String queryString = "";

		if (toIgnore)
			queryString = "INSERT INTO `dyn_character_ignore` (`accountUID`, `ignoringUID`, `characterName`) VALUES (?, ?, ?)";
		else
			queryString = "DELETE FROM `dyn_character_ignore` WHERE `accountUID` = ? && `ignoringUID` = ?";

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

			preparedStatement.setLong(1, sourceID);
			preparedStatement.setLong(2, targetID);

			if (toIgnore)
				preparedStatement.setString(3, charName);

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
			return false;
		}

	}

	public ArrayList<PlayerCharacter> GET_CHARACTERS_FOR_ACCOUNT(final int id) {

		ArrayList<PlayerCharacter> characterList = new ArrayList<>();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_character`.*, `object`.`parent` FROM `object` INNER JOIN `obj_character` ON `obj_character`.`UID` = `object`.`UID` WHERE `object`.`parent`=? && `obj_character`.`char_isActive`='1';")) {

			preparedStatement.setLong(1, (long) id);
			ResultSet rs = preparedStatement.executeQuery();
			characterList = getObjectsFromRs(rs, 10);

		} catch (SQLException e) {
			Logger.error(e);
		}

		return characterList;
	}

	public ArrayList<PlayerCharacter> GET_ALL_CHARACTERS() {

		ArrayList<PlayerCharacter> characterList = new ArrayList<>();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_character`.*, `object`.`parent` FROM `object` INNER JOIN `obj_character` ON `obj_character`.`UID` = `object`.`UID` WHERE `obj_character`.`char_isActive`='1';")) {

			ResultSet rs = preparedStatement.executeQuery();
			characterList = getObjectsFromRs(rs, 2000);

		} catch (SQLException e) {
			Logger.error(e);
		}

		return characterList;
	}

	/**
	 *
	 * <code>getFirstName</code> looks up the first name of a PlayerCharacter by
	 * first checking the GOM cache and then querying the database.
	 * PlayerCharacter objects that are not already cached won't be instantiated
	 * and cached.
	 *
	 */

	public ConcurrentHashMap<Integer, String> GET_IGNORE_LIST(final int objectUUID, final boolean skipActiveCheck) {

		ConcurrentHashMap<Integer, String> ignoreList = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_character_ignore` WHERE `accountUID` = ?;")) {

			preparedStatement.setLong(1, objectUUID);

			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {
				int ignoreCharacterID = rs.getInt("ignoringUID");

				if (ignoreCharacterID == 0)
					continue;

				String name = rs.getString("characterName");
				ignoreList.put(ignoreCharacterID, name);
			}

		} catch (SQLException e) {
			Logger.error(e);
		}

		return ignoreList;
	}

	public PlayerCharacter GET_PLAYER_CHARACTER(final int objectUUID) {

		if (objectUUID == 0)
			return null;

		PlayerCharacter pc = (PlayerCharacter) DbManager.getFromCache(Enum.GameObjectType.PlayerCharacter, objectUUID);
		if (pc != null)
			return pc;
		prepareCallable("SELECT `obj_character`.*, `object`.`parent` FROM `object` INNER JOIN `obj_character` ON `obj_character`.`UID` = `object`.`UID` WHERE `object`.`UID` = ?");
		setLong(1, (long) objectUUID);
		return (PlayerCharacter) getObjectSingle(objectUUID);
	}

	public boolean IS_CHARACTER_NAME_UNIQUE(final String firstName) {
		boolean unique = true;
		prepareCallable("SELECT `char_firstname` FROM `obj_character` WHERE `char_isActive`=1 && `char_firstname`=?");
		setString(1, firstName);
		try {
			ResultSet rs = executeQuery();
			if (rs.next()) {
				unique = false;
			}
			rs.close();
		} catch (SQLException e) {
			Logger.error("SQL Error number: " + e.getMessage());
			unique = false;
		} finally {
			closeCallable();
		}
		return unique;
	}

	public boolean UPDATE_NAME(String oldFirstName, String newFirstName, String newLastName) {
		prepareCallable("UPDATE `obj_character` SET `char_firstname`=?, `char_lastname`=? WHERE `char_firstname`=? AND `char_isActive`='1'");
		setString(1, newFirstName);
		setString(2, newLastName);
		setString(3, oldFirstName);
		return (executeUpdate() != 0);
	}

	public boolean SET_DELETED(final PlayerCharacter pc) {
		prepareCallable("UPDATE `obj_character` SET `char_isActive`=? WHERE `UID` = ?");
		setBoolean(1, !pc.isDeleted());
		setLong(2, (long) pc.getObjectUUID());
		return (executeUpdate() != 0);
	}
	public boolean SET_ACTIVE(final PlayerCharacter pc, boolean status) {
		prepareCallable("UPDATE `obj_character` SET `char_isActive`=? WHERE `UID` = ?");
		setBoolean(1, status);
		setLong(2, (long) pc.getObjectUUID());
		return (executeUpdate() != 0);
	}
	public boolean SET_BIND_BUILDING(final PlayerCharacter pc, int bindBuildingID) {
		prepareCallable("UPDATE `obj_character` SET `char_bindBuilding`=? WHERE `UID` = ?");
		setInt(1, bindBuildingID);
		setLong(2, (long) pc.getObjectUUID());
		return (executeUpdate() != 0);
	}

	public boolean SET_ANNIVERSERY(final PlayerCharacter pc, boolean flag) {
		prepareCallable("UPDATE `obj_character` SET `anniversery`=? WHERE `UID` = ?");
		setBoolean(1, flag);
		setLong(2, (long) pc.getObjectUUID());
		return (executeUpdate() != 0);
	}


	public boolean UPDATE_CHARACTER_EXPERIENCE(final PlayerCharacter pc) {
		prepareCallable("UPDATE `obj_character` SET `char_experience`=? WHERE `UID` = ?");
		setInt(1, pc.getExp());
		setLong(2, (long) pc.getObjectUUID());
		return (executeUpdate() != 0);
	}
	
	public boolean UPDATE_GUILD(final PlayerCharacter pc, int guildUUID) {
		prepareCallable("UPDATE `obj_character` SET `guildUID`=? WHERE `UID` = ?");
		setInt(1, guildUUID);
		setLong(2, (long) pc.getObjectUUID());
		return (executeUpdate() != 0);
	}

	public boolean UPDATE_CHARACTER_STATS(final PlayerCharacter pc) {
		prepareCallable("UPDATE `obj_character` SET `char_strMod`=?, `char_dexMod`=?, `char_conMod`=?, `char_intMod`=?, `char_spiMod`=? WHERE `UID`=?");
		setInt(1, pc.getStrMod());
		setInt(2, pc.getDexMod());
		setInt(3, pc.getConMod());
		setInt(4, pc.getIntMod());
		setInt(5, pc.getSpiMod());
		setLong(6, (long) pc.getObjectUUID());
		return (executeUpdate() != 0);
	}

	public String SET_PROPERTY(final PlayerCharacter c, String name, Object new_value) {
		prepareCallable("CALL character_SETPROP(?,?,?)");
		setLong(1, (long) c.getObjectUUID());
		setString(2, name);
		setString(3, String.valueOf(new_value));
		return getResult();
	}

	public String SET_PROPERTY(final PlayerCharacter c, String name, Object new_value, Object old_value) {
		prepareCallable("CALL character_GETSETPROP(?,?,?,?)");
		setLong(1, (long) c.getObjectUUID());
		setString(2, name);
		setString(3, String.valueOf(new_value));
		setString(4, String.valueOf(old_value));
		return getResult();
	}
	
	public boolean SET_PROMOTION_CLASS(PlayerCharacter player, int promotionClassID) {
		prepareCallable("UPDATE `obj_character` SET `char_promotionClassID`=?  WHERE `UID`=?;");
		setInt(1,promotionClassID);
		setInt(2, player.getObjectUUID());
		return (executeUpdate() != 0);
	}
	
	public boolean SET_INNERCOUNCIL(PlayerCharacter player, boolean isInnerCouncil) {
		prepareCallable("UPDATE `obj_character` SET `guild_isInnerCouncil`=?  WHERE `UID`=?;");
		setBoolean(1,isInnerCouncil);
		setInt(2, player.getObjectUUID());
		return (executeUpdate() != 0);
	}
	
	public boolean SET_FULL_MEMBER(PlayerCharacter player, boolean isFullMember) {
		prepareCallable("UPDATE `obj_character` SET `guild_isFullMember`=?  WHERE `UID`=?;");
		setBoolean(1,isFullMember);
		setInt(2, player.getObjectUUID());
		return (executeUpdate() != 0);
	}
	
	public boolean SET_TAX_COLLECTOR(PlayerCharacter player, boolean isTaxCollector) {
		prepareCallable("UPDATE `obj_character` SET `guild_isTaxCollector`=?  WHERE `UID`=?;");
		setBoolean(1,isTaxCollector);
		setInt(2, player.getObjectUUID());
		return (executeUpdate() != 0);
	}
	
	public boolean SET_RECRUITER(PlayerCharacter player, boolean isRecruiter) {
		prepareCallable("UPDATE `obj_character` SET `guild_isRecruiter`=?  WHERE `UID`=?;");
		setBoolean(1,isRecruiter);
		setInt(2, player.getObjectUUID());
		return (executeUpdate() != 0);
	}
	
	public boolean SET_GUILD_TITLE(PlayerCharacter player, int title) {
		prepareCallable("UPDATE `obj_character` SET `guild_title`=?  WHERE `UID`=?;");
		setInt(1,title);
		setInt(2, player.getObjectUUID());
		return (executeUpdate() != 0);
	}
	
	
	
	public boolean ADD_FRIEND(int source, long friend){
		prepareCallable("INSERT INTO `dyn_character_friends` (`playerUID`, `friendUID`) VALUES (?, ?)");
		setLong(1, (long) source);
		setLong(2, (long)friend);
		return (executeUpdate() != 0);
	}
	
	public boolean REMOVE_FRIEND(int source, int friend){
		prepareCallable("DELETE FROM `dyn_character_friends` WHERE (`playerUID`=?) AND (`friendUID`=?)");
		setLong(1, (long) source);
		setLong(2, (long)friend);
		return (executeUpdate() != 0);
	}
	
	public void LOAD_PLAYER_FRIENDS() {

		PlayerFriends playerFriend;


		prepareCallable("SELECT * FROM dyn_character_friends");

		try {
			ResultSet rs = executeQuery();

			while (rs.next()) {
				playerFriend = new PlayerFriends(rs);
			}


		} catch (SQLException e) {
			Logger.error("LoadMeshBounds: " + e.getErrorCode() + ' ' + e.getMessage(), e);
		} finally {
			closeCallable();
		}
	}
	
	public boolean ADD_HERALDY(int source, AbstractWorldObject character){
		prepareCallable("INSERT INTO `dyn_character_heraldy` (`playerUID`, `characterUID`,`characterType`) VALUES (?, ?,?)");
		setLong(1, (long) source);
		setLong(2, (long)character.getObjectUUID());
		setInt(3, character.getObjectType().ordinal());
		return (executeUpdate() != 0);
	}
	
	public boolean REMOVE_HERALDY(int source, int characterUID){
		prepareCallable("DELETE FROM `dyn_character_heraldy` WHERE (`playerUID`=?) AND (`characterUID`=?)");
		setLong(1, (long) source);
		setLong(2, (long)characterUID);
		return (executeUpdate() != 0);
	}
	
	public void LOAD_HERALDY() {

		Heraldry heraldy;


		prepareCallable("SELECT * FROM dyn_character_heraldy");

		try {
			ResultSet rs = executeQuery();

			while (rs.next()) {
				heraldy = new Heraldry(rs);
			}


		} catch (SQLException e) {
			Logger.error("LoadHeraldy: " + e.getErrorCode() + ' ' + e.getMessage(), e);
		} finally {
			closeCallable();
		}
	}
	
}
