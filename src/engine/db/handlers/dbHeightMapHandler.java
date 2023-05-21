package engine.db.handlers;

import engine.InterestManagement.HeightMap;
import engine.gameManager.DbManager;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class dbHeightMapHandler extends dbHandlerBase {

    public dbHeightMapHandler() {


    }

    public void LOAD_ALL_HEIGHTMAPS() {

        HeightMap thisHeightmap;
        HeightMap.heightMapsCreated = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_zone_heightmap INNER JOIN static_zone_size ON static_zone_size.loadNum = static_zone_heightmap.zoneLoadID")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                thisHeightmap = new HeightMap(rs);

                if (thisHeightmap.getHeightmapImage() == null) {
                    Logger.info("Imagemap for " + thisHeightmap.getHeightMapID() + " was null");
                    continue;
                }
            }

        } catch (SQLException e) {
            Logger.error(e);
        }
    }

}
