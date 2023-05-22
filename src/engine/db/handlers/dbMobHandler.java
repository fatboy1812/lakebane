// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.gameManager.DbManager;
import engine.objects.Mob;
import engine.objects.Zone;
import engine.server.MBServerStatics;
import org.joda.time.DateTime;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class dbMobHandler extends dbHandlerBase {

	public dbMobHandler() {
		this.localClass = Mob.class;
		this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
	}

	public Mob ADD_MOB(Mob toAdd) {

		Mob mobile = null;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("CALL `mob_CREATE`(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {

			preparedStatement.setLong(1, toAdd.getParentZoneID());
			preparedStatement.setInt(2, toAdd.getMobBaseID());
			preparedStatement.setInt(3, toAdd.getGuildUUID());
			preparedStatement.setFloat(4, toAdd.getSpawnX());
			preparedStatement.setFloat(5, toAdd.getSpawnY());
			preparedStatement.setFloat(6, toAdd.getSpawnZ());
			preparedStatement.setInt(7, 0);
			preparedStatement.setFloat(8, toAdd.getSpawnRadius());
			preparedStatement.setInt(9, toAdd.getTrueSpawnTime());

			if (toAdd.getContract() != null)
				preparedStatement.setInt(10, toAdd.getContract().getContractID());
			else
				preparedStatement.setInt(10, 0);

			preparedStatement.setInt(11, toAdd.getBuildingID());
			preparedStatement.setInt(12, toAdd.getLevel());
			preparedStatement.setString(13, toAdd.getFirstName());

			ResultSet rs = preparedStatement.executeQuery();

			int objectUUID = (int) rs.getLong("UID");

			if (objectUUID > 0)
				mobile = GET_MOB(objectUUID);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return mobile;
	}

	public boolean updateUpgradeTime(Mob mob, DateTime upgradeDateTime) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE obj_mob SET upgradeDate=? "
					 + "WHERE UID = ?")) {

			if (upgradeDateTime == null)
				preparedStatement.setNull(1, java.sql.Types.DATE);
			else
				preparedStatement.setTimestamp(1, new java.sql.Timestamp(upgradeDateTime.getMillis()));

			preparedStatement.setInt(2, mob.getObjectUUID());

			preparedStatement.execute();
			return true;
		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public int DELETE_MOB(final Mob mob) {

		int row_count = 0;
		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `object` WHERE `UID` = ?")) {

			preparedStatement.setLong(1, mob.getDBID());
			row_count = preparedStatement.executeUpdate();

		} catch (SQLException e) {
			Logger.error(e);

		}
		return row_count;
	}

	public void LOAD_PATROL_POINTS(Mob captain) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_guards` WHERE `captainUID` = ?")) {

			preparedStatement.setInt(1, captain.getObjectUUID());
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {
				String name = rs.getString("name");
				Mob toCreate = Mob.createGuardMob(captain, captain.getGuild(), captain.getParentZone(), captain.building.getLoc(), captain.getLevel(), name);

				if (toCreate == null)
					return;

				if (toCreate != null) {
					toCreate.setTimeToSpawnSiege(System.currentTimeMillis() + MBServerStatics.FIFTEEN_MINUTES);
					toCreate.setDeathTime(System.currentTimeMillis());
				}
			}

		} catch (SQLException e) {
			Logger.error(e);
		}
	}

	public boolean ADD_TO_GUARDS(final long captainUID, final int mobBaseID, final String name, final int slot) {
		prepareCallable("INSERT INTO `dyn_guards` (`captainUID`, `mobBaseID`,`name`, `slot`) VALUES (?,?,?,?)");
		setLong(1, captainUID);
		setInt(2, mobBaseID);
		setString(3, name);
		setInt(4, slot);
		return (executeUpdate() > 0);
	}

	public boolean REMOVE_FROM_GUARDS(final long captainUID, final int mobBaseID, final int slot) {
		prepareCallable("DELETE FROM `dyn_guards` WHERE `captainUID`=? AND `mobBaseID`=? AND `slot` =?");
		setLong(1, captainUID);
		setInt(2, mobBaseID);
		setInt(3, slot);
		return (executeUpdate() > 0);
	}


	public ArrayList<Mob> GET_ALL_MOBS_FOR_ZONE(Zone zone) {
		prepareCallable("SELECT `obj_mob`.*, `object`.`parent` FROM `object` INNER JOIN `obj_mob` ON `obj_mob`.`UID` = `object`.`UID` WHERE `object`.`parent` = ?;");
		setLong(1, zone.getObjectUUID());
		return getLargeObjectList();
	}

	public Mob GET_MOB(final int objectUUID) {
		prepareCallable("SELECT `obj_mob`.*, `object`.`parent` FROM `object` INNER JOIN `obj_mob` ON `obj_mob`.`UID` = `object`.`UID` WHERE `object`.`UID` = ?;");
		setLong(1, objectUUID);
		return (Mob) getObjectSingle(objectUUID);
	}

	public int MOVE_MOB(long mobID, long parentID, float locX, float locY, float locZ) {
		prepareCallable("UPDATE `object` INNER JOIN `obj_mob` On `object`.`UID` = `obj_mob`.`UID` SET `object`.`parent`=?, `obj_mob`.`mob_spawnX`=?, `obj_mob`.`mob_spawnY`=?, `obj_mob`.`mob_spawnZ`=? WHERE `obj_mob`.`UID`=?;");
		setLong(1, parentID);
		setFloat(2, locX);
		setFloat(3, locY);
		setFloat(4, locZ);
		setLong(5, mobID);
		return executeUpdate();
	}

	public String SET_PROPERTY(final Mob m, String name, Object new_value) {
		prepareCallable("CALL mob_SETPROP(?,?,?)");
		setLong(1, m.getObjectUUID());
		setString(2, name);
		setString(3, String.valueOf(new_value));
		return getResult();
	}

	public String SET_PROPERTY(final Mob m, String name, Object new_value, Object old_value) {
		prepareCallable("CALL mob_GETSETPROP(?,?,?,?)");
		setLong(1, m.getObjectUUID());
		setString(2, name);
		setString(3, String.valueOf(new_value));
		setString(4, String.valueOf(old_value));
		return getResult();
	}

}
