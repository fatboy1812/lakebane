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
import engine.objects.Mine;
import engine.objects.MineProduction;
import engine.objects.Resource;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class dbMineHandler extends dbHandlerBase {

	public dbMineHandler() {
		this.localClass = Mine.class;
		this.localObjectType = Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
	}

	public Mine GET_MINE(int id) {

		if (id == 0)
			return null;

		Mine mine = (Mine) DbManager.getFromCache(Enum.GameObjectType.Mine, id);

		if (mine != null)
			return mine;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_building`.*, `object`.`parent` FROM `object` INNER JOIN `obj_building` ON `obj_building`.`UID` = `object`.`UID` WHERE `object`.`UID` = ?;")) {

			preparedStatement.setLong(1, id);

			ResultSet rs = preparedStatement.executeQuery();
			mine = (Mine) getObjectFromRs(rs);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return mine;
	}

	public ArrayList<Mine> GET_ALL_MINES_FOR_SERVER() {

		ArrayList<Mine> mines = new ArrayList<>();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_mine`.*, `object`.`parent` FROM `object` INNER JOIN `obj_mine` ON `obj_mine`.`UID` = `object`.`UID`")) {

			ResultSet rs = preparedStatement.executeQuery();
			mines = getObjectsFromRs(rs, 1000);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return mines;
	}

	public boolean CHANGE_OWNER(Mine mine, int playerUID) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_mine` SET `mine_ownerUID`=? WHERE `UID`=?")) {

			preparedStatement.setInt(1, playerUID);
			preparedStatement.setLong(2, mine.getObjectUUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
			return false;
		}
	}

	public boolean CHANGE_RESOURCE(Mine mine, Resource resource) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_mine` SET `mine_resource`=? WHERE `UID`=?")) {

			preparedStatement.setString(1, resource.name());
			preparedStatement.setLong(2, mine.getObjectUUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
			return false;
		}
	}

	public boolean CHANGE_TYPE(Mine mine, MineProduction productionType) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_mine` SET `mine_type`=? WHERE `UID`=?")) {

			preparedStatement.setString(1, productionType.name());
			preparedStatement.setLong(2, mine.getObjectUUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
			return false;
		}

	}

	public boolean SET_FLAGS(Mine mine, int newFlags) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_mine` SET `flags`=? WHERE `UID`=?")) {

			preparedStatement.setInt(1, newFlags);
			preparedStatement.setLong(2, mine.getObjectUUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
			return false;
		}
	}

}
