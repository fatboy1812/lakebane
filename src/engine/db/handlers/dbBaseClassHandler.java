// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum;
import engine.Enum.GameObjectType;
import engine.gameManager.DbManager;
import engine.objects.BaseClass;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class dbBaseClassHandler extends dbHandlerBase {

    public dbBaseClassHandler() {
        this.localClass = BaseClass.class;
        this.localObjectType = Enum.GameObjectType.BaseClass;
    }

    public BaseClass GET_BASE_CLASS(final int id) {

        if (id == 0)
            return null;

        BaseClass baseClass = (BaseClass) DbManager.getFromCache(GameObjectType.BaseClass, id);

        if (baseClass != null)
            return baseClass;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `obj_account` WHERE `UID`=?")) {

            preparedStatement.setLong(1, id);

            ResultSet rs = preparedStatement.executeQuery();
            baseClass = (BaseClass) getObjectFromRs(rs);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return baseClass;
    }

    public ArrayList<BaseClass> GET_BASECLASS_FOR_RACE(final int id) {

        ArrayList<BaseClass> baseClasses = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT b.* FROM `static_rune_baseclass` b, `static_rune_racebaseclass` r WHERE b.`ID` = r.`BaseClassID` && r.`RaceID` = ?")) {

            preparedStatement.setInt(1, id);

            ResultSet rs = preparedStatement.executeQuery();
            baseClasses = getObjectsFromRs(rs, 20);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return baseClasses;
    }

    public ArrayList<BaseClass> GET_ALL_BASE_CLASSES() {

        ArrayList<BaseClass> baseClasses = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_rune_baseclass`;")) {

            ResultSet rs = preparedStatement.executeQuery();
            baseClasses = getObjectsFromRs(rs, 20);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return baseClasses;
    }
}
