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
import engine.objects.Contract;
import engine.objects.ItemBase;
import engine.objects.MobEquipment;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class dbContractHandler extends dbHandlerBase {

    public dbContractHandler() {
        this.localClass = Contract.class;
        this.localObjectType = Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public Contract GET_CONTRACT(final int objectUUID) {

        Contract contract = (Contract) DbManager.getFromCache(Enum.GameObjectType.Contract, objectUUID);

        if (contract != null)
            return contract;

        if (objectUUID == 0)
            return null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_npc_contract` WHERE `ID` = ?")) {

            preparedStatement.setInt(1, objectUUID);

            ResultSet rs = preparedStatement.executeQuery();
            contract = (Contract) getObjectFromRs(rs);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return contract;
    }

    public void LOAD_CONTRACT_INVENTORY(final Contract contract) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_npc_inventoryset` WHERE `inventorySet` = ?;")) {

            preparedStatement.setInt(1, contract.inventorySet);

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                //handle item base
                int itemBaseID = rs.getInt("itembaseID");

                ItemBase ib = ItemBase.getItemBase(itemBaseID);

                if (ib != null) {

                    MobEquipment me = new MobEquipment(ib, 0, 0);
                    contract.getSellInventory().add(me);

                    //handle magic effects
                    String prefix = rs.getString("prefix");
                    int pRank = rs.getInt("pRank");
                    String suffix = rs.getString("suffix");
                    int sRank = rs.getInt("sRank");

                    if (prefix != null) {
                        me.setPrefix(prefix, pRank);
                        me.setIsID(true);
                    }

                    if (suffix != null) {
                        me.setSuffix(suffix, sRank);
                        me.setIsID(true);
                    }

                }
            }
        } catch (SQLException e) {
            Logger.error(e);
        }
    }

    public void LOAD_SELL_LIST_FOR_CONTRACT(final Contract contract) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_npc_contract_selltype` WHERE `contractID` = ?;")) {

            preparedStatement.setInt(1, contract.getObjectUUID());

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                int type = rs.getInt("type");
                int value = rs.getInt("value");

                switch (type) {
                    case 1:
                        contract.getBuyItemType().add(value);
                        break;
                    case 2:
                        contract.getBuySkillToken().add(value);
                        break;
                    case 3:
                        contract.getBuyUnknownToken().add(value);
                        break;
                }
            }
        } catch (SQLException e) {
            Logger.error(e);
        }
    }

    public boolean updateAllowedBuildings(final Contract con, final long slotbitvalue) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `static_npc_contract` SET `allowedBuildingTypeID`=? WHERE `contractID`=?")) {

            preparedStatement.setLong(1, slotbitvalue);
            preparedStatement.setInt(2, con.getContractID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean updateDatabase(final Contract con) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `static_npc_contract` SET `contractID`=?, `name`=?, "
                     + "`mobbaseID`=?, `classID`=?, vendorDialog=?, iconID=?, allowedBuildingTypeID=? WHERE `ID`=?")) {

            preparedStatement.setInt(1, con.getContractID());
            preparedStatement.setString(2, con.getName());
            preparedStatement.setInt(3, con.getMobbaseID());
            preparedStatement.setInt(4, con.getClassID());
            preparedStatement.setInt(5, (con.getVendorDialog() != null) ? con.getVendorDialog().getObjectUUID() : 0);
            preparedStatement.setInt(6, con.getIconID());
            preparedStatement.setInt(8, con.getObjectUUID());
            preparedStatement.setLong(7, con.getAllowedBuildings().toLong());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }
}
