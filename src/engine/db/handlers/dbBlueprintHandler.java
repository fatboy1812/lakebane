package engine.db.handlers;

import engine.gameManager.DbManager;
import engine.objects.Blueprint;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class dbBlueprintHandler extends dbHandlerBase {

    public dbBlueprintHandler() {

    }

    public HashMap<Integer, Integer> LOAD_ALL_DOOR_NUMBERS() {

        HashMap<Integer, Integer> doorInfo;
        doorInfo = new HashMap<>();

        int doorUUID;
        int doorNum;
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_building_doors ORDER BY doorMeshUUID ASC")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                recordsRead++;
                doorUUID = rs.getInt("doorMeshUUID");
                doorNum = rs.getInt("doorNumber");
                doorInfo.put(doorUUID, doorNum);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + doorInfo.size());
        return doorInfo;
    }

    public HashMap<Integer, Blueprint> LOAD_ALL_BLUEPRINTS() {

        HashMap<Integer, Blueprint> blueprints;
        Blueprint thisBlueprint;

        blueprints = new HashMap<>();
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `obj_account` WHERE `acct_uname`=?")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                recordsRead++;
                thisBlueprint = new Blueprint(rs);

                blueprints.put(thisBlueprint.getBlueprintUUID(), thisBlueprint);

                // load mesh cache
                Blueprint._meshLookup.putIfAbsent(thisBlueprint.getMeshForRank(-1), thisBlueprint);
                Blueprint._meshLookup.putIfAbsent(thisBlueprint.getMeshForRank(0), thisBlueprint);
                Blueprint._meshLookup.putIfAbsent(thisBlueprint.getMeshForRank(1), thisBlueprint);
                Blueprint._meshLookup.putIfAbsent(thisBlueprint.getMeshForRank(3), thisBlueprint);
                Blueprint._meshLookup.putIfAbsent(thisBlueprint.getMeshForRank(7), thisBlueprint);

            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + blueprints.size());
        return blueprints;
    }
}
