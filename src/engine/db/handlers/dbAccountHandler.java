// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum;
import engine.Enum.GameObjectType;
import engine.gameManager.ConfigManager;
import engine.gameManager.DbManager;
import engine.objects.Account;
import engine.objects.PlayerCharacter;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class dbAccountHandler extends dbHandlerBase {

	public dbAccountHandler() {
		this.localClass = Account.class;
		this.localObjectType = Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
	}

	public Account GET_ACCOUNT(int accountID) {

		Account account;

		if (accountID == 0)
			return null;

		account = (Account) DbManager.getFromCache(GameObjectType.Account, accountID);

		if (account != null)
			return account;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement accountQuery = connection.prepareStatement("SELECT * FROM `obj_account` WHERE `UID`=?")) {

			accountQuery.setLong(1, accountID);

			ResultSet rs = accountQuery.executeQuery();
			account = (Account) getObjectFromRs(rs);

		} catch (SQLException e) {
			Logger.error(e);
		}

		if (account != null)
			account.runAfterLoad();

		return account;
	}

	public void WRITE_ADMIN_LOG(String adminName, String logEntry) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement accountQuery = connection.prepareStatement("INSERT INTO dyn_admin_log(`dateTime`, `charName`, `eventString`)"
					 + " VALUES (?, ?, ?)")) {

			accountQuery.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
			accountQuery.setString(2, adminName);
			accountQuery.setString(3, logEntry);

			accountQuery.execute();

		} catch (SQLException e) {
			Logger.error(e);
		}
	}

	public void SET_TRASH(String machineID) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO dyn_trash(`machineID`, `count`)"
					 + " VALUES (?, 1) ON DUPLICATE KEY UPDATE `count` = `count` + 1;")) {

			preparedStatement.setString(1, machineID);
			preparedStatement.execute();

		} catch (SQLException e) {
			Logger.error(e);
		}

	}

	public ArrayList<String> GET_TRASH_LIST() {

		ArrayList<String> machineList = new ArrayList<>();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement accountQuery = connection.prepareStatement("select `machineID` from `dyn_trash`")) {

			ResultSet rs = accountQuery.executeQuery();

			while (rs.next())
				machineList.add(rs.getString(1));

		} catch (SQLException e) {
			Logger.error(e);
		}

		return machineList;
	}

	public void DELETE_VAULT_FOR_ACCOUNT(final int accountID) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `object` WHERE `parent`=? && `type`='item'")) {

			preparedStatement.setLong(1, accountID);
			preparedStatement.execute();

		} catch (SQLException e) {
			Logger.error(e);
		}

	}

	public ArrayList<PlayerCharacter> GET_ALL_CHARS_FOR_MACHINE(String machineID) {

		ArrayList<PlayerCharacter> trashList = new ArrayList<>();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement accountQuery = connection.prepareStatement("select DISTINCT UID from object \n" +
					 "where parent IN (select AccountID from dyn_login_history " +
					 " WHERE`machineID`=?)")) {

			accountQuery.setString(1, machineID);
			ResultSet rs = accountQuery.executeQuery();

			while (rs.next()) {

				PlayerCharacter trashPlayer;
				int playerID;

				playerID = rs.getInt(1);
				trashPlayer = PlayerCharacter.getPlayerCharacter(playerID);

				if (trashPlayer == null)
					continue;

				if (trashPlayer.isDeleted() == false)
					trashList.add(trashPlayer);
			}

		} catch (SQLException e) {
			Logger.error(e);
		}

		return trashList;
	}

	public void CLEAR_TRASH_TABLE() {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM dyn_trash")) {

			preparedStatement.execute();

		} catch (SQLException e) {
			Logger.error(e);
		}

	}

	public void CREATE_SINGLE(String accountName, String password) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("CALL singleAccountCreate(?,?)")) {

			preparedStatement.setString(1, accountName);
			preparedStatement.setString(2, password);

			preparedStatement.execute();

		} catch (SQLException e) {
			Logger.error(e);
		}

	}

	public Account GET_ACCOUNT(String uname) {

		Account account = null;

		if (Account.AccountsMap.get(uname) != null)
			return this.GET_ACCOUNT(Account.AccountsMap.get(uname));

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement accountQuery = connection.prepareStatement("SELECT * FROM `obj_account` WHERE `acct_uname`=?")) {

			accountQuery.setString(1, uname);

			ResultSet rs = accountQuery.executeQuery();
			account = (Account) getObjectFromRs(rs);

		} catch (SQLException e) {
			Logger.error(e);
		}

		if (account != null) {
			account.runAfterLoad();

			if (ConfigManager.serverType.equals(Enum.ServerType.LOGINSERVER))
				Account.AccountsMap.put(uname, account.getObjectUUID());

		}
		return account;
	}

	public void SET_ACCOUNT_LOGIN(final Account acc, String playerName, final String ip, final String machineID) {

		if (acc.getObjectUUID() == 0 || ip == null || ip.length() == 0)
			return;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO dyn_login_history(`AccountID`, `accountName`, `characterName`, `ip`, `machineID`, `timeStamp`)"
					 + " VALUES (?, ?, ?, ?, ?, ?)")) {

			preparedStatement.setInt(1, acc.getObjectUUID());
			preparedStatement.setString(2, acc.getUname());
			preparedStatement.setString(3, playerName);
			preparedStatement.setString(4, ip);
			preparedStatement.setString(5, machineID);
			preparedStatement.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));

			preparedStatement.execute();

		} catch (SQLException e) {
			Logger.error(e);
		}

	}

	public void updateDatabase(final Account acc) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_account` SET `acct_passwd`=?, "
					 + " `acct_lastCharUID`=?, `acct_salt`=?, `discordAccount`=?, " +
					 " status = ? WHERE `UID`=?")) {

			preparedStatement.setString(1, acc.getPasswd());
			preparedStatement.setInt(2, acc.getLastCharIDUsed());
			preparedStatement.setString(3, acc.getSalt());
			preparedStatement.setString(4, acc.discordAccount);
			preparedStatement.setString(5, acc.status.name());
			preparedStatement.setInt(6, acc.getObjectUUID());

			preparedStatement.execute();

		} catch (SQLException e) {
			Logger.error(e);
		}

	}

	public void INVALIDATE_LOGIN_CACHE(long accountUID, String objectType) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("INSERT IGNORE INTO login_cachelist (`UID`, `type`) VALUES(?,?);")) {

			preparedStatement.setLong(1, accountUID);
			preparedStatement.setString(2, objectType);

			preparedStatement.execute();

		} catch (SQLException e) {
			Logger.error(e);
		}

	}

}
