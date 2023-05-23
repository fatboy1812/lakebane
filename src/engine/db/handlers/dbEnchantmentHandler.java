// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.gameManager.DbManager;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

public class dbEnchantmentHandler extends dbHandlerBase {

	public ConcurrentHashMap<String, Integer> GET_ENCHANTMENTS_FOR_ITEM(final int id) {

		ConcurrentHashMap<String, Integer> enchants = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_item_enchantment` WHERE `ItemID`=?;")) {

			preparedStatement.setLong(1, id);

			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next())
				enchants.put(rs.getString("powerAction"), rs.getInt("rank"));

		} catch (SQLException e) {
			Logger.error(e);
		}

		return enchants;
	}

	public boolean CREATE_ENCHANTMENT_FOR_ITEM(long itemID, String powerAction, int rank) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_item_enchantment` (`itemID`, `powerAction`, `rank`) VALUES (?, ?, ?);")) {

			preparedStatement.setLong(1, itemID);
			preparedStatement.setString(2, powerAction);
			preparedStatement.setInt(3, rank);

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean CLEAR_ENCHANTMENTS(long itemID) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_item_enchantment` WHERE `itemID`=?;")) {

			preparedStatement.setLong(1, itemID);
			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}
}
