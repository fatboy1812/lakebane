// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.gameManager.DbManager;
import engine.objects.BuildingLocation;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class dbBuildingLocationHandler extends dbHandlerBase {

    public dbBuildingLocationHandler() {
        this.localClass = BuildingLocation.class;
        this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public ArrayList<BuildingLocation> LOAD_BUILDING_LOCATIONS() {

        ArrayList<BuildingLocation> buildingLocations = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("select `ID`, `BuildingID`, `type`, `slot`, `unknown`, `locX`, `locY`, `locZ`, `w`,  `rotX`, `rotY`, `rotZ` from static_building_location " +
                     "where type = 6 or type = 8 " +
                     "GROUP BY buildingID, slot " +
                     "ORDER BY buildingID, slot ASC;")) {

            ResultSet rs = preparedStatement.executeQuery();
            buildingLocations = getObjectsFromRs(rs, 20);

        } catch (SQLException e) {
            Logger.error(e);
            return buildingLocations;
        }

        return buildingLocations;
    }
}
