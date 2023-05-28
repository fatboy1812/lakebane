// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum.GameObjectType;
import engine.Enum.ProtectionState;
import engine.Enum.TransactionType;
import engine.gameManager.DbManager;
import engine.math.Vector3fImmutable;
import engine.objects.*;
import engine.server.MBServerStatics;
import org.joda.time.DateTime;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class dbWarehouseHandler extends dbHandlerBase {

	private static final ConcurrentHashMap<Integer, String> columns = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);

	public dbWarehouseHandler() {
		this.localClass = Warehouse.class;
		this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());

	}

	public static void addObject(ArrayList<AbstractGameObject> list, ResultSet rs) throws SQLException {
		String type = rs.getString("type");
		switch (type) {
			case "building":
				Building building = new Building(rs);
				DbManager.addToCache(building);
				list.add(building);
				break;
			case "warehouse":
				Warehouse warehouse = new Warehouse(rs);
				DbManager.addToCache(warehouse);
				list.add(warehouse);
				break;
		}
	}

	public ArrayList<AbstractGameObject> CREATE_WAREHOUSE(int parentZoneID, int OwnerUUID, String name, int meshUUID,
														  Vector3fImmutable location, float meshScale, int currentHP,
														  ProtectionState protectionState, int currentGold, int rank,
														  DateTime upgradeDate, int blueprintUUID, float w, float rotY) {

		ArrayList<AbstractGameObject> warehouseList = new ArrayList<>();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("CALL `WAREHOUSE_CREATE`(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,? ,? ,?, ?);")) {

			preparedStatement.setInt(1, parentZoneID);
			preparedStatement.setInt(2, OwnerUUID);
			preparedStatement.setString(3, name);
			preparedStatement.setInt(4, meshUUID);
			preparedStatement.setFloat(5, location.x);
			preparedStatement.setFloat(6, location.y);
			preparedStatement.setFloat(7, location.z);
			preparedStatement.setFloat(8, meshScale);
			preparedStatement.setInt(9, currentHP);
			preparedStatement.setString(10, protectionState.name());
			preparedStatement.setInt(11, currentGold);
			preparedStatement.setInt(12, rank);

			if (upgradeDate != null)
				preparedStatement.setTimestamp(13, new java.sql.Timestamp(upgradeDate.getMillis()));
			else
				preparedStatement.setNull(13, java.sql.Types.DATE);

			preparedStatement.setInt(14, blueprintUUID);
			preparedStatement.setFloat(15, w);
			preparedStatement.setFloat(16, rotY);

			preparedStatement.execute();
			ResultSet rs = preparedStatement.getResultSet();

			while (rs.next())
				addObject(warehouseList, rs);

			while (preparedStatement.getMoreResults()) {
				rs = preparedStatement.getResultSet();

				while (rs.next())
					addObject(warehouseList, rs);
			}
		} catch (SQLException e) {
			Logger.error(e);
		}

		return warehouseList;
	}

	public boolean updateLocks(final Warehouse wh, long locks) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_locks`=? WHERE `UID` = ?")) {

			preparedStatement.setLong(1, locks);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateGold(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_gold`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateStone(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_stone`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateTruesteel(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_truesteel`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateIron(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_iron`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateAdamant(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_adamant`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateLumber(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_lumber`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateOak(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_oak`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateBronzewood(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_bronzewood`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateMandrake(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_mandrake`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateCoal(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_coal`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateAgate(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_agate`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateDiamond(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_diamond`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateOnyx(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_onyx`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateAzoth(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_azoth`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateOrichalk(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_orichalk`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateAntimony(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_antimony`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateSulfur(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_sulfur`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateQuicksilver(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_quicksilver`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateGalvor(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_galvor`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateWormwood(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_wormwood`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateObsidian(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_obsidian`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateBloodstone(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_bloodstone`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean updateMithril(final Warehouse wh, int amount) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_warehouse` SET `warehouse_mithril`=? WHERE `UID` = ?")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setInt(2, wh.getUID());

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public boolean CREATE_TRANSACTION(int warehouseBuildingID, GameObjectType targetType, int targetUUID, TransactionType transactionType, Resource resource, int amount, DateTime date) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_warehouse_transactions` (`warehouseUID`, `targetType`,`targetUID`, `type`,`resource`,`amount`,`date` ) VALUES (?,?,?,?,?,?,?)")) {

			preparedStatement.setInt(1, amount);
			preparedStatement.setLong(1, warehouseBuildingID);
			preparedStatement.setString(2, targetType.name());
			preparedStatement.setLong(3, targetUUID);
			preparedStatement.setString(4, transactionType.name());
			preparedStatement.setString(5, resource.name());
			preparedStatement.setInt(6, amount);
			preparedStatement.setTimestamp(7, new java.sql.Timestamp(date.getMillis()));

			return (preparedStatement.executeUpdate() > 0);

		} catch (SQLException e) {
			Logger.error(e);
		}
		return false;
	}

	public ArrayList<Transaction> GET_TRANSACTIONS_FOR_WAREHOUSE(final int warehouseUUID) {

		ArrayList<Transaction> transactionsList = new ArrayList<>();


		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM dyn_warehouse_transactions WHERE `warehouseUID` = ?;")) {

			preparedStatement.setInt(1, warehouseUUID);

			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {
				Transaction transactions = new Transaction(rs);
				transactionsList.add(transactions);
			}

		} catch (SQLException e) {
			Logger.error(e);
		}

		return transactionsList;
	}

	public void LOAD_ALL_WAREHOUSES() {

		Warehouse warehouse;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_warehouse`.*, `object`.`parent`, `object`.`type` FROM `object` LEFT JOIN `obj_warehouse` ON `object`.`UID` = `obj_warehouse`.`UID` WHERE `object`.`type` = 'warehouse';")) {


			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next()) {
				warehouse = new Warehouse(rs);
				warehouse.runAfterLoad();
				warehouse.loadAllTransactions();
			}

		} catch (SQLException e) {
			Logger.error(e);
		}
	}
}
