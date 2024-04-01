// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import engine.Enum;
import engine.Enum.DamageType;
import engine.Enum.ModType;
import engine.Enum.SourceType;
import engine.gameManager.ChatManager;
import engine.gameManager.DbManager;
import engine.powers.EffectsBase;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class Resists {

    private static ConcurrentHashMap<Integer, Resists> mobResists = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    private ConcurrentHashMap<DamageType, Float> resists = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    private ConcurrentHashMap<DamageType, Boolean> immuneTo = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    private DamageType protection;
    private int protectionTrains = 0;
    private boolean immuneToAll;

    /**
     * Generic Constructor
     */

    public Resists(String type) {
        switch (type) {
            case "Building":
                setBuildingResists();
                break;
            case "Mine":
                setMineResists();
                break;
            default:
                setGenericResists();
                break;
        }
    }

    public Resists(Resists r) {
        for (DamageType dt : r.resists.keySet())
            this.resists.put(dt, r.resists.get(dt));
        for (DamageType dt : r.immuneTo.keySet())
            this.immuneTo.put(dt, r.immuneTo.get(dt));
        this.protection = r.protection;
        this.protectionTrains = r.protectionTrains;
        this.immuneToAll = r.immuneToAll;
    }

    /**
     * Generic Constructor for player
     */
    public Resists(PlayerCharacter pc) {
        setGenericResists();
    }

    public Resists(Mob mob) {
        setGenericResists();
    }

    /**
     * Called for mobBase when getting from the db fails
     */
    public Resists(MobBase mobBase) {
        setGenericResists();
    }

    /**
     * Database Constructor
     */
    public Resists(ResultSet rs) throws SQLException {
        this.immuneToAll = false;
        this.resists.put(DamageType.SLASHING, rs.getFloat("slash"));
        this.resists.put(DamageType.CRUSHING, rs.getFloat("crush"));
        this.resists.put(DamageType.PIERCING, rs.getFloat("pierce"));
        this.resists.put(DamageType.MAGIC, rs.getFloat("magic"));
        this.resists.put(DamageType.BLEEDING, rs.getFloat("bleed"));
        this.resists.put(DamageType.POISON, rs.getFloat("poison"));
        this.resists.put(DamageType.MENTAL, rs.getFloat("mental"));
        this.resists.put(DamageType.HOLY, rs.getFloat("holy"));
        this.resists.put(DamageType.UNHOLY, rs.getFloat("unholy"));
        this.resists.put(DamageType.LIGHTNING, rs.getFloat("lightning"));
        this.resists.put(DamageType.FIRE, rs.getFloat("fire"));
        this.resists.put(DamageType.COLD, rs.getFloat("cold"));
        this.resists.put(DamageType.HEALING, 0f);
    }

    //Handle Fortitudes
    private static float handleFortitude(AbstractCharacter target, DamageType type, float damage) {
        if (target == null || !(target.getObjectType().equals(Enum.GameObjectType.PlayerCharacter)))
            return damage;
        PlayerBonuses bonus = target.getBonuses();

        //see if there is a fortitude
        float damageCap = bonus.getFloatPercentAll(ModType.DamageCap, SourceType.None);
        if (damageCap == 0f || type == DamageType.HEALING)
            return damage;

        //is fortitude, Are we under the cap?
        float maxHealth = target.getHealthMax();
        float capFire = maxHealth * (damageCap);
        if (damage < capFire)
            return damage;

        //let's see if valid damagetype to apply it
        boolean exclusive;
        HashSet<SourceType> forts = bonus.getList(ModType.IgnoreDamageCap);
        if (forts == null) {
            exclusive = true;
            forts = bonus.getList(ModType.ExclusiveDamageCap);
        } else
            exclusive = false;
        if (forts == null || !isValidDamageCapType(forts, type, exclusive))
            return damage;

        float adjustedDamage = bonus.getFloatPercentAll(ModType.AdjustAboveDmgCap, SourceType.None);
        //Adjust damage down and return new amount
        float aadc = 1 + adjustedDamage;
        return capFire * aadc;
    }

    //Test if Damagetype is valid for foritude
    private static boolean isValidDamageCapType(HashSet<SourceType> forts, DamageType damageType, boolean exclusive) {
        for (SourceType fort : forts) {
            DamageType dt = DamageType.valueOf(fort.name());

            if (dt == DamageType.NONE)
                continue;

            if (dt == damageType) {
                return exclusive;
            }
        }
        return !exclusive;
    }

    /**
     * Calculate Current Resists for Player
     */
    public static void calculateResists(AbstractCharacter ac) {
        if (ac.getResists() != null)
            ac.getResists().calculateResists(ac, true);
        else
            Logger.error("Unable to find resists for character " + ac.getObjectUUID());
    }

    private static float[] getArmorResists(Item armor, float[] phys) {
        if (armor == null)
            return phys;
        ItemBase ab = armor.getItemBase();
        if (ab == null)
            return phys;
        phys[0] += ab.getSlashResist();
        phys[1] += ab.getCrushResist();
        phys[2] += ab.getPierceResist();
        return phys;
    }

    /**
     * Get mob resists from db if there, otherwise set defaults
     */
    public static Resists getResists(int resistID) {
        //check cache first
        if (mobResists.containsKey(resistID))
            return new Resists(mobResists.get(resistID));

        //get from database
        Resists resists = DbManager.ResistQueries.GET_RESISTS_FOR_MOB(resistID);
        if (resists != null) {
            mobResists.put(resistID, resists);
            return new Resists(resists);
        }

        //failed, may want to debug this
        return null;
    }

    /**
     * Create generic resists for buildings
     */
    public final void setBuildingResists() {
        this.immuneToAll = false;
        this.resists.put(DamageType.SLASHING, 85f);
        this.resists.put(DamageType.CRUSHING, 85f);
        this.resists.put(DamageType.SIEGE, 0f);
        this.immuneTo.put(DamageType.PIERCING, true);
        this.immuneTo.put(DamageType.MAGIC, true);
        this.immuneTo.put(DamageType.BLEEDING, true);
        this.immuneTo.put(DamageType.POISON, true);
        this.immuneTo.put(DamageType.MENTAL, true);
        this.immuneTo.put(DamageType.HOLY, true);
        this.immuneTo.put(DamageType.UNHOLY, true);
        this.immuneTo.put(DamageType.LIGHTNING, true);
        this.immuneTo.put(DamageType.FIRE, true);
        this.immuneTo.put(DamageType.COLD, true);

    }

    /**
     * Create generic resists for mines
     */
    public final void setMineResists() {
        this.immuneToAll = false;
        this.immuneTo.put(DamageType.SLASHING, true);
        this.immuneTo.put(DamageType.CRUSHING, true);
        this.immuneTo.put(DamageType.PIERCING, true);
        this.immuneTo.put(DamageType.MAGIC, true);
        this.immuneTo.put(DamageType.BLEEDING, true);
        this.immuneTo.put(DamageType.POISON, true);
        this.immuneTo.put(DamageType.MENTAL, true);
        this.immuneTo.put(DamageType.HOLY, true);
        this.immuneTo.put(DamageType.UNHOLY, true);
        this.immuneTo.put(DamageType.LIGHTNING, true);
        this.immuneTo.put(DamageType.FIRE, true);
        this.immuneTo.put(DamageType.COLD, true);
        this.resists.put(DamageType.SIEGE, 0f);
    }

    /**
     * Create generic resists
     */
    public final void setGenericResists() {
        this.immuneToAll = false;
        this.resists.put(DamageType.SLASHING, 0f);
        this.resists.put(DamageType.CRUSHING, 0f);
        this.resists.put(DamageType.PIERCING, 0f);
        this.resists.put(DamageType.MAGIC, 0f);
        this.resists.put(DamageType.BLEEDING, 0f);
        this.resists.put(DamageType.POISON, 0f);
        this.resists.put(DamageType.MENTAL, 0f);
        this.resists.put(DamageType.HOLY, 0f);
        this.resists.put(DamageType.UNHOLY, 0f);
        this.resists.put(DamageType.LIGHTNING, 0f);
        this.resists.put(DamageType.FIRE, 0f);
        this.resists.put(DamageType.COLD, 0f);
        this.resists.put(DamageType.HEALING, 0f);
        this.immuneTo.put(DamageType.SIEGE, true);

    }

    /**
     * Get a resist
     */
    public float getResist(DamageType type, int trains) {
        //get resisted amount
        Float amount = 0f;
        if (this.resists.containsKey(type))
            amount = this.resists.get(type);

        //add protection
        if (trains > 0 && protection != null && type.equals(this.protection)) {
            float prot = 50 + this.protectionTrains - trains;
            amount += (prot >= 0) ? prot : 0;
        }

        if (amount == null)
            return 0f;
        if (amount > 75f)
            return 75f;
        return amount;
    }

    /**
     * get immuneTo
     */
    public boolean immuneTo(DamageType type) {
        if (this.immuneTo.containsKey(type))
            return this.immuneTo.get(type);
        else
            return false;
    }

    /**
     * get immuneToAll
     */
    public boolean immuneToAll() {
        return this.immuneToAll;
    }

    public boolean immuneToPowers() {
        return immuneTo(DamageType.POWERS);
    }

    public boolean immuneToAttacks() {
        return immuneTo(DamageType.ATTACK);
    }

    public boolean immuneToSpires() {
        return immuneTo(DamageType.SPIRES);
    }

    /**
     * gets immuneTo(type) and immuneToAll
     */
    public boolean isImmune(DamageType type) {
        if (this.immuneToAll)
            return true;
        return this.immuneTo(type);
    }

    /**
     * Set a resist
     */
    public void setResist(DamageType type, float value) {
        this.resists.put(type, value);
    }

    /**
     * add to a resist
     */
    public void incResist(DamageType type, float value) {
        Float amount = this.resists.get(type);
        if (amount == null)
            this.resists.put(type, value);
        else
            this.resists.put(type, amount + value);
    }

    /**
     * subtract from a resist
     */
    public void decResist(DamageType type, float value) {
        Float amount = this.resists.get(type);
        if (amount == null)
            this.resists.put(type, (0 - value));
        else
            this.resists.put(type, amount - value);
    }

    /**
     * set immunities from mobbase
     */
    public void setImmuneTo(int immune) {
        setImmuneTo(DamageType.STUN, ((immune & 1) != 0));
        setImmuneTo(DamageType.POWERBLOCK, ((immune & 2) != 0));
        setImmuneTo(DamageType.DRAIN, ((immune & 4) != 0));
        setImmuneTo(DamageType.SNARE, ((immune & 8) != 0));
        setImmuneTo(DamageType.SIEGE, ((immune & 16) != 0));
        setImmuneTo(DamageType.SLASHING, ((immune & 32) != 0));
        setImmuneTo(DamageType.CRUSHING, ((immune & 64) != 0));
        setImmuneTo(DamageType.PIERCING, ((immune & 128) != 0));
        setImmuneTo(DamageType.MAGIC, ((immune & 256) != 0));
        setImmuneTo(DamageType.BLEEDING, ((immune & 512) != 0));
        setImmuneTo(DamageType.POISON, ((immune & 1024) != 0));
        setImmuneTo(DamageType.MENTAL, ((immune & 2048) != 0));
        setImmuneTo(DamageType.HOLY, ((immune & 4096) != 0));
        setImmuneTo(DamageType.UNHOLY, ((immune & 8192) != 0));
        setImmuneTo(DamageType.LIGHTNING, ((immune & 16384) != 0));
        setImmuneTo(DamageType.FIRE, ((immune & 32768) != 0));
        setImmuneTo(DamageType.COLD, ((immune & 65536) != 0));
        setImmuneTo(DamageType.STEAL, ((immune & 131072) != 0));
    }

    /**
     * set/unset immuneTo
     */
    public void setImmuneTo(DamageType type, boolean value) {
        this.immuneTo.put(type, value);
    }

    /**
     * set immuneToAll
     */
    public void setImmuneToAll(boolean value) {
        this.immuneToAll = value;
    }

    /**
     * set resists from mobbase
     */
    public void setMobResists(int resistID) {
        //TODO add this in later
        //calls `static_npc_mob_resists` table WHERE `ID`='resistID'
    }

    /**
     * get Damage after resist
     * Expects heals as negative damage and damage as positive damage for fortitudes.
     */
    public float getResistedDamage(AbstractCharacter source, AbstractCharacter target, DamageType type, float damage, int trains) {
        //handle fortitudes
        damage = handleFortitude(target, type, damage);
        //calculate armor piercing
        float ap = source.getBonuses().getFloatPercentAll(ModType.ArmorPiercing, SourceType.None);
        float damageAfterResists = damage * (1 - (this.getResist(type, trains) * 0.01f) + ap);
        //check to see if any damage absorbers should cancel
        if (target != null) {
            //debug damage shields if any found
            if (source.getDebug(2) && source.getObjectType().equals(Enum.GameObjectType.PlayerCharacter)) {
                Effect da = target.getDamageAbsorber();
                if (da != null && da.getEffectsBase() != null) {
                    EffectsBase eb = da.getEffectsBase();
                    String text = "Damage: " + damage + '\n';
                    text += "Damage after resists: " + damageAfterResists + '\n';
                    text += "Attack damage type: " + type.name() + '\n';
                    text += "Fortitude damage types; " + eb.getDamageTypes() + '\n';
                    text += "Fortitude damage before attack: " + da.getDamageAmount() + '\n';
                    text += "Fortitude total health: " + eb.getDamageAmount(da.getTrains()) + '\n';
                    text += "Fortitude trains: " + da.getTrains();
                    ChatManager.chatSystemInfo((PlayerCharacter) source, text);
                }
            }
            target.cancelOnTakeDamage(type, (damageAfterResists));
        }
        return damageAfterResists;
    }

    public void calculateResists(AbstractCharacter ac, boolean val) {
        this.immuneTo.clear();

        // get resists for runes
        PlayerBonuses rb = ac.getBonuses();
        float slash = 0f, crush = 0f, pierce = 0f, magic = 0f, bleed = 0f, mental = 0f, holy = 0f, unholy = 0f, poison = 0f, lightning = 0f, fire = 0f, cold = 0f, healing = 0f;

        if (rb != null) {
            // Handle immunities
            if (rb.getBool(ModType.ImmuneTo, SourceType.Stun))
                this.immuneTo.put(DamageType.STUN, true);
            if (rb.getBool(ModType.ImmuneTo, SourceType.Blind))
                this.immuneTo.put(DamageType.BLIND, true);
            if (rb.getBool(ModType.ImmuneToAttack, SourceType.None))
                this.immuneTo.put(DamageType.ATTACK, true);
            if (rb.getBool(ModType.ImmuneToPowers, SourceType.None))
                this.immuneTo.put(DamageType.POWERS, true);
            if (rb.getBool(ModType.ImmuneTo, SourceType.Powerblock))
                this.immuneTo.put(DamageType.POWERBLOCK, true);
            if (rb.getBool(ModType.ImmuneTo, SourceType.DeBuff))
                this.immuneTo.put(DamageType.DEBUFF, true);
            if (rb.getBool(ModType.ImmuneTo, SourceType.Fear))
                this.immuneTo.put(DamageType.FEAR, true);
            if (rb.getBool(ModType.ImmuneTo, SourceType.Charm))
                this.immuneTo.put(DamageType.CHARM, true);
            if (rb.getBool(ModType.ImmuneTo, SourceType.Root))
                this.immuneTo.put(DamageType.ROOT, true);
            if (rb.getBool(ModType.ImmuneTo, SourceType.Snare))
                this.immuneTo.put(DamageType.SNARE, true);

            // Handle resists
            slash += rb.getFloat(ModType.Resistance, SourceType.Slash);
            crush += rb.getFloat(ModType.Resistance, SourceType.Crush);
            pierce += rb.getFloat(ModType.Resistance, SourceType.Pierce);
            magic += rb.getFloat(ModType.Resistance, SourceType.Magic);
            bleed += rb.getFloat(ModType.Resistance, SourceType.Bleed);
            poison += rb.getFloat(ModType.Resistance, SourceType.Poison);
            mental += rb.getFloat(ModType.Resistance, SourceType.Mental);
            holy += rb.getFloat(ModType.Resistance, SourceType.Holy);
            unholy += rb.getFloat(ModType.Resistance, SourceType.Unholy);
            lightning += rb.getFloat(ModType.Resistance, SourceType.Lightning);
            fire += rb.getFloat(ModType.Resistance, SourceType.Fire);
            cold += rb.getFloat(ModType.Resistance, SourceType.Cold);
            healing += rb.getFloat(ModType.Resistance, SourceType.Healing); // DamageType.Healing.name());

            //HHO

//			String protectionString = rb.getString("protection");
//
//			if (protectionString.isEmpty())
//				this.protection = null;
//			else try {
//				this.protection = DamageType.valueOf(rb.getString("protection"));
//			} catch (IllegalArgumentException e) {
//				Logger.error( "No enum for: " + protectionString);
//				this.protection = null;
//			}
//			this.protectionTrains = rb.getFloat("protection");
        }

        // get resists from equipment
        if (ac.getObjectType().equals(Enum.GameObjectType.PlayerCharacter)) {
            if (ac.getCharItemManager() != null && ac.getCharItemManager().getEquipped() != null) {
                float[] phys = {0f, 0f, 0f};
                ConcurrentHashMap<Integer, Item> equip = ac.getCharItemManager().getEquipped();

                // get base physical resists
                phys = Resists.getArmorResists(equip.get(MBServerStatics.SLOT_HELMET), phys);
                phys = Resists.getArmorResists(equip.get(MBServerStatics.SLOT_CHEST), phys);
                phys = Resists.getArmorResists(equip.get(MBServerStatics.SLOT_ARMS), phys);
                phys = Resists.getArmorResists(equip.get(MBServerStatics.SLOT_GLOVES), phys);
                phys = Resists.getArmorResists(equip.get(MBServerStatics.SLOT_LEGGINGS), phys);
                phys = Resists.getArmorResists(equip.get(MBServerStatics.SLOT_FEET), phys);
                slash += phys[0];
                crush += phys[1];
                pierce += phys[2];

            }
        }

        this.resists.put(DamageType.SLASHING, slash);
        this.resists.put(DamageType.CRUSHING, crush);
        this.resists.put(DamageType.PIERCING, pierce);
        this.resists.put(DamageType.MAGIC, magic);
        this.resists.put(DamageType.BLEEDING, bleed);
        this.resists.put(DamageType.POISON, poison);
        this.resists.put(DamageType.MENTAL, mental);
        this.resists.put(DamageType.HOLY, holy);
        this.resists.put(DamageType.UNHOLY, unholy);
        this.resists.put(DamageType.LIGHTNING, lightning);
        this.resists.put(DamageType.FIRE, fire);
        this.resists.put(DamageType.COLD, cold);
        this.resists.put(DamageType.HEALING, healing);

        this.immuneTo.put(DamageType.SIEGE, true);

        // debug printing of resists
        // printResists(pc);
    }

    public void printResistsToClient(PlayerCharacter pc) {
        for (DamageType dt : resists.keySet())
            ChatManager.chatSystemInfo(pc, "  resist." + dt.name() + ": " + resists.get(dt));
        for (DamageType dt : immuneTo.keySet())
            ChatManager.chatSystemInfo(pc, "  immuneTo." + dt.name() + ": " + immuneTo.get(dt));
        ChatManager.chatSystemInfo(pc, "  immuneToAll: " + this.immuneToAll);
        if (protection != null)
            ChatManager.chatSystemInfo(pc, "  Protection: " + protection.name() + ", Trains: " + protectionTrains);
        else
            ChatManager.chatSystemInfo(pc, "  Protection: None");
    }

    public String getResists(PlayerCharacter pc) {
        String out = pc.getName();

        out += "Resists: ";
        Iterator<DamageType> it = this.resists.keySet().iterator();
        while (it.hasNext()) {
            DamageType damType = it.next();
            String dtName = damType.name();
            out += dtName + '=' + this.resists.get(dtName) + ", ";
        }

        out += "ImmuneTo: ";
        it = this.immuneTo.keySet().iterator();
        while (it.hasNext()) {
            DamageType damType = it.next();

            String dtName = damType.name();
            out += dtName + '=' + this.resists.get(dtName) + ", ";
        }

        if (protection != null)
            out += "Protection: " + protection.name() + ", Trains: " + protectionTrains;
        else
            out += "Protection: none";

        return out;
    }
}
