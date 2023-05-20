// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.objects.PreparedStatementShared;
import engine.powers.EffectsBase;
import org.pmw.tinylog.Logger;

import java.sql.ResultSet;
import java.util.ArrayList;

public class dbEffectsBaseHandler extends dbHandlerBase {

	public dbEffectsBaseHandler() {

	}


	public static ArrayList<EffectsBase> getAllEffectsBase() {
		PreparedStatementShared ps = null;
		ArrayList<EffectsBase> out = new ArrayList<>();
		try {
			ps = new PreparedStatementShared("SELECT * FROM static_power_effectbase ORDER BY `IDString` DESC");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				EffectsBase toAdd = new EffectsBase(rs);
				out.add(toAdd);
			}
			rs.close();
		} catch (Exception e) {
			Logger.error(e);
		} finally {
			ps.release();
		}
		//testHash(out);
		return out;
	}
}
