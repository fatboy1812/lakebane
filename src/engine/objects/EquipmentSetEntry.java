// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import engine.gameManager.DbManager;
import engine.gameManager.NPCManager;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EquipmentSetEntry {

	private float dropChance;
	private int itemID;

	/**
	 * ResultSet Constructor
	 */

	public EquipmentSetEntry(ResultSet rs) throws SQLException {
		this.dropChance = (rs.getFloat("dropChance"));
		this.itemID = (rs.getInt("itemID"));
	}

	public static void LoadAllEquipmentSets() {
		NPCManager.EquipmentSetMap = DbManager.ItemBaseQueries.LOAD_EQUIPMENT_FOR_NPC_AND_MOBS();
	}

	float getDropChance() {
		return dropChance;
	}

	public int getItemID() {
		return itemID;
	}

}
