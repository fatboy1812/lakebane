// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.gameManager.DbManager;
import engine.objects.Account;
import engine.objects.PlayerCharacter;
import engine.session.CSSession;
import engine.util.StringUtils;
import org.pmw.tinylog.Logger;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class dbCSSessionHandler extends dbHandlerBase {

    public dbCSSessionHandler() {
        this.localClass = CSSession.class;
        this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public boolean ADD_CSSESSION(String secKey, Account acc, InetAddress inet, String machineID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_session` (`secretKey`, `accountID`, `discordAccount`, `sessionIP`, machineID) VALUES (?,?,?,INET_ATON(?),?)")) {

            preparedStatement.setString(1, secKey);
            preparedStatement.setLong(2, acc.getObjectUUID());
            preparedStatement.setString(3, acc.discordAccount);
            preparedStatement.setString(4, StringUtils.InetAddressToClientString(inet));
            preparedStatement.setString(5, machineID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean DELETE_UNUSED_CSSESSION(String secKey) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_session` WHERE `secretKey`=? && `characterID` IS NULL")) {

            preparedStatement.setString(1, secKey);
            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean DELETE_CSSESSION(String secKey) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_session` WHERE `secretKey`=?")) {

            preparedStatement.setString(1, secKey);
            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean UPDATE_CSSESSION(String secKey, int charID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_session` SET `characterID`=? WHERE `secretKey`=?")) {

            preparedStatement.setInt(1, charID);
            preparedStatement.setString(2, secKey);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public CSSession GET_CSSESSION(String secKey) {

        CSSession css = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `accountID`, `characterID`, `machineID` FROM `dyn_session` WHERE `secretKey`=?")) {

            preparedStatement.setString(1, secKey);
            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next())
                css = new CSSession(secKey, DbManager.AccountQueries.GET_ACCOUNT(rs.getInt("accountID")), PlayerCharacter.getPlayerCharacter(rs.getInt("characterID")), rs.getString("machineID"));

        } catch (SQLException e) {
            Logger.error(e);
        }

        return css;
    }
}
