// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum;
import engine.Enum.GuildHistoryType;
import engine.gameManager.DbManager;
import engine.objects.*;
import engine.server.MBServerStatics;
import org.joda.time.DateTime;
import org.pmw.tinylog.Logger;

import java.sql.*;
import java.util.ArrayList;

public class dbGuildHandler extends dbHandlerBase {

    public dbGuildHandler() {
        this.localClass = Guild.class;
        this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public static ArrayList<PlayerCharacter> GET_GUILD_BANISHED(final int id) {

        return new ArrayList<>();

        // Bugfix
        // prepareCallable("SELECT * FROM `obj_character`, `dyn_guild_banishlist` WHERE `obj_character.char_isActive` = 1 AND `dyn_guild_banishlist.CharacterID` = `obj_character.UID` AND `obj_character.GuildID`=?");

        //prepareCallable("SELECT * FROM `obj_character` `,` `dyn_guild_banishlist` WHERE obj_character.char_isActive = 1 AND dyn_guild_banishlist.CharacterID = obj_character.UID AND dyn_guild_banishlist.GuildID = ?");
        //setLong(1, (long) id);

        //return getObjectList();
    }

    public static void LOAD_GUILD_HISTORY_FOR_PLAYER(PlayerCharacter playerCharacter) {

        if (playerCharacter == null)
            return;

        ArrayList<GuildHistory> guildList = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_guild_allianceenemylist` WHERE `GuildID` = ?")) {

            preparedStatement.setInt(1, playerCharacter.getObjectUUID());
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                GuildHistory guildEntry = new GuildHistory(rs);
                guildList.add(guildEntry);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        playerCharacter.setGuildHistory(guildList);
    }

    public int BANISH_FROM_GUILD_OFFLINE(final int target, boolean sourceIsGuildLeader) {

        String queryString;
        int rowCount;

        // Only a Guild Leader can remove inner council

        if (sourceIsGuildLeader)
            queryString = "UPDATE `obj_character` SET `guildUID`=NULL, `guild_isInnerCouncil`=0, `guild_isTaxCollector`=0,"
                    + " `guild_isRecruiter`=0, `guild_isFullMember`=0, `guild_title`=0 WHERE `UID`=?";
        else
            queryString = "UPDATE `obj_character` SET `guildUID`=NULL, `guild_isInnerCouncil`=0, `guild_isTaxCollector`=0,"
                    + " `guild_isRecruiter`=0, `guild_isFullMember`=0, `guild_title`=0 WHERE `UID`=? && `guild_isInnerCouncil`=0";

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            preparedStatement.setLong(1, target);
            rowCount = preparedStatement.executeUpdate();

        } catch (SQLException e) {
            Logger.error(e);
            return 0;
        }

        return rowCount;
    }

    public boolean ADD_TO_BANISHED_FROM_GUILDLIST(int target, long characterID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO  `dyn_guild_banishlist` (`GuildID`, `CharacterID`) VALUES (?,?)")) {

            preparedStatement.setLong(1, target);
            preparedStatement.setLong(2, characterID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean REMOVE_FROM_BANISH_LIST(int target, long characterID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_guild_banishlist` (`GuildID`, `CharacterID`) VALUES (?,?)")) {

            preparedStatement.setLong(1, target);
            preparedStatement.setLong(2, characterID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean ADD_TO_GUILDHISTORY(int target, PlayerCharacter playerCharacter, DateTime historyDate, GuildHistoryType historyType) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO  `dyn_character_guildhistory` (`GuildID`, `CharacterID`, `historyDate`, `historyType`) VALUES (?,?,?,?)")) {

            preparedStatement.setLong(1, target);
            preparedStatement.setLong(2, playerCharacter.getObjectUUID());

            if (historyDate == null)
                preparedStatement.setNull(3, java.sql.Types.DATE);
            else
                preparedStatement.setTimestamp(3, new Timestamp(historyDate.getMillis()));

            preparedStatement.setString(4, historyType.name());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    //TODO Need to get this working.
    public ArrayList<Guild> GET_GUILD_HISTORY_OF_PLAYER(final int id) {

        ArrayList<Guild> guildList = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT g.* FROM `obj_guild` g, `dyn_character_guildhistory` l WHERE  g.`UID` = l.`GuildID` && l.`CharacterID` = ?")) {

            preparedStatement.setLong(1, id);

            ResultSet rs = preparedStatement.executeQuery();
            guildList = getObjectsFromRs(rs, 20);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return guildList;
    }

    public ArrayList<Guild> GET_GUILD_ALLIES(final int id) {

        ArrayList<Guild> guildList = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT g.* FROM `obj_guild` g, `dyn_guild_allianceenemylist` l "
                     + "WHERE l.isAlliance = 1 && l.OtherGuildID = g.UID && l.GuildID=?")) {

            preparedStatement.setLong(1, id);

            ResultSet rs = preparedStatement.executeQuery();
            guildList = getObjectsFromRs(rs, 20);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return guildList;

    }

    public ArrayList<Guild> GET_GUILD_ENEMIES(final int id) {

        ArrayList<Guild> guildList = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT g.* FROM `obj_guild` g, `dyn_guild_allianceenemylist` l "
                     + "WHERE l.isAlliance = 0 && l.OtherGuildID = g.UID && l.GuildID=?")) {

            preparedStatement.setLong(1, id);

            ResultSet rs = preparedStatement.executeQuery();
            guildList = getObjectsFromRs(rs, 20);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return guildList;

    }

    public ArrayList<PlayerCharacter> GET_GUILD_KOS_CHARACTER(final int id) {

        ArrayList<PlayerCharacter> kosList = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT c.* FROM `obj_character` c, `dyn_guild_characterkoslist` l WHERE c.`char_isActive` = 1 && l.`KOSCharacterID` = c.`UID` && l.`GuildID`=?")) {

            preparedStatement.setLong(1, id);
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                int playerUUID = rs.getInt(1);
                PlayerCharacter kosPlayer = PlayerCharacter.getPlayerCharacter(playerUUID);

                if (kosPlayer != null)
                    kosList.add(kosPlayer);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        return kosList;
    }

    public ArrayList<Guild> GET_GUILD_KOS_GUILD(final int id) {

        ArrayList<Guild> guildList = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT g.* FROM `obj_guild` g, `dyn_guild_guildkoslist` l "
                     + "WHERE l.KOSGuildID = g.UID && l.GuildID = ?")) {

            preparedStatement.setLong(1, id);

            ResultSet rs = preparedStatement.executeQuery();
            guildList = getObjectsFromRs(rs, 20);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return guildList;
    }

    public ArrayList<Guild> GET_SUB_GUILDS(final int guildID) {

        ArrayList<Guild> guildList = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_guild`.*, `object`.`parent` FROM `object` INNER JOIN `obj_guild` ON `obj_guild`.`UID` = `object`.`UID` WHERE `object`.`parent` = ?;")) {

            preparedStatement.setInt(1, guildID);

            ResultSet rs = preparedStatement.executeQuery();
            guildList = getObjectsFromRs(rs, 20);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return guildList;
    }

    public Guild GET_GUILD(int id) {

        Guild guild = (Guild) DbManager.getFromCache(Enum.GameObjectType.Guild, id);

        if (guild != null)
            return guild;

        if (id == 0)
            return Guild.getErrantGuild();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_guild`.*, `object`.`parent` FROM `obj_guild` INNER JOIN `object` ON `object`.`UID` = `obj_guild`.`UID` WHERE `object`.`UID`=?")) {

            preparedStatement.setLong(1, id);

            ResultSet rs = preparedStatement.executeQuery();
            guild = (Guild) getObjectFromRs(rs);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return guild;

    }

    public ArrayList<Guild> GET_ALL_GUILDS() {

        ArrayList<Guild> guildList = null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `obj_guild`.*, `object`.`parent` FROM `obj_guild` INNER JOIN `object` ON `object`.`UID` = `obj_guild`.`UID`")) {

            ResultSet rs = preparedStatement.executeQuery();
            guildList = getObjectsFromRs(rs, 100);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return guildList;
    }

    public boolean IS_CREST_UNIQUE(final GuildTag gt) {

        boolean valid = false;
        String queryString;

        // Ignore bg symbol if bg color is the same as fg color.

        if (gt.backgroundColor01 == gt.backgroundColor02)
            queryString = "SELECT `name` FROM `obj_guild` WHERE `backgroundColor01`=? && `backgroundColor02`=? && `symbolColor`=? && `symbol`=?;";
        else
            queryString = "SELECT `name` FROM `obj_guild` WHERE `backgroundColor01`=? && `backgroundColor02`=? && `symbolColor`=? && `backgroundDesign`=? && `symbol`=?;";


        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(queryString)) {

            if (gt.backgroundColor01 == gt.backgroundColor02) {
                preparedStatement.setInt(1, gt.backgroundColor01);
                preparedStatement.setInt(2, gt.backgroundColor02);
                preparedStatement.setInt(3, gt.symbolColor);
                preparedStatement.setInt(4, gt.symbol);
            } else {
                preparedStatement.setInt(1, gt.backgroundColor01);
                preparedStatement.setInt(2, gt.backgroundColor02);
                preparedStatement.setInt(3, gt.symbolColor);
                preparedStatement.setInt(4, gt.backgroundDesign);
                preparedStatement.setInt(5, gt.symbol);
            }

            ResultSet rs = preparedStatement.executeQuery();

            if (!rs.next())
                valid = true;

        } catch (SQLException e) {
            Logger.error(e);
        }

        return valid;
    }

    public boolean SET_GUILD_OWNED_CITY(int guildID, int cityID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_guild` SET `ownedCity`=? WHERE `UID`=?")) {

            preparedStatement.setLong(1, cityID);
            preparedStatement.setLong(2, guildID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean SET_GUILD_LEADER(int objectUUID, int guildID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_guild` SET `leaderUID`=? WHERE `UID`=?")) {

            preparedStatement.setLong(1, objectUUID);
            preparedStatement.setLong(2, guildID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean IS_NAME_UNIQUE(final String name) {

        boolean valid = false;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `name` FROM `obj_guild` WHERE `name`=?;")) {

            preparedStatement.setString(1, name);
            ResultSet rs = preparedStatement.executeQuery();

            if (!rs.next())
                valid = true;

        } catch (SQLException e) {
            Logger.error(e);
        }

        return valid;
    }

    public Guild SAVE_TO_DATABASE(Guild g) {

        Guild guild = null;
        GuildTag guildTag = g.getGuildTag();

        if (guildTag == null)
            return null;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("CALL `guild_CREATE`(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

            preparedStatement.setLong(1, MBServerStatics.worldUUID);
            preparedStatement.setLong(2, g.getGuildLeaderUUID());
            preparedStatement.setString(3, g.getName());
            preparedStatement.setInt(4, guildTag.backgroundColor01);
            preparedStatement.setInt(5, guildTag.backgroundColor02);
            preparedStatement.setInt(6, guildTag.symbolColor);
            preparedStatement.setInt(7, guildTag.backgroundDesign);
            preparedStatement.setInt(8, guildTag.symbol);
            preparedStatement.setInt(9, g.getCharter());
            preparedStatement.setString(10, g.getLeadershipType());
            preparedStatement.setString(11, g.getMotto());

            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {
                int objectUUID = (int) rs.getLong("UID");

                if (objectUUID > 0)
                    guild = GET_GUILD(objectUUID);
            }
        } catch (SQLException e) {
            Logger.error(e);
        }
        return guild;
    }

    public boolean UPDATE_GUILD_RANK_OFFLINE(int target, int newRank, int guildId) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `guild_title`=? WHERE `UID`=? && `guildUID`=?")) {

            preparedStatement.setInt(1, newRank);
            preparedStatement.setInt(2, target);
            preparedStatement.setInt(3, guildId);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean UPDATE_PARENT(int guildUID, int parentUID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `object` SET `parent`=? WHERE `UID`=?")) {

            preparedStatement.setInt(1, parentUID);
            preparedStatement.setInt(2, guildUID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public int DELETE_GUILD(final Guild guild) {

        int row_count = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `object` WHERE `UID` = ?")) {

            preparedStatement.setLong(1, guild.getObjectUUID());
            row_count = preparedStatement.executeUpdate();

        } catch (SQLException e) {
            Logger.error(e);
        }
        return row_count;
    }

    public boolean UPDATE_MINETIME(int guildUID, int mineTime) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_guild` SET `mineTime`=? WHERE `UID`=?")) {

            preparedStatement.setInt(1, mineTime);
            preparedStatement.setInt(2, guildUID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }


    // *** Refactor: Why are we saving tags/charter in update?
    //               It's not like this shit ever changes.

    public int UPDATE_GUILD_STATUS_OFFLINE(int target, boolean isInnerCouncil, boolean isRecruiter, boolean isTaxCollector, int guildId) {

        int updateMask = 0;
        int row_count = 0;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT `guild_isInnerCouncil`, `guild_isTaxCollector`, `guild_isRecruiter` FROM `obj_character` WHERE `UID`=? && `guildUID`=?")) {

            preparedStatement.setLong(1, target);
            preparedStatement.setLong(2, guildId);

            ResultSet rs = preparedStatement.executeQuery();

            //If the first query had no results, neither will the second

            //Determine what is different

            if (rs.first()) {
                if (rs.getBoolean("guild_isInnerCouncil") != isInnerCouncil)
                    updateMask |= 4;
                if (rs.getBoolean("guild_isRecruiter") != isRecruiter)
                    updateMask |= 2;
                if (rs.getBoolean("guild_isTaxCollector") != isTaxCollector)
                    updateMask |= 1;
            }
        } catch (SQLException e) {
            Logger.error(e);
        }

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_character` SET `guild_isInnerCouncil`=?, `guild_isTaxCollector`=?, `guild_isRecruiter`=?, `guild_isFullMember`=? WHERE `UID`=? && `guildUID`=?")) {

            preparedStatement.setBoolean(1, isInnerCouncil);
            preparedStatement.setBoolean(2, isRecruiter);
            preparedStatement.setBoolean(3, isTaxCollector);
            preparedStatement.setBoolean(4, ((updateMask > 0))); //If you are becoming an officer, or where an officer, your a full member...
            preparedStatement.setLong(5, target);
            preparedStatement.setLong(6, guildId);

            row_count = preparedStatement.executeUpdate();

        } catch (SQLException e) {
            Logger.error(e);
        }
        return row_count;
    }

    public boolean updateDatabase(final Guild g) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `obj_guild` SET `name`=?, `backgroundColor01`=?, `backgroundColor02`=?, `symbolColor`=?, `backgroundDesign`=?, `symbol`=?, `charter`=?, `motd`=?, `icMotd`=?, `nationMotd`=?, `leaderUID`=? WHERE `UID`=?")) {

            preparedStatement.setString(1, g.getName());
            preparedStatement.setInt(2, g.getGuildTag().backgroundColor01);
            preparedStatement.setInt(3, g.getGuildTag().backgroundColor02);
            preparedStatement.setInt(4, g.getGuildTag().symbolColor);
            preparedStatement.setInt(5, g.getGuildTag().backgroundDesign);
            preparedStatement.setInt(6, g.getGuildTag().symbol);
            preparedStatement.setInt(7, g.getCharter());
            preparedStatement.setString(8, g.getMOTD());
            preparedStatement.setString(9, g.getICMOTD());
            preparedStatement.setString(10, "");
            preparedStatement.setInt(11, g.getGuildLeaderUUID());
            preparedStatement.setLong(12, g.getObjectUUID());

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean ADD_TO_ALLIANCE_LIST(final long sourceGuildID, final long targetGuildID, boolean isRecommended, boolean isAlly, String recommender) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `dyn_guild_allianceenemylist` (`GuildID`, `OtherGuildID`,`isRecommended`, `isAlliance`, `recommender`) VALUES (?,?,?,?,?)")) {

            preparedStatement.setLong(1, sourceGuildID);
            preparedStatement.setLong(2, targetGuildID);
            preparedStatement.setBoolean(3, isRecommended);
            preparedStatement.setBoolean(4, isAlly);
            preparedStatement.setString(5, recommender);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean REMOVE_FROM_ALLIANCE_LIST(final long sourceGuildID, long targetGuildID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `dyn_guild_allianceenemylist` WHERE `GuildID`=? AND `OtherGuildID`=?")) {

            preparedStatement.setLong(1, sourceGuildID);
            preparedStatement.setLong(2, targetGuildID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
        }
        return false;
    }

    public boolean UPDATE_RECOMMENDED(final long sourceGuildID, long targetGuildID) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_guild_allianceenemylist` SET `isRecommended` = ? WHERE `GuildID`=? AND `OtherGuildID`=?")) {

            preparedStatement.setLong(1, sourceGuildID);
            preparedStatement.setLong(2, targetGuildID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public boolean UPDATE_ALLIANCE(final long sourceGuildID, long targetGuildID, boolean isAlly) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_guild_allianceenemylist` SET `isAlliance` = ? WHERE `GuildID`=? AND `OtherGuildID`=?")) {

            preparedStatement.setLong(1, sourceGuildID);
            preparedStatement.setLong(2, targetGuildID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }
    }

    public boolean UPDATE_ALLIANCE_AND_RECOMMENDED(final long sourceGuildID, long targetGuildID, boolean isAlly) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `dyn_guild_allianceenemylist` SET `isRecommended` = ?, `isAlliance` = ? WHERE `GuildID`=? AND `OtherGuildID`=?")) {

            preparedStatement.setByte(1, (byte) 0);
            preparedStatement.setBoolean(2, isAlly);
            preparedStatement.setLong(3, sourceGuildID);
            preparedStatement.setLong(4, targetGuildID);

            return (preparedStatement.executeUpdate() > 0);

        } catch (SQLException e) {
            Logger.error(e);
            return false;
        }

    }

    public void LOAD_ALL_ALLIANCES_FOR_GUILD(Guild guild) {

        if (guild == null)
            return;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `dyn_guild_allianceenemylist` WHERE `GuildID` = ?")) {

            preparedStatement.setInt(1, guild.getObjectUUID());
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                GuildAlliances guildAlliance = new GuildAlliances(rs);
                guild.guildAlliances.put(guildAlliance.getAllianceGuild(), guildAlliance);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }
    }

    //TODO uncomment this when finished with guild history warehouse integration
//	public HashMap<Integer, GuildRecord> GET_WAREHOUSE_GUILD_HISTORY(){
//		
//		HashMap<Integer, GuildRecord> tempMap = new HashMap<>();
//		prepareCallable("SELECT * FROM `warehouse_guildhistory` WHERE `eventType` = 'CREATE'");
//		try {
//			ResultSet rs = executeQuery();
//			
//			while (rs.next()) {
//				GuildRecord guildRecord = new GuildRecord(rs);
//				tempMap.put(guildRecord.guildID, guildRecord);
//			}
//		}catch (Exception e){
//			Logger.error(e);
//		}
//		return tempMap;
//		
//	}


}
