// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.gameManager.DbManager;
import engine.objects.SkillReq;
import engine.powers.PowersBase;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class dbSkillReqHandler extends dbHandlerBase {

    public dbSkillReqHandler() {
        this.localClass = SkillReq.class;
        this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public static ArrayList<PowersBase> getAllPowersBase() {

        ArrayList<PowersBase> powerBaseList = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM static_power_powerbase")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                PowersBase toAdd = new PowersBase(rs);
                powerBaseList.add(toAdd);
            }

        } catch (SQLException e) {
            Logger.error(e);
        }

        return powerBaseList;
    }

    public static void getFailConditions(HashMap<String, PowersBase> powers) {

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT IDString, type FROM static_power_failcondition where powerOrEffect = 'Power'")) {

            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                String type = rs.getString("type");
                String IDString = rs.getString("IDString");
                PowersBase pb = powers.get(IDString);

                if (pb != null)
                    switch (type) {
                        case "CastSpell":
                            pb.cancelOnCastSpell = true;
                            break;
                        case "TakeDamage":
                            pb.cancelOnTakeDamage = true;
                            break;
                    }
            }
        } catch (SQLException e) {
            Logger.error(e);
        }
    }

    public ArrayList<SkillReq> GET_REQS_FOR_RUNE(final int objectUUID) {

        ArrayList<SkillReq> skillReqsList = new ArrayList<>();

        try (Connection connection = DbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_skill_skillreq` WHERE `runeID`=?")) {

            preparedStatement.setInt(1, objectUUID);

            ResultSet rs = preparedStatement.executeQuery();
            skillReqsList = getObjectsFromRs(rs, 5);

        } catch (SQLException e) {
            Logger.error(e);
        }

        return skillReqsList;
    }

}
