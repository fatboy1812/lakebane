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
import engine.objects.MaxSkills;
import engine.objects.SkillsBase;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class dbSkillBaseHandler extends dbHandlerBase {

    public dbSkillBaseHandler() {
        this.localClass = SkillsBase.class;
        this.localObjectType = Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public SkillsBase GET_BASE(final int objectUUID) {

        SkillsBase skillsBase = (SkillsBase) DbManager.getFromCache(GameObjectType.SkillsBase, objectUUID);

        if (skillsBase != null)
            return skillsBase;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_skill_skillsbase WHERE ID = ?")) {

            preparedStatement.setInt(1, objectUUID);
            ResultSet rs = preparedStatement.executeQuery();

            skillsBase = (SkillsBase) getObjectFromRs(rs);

        } catch (SQLException e) {
            Logger.error(e);
        }

        SkillsBase.putInCache(skillsBase);
        return skillsBase;
    }

    public SkillsBase GET_BASE_BY_NAME(String name) {

        SkillsBase skillsBase = SkillsBase.getFromCache(name);

        if (skillsBase != null)
            return skillsBase;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_skill_skillsbase WHERE name = ?")) {

            preparedStatement.setString(1, name);
            ResultSet rs = preparedStatement.executeQuery();

            skillsBase = (SkillsBase) getObjectFromRs(rs);

        } catch (SQLException e) {
            Logger.error(e);
        }

        SkillsBase.putInCache(skillsBase);
        return skillsBase;
    }

    public SkillsBase GET_BASE_BY_TOKEN(final int token) {

        SkillsBase skillsBase = SkillsBase.getFromCache(token);

        if (skillsBase != null)
            return skillsBase;

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_skill_skillsbase WHERE token = ?")) {

            preparedStatement.setInt(1, token);
            ResultSet rs = preparedStatement.executeQuery();

            skillsBase = (SkillsBase) getObjectFromRs(rs);

        } catch (SQLException e) {
            Logger.error(e);
        }

        SkillsBase.putInCache(skillsBase);
        return skillsBase;
    }

    public void LOAD_ALL_MAX_SKILLS_FOR_CONTRACT() {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_rune_maxskills`")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                MaxSkills maxSKills = new MaxSkills(rs);

                if (MaxSkills.MaxSkillsSet.get(maxSKills.getRuneID()) == null) {
                    ArrayList<MaxSkills> newMaxSkillsList = new ArrayList<>();
                    newMaxSkillsList.add(maxSKills);
                    MaxSkills.MaxSkillsSet.put(maxSKills.getRuneID(), newMaxSkillsList);
                } else
                    MaxSkills.MaxSkillsSet.get(maxSKills.getRuneID()).add(maxSKills);

            }

        } catch (SQLException e) {
            Logger.error(e);
        }
    }


    public void LOAD_ALL_RUNE_SKILLS() {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_skill_skillsgranted`")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {

                int runeID = rs.getInt("runeID");
                int token = rs.getInt("token");
                int amount = rs.getInt("amount");

                if (SkillsBase.runeSkillsCache.get(runeID) == null)
                    SkillsBase.runeSkillsCache.put(runeID, new HashMap<>());

                SkillsBase.runeSkillsCache.get(runeID).put(token, amount);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }
    }

}
