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
import engine.objects.CharacterRune;
import org.pmw.tinylog.Logger;

import java.sql.*;
import java.util.ArrayList;

public class dbCharacterRuneHandler extends dbHandlerBase {

    public dbCharacterRuneHandler() {
        this.localClass = CharacterRune.class;
        this.localObjectType = Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public CharacterRune ADD_CHARACTER_RUNE(final CharacterRune toAdd) {

        CharacterRune characterRune = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_character_rune` (`CharacterID`, `RuneBaseID`) VALUES (?, ?);", Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setLong(1, toAdd.getPlayerID());
            preparedStatement.setInt(2, toAdd.getRuneBaseID());

            preparedStatement.executeUpdate();
            ResultSet rs = preparedStatement.getGeneratedKeys();

            if (rs.next())
                characterRune = GET_CHARACTER_RUNE(rs.getInt(1));

        } catch (SQLException e) {
            Logger.error(e);
        }

        return characterRune;
    }

    public CharacterRune GET_CHARACTER_RUNE(int runeID) {

        CharacterRune characterRune = (CharacterRune) DbManager.getFromCache(Enum.GameObjectType.CharacterRune, runeID);

        if (characterRune != null)
            return characterRune;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_character_rune` WHERE `UID`=?")) {

            preparedStatement.setInt(1, runeID);
            ResultSet rs = preparedStatement.executeQuery();

            characterRune = (CharacterRune) getObjectFromRs(rs);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return characterRune;
    }

    public boolean DELETE_CHARACTER_RUNE(final CharacterRune characterRune) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_character_rune` WHERE `UID`=?;")) {

            preparedStatement.setLong(1, characterRune.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public ArrayList<CharacterRune> GET_RUNES_FOR_CHARACTER(final int characterId) {

        ArrayList<CharacterRune> characterRunes = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_character_rune` WHERE `CharacterID` = ?")) {

            preparedStatement.setInt(1, characterId);

            ResultSet rs = preparedStatement.executeQuery();
            characterRunes = getObjectsFromRs(rs, 10);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return characterRunes;
    }

    public void updateDatabase(final CharacterRune characterRune) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_character_rune` SET `CharacterID`=?, `RuneBaseID`=? WHERE `UID` = ?")) {

            preparedStatement.setInt(1, characterRune.getPlayerID());
            preparedStatement.setInt(2, characterRune.getRuneBaseID());
            preparedStatement.setLong(3, characterRune.getObjectUUID());

            preparedStatement.execute();

        } catch (SQLException e) {
            Logger.error(e);
        }
    }

}
