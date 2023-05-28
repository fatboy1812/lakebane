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

            if (rs.next()) {
                int objectUUID = (int) rs.getLong("UID");

                if (objectUUID > 0)
                    return GET_ITEM(objectUUID);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        return null;
    }

    public boolean DO_TRADE(HashSet<Integer> from1, HashSet<Integer> from2,
                            CharacterItemManager man1, CharacterItemManager man2,
                            Item inventoryGold1, Item inventoryGold2, int goldFrom1, int goldFrom2) {

        AbstractCharacter ac1 = man1.getOwner();
        AbstractCharacter ac2 = man2.getOwner();
        boolean worked = false;

        if (ac1 == null || ac2 == null || inventoryGold1 == null || inventoryGold2 == null)
            return false;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("CALL `item_TRADE`(?, ?, ?, ?, ?, ?, ?, ?)")) {

            preparedStatement.setString(1, formatTradeString(from1));
            preparedStatement.setLong(2, ac1.getObjectUUID());
            preparedStatement.setString(3, formatTradeString(from2));
            preparedStatement.setLong(4, ac2.getObjectUUID());
            preparedStatement.setInt(5, goldFrom1);
            preparedStatement.setLong(6, inventoryGold1.getObjectUUID());
            preparedStatement.setInt(7, goldFrom2);
            preparedStatement.setLong(8, inventoryGold2.getObjectUUID());

            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next())
                worked = rs.getBoolean("result");

        } catch (SQLException e) {
            Logger.error(e);
        }
        return worked;
    }

    public ArrayList<Item> GET_EQUIPPED_ITEMS(final int targetId) {

        ArrayList<Item> itemList;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_item`.*, `object`.`parent`, `object`.`type` FROM `object` INNER JOIN `obj_item` ON `object`.`UID` = `obj_item`.`UID` WHERE `object`.`parent`=? && `obj_item`.`item_container`='equip';")) {

            preparedStatement.setLong(1, targetId);
            ResultSet rs = preparedStatement.executeQuery();

            itemList = getObjectsFromRs(rs, 10);

        } catch (SQLException e) {
            Logger.error(e);
            return null;
        }

        return itemList;
    }

    public Item GET_ITEM(final int itemUUID) {

        Item item;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_item`.*, `object`.`parent`, `object`.`type` FROM `object` INNER JOIN `obj_item` ON `object`.`UID` = `obj_item`.`UID` WHERE `object`.`UID`=?;")) {

            preparedStatement.setLong(1, itemUUID);
            ResultSet rs = preparedStatement.executeQuery();

            item = (Item) getObjectFromRs(rs);

        } catch (SQLException e) {
            Logger.error(e);
            return null;
        }
        return item;
    }

    public ArrayList<Item> GET_ITEMS_FOR_ACCOUNT(final int accountId) {

        ArrayList<Item> itemList;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_item`.*, `object`.`parent`, `object`.`type` FROM `object` INNER JOIN `obj_item` ON `object`.`UID` = `obj_item`.`UID` WHERE `object`.`parent`=?;")) {

            preparedStatement.setLong(1, accountId);
            ResultSet rs = preparedStatement.executeQuery();

            itemList = getObjectsFromRs(rs, 100);

        } catch (SQLException e) {
            Logger.error(e);
            return null;
        }
        return itemList;
    }

    public ArrayList<Item> GET_ITEMS_FOR_NPC(final int npcId) {

        ArrayList<Item> itemList;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_item`.*, `object`.`parent`, `object`.`type` FROM `object` INNER JOIN `obj_item` ON `object`.`UID` = `obj_item`.`UID` WHERE `object`.`parent`=?;")) {

            preparedStatement.setLong(1, npcId);
            ResultSet rs = preparedStatement.executeQuery();

            itemList = getObjectsFromRs(rs, 20);

        } catch (SQLException e) {
            Logger.error(e);
            return null;
        }
        return itemList;
    }

    public ArrayList<Item> GET_ITEMS_FOR_PC(final int id) {

        ArrayList<Item> itemList;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_item`.*, `object`.`parent`, `object`.`type` FROM `object` INNER JOIN `obj_item` ON `object`.`UID` = `obj_item`.`UID` WHERE `object`.`parent`=?")) {

            preparedStatement.setLong(1, id);
            ResultSet rs = preparedStatement.executeQuery();

            itemList = getObjectsFromRs(rs, 100);

        } catch (SQLException e) {
            Logger.error(e);
            return null;
        }
        return itemList;
    }

    public boolean MOVE_GOLD(final Item from, final Item to, final int amt) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_item` SET `item_numberOfItems` = CASE WHEN `UID`=?  THEN ? WHEN `UID`=? THEN ? END WHERE `UID` IN (?, ?);")) {

            int newFromAmt = from.getNumOfItems() - amt;
            int newToAmt = to.getNumOfItems() + amt;

            preparedStatement.setLong(1, from.getObjectUUID());
            preparedStatement.setInt(2, newFromAmt);
            preparedStatement.setLong(3, to.getObjectUUID());
            preparedStatement.setInt(4, newToAmt);
            preparedStatement.setLong(5, from.getObjectUUID());
            preparedStatement.setLong(6, to.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean ORPHAN_INVENTORY(final HashSet<Item> inventory) {

        boolean worked = true;

        for (Item item : inventory) {

            if (item.getItemBase().getType().equals(ItemType.GOLD))
                continue;

            try (Connection connection = DbManager.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_item` LEFT JOIN `object` ON `object`.`UID` = `obj_item`.`UID` SET `object`.`parent`=NULL, `obj_item`.`item_container`='none' WHERE `object`.`UID`=?;")) {

                preparedStatement.setLong(1, item.getObjectUUID());
                worked = (preparedStatement.executeUpdate() > 0);

                if (worked)
                    item.zeroItem();

            } catch (SQLException e) {
                Logger.error(e);
                return false;
            }
        }
        return worked;
    }

    public HashSet<Integer> GET_ITEMS_FOR_VENDOR(final int vendorID) {

        HashSet<Integer> itemSet = new HashSet<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT ID FROM static_itembase WHERE vendorType = ?")) {

            preparedStatement.setInt(1, vendorID);

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next())
                itemSet.add(rs.getInt(1));

        } catch (SQLException e) {
            Logger.error(e);
            return itemSet;
        }

        return itemSet;
    }

    //Used to transfer a single item between owners or equip or vault or bank or inventory
    public boolean UPDATE_OWNER(final Item item, int newOwnerID, boolean ownerNPC, boolean ownerPlayer,
                                boolean ownerAccount, ItemContainerType containerType, int slot) {

        boolean worked = false;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("CALL `item_TRANSFER_OWNER`(?, ?, ?, ? )")) {

            preparedStatement.setLong(1, item.getObjectUUID());

            if (newOwnerID != 0)
                preparedStatement.setLong(2, newOwnerID);
            else
                preparedStatement.setNull(2, java.sql.Types.BIGINT);

            switch (containerType) {
                case INVENTORY:
                    preparedStatement.setString(3, "inventory");
                    break;
                case EQUIPPED:
                    preparedStatement.setString(3, "equip");
                    break;
                case BANK:
                    preparedStatement.setString(3, "bank");
                    break;
                case VAULT:
                    preparedStatement.setString(3, "vault");
                    break;
                case FORGE:
                    preparedStatement.setString(3, "forge");
                    break;
                default:
                    preparedStatement.setString(3, "none"); //Shouldn't be here
                    break;
            }
            preparedStatement.setInt(4, slot);
            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next())
                worked = rs.getBoolean("result");

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
        return worked;
    }

    public boolean SET_DURABILITY(final Item item, int value) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_item` SET `item_durabilityCurrent`=? WHERE `UID`=? AND `item_durabilityCurrent`=?")) {

            preparedStatement.setInt(1, value);
            preparedStatement.setLong(2, item.getObjectUUID());
            preparedStatement.setInt(3, item.getDurabilityCurrent());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public boolean UPDATE_FORGE_TO_INVENTORY(final Item item) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_item` SET `item_container` = ? WHERE `UID` = ? AND `item_container` = 'forge';")) {

            preparedStatement.setString(1, "inventory");
            preparedStatement.setLong(2, item.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }

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

        if (!item.getItemBase().getType().equals(ItemType.GOLD))
            return false;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_item` SET `item_numberOfItems`=? WHERE `UID`=?")) {

            preparedStatement.setInt(1, newValue);
            preparedStatement.setLong(2, item.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }

    }

    public boolean UPDATE_REMAINING_CHARGES(final Item item) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_item` SET `item_chargesRemaining` = ? WHERE `UID` = ?")) {

            preparedStatement.setInt(1, item.getChargesRemaining());
            preparedStatement.setLong(2, item.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }

    }

    // This is necessary because default number of items is 1.
    // When we create gold, we want it to start at 0 quantity.

    public boolean ZERO_ITEM_STACK(Item item) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_item` SET `item_numberOfItems`=0 WHERE `UID` = ?")) {

            preparedStatement.setLong(1, item.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public boolean UPDATE_FLAGS(Item item) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_item` SET `item_flags`=? WHERE `UID` = ?")) {

            preparedStatement.setInt(1, item.getFlags());
            preparedStatement.setLong(2, item.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public boolean UPDATE_VALUE(Item item, int value) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_item` SET `item_value`=? WHERE `UID` = ?")) {

            preparedStatement.setInt(1, value);
            preparedStatement.setLong(2, item.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }
}
