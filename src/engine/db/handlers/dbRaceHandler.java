// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.gameManager.DbManager;
import engine.objects.Race;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class dbRaceHandler extends dbHandlerBase {

    public dbRaceHandler() {
    }

    public HashSet<Integer> BEARD_COLORS_FOR_RACE(final int id) {

        HashSet<Integer> integerSet = new HashSet<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `color` FROM `static_rune_racebeardcolor` WHERE `RaceID` = ?")) {

            preparedStatement.setInt(1, id);

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next())
                integerSet.add(rs.getInt(1));

        } catch (SQLException e) {
            Logger.error(e);
        }

        return integerSet;
    }

    public HashSet<Integer> BEARD_STYLES_FOR_RACE(final int id) {

        HashSet<Integer> integerSet = new HashSet<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `beardStyle` FROM `static_rune_racebeardstyle` WHERE `RaceID` = ?")) {

            preparedStatement.setInt(1, id);

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next())
                integerSet.add(rs.getInt(1));

        } catch (SQLException e) {
            Logger.error(e);
        }

        return integerSet;
    }

    public HashSet<Integer> HAIR_COLORS_FOR_RACE(final int id) {

        HashSet<Integer> integerSet = new HashSet<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `color` FROM `static_rune_racehaircolor` WHERE `RaceID` = ?")) {

            preparedStatement.setInt(1, id);

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next())
                integerSet.add(rs.getInt(1));

        } catch (SQLException e) {
            Logger.error(e);
        }

        return integerSet;

    }

    public HashSet<Integer> HAIR_STYLES_FOR_RACE(final int id) {

        HashSet<Integer> integerSet = new HashSet<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `hairStyle` FROM `static_rune_racehairstyle` WHERE `RaceID` = ?")) {

            preparedStatement.setInt(1, id);

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next())
                integerSet.add(rs.getInt(1));

        } catch (SQLException e) {
            Logger.error(e);
        }

        return integerSet;

    }

    public HashSet<Integer> SKIN_COLOR_FOR_RACE(final int id) {

        HashSet<Integer> integerSet = new HashSet<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `color` FROM `static_rune_raceskincolor` WHERE `RaceID` = ?")) {

            preparedStatement.setInt(1, id);

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next())
                integerSet.add(rs.getInt(1));

        } catch (SQLException e) {
            Logger.error(e);
        }

        return integerSet;
    }

    public ConcurrentHashMap<Integer, Race> LOAD_ALL_RACES() {

        ConcurrentHashMap<Integer, Race> races;
        Race thisRace;

        races = new ConcurrentHashMap<>();
        int recordsRead = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_rune_race")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                recordsRead++;
                thisRace = new Race(rs);

                races.put(thisRace.getRaceRuneID(), thisRace);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        Logger.info("read: " + recordsRead + " cached: " + races.size());
        return races;
    }
}
