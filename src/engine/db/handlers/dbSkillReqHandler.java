// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.objects.PreparedStatementShared;
import engine.objects.SkillReq;
import engine.powers.PowersBase;
import org.pmw.tinylog.Logger;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

public class dbSkillReqHandler extends dbHandlerBase {

    public dbSkillReqHandler() {
        this.localClass = SkillReq.class;
        this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
    }

    public static ArrayList<PowersBase> getAllPowersBase() {
        PreparedStatementShared ps = null;
        ArrayList<PowersBase> out = new ArrayList<>();
        try {
            ps = new PreparedStatementShared("SELECT * FROM static_power_powerbase");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PowersBase toAdd = new PowersBase(rs);
                out.add(toAdd);
            }
            rs.close();
        } catch (Exception e) {
            Logger.error(e.toString());
        } finally {
            ps.release();
        }
        return out;
    }

    public static void getFailConditions(HashMap<String, PowersBase> powers) {
        PreparedStatementShared ps = null;
        try {
            ps = new PreparedStatementShared("SELECT IDString, type FROM static_power_failcondition where powerOrEffect = 'Power'");
            ResultSet rs = ps.executeQuery();
            String type, IDString;
            PowersBase pb;
            while (rs.next()) {
                type = rs.getString("type");
                IDString = rs.getString("IDString");
                pb = powers.get(IDString);
                if (pb != null) {
                    switch (type) {
                        case "CastSpell":
                            pb.cancelOnCastSpell = true;
                            break;
                        case "TakeDamage":
                            pb.cancelOnTakeDamage = true;
                            break;
                    }
                } else {
                    Logger.error("null power for Grief " + IDString);
                }
            }
            rs.close();
        } catch (Exception e) {
            Logger.error(e.toString());
        } finally {
            ps.release();
        }
    }

    public ArrayList<SkillReq> GET_REQS_FOR_RUNE(final int objectUUID) {
        prepareCallable("SELECT * FROM `static_skill_skillreq` WHERE `runeID`=?");
        setInt(1, objectUUID);
        return getObjectList();
    }

}
