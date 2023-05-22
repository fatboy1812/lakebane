// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.gameManager.DbManager;
import engine.objects.Realm;
import org.pmw.tinylog.Logger;

import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

public class dbRealmHandler extends dbHandlerBase {

    public dbRealmHandler() {

    }

    public ConcurrentHashMap<Integer, Realm> LOAD_ALL_REALMS() {

        ConcurrentHashMap<Integer, Realm> realmList;
        Realm thisRealm;

        realmList = new ConcurrentHashMap<>();
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM obj_realm")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                recordsRead++;
                thisRealm = new Realm(rs);
                realmList.put(thisRealm.getRealmID(), thisRealm);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + realmList.size());
        return realmList;
    }

    public void REALM_UPDATE(Realm realm) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("CALL realm_UPDATE(?,?,?,?)")) {

            preparedStatement.setInt(1, realm.getRealmID());
            preparedStatement.setInt(2, (realm.getRulingCity() == null) ? 0 : realm.getRulingCity().getObjectUUID());
            preparedStatement.setInt(3, realm.getCharterType());

            if (realm.ruledSince != null)
                preparedStatement.setTimestamp(4, Timestamp.valueOf(realm.ruledSince));
            else
                preparedStatement.setNull(4, java.sql.Types.DATE);

            preparedStatement.execute();

        } catch (SQLException e) {
            Logger.error(e);
        }
    }
}
