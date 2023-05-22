// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.db.handlers;

import engine.Enum.GameObjectType;
import engine.gameManager.DbManager;
import engine.objects.AbstractGameObject;
import engine.objects.RuneBaseEffect;
import org.pmw.tinylog.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class dbRuneBaseEffectHandler extends dbHandlerBase {

	public dbRuneBaseEffectHandler() {
		this.localClass = RuneBaseEffect.class;
		this.localObjectType = engine.Enum.GameObjectType.valueOf(this.localClass.getSimpleName());
	}

	public ArrayList<RuneBaseEffect> GET_EFFECTS_FOR_RUNEBASE(int id) {

		ArrayList<RuneBaseEffect> runeBaseEffectsList = new ArrayList<>();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_rune_baseeffect` WHERE `runeID`=?")) {

			preparedStatement.setInt(1, id);

			ResultSet rs = preparedStatement.executeQuery();
			runeBaseEffectsList = getObjectsFromRs(rs, 250);

		} catch (SQLException e) {
			Logger.error(e);
		}

		return runeBaseEffectsList;
	}

	public ArrayList<RuneBaseEffect> GET_ALL_RUNEBASE_EFFECTS() {

		ArrayList<RuneBaseEffect> runeBaseEffectsList = new ArrayList<>();

		try (Connection connection = DbManager.getConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `static_rune_baseeffect`;")) {


			ResultSet rs = preparedStatement.executeQuery();
			runeBaseEffectsList = getObjectsFromRs(rs, 250);

		} catch (SQLException e) {
			Logger.error(e);
		}

		return runeBaseEffectsList;
	}

	//This calls from cache only. Call this AFTER caching all runebase effects;

	public HashMap<Integer, ArrayList<RuneBaseEffect>> LOAD_BASEEFFECTS_FOR_RUNEBASE() {

		HashMap<Integer, ArrayList<RuneBaseEffect>> runeBaseEffectSet;
		runeBaseEffectSet = new HashMap<>();

		for (AbstractGameObject runeBaseEffect:DbManager.getList(GameObjectType.RuneBaseEffect)){

			int runeBaseID = ((RuneBaseEffect)runeBaseEffect).getRuneBaseID();
			if (runeBaseEffectSet.get(runeBaseID) == null){
				ArrayList<RuneBaseEffect> runeBaseEffectList = new ArrayList<>();
				runeBaseEffectList.add((RuneBaseEffect)runeBaseEffect);
				runeBaseEffectSet.put(runeBaseID, runeBaseEffectList);
			}
			else{
				ArrayList<RuneBaseEffect>runeBaseEffectList = runeBaseEffectSet.get(runeBaseID);
				runeBaseEffectList.add((RuneBaseEffect)runeBaseEffect);
				runeBaseEffectSet.put(runeBaseID, runeBaseEffectList);
			}
		}
		return runeBaseEffectSet;
	}

}
