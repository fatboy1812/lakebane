// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import engine.Enum;
import engine.Enum.*;
import engine.InterestManagement.WorldGrid;
import engine.exception.SerializationException;
import engine.gameManager.*;
import engine.job.JobContainer;
import engine.job.JobScheduler;
import engine.jobs.UpgradeNPCJob;
import engine.math.Bounds;
import engine.math.Vector3f;
import engine.math.Vector3fImmutable;
import engine.net.ByteBufferWriter;
import engine.net.Dispatch;
import engine.net.DispatchMessage;
import engine.net.client.msg.ErrorPopupMsg;
import engine.net.client.msg.ItemProductionMsg;
import engine.net.client.msg.PetMsg;
import engine.net.client.msg.PlaceAssetMsg;
import engine.server.MBServerStatics;
import org.joda.time.DateTime;
import org.pmw.tinylog.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static engine.net.client.msg.ErrorPopupMsg.sendErrorPopup;
import static engine.objects.MobBase.loadEquipmentSet;
import static engine.util.StringUtils.wordCount;

public class NPC extends AbstractCharacter {

    public static int SVR_CLOSE_WINDOW = 4;
    public static HashMap<Integer, ArrayList<String>> _pirateNames = new HashMap<>();
    // Used for thread safety
    public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ArrayList<MobLoot> rolling = new ArrayList<>();
    private final ConcurrentHashMap<Mob, Integer> siegeMinionMap = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    public ReentrantReadWriteLock minionLock = new ReentrantReadWriteLock();
    public ArrayList<ProducedItem> forgedItems = new ArrayList<>();
    public HashMap<Integer, MobEquipment> equip = null;
    public int runeSetID = 0;
    public int extraRune2 = 0;
    protected int loadID;
    protected MobBase mobBase;
    protected String name;
    protected int dbID;
    protected int currentID;
    //used by static npcs
    protected Zone parentZone;
    protected float statLat;
    protected float statLon;
    protected float statAlt;
    public float sellPercent; //also train percent
    public float buyPercent;
    protected int vendorID;
    protected ArrayList<Integer> modTypeTable;
    protected ArrayList<Integer> modSuffixTable;
    protected ArrayList<Byte> itemModTable;
    protected int symbol;
    // Variables NOT to be stored in db
    protected boolean isStatic = false;
    private DateTime upgradeDateTime = null;
    private HashSet<Integer> canRoll = null;
    public int parentZoneUUID;
    public int equipmentSetID = 0;
    private int repairCost = 5;

    // New NPC constructor.  Fill in the blanks and then call
    // PERSIST.

    public NPC() {

        this.dbID = MBServerStatics.NO_DB_ROW_ASSIGNED_YET;
        this.currentID = MBServerStatics.NO_DB_ROW_ASSIGNED_YET;
    }

    /**
     * ResultSet Constructor
     */
    public NPC(ResultSet rs) throws SQLException {

        super(rs);

        java.util.Date sqlDateTime;

        try {
            this.dbID = rs.getInt(1);
            this.currentID = this.dbID;
            this.setObjectTypeMask(MBServerStatics.MASK_NPC);
            this.contractUUID = rs.getInt("npc_contractID");
            this.parentZoneUUID = rs.getInt("parent");
            this.gridObjectType = GridObjectType.STATIC;
            this.equipmentSetID = rs.getInt("equipmentSet");
            this.runeSetID = rs.getInt("runeSet");

            this.loadID = rs.getInt("npc_raceID");

            this.level = rs.getByte("npc_level");

            buildingUUID = rs.getInt("npc_buildingID");

            // Most objects from the cache have a default buy
            // percentage of 100% which was a dupe source due
            // to the way MB calculated item values.

            // this.buyPercent = rs.getFloat("npc_buyPercent");

            this.buyPercent = .33f;
            if(ZoneManager.findSmallestZone(this.loc) != null && ZoneManager.findSmallestZone(this.loc).getSafeZone() == 1){
                this.sellPercent = 0;
            }else{
                this.sellPercent = 1;
            }

            this.setRot(new Vector3f(0, rs.getFloat("npc_rotation"), 0));

            this.statLat = rs.getFloat("npc_spawnX");
            this.statAlt = rs.getFloat("npc_spawnY");
            this.statLon = rs.getFloat("npc_spawnZ");

            this.guildUUID = rs.getInt("npc_guildID");

            // Set upgrade date JodaTime DateTime object
            // if one exists in the database.

            sqlDateTime = rs.getTimestamp("upgradeDate");

            if (sqlDateTime != null)
                upgradeDateTime = new DateTime(sqlDateTime);
            else
                upgradeDateTime = null;

            // Submit upgrade job if NPC is currently set to rank.

            if (this.upgradeDateTime != null)
                submitUpgradeJob();

            this.name = rs.getString("npc_name");

        } catch (Exception e) {
            Logger.error("NPC: " + this.dbID + " :" + e);
            e.printStackTrace();
        }

    }

    public static boolean ISWallArcher(Contract contract) {

        if (contract == null)
            return false;

        //838, 950, 1051, 1181, 1251, 1351, 1451, 1501, 1526, 1551, 980101,

        return contract.getAllowedBuildings().contains(BuildingGroup.WALLCORNER) ||
                contract.getAllowedBuildings().contains(BuildingGroup.WALLSTRAIGHTTOWER);
    }

    //This method restarts an upgrade timer when a building is loaded from the database.
    // Submit upgrade job for this building based upon it's current upgradeDateTime

    public static NPC getFromCache(int id) {
        return (NPC) DbManager.getFromCache(GameObjectType.NPC, id);
    }

    public static boolean UpdateName(NPC npc, String value) {

        if (!DbManager.NPCQueries.UPDATE_NAME(npc, value))
            return false;

        npc.name = value;
        return true;

    }

    public static void serializeNpcForClientMsgOtherPlayer(NPC npc, ByteBufferWriter writer, boolean hideAsciiLastName)
            throws SerializationException {
        serializeForClientMsgOtherPlayer(npc, writer);
    }

    public boolean isInSafeZone() {

        Zone zone = ZoneManager.findSmallestZone(this.getLoc());

        if (zone != null) {
            return zone.getSafeZone() == (byte) 1;
        }

        return false;
        //return this.safeZone;
    }

    public static void serializeForClientMsgOtherPlayer(NPC npc, ByteBufferWriter writer)
            throws SerializationException {

        writer.putInt(0);
        writer.putInt(0);

        //num Runes
        int cnt = 3;
        boolean isVamp = false, isHealer = false, isArcher = false, isTrainer = false;
        int contractID = 0, classID = 0;
        int extraRune = 0;

        if (npc.contract != null) {
            contractID = npc.contract.getContractID();
            classID = npc.contract.getClassID();
            extraRune = npc.contract.getExtraRune();

            if (extraRune == contractID)
                extraRune = 0;

        }

        if ((contractID > 252642 && contractID < 252647) || contractID == 252652) {
            isVamp = true;
            cnt++;
        }

        if (contractID == 252582 || contractID == 252579 || contractID == 252581
                || contractID == 252584 || contractID == 252597 || contractID == 252598
                || contractID == 252628 || extraRune == 252582 || extraRune == 252579
                || extraRune == 252581 || extraRune == 252584 || extraRune == 252597
                || extraRune == 252598 || extraRune == 252628) {
            isHealer = true;
            cnt++;
        }

        if (contractID == 252570) {
            isArcher = true;
            cnt++;
        }

        if (classID != 0)
            cnt++;

        if (extraRune != 0 && extraRune != contractID)
            cnt++;

        writer.putInt(cnt);

        //Race
        writer.putInt(1);
        writer.putInt(0);

        if (npc.mobBase != null)
            writer.putInt(npc.mobBase.getLoadID());
        else
            writer.putInt(2011);

        writer.putInt(GameObjectType.NPCRaceRune.ordinal());
        writer.putInt(npc.currentID);

        //Class/Trainer/Whatever
        writer.putInt(5);
        writer.putInt(0);

        if (npc.contract != null)
            writer.putInt(contractID);
        else
            writer.putInt(2500);

        writer.putInt(GameObjectType.NPCClassRune.ordinal());
        writer.putInt(npc.currentID);

        //vampire trainer
        cnt = 0;

        if (extraRune != 0)
            cnt = serializeExtraRune(npc, extraRune, cnt, writer);
        if (isVamp)
            cnt = serializeExtraRune(npc, 252647, cnt, writer);

        //Healer trainer
        if (isHealer)
            cnt = serializeExtraRune(npc, 252592, cnt, writer);

        if (classID != 0) {
            writer.putInt(4);
            writer.putInt(0);
            writer.putInt(classID);
            writer.putInt(GameObjectType.NPCExtraRune.ordinal());
            writer.putInt(npc.currentID);
        }

        //Scout trainer
        if (isArcher)
            cnt = serializeExtraRune(npc, 252654, cnt, writer);

        //Shopkeeper
        writer.putInt(5);
        writer.putInt(0);
        writer.putInt(0x3DACC);
        writer.putInt(GameObjectType.NPCShopkeeperRune.ordinal());
        writer.putInt(npc.currentID);

        //Send Stats
        writer.putInt(5);
        writer.putInt(0x8AC3C0E6); //Str
        writer.putInt(0);
        writer.putInt(0xACB82E33); //Dex
        writer.putInt(0);
        writer.putInt(0xB15DC77E); //Con
        writer.putInt(0);
        writer.putInt(0xE07B3336); //Int
        writer.putInt(0);
        writer.putInt(0xFF665EC3); //Spi
        writer.putInt(0);

        writer.putString(npc.name);
        writer.putString("");

        writer.putInt(0);
        writer.putInt(0);
        writer.putInt(0);
        writer.putInt(0);

        writer.put((byte) 0);
        writer.putInt(npc.getObjectType().ordinal());
        writer.putInt(npc.currentID);

        writer.putFloat(1.0f);
        writer.putFloat(1.0f);
        writer.putFloat(1.0f);

        if (npc.region != null)
            writer.putVector3f(ZoneManager.convertWorldToLocal(npc.building, npc.getLoc()));
        else
            writer.putVector3f(npc.getLoc());

        //Rotation
        float radians = (float) Math.acos(npc.getRot().y) * 2;

        if (npc.building != null)
            if (npc.building.getBounds() != null && npc.building.getBounds().getQuaternion() != null)
                radians += (npc.building.getBounds().getQuaternion()).angleY;

        writer.putFloat(radians);

        //Running Speed
        writer.putInt(0);

        // get a copy of the equipped items.

        if (npc.equip != null) {
            writer.putInt(npc.equip.size());

            for (MobEquipment me : npc.equip.values())
                MobEquipment.serializeForClientMsg(me, writer);
        } else
            writer.putInt(0);

        writer.putInt((npc.level / 10));
        writer.putInt(npc.level);
        writer.putInt(npc.getIsSittingAsInt()); //Standing
        writer.putInt(npc.getIsWalkingAsInt()); //Walking
        writer.putInt(npc.getIsCombatAsInt()); //Combat
        writer.putInt(2); //Unknown
        writer.putInt(1); //Unknown - Headlights?
        writer.putInt(0);

        if (npc.building != null && npc.region != null) {
            writer.putInt(npc.building.getObjectType().ordinal());
            writer.putInt(npc.building.getObjectUUID());
        } else {
            writer.putInt(0); //<-Building Object Type
            writer.putInt(0); //<-Building Object ID
        }
        writer.put((byte) 0);
        writer.put((byte) 0);
        writer.put((byte) 0);

        //npc dialog menus from contracts

        if (npc.contract != null) {
            ArrayList<Integer> npcMenuOptions = npc.contract.getNPCMenuOptions();
            writer.putInt(npcMenuOptions.size());
            for (Integer val : npcMenuOptions) {
                writer.putInt(val);
            }

        } else
            writer.putInt(0);

        writer.put((byte) 1);

        if (npc.building != null) {
            writer.putInt(GameObjectType.StrongBox.ordinal());
            writer.putInt(npc.currentID);
            writer.putInt(GameObjectType.StrongBox.ordinal());
            writer.putInt(npc.building.getObjectUUID());
        } else {
            writer.putLong(0);
            writer.putLong(0);
        }

        if (npc.contract != null)
            writer.putInt(npc.contract.getIconID());
        else
            writer.putInt(0); //npc icon ID

        writer.putInt(0);
        writer.putShort((short) 0);

        if (npc.contract != null && npc.contract.isTrainer()) {
            writer.putInt(classID);
        } else {
            writer.putInt(0);
        }

        if (npc.contract != null && npc.contract.isTrainer())
            writer.putInt(classID);
        else
            writer.putInt(0);

        writer.putInt(0);
        writer.putInt(0);

        writer.putFloat(4);
        writer.putInt(0);
        writer.putInt(0);
        writer.putInt(0);
        writer.put((byte) 0);

        //Pull guild info from building if linked to one

        Guild.serializeForClientMsg(npc.guild, writer, null, true);

        writer.putInt(1);
        writer.putInt(0x8A2E);
        writer.putInt(0);
        writer.putInt(0);

        //TODO Guard
        writer.put((byte) 0); //Is guard..

        writer.putFloat(1500f); //npc.healthMax
        writer.putFloat(1500f); //npc.health

        //TODO Peace Zone
        writer.put((byte) 1); //0=show tags, 1=don't
        writer.putInt(0);
        writer.put((byte) 0);
    }

    private static int serializeExtraRune(NPC npc, int runeID, int cnt, ByteBufferWriter writer) {

        writer.putInt(5);
        writer.putInt(0);
        writer.putInt(runeID);

        if (cnt == 0)
            writer.putInt(GameObjectType.NPCClassRuneTwo.ordinal());
        else
            writer.putInt(GameObjectType.NPCClassRuneThree.ordinal());

        writer.putInt(npc.currentID);
        return cnt + 1;
    }

    public static NPC createNPC(String name, int contractID, Vector3fImmutable spawn, Guild guild, Zone parent, short level, Building building) {

        NPC newNPC = new NPC();

        newNPC.name = name;
        newNPC.contractUUID = contractID;

        if (building == null)
            newNPC.bindLoc = spawn;
        else
            newNPC.bindLoc = Vector3fImmutable.ZERO;

        newNPC.parentZoneUUID = parent.getObjectUUID();
        newNPC.guildUUID = guild.getObjectUUID();

        if (building == null)
            newNPC.buildingUUID = 0;
        else
            newNPC.buildingUUID = building.getObjectUUID();

        newNPC.level = level;

        newNPC.buyPercent = .33f;
        newNPC.sellPercent = 1;

        NPC npc;

        try {
            npc = DbManager.NPCQueries.PERSIST(newNPC);
            npc.setObjectTypeMask(MBServerStatics.MASK_NPC);
        } catch (Exception e) {
            Logger.error(e);
            npc = null;
        }

        return npc;
    }

    public static NPC getNPC(int id) {

        if (id == 0)
            return null;

        NPC npc = (NPC) DbManager.getFromCache(GameObjectType.NPC, id);

        if (npc != null)
            return npc;

        return DbManager.NPCQueries.GET_NPC(id);
    }

    /*
     * Getters
     */

    public static boolean ISGuardCaptain(int contractID) {
        return MinionType.ContractToMinionMap.containsKey(contractID);
    }

    public static boolean UpdateEquipSetID(NPC npc, int equipSetID) {

        if (!LootManager._bootySetMap.containsKey(equipSetID))
            return false;

        if (!DbManager.NPCQueries.UPDATE_EQUIPSET(npc, equipSetID))
            return false;

        npc.equipmentSetID = equipSetID;

        return true;
    }

    public static boolean UpdateRaceID(NPC npc, int raceID) {

        if (!DbManager.NPCQueries.UPDATE_MOBBASE(npc, raceID))
            return false;

        npc.loadID = raceID;
        npc.mobBase = MobBase.getMobBase(npc.loadID);
        return true;
    }

    public static NPCProfits GetNPCProfits(NPC npc) {
        return NPCProfits.ProfitCache.get(npc.currentID);
    }

    public final void submitUpgradeJob() {

        JobContainer jc;

        if (this.getUpgradeDateTime() == null) {
            Logger.error("Attempt to submit upgrade job for non-ranking NPC");
            return;
        }

        // Submit upgrade job for future date or current instant

        if (this.getUpgradeDateTime().isAfter(DateTime.now()))
            jc = JobScheduler.getInstance().scheduleJob(new UpgradeNPCJob(this),
                    this.getUpgradeDateTime().getMillis());
        else
            JobScheduler.getInstance().scheduleJob(new UpgradeNPCJob(this), 0);

    }

    public void setRank(int newRank) {

        DbManager.NPCQueries.SET_PROPERTY(this, "npc_level", newRank);
        this.level = (short) newRank;
    }

    public int getDBID() {
        return this.dbID;
    }

    @Override
    public int getObjectUUID() {
        return currentID;
    }

    public MobBase getMobBase() {
        return this.mobBase;
    }

    public Contract getContract() {
        return this.contract;
    }

    public int getContractID() {

        if (this.contract != null)
            return this.contract.getObjectUUID();

        return 0;
    }

    public boolean isStatic() {
        return this.isStatic;
    }

    public Building getBuilding() {
        return this.building;
    }

    public void setBuilding(Building value) {
        this.building = value;
    }

    @Override
    public String getFirstName() {
        return this.name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    @Override
    public String getLastName() {
        return "";
    }

    @Override
    public Vector3fImmutable getBindLoc() {
        return this.bindLoc;
    }

    @Override
    public int getGuildUUID() {

        if (this.guild == null)
            return 0;

        return this.guild.getObjectUUID();
    }

    public void removeMinions() {

        for (Mob toRemove : this.siegeMinionMap.keySet()) {

            try {
                toRemove.clearEffects();
            } catch (Exception e) {
                Logger.error(e.getMessage());
            }

            if (toRemove.getParentZone() != null)
                toRemove.getParentZone().zoneMobSet.remove(toRemove);

            WorldGrid.RemoveWorldObject(toRemove);
            DbManager.removeFromCache(toRemove);

            PlayerCharacter petOwner = toRemove.getOwner();

            if (petOwner != null) {

                petOwner.setPet(null);
                toRemove.setOwner(null);

                PetMsg petMsg = new PetMsg(5, null);
                Dispatch dispatch = Dispatch.borrow(petOwner, petMsg);
                DispatchMessage.dispatchMsgDispatch(dispatch, Enum.DispatchChannel.PRIMARY);

            }
        }
    }

    @Override
    public float getSpeed() {

        if (this.isWalk())
            return MBServerStatics.WALKSPEED;
        else
            return MBServerStatics.RUNSPEED;
    }

    @Override
    public float getPassiveChance(String type, int AttackerLevel, boolean fromCombat) {
        //TODO add this later for dodge
        return 0f;
    }

    /**
     * @ Kill this Character
     */
    @Override
    public void killCharacter(AbstractCharacter attacker) {
        //TODO Handle Death
        killCleanup();

        //TODO Send death message if needed
    }

    @Override
    public void killCharacter(String reason) {
        //TODO Handle Death
        killCleanup();

        //Orphan inventory so it can be looted
        //if (!this.inSafeZone)
        if (this.charItemManager != null)
            this.charItemManager.orphanInventory();

        //TODO Send death message if needed
        //Question? How would a mob die to water?
    }

    private void killCleanup() {
        //TODO handle cleanup from death here

        //set so character won't load
        this.load = false;

        //Create Corpse and add to world
        //Corpse.makeCorpse(this);
        //TODO damage equipped items
        //TODO cleanup any timers
    }

    public Zone getParentZone() {
        return this.parentZone;
    }

    public int getParentZoneUUID() {

        if (this.parentZone != null)
            return this.parentZone.getObjectUUID();

        return 0;
    }

    @Override
    public Vector3fImmutable getLoc() {

        return super.getLoc();
    }

    public float getSellPercent() {
        return this.sellPercent;
    }

    public float getBuyPercent() {
        return this.buyPercent;
    }

    public float getBuyPercent(PlayerCharacter player) {
        if (NPC.GetNPCProfits(this) == null || this.guild == null)
            return this.buyPercent;
        NPCProfits profits = NPC.GetNPCProfits(this);
        if (player.getGuild().equals(this.guild))
            return profits.buyGuild;
        if (player.getGuild().getNation().equals(this.guild.getNation()))
            return profits.buyNation;

        return profits.buyNormal;
    }

    public float getSellPercent(PlayerCharacter player) {
        if (NPC.GetNPCProfits(this) == null || this.guild == null)
            return 1 + this.sellPercent;
        NPCProfits profits = NPC.GetNPCProfits(this);
        if (player.getGuild().equals(this.guild))
            return 1 + profits.sellGuild;
        if (player.getGuild().getNation().equals(this.guild.getNation()))
            return 1 + profits.sellNation;

        return 1 + profits.sellNormal;
    }

    @Override
    public boolean canBeLooted() {
        return !this.isAlive();
    }

    public ArrayList<Integer> getModTypeTable() {
        return this.modTypeTable;
    }

    @Override
    public void updateDatabase() {
        DbManager.NPCQueries.updateDatabase(this);
    }

    public int getSymbol() {
        return symbol;
    }

    public ArrayList<Integer> getModSuffixTable() {
        return modSuffixTable;
    }

    public ArrayList<Byte> getItemModTable() {
        return itemModTable;
    }

    public boolean isRanking() {

        return this.upgradeDateTime != null;
    }

    @Override
    public void runAfterLoad() {

        if (ConfigManager.serverType.equals(ServerType.LOGINSERVER))
            return;

        this.contract = DbManager.ContractQueries.GET_CONTRACT(this.contractUUID);

        if (this.equipmentSetID == 0 && this.contract != null)
            this.equipmentSetID = this.contract.equipmentSet;

        // Default to contract load ID

        if (loadID == 0) {

            if (this.contract != null)
                loadID = this.contract.getMobbaseID();
            else {
                Logger.error("Invalid contract for NPC: " + this.getObjectUUID());
                loadID = 2100; // Default human male
            }
        }

        this.mobBase = MobBase.getMobBase(this.loadID);

        this.building = BuildingManager.getBuilding(this.buildingUUID);

        if (this.building != null)
            this.guild = this.building.getGuild();
        else
            this.guild = Guild.getGuild(this.guildUUID);


        if (this.guild == null)
            this.guild = Guild.getErrantGuild();

        if (this.contract == null)
            return; // Early exit for npc guild owners

        // Name override for this npc

        if (wordCount(this.name) < 2 && this.contract != null)
            this.name += " the " + this.contract.getName();

        // Initialize inventory

        this.charItemManager = new CharacterItemManager(this);

        // Configure parent zone adding this NPC to the
        // zone collection

        this.parentZone = ZoneManager.getZoneByUUID(this.parentZoneUUID);
        this.parentZone.zoneNPCSet.remove(this);
        this.parentZone.zoneNPCSet.add(this);

        // Setup location for this NPC

        this.bindLoc = new Vector3fImmutable(this.statLat, this.statAlt, this.statLon);
        this.bindLoc = this.parentZone.getLoc().add(this.bindLoc);
        this.loc = new Vector3fImmutable(bindLoc);

        // Handle NPCs within buildings

        if (this.building != null)
            NPCManager.slotCharacterInBuilding(this);

        if (this.contract != null) {
            this.symbol = this.contract.getIconID();
            this.modTypeTable = this.contract.getNPCModTypeTable();
            this.modSuffixTable = this.contract.getNpcModSuffixTable();
            this.itemModTable = this.contract.getItemModTable();
            int VID = this.contract.getVendorID();

            if (VID != 0)
                this.vendorID = VID;
            else
                this.vendorID = 1; //no vendor items
        }

        if (this.mobBase != null) {
            this.healthMax = this.mobBase.getHealthMax();
            this.manaMax = 0;
            this.staminaMax = 0;
            this.setHealth(this.healthMax);
            this.mana.set(this.manaMax);
            this.stamina.set(this.staminaMax);
        }

        if (this.parentZone.isPlayerCity())
            if (NPC.GetNPCProfits(this) == null)
                NPCProfits.CreateProfits(this);

        //TODO set these correctly later
        this.rangeHandOne = 8;
        this.rangeHandTwo = -1;
        this.minDamageHandOne = 1;
        this.maxDamageHandOne = 4;
        this.minDamageHandTwo = 1;
        this.maxDamageHandTwo = 4;
        this.atrHandOne = 300;
        this.defenseRating = 200;
        this.isActive = true;

        this.charItemManager.load();

        this.equip = loadEquipmentSet(this.equipmentSetID);

        if (this.equip == null)
            this.equip = new HashMap<>();

        try {

            DbManager.NPCQueries.LOAD_ALL_ITEMS_TO_PRODUCE(this);

            for (ProducedItem producedItem : this.forgedItems) {
                MobLoot ml = new MobLoot(this, ItemBase.getItemBase(producedItem.getItemBaseID()), false);

                DbManager.NPCQueries.UPDATE_ITEM_ID(producedItem.getID(), currentID, ml.getObjectUUID());

                if (producedItem.isInForge()) {

                    if (producedItem.getPrefix() != null && !producedItem.getPrefix().isEmpty()) {
                        ml.addPermanentEnchantment(producedItem.getPrefix(), 0, 0, true);
                        ml.setPrefix(producedItem.getPrefix());
                    }

                    if (producedItem.getSuffix() != null && !producedItem.getSuffix().isEmpty()) {
                        ml.addPermanentEnchantment(producedItem.getSuffix(), 0, 0, false);
                        ml.setSuffix(producedItem.getSuffix());
                    }

                    if (!producedItem.isRandom())
                        ml.setIsID(true);

                    ml.loadEnchantments();

                    ml.setValue(producedItem.getValue());
                    ml.setDateToUpgrade(producedItem.getDateToUpgrade().getMillis());
                    ml.containerType = Enum.ItemContainerType.FORGE;
                    this.addItemToForge(ml);

                } else {

                    if (producedItem.getPrefix() != null && !producedItem.getPrefix().isEmpty()) {
                        ml.addPermanentEnchantment(producedItem.getPrefix(), 0, 0, true);
                        ml.setPrefix(producedItem.getPrefix());
                    }

                    if (producedItem.getSuffix() != null && !producedItem.getSuffix().isEmpty()) {
                        ml.addPermanentEnchantment(producedItem.getSuffix(), 0, 0, false);
                        ml.setSuffix(producedItem.getSuffix());
                    }

                    ml.setDateToUpgrade(producedItem.getDateToUpgrade().getMillis());
                    ml.containerType = Enum.ItemContainerType.INVENTORY;
                    ml.setIsID(true);

                    this.charItemManager.addItemToInventory(ml);
                }
                ml.setValue(producedItem.getValue());
            }

            // Create NPC bounds object
            Bounds npcBounds = Bounds.borrow();
            npcBounds.setBounds(this.getLoc());

        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }

    public void removeFromZone() {
        this.parentZone.zoneNPCSet.remove(this);
    }

    @Override
    protected ConcurrentHashMap<Integer, CharacterPower> initializePowers() {
        return new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    }

    public DateTime getUpgradeDateTime() {
        lock.readLock().lock();
        try {
            return upgradeDateTime;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setUpgradeDateTime(DateTime upgradeDateTime) {

        if (!DbManager.NPCQueries.updateUpgradeTime(this, upgradeDateTime)) {
            Logger.error("Failed to set upgradeTime for building " + currentID);
            return;
        }

        this.upgradeDateTime = upgradeDateTime;
    }

    public ArrayList<MobLoot> getRolling() {
        synchronized (rolling) {
            return rolling;
        }
    }

    public int getRollingCount() {
        synchronized (this.rolling) {
            return rolling.size();
        }
    }

    public void addItemToForge(MobLoot item) {
        synchronized (this.rolling) {
            this.rolling.add(item);
        }
    }

    public void removeItemFromForge(Item item) {
        synchronized (this.rolling) {
            this.rolling.remove(item);
        }
    }

    @Override
    public Guild getGuild() {
        if (this.building != null)
            return building.getGuild();
        return this.guild;
    }

    public HashSet<Integer> getCanRoll() {

        if (this.canRoll == null)
            this.canRoll = DbManager.ItemQueries.GET_ITEMS_FOR_VENDOR(this.vendorID);

        HashSet<Integer> fullItemList = this.canRoll;
        HashSet<Integer> filteredItemList = new HashSet<>();
        short maxSkill = 25;

        switch (this.getRank()) {
            case 1:
                maxSkill = 25;
                break;
            case 2:
                maxSkill = 50;
                break;
            case 3:
            case 4:
                maxSkill = 75;
                break;
            case 5:
            case 6:
                maxSkill = 100;
                break;
            case 7:
                maxSkill = 110;
                break;
        }
        ItemBase itemBase;
        for (Integer itemID : fullItemList) {
            itemBase = ItemBase.getItemBase(itemID);
            boolean exclude = itemBase.getPercentRequired() == 0 && itemBase.getType() == ItemType.WEAPON;
            if (itemBase.getPercentRequired() <= maxSkill && !exclude)
                filteredItemList.add(itemID);
        }
        if (this.contract.getVendorID() == 102) {

            for (int i = 0; i < this.getRank(); i++) {
                int subID = i + 1;
                filteredItemList.add(910010 + subID);
            }

            if (this.getRank() == 7)
                filteredItemList.add(910018);
        }

        return filteredItemList;
    }

    public int getRollingTimeInSeconds(int itemID) {

        ItemBase ib = ItemBase.getItemBase(itemID);

        if (ib == null)
            return 0;

        if (ib.getType() == ItemType.SCROLL)
            return this.getRank() * 60 * 60 * 3;

        float time;

        if (this.building == null)
            return 600;

        float rank = this.building.getRank() - 1;
        float rate = (float) (2.5 * rank);
        time = (20 - rate);
        time *= 60;
        return (int) time;
    }

    public ConcurrentHashMap<Mob, Integer> getSiegeMinionMap() {
        return siegeMinionMap;
    }

    public boolean remove() {

        Building building;

        // Remove npc from it's building

        building = this.building;

        if (building != null) {
            building.getHirelings().remove(this);
            this.removeMinions();
        }

        // Delete npc from database

        if (DbManager.NPCQueries.DELETE_NPC(this) == 0) {
            return false;
        }

        // Remove npc from the simulation

        this.removeFromCache();
        WorldGrid.RemoveWorldObject(this);
        WorldGrid.removeObject(this);
        return true;
    }

    public int getUpgradeCost() {

        int upgradeCost;

        upgradeCost = Integer.MAX_VALUE;

        if (this.getRank() < 7)
            return (this.getRank() * 100650) + 21450;

        return upgradeCost;
    }

    public int getUpgradeTime() {

        int upgradeTime; // expressed in hours

        upgradeTime = Integer.MAX_VALUE;

        if (this.getRank() < 7)
            return (this.getRank() * 8);

        return 0;
    }

    public synchronized Item produceItem(int playerID, int amount, boolean isRandom, int pToken, int sToken, String customName, int itemID) {

        Zone serverZone;
        City city;
        Item item = null;

        PlayerCharacter player = null;

        if (playerID != 0)
            player = SessionManager.getPlayerCharacterByID(playerID);

        try {

            if (this.getRollingCount() >= this.getRank()) {

                if (player != null)
                    ChatManager.chatSystemInfo(player, this.getName() + " " + this.getContract().getName() + " slots are full");

                return null;
            }

            // Cannot roll items without a warehouse.
            // Due to the fact fillForge references the
            // warehouse and early exits.  *** Refactor???

            serverZone = this.building.getParentZone();

            if (serverZone == null)
                return null;

            city = City.GetCityFromCache(serverZone.getPlayerCityUUID());

            if (city == null) {

                if (player != null)
                    ErrorPopupMsg.sendErrorMsg(player, "Could not find city."); // Production denied: This building must be protected to gain access to warehouse resources.

                return null;
            }

            if (this.building == null) {

                if (player != null)
                    ErrorPopupMsg.sendErrorMsg(player, "Could not find building."); // Production denied: This building must be protected to gain ac

                return null;
            }

            //TODO create Normal Items.

            if (amount == 0)
                amount = 1;

            if (isRandom)
                item = ItemFactory.randomRoll(this, player, amount, itemID);
            else
                item = ItemFactory.fillForge(this, player, amount, itemID, pToken, sToken, customName);

            if (item == null)
                return null;

            ItemProductionMsg outMsg = new ItemProductionMsg(this.building, this, item, 8, true);
            DispatchMessage.dispatchMsgToInterestArea(this, outMsg, DispatchChannel.SECONDARY, 700, false, false);

        } catch (Exception e) {
            Logger.error(e);
        }
        return item;
    }

    public synchronized boolean completeItem(int itemUUID) {

        MobLoot targetItem;

        try {
            targetItem = MobLoot.getFromCache(itemUUID);

            if (targetItem == null)
                return false;

            if (!this.getCharItemManager().forgeContains(targetItem, this))
                return false;

            if (!DbManager.NPCQueries.UPDATE_ITEM_TO_INVENTORY(targetItem.getObjectUUID(), currentID))
                return false;

            targetItem.setIsID(true);

            this.rolling.remove(targetItem);
            this.getCharItemManager().addItemToInventory(targetItem);

            //remove from client forge window

            ItemProductionMsg outMsg1 = new ItemProductionMsg(this.building, this, targetItem, 9, true);
            DispatchMessage.dispatchMsgToInterestArea(this, outMsg1, DispatchChannel.SECONDARY, MBServerStatics.STRUCTURE_LOAD_RANGE, false, false);
            ItemProductionMsg outMsg = new ItemProductionMsg(this.building, this, targetItem, 10, true);
            DispatchMessage.dispatchMsgToInterestArea(this, outMsg, DispatchChannel.SECONDARY, MBServerStatics.STRUCTURE_LOAD_RANGE, false, false);

        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
        return true;
    }

    public HashMap<Integer, MobEquipment> getEquip() {
        return equip;
    }

    public int getEquipmentSetID() {
        return equipmentSetID;
    }

    public String getNameOverride() {
        return name;
    }

    public int getRepairCost() {
        return repairCost;
    }

    public void setRepairCost(int repairCost) {
        this.repairCost = repairCost;
    }

    public void processUpgradeNPC(PlayerCharacter player) {

        int rankCost;
        Building building;
        DateTime dateToUpgrade;


        this.lock.writeLock().lock();
        try {


            building = this.getBuilding();

            // Cannot upgrade an npc not within a building

            if (building == null)
                return;

            // Cannot upgrade an npc at max rank

            if (this.getRank() == 7)
                return;

            // Cannot upgrade an npc who is currently ranking

            if (this.isRanking())
                return;

            rankCost = this.getUpgradeCost();

            // SEND NOT ENOUGH GOLD ERROR

            if (!building.hasFunds(rankCost)) {
                sendErrorPopup(player, 127);
                return;
            }

            if (rankCost > building.getStrongboxValue()) {
                sendErrorPopup(player, 127);
                return;
            }

            try {

                if (!building.transferGold(-rankCost, false))
                    return;

                dateToUpgrade = DateTime.now().plusHours(this.getUpgradeTime());

                this.setUpgradeDateTime(dateToUpgrade);

                // Schedule upgrade job

                this.submitUpgradeJob();

            } catch (Exception e) {
                PlaceAssetMsg.sendPlaceAssetError(player.getClientConnection(), 1, "A Serious error has occurred. Please post details for to ensure transaction integrity");
            }
        } catch (Exception e) {
            Logger.error(e);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public ArrayList<MobEquipment> getSellInventorySteward() {

        ArrayList<MobEquipment> smallList = new ArrayList<>();
        for (MobEquipment me : this.contract.getSellInventory()) {
            int rankRequired = 0;
            if (me.getItemBase().getType().equals(Enum.ItemType.CONTRACT)) {
                switch (me.getItemBase().getUUID()) {
                    case 866:
                    case 889:
                    case 860:
                    case 850:
                    case 892:
                    case 1502003:
                    case 890:
                    case 896:
                        rankRequired = 1;
                        break;
                    case 899:
                    case 801:
                    case 803:
                    case 802:
                    case 821:
                    case 810:
                    case 806:
                    case 818:
                    case 800:
                        rankRequired = 2;
                        break;

                    case 840:
                    case 848:
                    case 813:
                    case 805:
                    case 804:
                    case 838:
                        rankRequired = 3;
                        break;
                    case 815:
                        rankRequired = 4;
                        break;
                    case 847:
                    case 830:
                    case 820:
                        rankRequired = 6;
                        break;
                    case 865:
                    case 252637:
                        rankRequired = 7;
                        break;
                    case 1502002: // harvesters don't need to exist but might one day
                    case 1502001: //mine guards don't need to exist but might one day
                        continue;
                }
                if(me.getItemBase().getName().toLowerCase().contains("trainer") || me.getItemBase().getName().toLowerCase().contains("refiner")){
                    rankRequired = 5;
                }
                if (this.getRank() >= rankRequired)
                    smallList.add(me);
            }
        }
        return smallList;
    }

    public ArrayList<MobEquipment> getSellInventoryBuilder() {

        ArrayList<MobEquipment> smallList = new ArrayList<>();
        int maxValue = 0;
        switch(this.getRank()){
            case 1:
                maxValue = 300000;
                break;
            case 2:
                maxValue = 450000;
                break;
            case 3:
                maxValue = 550000;
                break;
            case 4:
                maxValue = 650000;
                break;
            case 5:
                maxValue = 1000000;
                break;
            case 6:
            case 7:
                maxValue = 999999999;
                break;

        }
        for(MobEquipment me : this.contract.getSellInventory()){
            if(me.getItemBase().getBaseValue() <= maxValue)
                smallList.add(me);
        }
        return smallList;
    }

}
