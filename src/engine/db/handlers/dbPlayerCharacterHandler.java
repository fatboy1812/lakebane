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
import engine.objects.AbstractWorldObject;
import engine.objects.Heraldry;
import engine.objects.PlayerCharacter;
import engine.objects.PlayerFriends;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class dbPlayerCharacterHandler extends dbHandlerBase {

    public dbPlayerCharacterHandler() {
        this.localClass = PlayerCharacter.class;
        this.localObjectType = Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public PlayerCharacter ADD_PLAYER_CHARACTER(final PlayerCharacter toAdd) {

        PlayerCharacter playerCharacter = null;

        if (toAdd.getAccount() == null)
            return null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("CALL `character_CREATE`(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {

            preparedStatement.setLong(1, toAdd.getAccount().getObjectUUID());
            preparedStatement.setString(2, toAdd.getFirstName());
            preparedStatement.setString(3, toAdd.getLastName());
            preparedStatement.setInt(4, toAdd.getRace().getRaceRuneID());
            preparedStatement.setInt(5, toAdd.getBaseClass().getObjectUUID());
            preparedStatement.setInt(6, toAdd.getStrMod());
            preparedStatement.setInt(7, toAdd.getDexMod());
            preparedStatement.setInt(8, toAdd.getConMod());
            preparedStatement.setInt(9, toAdd.getIntMod());
            preparedStatement.setInt(10, toAdd.getSpiMod());
            preparedStatement.setInt(11, toAdd.getExp());
            preparedStatement.setInt(12, toAdd.getSkinColor());
            preparedStatement.setInt(13, toAdd.getHairColor());
            preparedStatement.setByte(14, toAdd.getHairStyle());
            preparedStatement.setInt(15, toAdd.getBeardColor());
            preparedStatement.setByte(16, toAdd.getBeardStyle());

            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {

                int objectUUID = (int) rs.getLong("UID");

                if (objectUUID > 0)
                    playerCharacter = GET_PLAYER_CHARACTER(objectUUID);
            }
        } catch (SQLException e) {
            Logger.error(e);
        }

        return playerCharacter;
    }

    public boolean SET_IGNORE_LIST(int sourceID, int targetID, boolean toIgnore, String charName) {

        String queryString = "";

        if (toIgnore)
            queryString = "INSERT INTO `dyn_character_ignore` (`accountUID`, `ignoringUID`, `characterName`) VALUES (?, ?, ?)";
        else
            queryString = "DELETE FROM `dyn_character_ignore` WHERE `accountUID` = ? && `ignoringUID` = ?";

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            preparedStatement.setLong(1, sourceID);
            preparedStatement.setLong(2, targetID);

            if (toIgnore)
                preparedStatement.setString(3, charName);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public ArrayList<PlayerCharacter> GET_CHARACTERS_FOR_ACCOUNT(final int id) {

        ArrayList<PlayerCharacter> characterList = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_character`.*, `object`.`parent` FROM `object` INNER JOIN `obj_character` ON `obj_character`.`UID` = `object`.`UID` WHERE `object`.`parent`=? && `obj_character`.`char_isActive`='1';")) {

            preparedStatement.setLong(1, id);
            ResultSet rs = preparedStatement.executeQuery();
            characterList = getObjectsFromRs(rs, 10);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return characterList;
    }

    public ArrayList<PlayerCharacter> GET_ALL_CHARACTERS() {

        ArrayList<PlayerCharacter> characterList = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_character`.*, `object`.`parent` FROM `object` INNER JOIN `obj_character` ON `obj_character`.`UID` = `object`.`UID` WHERE `obj_character`.`char_isActive`='1';")) {

            ResultSet rs = preparedStatement.executeQuery();
            characterList = getObjectsFromRs(rs, 2000);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return characterList;
    }

    /**
     * <code>getFirstName</code> looks up the first name of a PlayerCharacter by
     * first checking the GOM cache and then querying the database.
     * PlayerCharacter objects that are not already cached won't be instantiated
     * and cached.
     */

    public ConcurrentHashMap<Integer, String> GET_IGNORE_LIST(final int objectUUID, final boolean skipActiveCheck) {

        ConcurrentHashMap<Integer, String> ignoreList = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_character_ignore` WHERE `accountUID` = ?;")) {

            preparedStatement.setLong(1, objectUUID);

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                int ignoreCharacterID = rs.getInt("ignoringUID");

                if (ignoreCharacterID == 0)
                    continue;

                String name = rs.getString("characterName");
                ignoreList.put(ignoreCharacterID, name);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        return ignoreList;
    }

    public PlayerCharacter GET_PLAYER_CHARACTER(final int objectUUID) {

        if (objectUUID == 0)
            return null;

        PlayerCharacter playerCharacter = (PlayerCharacter) DbManager.getFromCache(Enum.GameObjectType.PlayerCharacter, objectUUID);

        if (playerCharacter != null)
            return playerCharacter;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_character`.*, `object`.`parent` FROM `object` INNER JOIN `obj_character` ON `obj_character`.`UID` = `object`.`UID` WHERE `object`.`UID` = ?")) {

            preparedStatement.setLong(1, objectUUID);

            ResultSet rs = preparedStatement.executeQuery();
            playerCharacter = (PlayerCharacter) getObjectFromRs(rs);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return playerCharacter;
    }

    public boolean IS_CHARACTER_NAME_UNIQUE(final String firstName) {

        boolean unique = true;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `char_firstname` FROM `obj_character` WHERE `char_isActive`=1 && `char_firstname`=?")) {

            preparedStatement.setString(1, firstName);

            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next())
                unique = false;

        } catch (SQLException e) {
            Logger.error(e);
        }

        return unique;
    }

    public boolean UPDATE_NAME(String oldFirstName, String newFirstName, String newLastName) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `char_firstname`=?, `char_lastname`=? WHERE `char_firstname`=? AND `char_isActive`='1'")) {

            preparedStatement.setString(1, newFirstName);
            preparedStatement.setString(2, newLastName);
            preparedStatement.setString(3, oldFirstName);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean SET_DELETED(final PlayerCharacter pc) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `char_isActive`=? WHERE `UID` = ?")) {

            preparedStatement.setBoolean(1, !pc.isDeleted());
            preparedStatement.setLong(2, pc.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean SET_ACTIVE(final PlayerCharacter pc, boolean status) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `char_isActive`=? WHERE `UID` = ?")) {

            preparedStatement.setBoolean(1, status);
            preparedStatement.setLong(2, pc.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean SET_BIND_BUILDING(final PlayerCharacter pc, int bindBuildingID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `char_bindBuilding`=? WHERE `UID` = ?")) {

            preparedStatement.setInt(1, bindBuildingID);
            preparedStatement.setLong(2, pc.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean SET_ANNIVERSERY(final PlayerCharacter pc, boolean flag) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `anniversery`=? WHERE `UID` = ?")) {

            preparedStatement.setBoolean(1, flag);
            preparedStatement.setLong(2, pc.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }


    public boolean UPDATE_CHARACTER_EXPERIENCE(final PlayerCharacter pc) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `char_experience`=? WHERE `UID` = ?")) {

            preparedStatement.setInt(1, pc.getExp());
            preparedStatement.setLong(2, pc.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean UPDATE_GUILD(final PlayerCharacter pc, int guildUUID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `guildUID`=? WHERE `UID` = ?")) {

            preparedStatement.setInt(1, guildUUID);
            preparedStatement.setLong(2, pc.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean UPDATE_CHARACTER_STATS(final PlayerCharacter pc) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `char_strMod`=?, `char_dexMod`=?, `char_conMod`=?, `char_intMod`=?, `char_spiMod`=? WHERE `UID`=?")) {

            preparedStatement.setInt(1, pc.getStrMod());
            preparedStatement.setInt(2, pc.getDexMod());
            preparedStatement.setInt(3, pc.getConMod());
            preparedStatement.setInt(4, pc.getIntMod());
            preparedStatement.setInt(5, pc.getSpiMod());
            preparedStatement.setLong(6, pc.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public String SET_PROPERTY(final PlayerCharacter playerCharacter, String name, Object new_value) {

        String result = "";

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("CALL character_SETPROP(?,?,?)")) {

            preparedStatement.setLong(1, playerCharacter.getObjectUUID());
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, String.valueOf(new_value));

            ResultSet rs = preparedStatement.executeQuery();
            result = rs.getString("result");

        } catch (SQLException e) {
            Logger.error(e);
        }
        return result;
    }

    public boolean SET_PROMOTION_CLASS(PlayerCharacter player, int promotionClassID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `char_promotionClassID`=?  WHERE `UID`=?;")) {

            preparedStatement.setInt(1, promotionClassID);
            preparedStatement.setInt(2, player.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean SET_INNERCOUNCIL(PlayerCharacter player, boolean isInnerCouncil) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `guild_isInnerCouncil`=?  WHERE `UID`=?;")) {

            preparedStatement.setBoolean(1, isInnerCouncil);
            preparedStatement.setInt(2, player.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean SET_FULL_MEMBER(PlayerCharacter player, boolean isFullMember) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `guild_isFullMember`=?  WHERE `UID`=?;")) {

            preparedStatement.setBoolean(1, isFullMember);
            preparedStatement.setInt(2, player.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean SET_TAX_COLLECTOR(PlayerCharacter player, boolean isTaxCollector) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `guild_isTaxCollector`=?  WHERE `UID`=?;")) {

            preparedStatement.setBoolean(1, isTaxCollector);
            preparedStatement.setInt(2, player.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean SET_RECRUITER(PlayerCharacter player, boolean isRecruiter) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `guild_isRecruiter`=?  WHERE `UID`=?;")) {

            preparedStatement.setBoolean(1, isRecruiter);
            preparedStatement.setInt(2, player.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean SET_GUILD_TITLE(PlayerCharacter player, int title) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `guild_title`=?  WHERE `UID`=?;")) {

            preparedStatement.setInt(1, title);
            preparedStatement.setInt(2, player.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean ADD_FRIEND(int source, long friend) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_character_friends` (`playerUID`, `friendUID`) VALUES (?, ?)")) {

            preparedStatement.setLong(1, source);
            preparedStatement.setLong(2, friend);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean REMOVE_FRIEND(int source, int friend) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_character_friends` WHERE (`playerUID`=?) AND (`friendUID`=?)")) {

            preparedStatement.setLong(1, source);
            preparedStatement.setLong(2, friend);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public void LOAD_PLAYER_FRIENDS() {

        PlayerFriends playerFriend;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM dyn_character_friends")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next())
                playerFriend = new PlayerFriends(rs);

        } catch (SQLException e) {
            Logger.error(e);
        }

    }

    public boolean ADD_HERALDY(int source, AbstractWorldObject character) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_character_heraldy` (`playerUID`, `characterUID`,`characterType`) VALUES (?, ?,?)")) {

            preparedStatement.setLong(1, source);
            preparedStatement.setLong(2, character.getObjectUUID());
            preparedStatement.setInt(3, character.getObjectType().ordinal());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean REMOVE_HERALDY(int source, int characterUID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_character_heraldy` WHERE (`playerUID`=?) AND (`characterUID`=?)")) {

            preparedStatement.setLong(1, source);
            preparedStatement.setLong(2, characterUID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public void LOAD_HERALDY() {

        Heraldry heraldy;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM dyn_character_heraldy")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next())
                heraldy = new Heraldry(rs);

        } catch (SQLException e) {
            Logger.error(e);
        }
    }

}
