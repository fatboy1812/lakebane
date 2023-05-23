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
import engine.gameManager.ZoneManager;
import engine.math.Vector2f;
import engine.objects.Zone;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class dbZoneHandler extends dbHandlerBase {

	public dbZoneHandler() {
		this.localClass = Zone.class;
		this.localObjectType = Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
	}

	public ArrayList<Zone> GET_ALL_NODES(Zone zone) {
		ArrayList<Zone> wsmList = new ArrayList<>();
		wsmList.addAll(zone.getNodes());
		if (zone.absX == 0.0f) {
			zone.absX = zone.getXCoord();
		}
		if (zone.absY == 0.0f) {
			zone.absY = zone.getYCoord();
		}
		if (zone.absZ == 0.0f) {
			zone.absZ = zone.getZCoord();
		}
		for (Zone child : zone.getNodes()) {
			child.absX = child.getXCoord() + zone.absX;
			child.absY = child.getYCoord() + zone.absY;
			child.absZ = child.getZCoord() + zone.absZ;
			wsmList.addAll(this.GET_ALL_NODES(child));
		}
		return wsmList;
	}

	public Zone GET_BY_UID(long ID) {

		Zone zone = (Zone) DbManager.getFromCache(Enum.GameObjectType.Zone, (int) ID);

		if (zone != null)
			return zone;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_zone`.*, `object`.`parent` FROM `object` INNER JOIN `obj_zone` ON `obj_zone`.`UID` = `object`.`UID` WHERE `object`.`UID` = ?;")) {

			preparedStatement.setLong(1, ID);

			ResultSet rs = preparedStatement.executeQuery();
			zone = (Zone) getObjectFromRs(rs);

		} catch (SQLException e) {
			Logger.error(e);
		}

		return zone;
	}

	public ArrayList<Zone> GET_MAP_NODES(final int objectUUID) {

		ArrayList<Zone> zoneList = new ArrayList<>();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_zone`.*, `object`.`parent` FROM `object` INNER JOIN `obj_zone` ON `obj_zone`.`UID` = `object`.`UID` WHERE `object`.`parent` = ?;")) {

			preparedStatement.setLong(1, objectUUID);

			ResultSet rs = preparedStatement.executeQuery();
			zoneList = getObjectsFromRs(rs, 2000);

		} catch (SQLException e) {
			Logger.error(e);
		}

		return zoneList;
	}

	public void LOAD_ZONE_EXTENTS() {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_zone_size`;")) {

			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {
				Vector2f zoneSize = new Vector2f();
				int loadNum = rs.getInt("loadNum");
				zoneSize.x = rs.getFloat("xRadius");
				zoneSize.y = rs.getFloat("zRadius");
				ZoneManager._zone_size_data.put(loadNum, zoneSize);
			}

		} catch (SQLException e) {
			Logger.error(e);
		}
	}

	public boolean DELETE_ZONE(final Zone zone) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `object` WHERE `UID` = ? AND `type` = 'zone'")) {

			preparedStatement.setInt(1, zone.getObjectUUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}

		return false;
	}

}
