// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import ch.claude_martin.enumbitset.EnumBitSet;
import engine.Enum;
import engine.gameManager.DbManager;
import engine.gameManager.NPCManager;
import engine.server.MBServerStatics;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class MobBase extends AbstractGameObject {

	private final int loadID;
	private final String firstName;
	private final byte level;
	private float healthMax;
	private int attackRating;
	private int defenseRating;
	private float damageMin;
	private float damageMax;
	private float hitBoxRadius;
	private final float scale;
	private EnumBitSet<Enum.MobFlagType> flags;
	private int mask;
	private int goldMod;
	private int seeInvis;
	private int spawnTime = 0;
	private int defense = 0;
	private int atr = 0;
	private float minDmg = 0;
	private float maxDmg = 0;
	private float attackRange;
	private boolean isNecroPet = false;
	private MobBaseStats mobBaseStats;

	private float walk = 0;
	private float run = 0;
	private float walkCombat = 0;
	private float runCombat = 0;
	public int bootySet;
	public Enum.MobBehaviourType fsm;

	public EnumBitSet<Enum.MonsterType> notEnemy;
	public EnumBitSet<Enum.MonsterType> enemy;

	/**
	 * ResultSet Constructor
	 */
	public MobBase(ResultSet rs) throws SQLException {
		super(rs, rs.getInt("ID"));

		this.loadID = rs.getInt("loadID");

		this.firstName = rs.getString("name");
		this.level = rs.getByte("level");

		this.goldMod = rs.getInt("goldMod");
		this.spawnTime = rs.getInt("spawnTime");

		this.healthMax = rs.getInt("health");
		this.damageMin = rs.getFloat("minDmg");
		this.damageMax = rs.getFloat("maxDmg");

		this.attackRating = rs.getInt("atr");
		this.defenseRating = rs.getInt("defense");
		this.attackRange = rs.getFloat("attackRange");
		this.bootySet = rs.getInt("bootySet");

		this.fsm = Enum.MobBehaviourType.valueOf(rs.getString("fsm"));

		this.flags = EnumBitSet.asEnumBitSet(rs.getLong("flags"), Enum.MobFlagType.class);
		this.notEnemy = EnumBitSet.asEnumBitSet(rs.getLong("notEnemy"), Enum.MonsterType.class);
		this.enemy = EnumBitSet.asEnumBitSet(rs.getLong("enemy"), Enum.MonsterType.class);

		this.seeInvis = rs.getInt("seeInvis");
		this.scale = rs.getFloat("scale");
		this.hitBoxRadius = 5f;
		this.mask = 0;

		if (this.getObjectUUID() == 12021 || this.getObjectUUID() == 12022)
			this.isNecroPet = true;

		if (Enum.MobFlagType.HUMANOID.elementOf(this.flags))
			this.mask += MBServerStatics.MASK_HUMANOID;

		if (Enum.MobFlagType.UNDEAD.elementOf(this.flags))
			this.mask += MBServerStatics.MASK_UNDEAD;

		if (Enum.MobFlagType.BEAST.elementOf(this.flags))
			this.mask += MBServerStatics.MASK_BEAST;

		if (Enum.MobFlagType.DRAGON.elementOf(this.flags))
			this.mask += MBServerStatics.MASK_DRAGON;

		if (Enum.MobFlagType.RAT.elementOf(this.flags))
			this.mask += MBServerStatics.MASK_RAT;

		this.mobBaseStats = DbManager.MobBaseQueries.LOAD_STATS(this.loadID);
		DbManager.MobBaseQueries.LOAD_ALL_MOBBASE_SPEEDS(this);

	}

	public static HashMap<Integer, MobEquipment> loadEquipmentSet(int equipmentSetID){

		ArrayList<BootySetEntry> equipList;
		HashMap<Integer, MobEquipment> equip = new HashMap<>();

		if (equipmentSetID == 0)
			return equip;

		equipList = NPCManager._bootySetMap.get(equipmentSetID);

		if (equipList == null)
			return equip;

		for (BootySetEntry equipmentSetEntry : equipList) {

			MobEquipment mobEquipment = new MobEquipment(equipmentSetEntry.itemBase, equipmentSetEntry.dropChance);
			ItemBase itemBase = mobEquipment.getItemBase();

			if (itemBase != null) {
				if (itemBase.getType().equals(Enum.ItemType.WEAPON))
					if (mobEquipment.getSlot() == 1 && itemBase.getEquipFlag() == 2)
						mobEquipment.setSlot(2);

				equip.put(mobEquipment.getSlot(), mobEquipment);
			}
		}

		return equip;
	}

	public void updateSpeeds(float walk, float walkCombat,float run, float runCombat){
		this.walk = walk;
		this.walkCombat = walkCombat;
		this.run = run;
		this.runCombat = runCombat;

	}

	/*
	 * Getters
	 */
	public String getFirstName() {
		return this.firstName;
	}

	public int getLoadID() {
		return this.loadID;
	}

	public int getLevel() {
		return this.level;
	}

	public float getHealthMax() {
		return this.healthMax;
	}

	public float getDamageMin() {
		return this.damageMin;
	}

	public float getDamageMax() {
		return this.damageMax;
	}

	public int getAttackRating() {
		return this.attackRating;
	}

	public int getDefenseRating() {
		return this.defenseRating;
	}


	public EnumBitSet<Enum.MobFlagType> getFlags() {
		return this.flags;
	}

	public float getScale() {
		return this.scale;
	}

	public int getTypeMasks() {
		return this.mask;
	}

	public int getSeeInvis() {
		return this.seeInvis;
	}

	public int getSpawnTime() {
		return this.spawnTime;
	}

	public static MobBase getMobBase(int id) {
		return DbManager.MobBaseQueries.GET_MOBBASE(id);
	}

	@Override
	public void updateDatabase() {
		// TODO Create update logic.
	}

	public float getHitBoxRadius() {
		if (this.hitBoxRadius < 0f) {
			return 0f;
		} else {
			return this.hitBoxRadius;
		}
	}

	public MobBaseStats getMobBaseStats() {
		return mobBaseStats;
	}

	public float getMaxDmg() {
		return maxDmg;
	}

	public float getMinDmg() {
		return minDmg;
	}

	public int getAtr() {
		return atr;
	}

	public void setAtr(int atr) {
		this.atr = atr;
	}

	public int getDefense() {
		return defense;
	}

	public void setDefense(int defense) {
		this.defense = defense;
	}

	public float getAttackRange() {
		return attackRange;
	}

	public boolean isNecroPet() {
		return isNecroPet;
	}

	public static int GetClassType(int mobbaseID){

		switch (mobbaseID){
		case 17235:
		case 17233:
		case 17256:
		case 17259:
		case 17260:
		case 17261:
			return 2518;
		case 17258:
		case 17257:
		case 17237:
		case 17234:
			return 2521;
		default:
			return 2518;
		}
	}

	public float getWalk() {
		return walk;
	}

	public void setWalk(float walk) {
		this.walk = walk;
	}

	public float getRun() {
		return run;
	}

	public void setRun(float run) {
		this.run = run;
	}

	public float getWalkCombat() {
		return walkCombat;
	}

	public float getRunCombat() {
		return runCombat;
	}

}
