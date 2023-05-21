// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum.ItemContainerType;
import engine.Enum.ItemType;
import engine.gameManager.DbManager;
import engine.objects.AbstractCharacter;
import engine.objects.CharacterItemManager;
import engine.objects.Item;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;


public class dbItemHandler extends dbHandlerBase {

	public dbItemHandler() {
		this.localClass = Item.class;
		this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
	}

	public Item ADD_ITEM(Item toAdd) {

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("CALL `item_CREATE`(?, ?, ?, ?, ?, ?, ?, ?, ?,?);")) {

			preparedStatement.setInt(1, toAdd.getOwnerID());
			preparedStatement.setInt(2, toAdd.getItemBaseID());
			preparedStatement.setInt(3, toAdd.getChargesRemaining());
			preparedStatement.setInt(4, toAdd.getDurabilityCurrent());
			preparedStatement.setInt(5, toAdd.getDurabilityMax());

			if (toAdd.getNumOfItems() < 1)
				preparedStatement.setInt(6, 1);
			else
				preparedStatement.setInt(6, toAdd.getNumOfItems());

			switch (toAdd.containerType) {
				case INVENTORY:
					preparedStatement.setString(7, "inventory");
					break;
				case EQUIPPED:
					preparedStatement.setString(7, "equip");
					break;
				case BANK:
					preparedStatement.setString(7, "bank");
					break;
				case VAULT:
					preparedStatement.setString(7, "vault");
					break;
				case FORGE:
					preparedStatement.setString(7, "forge");
					break;
				default:
					preparedStatement.setString(7, "none"); //Shouldn't be here
					break;
			}

			preparedStatement.setByte(8, toAdd.getEquipSlot());
			preparedStatement.setInt(9, toAdd.getFlags());
			preparedStatement.setString(10, toAdd.getCustomName());

			ResultSet rs = preparedStatement.executeQuery();

			int objectUUID = (int) rs.getLong("UID");

			if (objectUUID > 0)
				return GET_ITEM(objectUUID);

		} catch (SQLException e) {
			Logger.error(e);
		}

		return null;
	}

	public String GET_OWNER(int ownerID) {

		String ownerType = "";

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT `type` FROM `object` WHERE `UID`=?")) {

			preparedStatement.setInt(1, ownerID);

			ResultSet rs = preparedStatement.executeQuery();
			ownerType = rs.getString("type");

		} catch (SQLException e) {
			Logger.error(e);
		}

		return ownerType;
	}

	public boolean DO_TRADE(HashSet<Integer> from1, HashSet<Integer> from2,
			CharacterItemManager man1, CharacterItemManager man2,
			Item inventoryGold1, Item inventoryGold2, int goldFrom1, int goldFrom2) {

		AbstractCharacter ac1 = man1.getOwner();
		AbstractCharacter ac2 = man2.getOwner();

		if (ac1 == null || ac2 == null || inventoryGold1 == null || inventoryGold2 == null)
			return false;

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("CALL `item_TRADE`(?, ?, ?, ?, ?, ?, ?, ?)")) {

			preparedStatement.setString(1, formatTradeString(from1));
			preparedStatement.setLong(2, (long) ac1.getObjectUUID());
			preparedStatement.setString(3, formatTradeString(from2));
			preparedStatement.setLong(4, (long) ac2.getObjectUUID());
			preparedStatement.setInt(5, goldFrom1);
			preparedStatement.setLong(6, (long) inventoryGold1.getObjectUUID());
			preparedStatement.setInt(7, goldFrom2);
			preparedStatement.setLong(8, (long) inventoryGold2.getObjectUUID());

			ResultSet rs = preparedStatement.executeQuery();

			return rs.getBoolean("result");

		} catch (SQLException e) {
			Logger.error(e);
			return false;
		}
	}

	private static String formatTradeString(HashSet<Integer> list) {
		int size = list.size();

		String ret = "";

		if (size == 0)
			return ret;

		boolean start = true;

		for (int i : list) {
			if (start) {
				ret += i;
				start = false;
			} else
				ret += "," + i;
		}
		return ret;
	}

	public ArrayList<Item> GET_EQUIPPED_ITEMS(final int targetId) {
		prepareCallable("SELECT `obj_item`.*, `object`.`parent`, `object`.`type` FROM `object` INNER JOIN `obj_item` ON `object`.`UID` = `obj_item`.`UID` WHERE `object`.`parent`=? && `obj_item`.`item_container`='equip';");
		setLong(1, (long) targetId);
		return getObjectList();
	}

	public Item GET_ITEM(final int id) {


		prepareCallable("SELECT `obj_item`.*, `object`.`parent`, `object`.`type` FROM `object` INNER JOIN `obj_item` ON `object`.`UID` = `obj_item`.`UID` WHERE `object`.`UID`=?;");
		setLong(1, (long) id);
		return (Item) getObjectSingle(id);
	}

	public ArrayList<Item> GET_ITEMS_FOR_ACCOUNT(final int accountId) {
		prepareCallable("SELECT `obj_item`.*, `object`.`parent`, `object`.`type` FROM `object` INNER JOIN `obj_item` ON `object`.`UID` = `obj_item`.`UID` WHERE `object`.`parent`=?;");
		setLong(1, (long) accountId);
		return getObjectList();
	}

	public ArrayList<Item> GET_ITEMS_FOR_NPC(final int npcId) {
		prepareCallable("SELECT `obj_item`.*, `object`.`parent`, `object`.`type` FROM `object` INNER JOIN `obj_item` ON `object`.`UID` = `obj_item`.`UID` WHERE `object`.`parent`=?");
		setLong(1, (long) npcId);
		return getObjectList();
	}

	public ArrayList<Item> GET_ITEMS_FOR_PC(final int id) {
		prepareCallable("SELECT `obj_item`.*, `object`.`parent`, `object`.`type` FROM `object` INNER JOIN `obj_item` ON `object`.`UID` = `obj_item`.`UID` WHERE `object`.`parent`=?");
		setLong(1, (long) id);
		return getLargeObjectList();
	}

	public boolean MOVE_GOLD(final Item from, final Item to, final int amt) {
		int newFromAmt = from.getNumOfItems() - amt;
		int newToAmt = to.getNumOfItems() + amt;
		prepareCallable("UPDATE `obj_item` SET `item_numberOfItems` = CASE WHEN `UID`=?  THEN ? WHEN `UID`=? THEN ? END WHERE `UID` IN (?, ?);");
		setLong(1, (long) from.getObjectUUID());
		setInt(2, newFromAmt);
		setLong(3, (long) to.getObjectUUID());
		setInt(4, newToAmt);
		setLong(5, (long) from.getObjectUUID());
		setLong(6, (long) to.getObjectUUID());
		return (executeUpdate() != 0);
	}

	public boolean ORPHAN_INVENTORY(final HashSet<Item> inventory) {
		boolean worked = true;
		for (Item item : inventory) {

			if (item.getItemBase().getType().equals(ItemType.GOLD))
				continue;

			prepareCallable("UPDATE `obj_item` LEFT JOIN `object` ON `object`.`UID` = `obj_item`.`UID` SET `object`.`parent`=NULL, `obj_item`.`item_container`='none' WHERE `object`.`UID`=?;");
			setLong(1, (long) item.getObjectUUID());
			if (executeUpdate() == 0)
				worked = false;
			else
				item.zeroItem();
		}
		return worked;
	}

	public HashSet<Integer> GET_ITEMS_FOR_VENDOR(final int vendorID) {
		prepareCallable("SELECT ID FROM static_itembase WHERE vendorType = ?");
		setInt(1, vendorID);
		return getIntegerList(1);
	}

	public String SET_PROPERTY(final Item i, String name, Object new_value) {
		prepareCallable("CALL item_SETPROP(?,?,?)");
		setLong(1, (long) i.getObjectUUID());
		setString(2, name);
		setString(3, String.valueOf(new_value));
		return getResult();
	}

	public String SET_PROPERTY(final Item i, String name, Object new_value, Object old_value) {
		prepareCallable("CALL item_GETSETPROP(?,?,?,?)");
		setLong(1, (long) i.getObjectUUID());
		setString(2, name);
		setString(3, String.valueOf(new_value));
		setString(4, String.valueOf(old_value));
		return getResult();
	}

	//Used to transfer a single item between owners or equip or vault or bank or inventory
	public boolean UPDATE_OWNER(final Item item, int newOwnerID, boolean ownerNPC, boolean ownerPlayer,
			boolean ownerAccount, ItemContainerType containerType, int slot) {

		prepareCallable("CALL `item_TRANSFER_OWNER`(?, ?, ?, ? )");
		setLong(1, (long) item.getObjectUUID());
		if (newOwnerID != 0)
			setLong(2, (long) newOwnerID);
		else
			setNULL(2, java.sql.Types.BIGINT);
		
		switch (containerType) {
			case INVENTORY:
				setString(3, "inventory");
				break;
			case EQUIPPED:
				setString(3, "equip");
				break;
			case BANK:
				setString(3, "bank");
				break;
			case VAULT:
				setString(3, "vault");
				break;
			case FORGE:
				setString(3, "forge");
				break;
			default:
				setString(3, "none"); //Shouldn't be here
				break;
		}
		setInt(4, slot);
		return worked();
	}

	public boolean SET_DURABILITY(final Item item, int value) {
		prepareCallable("UPDATE `obj_item` SET `item_durabilityCurrent`=? WHERE `UID`=? AND `item_durabilityCurrent`=?");
		setInt(1, value);
		setLong(2, (long) item.getObjectUUID());
		setInt(3, (int) item.getDurabilityCurrent());
		return (executeUpdate() != 0);

	}

	public boolean UPDATE_FORGE_TO_INVENTORY(final Item item) {
		prepareCallable("UPDATE `obj_item` SET `item_container` = ? WHERE `UID` = ? AND `item_container` = 'forge';");
		setString(1, "inventory");
		setLong(2, (long) item.getObjectUUID());
		return (executeUpdate() != 0);
	}

	/**
	 * Attempts to update the quantity of this gold item
	 *
	 * @param value New quantity of gold
	 * @return True on success
	 */
	public boolean UPDATE_GOLD(final Item item, int value) {
		if (item == null)
			return false;
		return UPDATE_GOLD(item, value, item.getNumOfItems());
	}

	/**
	 * Attempts to update the quantity of this gold item using CAS
	 *
	 * @return True on success
	 */
	public boolean UPDATE_GOLD(final Item item, int newValue, int oldValue) {

		if (item.getItemBase().getType().equals(ItemType.GOLD) == false)
			return false;

		prepareCallable("UPDATE `obj_item` SET `item_numberOfItems`=? WHERE `UID`=?");
		setInt(1, newValue);
		setLong(2, (long) item.getObjectUUID());
		return (executeUpdate() != 0);
	}

	public boolean UPDATE_REMAINING_CHARGES(final Item item) {
		prepareCallable("UPDATE `obj_item` SET `item_chargesRemaining` = ? WHERE `UID` = ?");
		setInt(1, item.getChargesRemaining());
		setLong(2, (long) item.getObjectUUID());
		return (executeUpdate() != 0);
	}

	// This is necessary because default number of items is 1.
	// When we create gold, we want it to start at 0 quantity.

	public boolean ZERO_ITEM_STACK(Item item) {
		prepareCallable("UPDATE `obj_item` SET `item_numberOfItems`=0 WHERE `UID` = ?");
		setLong(1, (long) item.getObjectUUID());
		return (executeUpdate() != 0);
	}

	public boolean UPDATE_FLAGS(Item item) {
		prepareCallable("UPDATE `obj_item` SET `item_flags`=? WHERE `UID` = ?");
		setInt(1, item.getFlags());
		setLong(2, (long) item.getObjectUUID());
		return (executeUpdate() != 0);
	}

	public boolean UPDATE_VALUE(Item item,int value) {
		prepareCallable("UPDATE `obj_item` SET `item_value`=? WHERE `UID` = ?");
		setInt(1, value);
		setLong(2, (long) item.getObjectUUID());
		return (executeUpdate() != 0);
	}

}
