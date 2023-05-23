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
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_gold`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());
		return (executeUpdate() != 0);
	}

	public boolean updateStone(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_stone`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateTruesteel(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_truesteel`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateIron(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_iron`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateAdamant(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_adamant`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateLumber(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_lumber`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateOak(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_oak`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateBronzewood(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_bronzewood`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateMandrake(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_mandrake`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateCoal(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_coal`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateAgate(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_agate`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateDiamond(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_diamond`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateOnyx(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_onyx`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateAzoth(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_azoth`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateOrichalk(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_orichalk`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateAntimony(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_antimony`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateSulfur(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_sulfur`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateQuicksilver(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_quicksilver`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateGalvor(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_galvor`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateWormwood(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_wormwood`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateObsidian(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_obsidian`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateBloodstone(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_bloodstone`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean updateMithril(final Warehouse wh, int amount) {
		prepareCallable("UPDATE `obj_warehouse` SET `warehouse_mithril`=? WHERE `UID` = ?");
		setInt(1, amount);
		setInt(2, wh.getUID());

		return (executeUpdate() != 0);
	}

	public boolean CREATE_TRANSACTION(int warehouseBuildingID, GameObjectType targetType, int targetUUID, TransactionType transactionType, Resource resource, int amount, DateTime date) {
		Transaction transactions = null;
		prepareCallable("INSERT INTO `dyn_warehouse_transactions` (`warehouseUID`, `targetType`,`targetUID`, `type`,`resource`,`amount`,`date` ) VALUES (?,?,?,?,?,?,?)");
		setLong(1, warehouseBuildingID);
		setString(2, targetType.name());
		setLong(3, targetUUID);
		setString(4, transactionType.name());
		setString(5, resource.name());
		setInt(6, amount);
		setTimeStamp(7, date.getMillis());
		return (executeUpdate() != 0);
	}

	public ArrayList<Transaction> GET_TRANSACTIONS_FOR_WAREHOUSE(final int warehouseUUID) {
		ArrayList<Transaction> transactionsList = new ArrayList<>();
		prepareCallable("SELECT * FROM dyn_warehouse_transactions WHERE `warehouseUID` = ?;");
		setInt(1, warehouseUUID);
		try {
			ResultSet rs = executeQuery();

			//shrines cached in rs for easy cache on creation.
			while (rs.next()) {
				Transaction transactions = new Transaction(rs);
				transactionsList.add(transactions);
			}

		} catch (SQLException e) {
			Logger.error(e.getErrorCode() + ' ' + e.getMessage(), e);
		} finally {
			closeCallable();
		}
		return transactionsList;
	}

	public void LOAD_ALL_WAREHOUSES() {

		Warehouse thisWarehouse;

		prepareCallable("SELECT `obj_warehouse`.*, `object`.`parent`, `object`.`type` FROM `object` LEFT JOIN `obj_warehouse` ON `object`.`UID` = `obj_warehouse`.`UID` WHERE `object`.`type` = 'warehouse';");

		try {
			ResultSet rs = executeQuery();
			while (rs.next()) {
				thisWarehouse = new Warehouse(rs);
				thisWarehouse.runAfterLoad();
				thisWarehouse.loadAllTransactions();
			}

		} catch (SQLException e) {
			Logger.error(e.getErrorCode() + ' ' + e.getMessage(), e);
		} finally {
			closeCallable();
		}

	}
}
