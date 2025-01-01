// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import engine.Enum;
import engine.InterestManagement.InterestManager;
import engine.InterestManagement.WorldGrid;
import engine.gameManager.*;
import engine.math.Vector3f;
import engine.math.Vector3fImmutable;
import engine.net.ByteBufferWriter;
import engine.net.client.msg.ErrorPopupMsg;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import static engine.gameManager.DbManager.MineQueries;
import static engine.gameManager.DbManager.getObject;
import static engine.math.FastMath.sqr;

public class Mine extends AbstractGameObject {

    public static ConcurrentHashMap<Mine, Integer> mineMap = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    public static ConcurrentHashMap<Integer, Mine> towerMap = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    private final String zoneName;
    private final Zone parentZone;
    public boolean isActive = false;
    public PlayerCharacter lastClaimer;
    public boolean wasClaimed = false;
    // Not persisted to DB
    public String guildName;
    public GuildTag guildTag;
    public String nationName;
    public GuildTag nationTag;
    private Resource production;
    private Guild owningGuild;
    private int flags;
    private int buildingID;
    private MineProduction mineType;

    public int openHour;
    public int openMinute;
    public int capSize;
    public LocalDateTime liveTime;
    public final HashSet<Integer> _playerMemory = new HashSet<>();
    public ArrayList<PlayerCharacter> affectedPlayers = new ArrayList<>();

    //stronghold stuff
    public boolean isStronghold = false;
    public ArrayList<Mob> strongholdMobs;
    public HashMap<Integer,Integer> oldBuildings;

    /**
     * ResultSet Constructor
     */
    public Mine(ResultSet rs) throws SQLException, UnknownHostException {
        super(rs);

        if (rs.getInt("capSize") == 0) {
            throw new IllegalArgumentException("Mine creation canceled: capSize cannot be 0");
        }

        this.mineType = MineProduction.getByName(rs.getString("mine_type"));

        int ownerUID = rs.getInt("mine_ownerUID");
        this.buildingID = rs.getInt("mine_buildingUID");
        this.flags = rs.getInt("flags");
        int parent = rs.getInt("parent");
        if(ZoneManager.getZoneByUUID(parent) != null) {
            this.parentZone = ZoneManager.getZoneByUUID(parent);
            this.zoneName = this.parentZone.getParent().getName();
        }else{
            this.parentZone = ZoneManager.getSeaFloor();
            if(this.parentZone.getParent() != null)
                this.zoneName = this.parentZone.getParent().getName();
            else
                this.zoneName = "FAILED TO LOAD ZONE";
            Logger.error("MINE FAILED TO LOAD PARENT: ");
            Logger.error("MINE UID: " + rs.getInt("UID"));
            Logger.error("MINE buildingID: " + buildingID);

        }

        this.owningGuild = Guild.getGuild(ownerUID);
        Guild nation = null;

        if (this.owningGuild.isEmptyGuild()) {
            this.guildName = "";
            this.guildTag = GuildTag.ERRANT;
            nation = Guild.getErrantGuild();
            this.owningGuild = Guild.getErrantGuild();
        } else {
            this.guildName = this.owningGuild.getName();
            this.guildTag = this.owningGuild.getGuildTag();
            nation = this.owningGuild.getNation();
        }

        if (!nation.isEmptyGuild()) {
            this.nationName = nation.getName();
            this.nationTag = nation.getGuildTag();
        } else {
            this.nationName = "";
            this.nationTag = GuildTag.ERRANT;
        }

        this.production = Resource.valueOf(rs.getString("mine_resource"));
        this.lastClaimer = null;
        this.openHour = rs.getInt("mineLiveHour");
        this.openMinute = rs.getInt("mineLiveMinute");
        this.capSize = rs.getInt("capSize");
        this.liveTime = LocalDateTime.now().withHour(this.openHour).withMinute(this.openMinute);
        Building tower = BuildingManager.getBuildingFromCache(this.buildingID);
        if(tower != null){
            tower.setMaxHitPoints(5000f * this.capSize);
            tower.setCurrentHitPoints(tower.healthMax);
        }
    }

    public static void releaseMineClaims(PlayerCharacter playerCharacter) {

        if (playerCharacter == null)
            return;

        for (Mine mine : Mine.getMines()) {

            if (mine.lastClaimer != null)
                if (mine.lastClaimer.equals(playerCharacter)) {
                    mine.lastClaimer = null;
                    mine.updateGuildOwner(null);
                }

        }
    }

    public static void SendMineAttackMessage(Building mine) {

        if (mine.getBlueprint() == null)
            return;

        if (mine.getBlueprint().getBuildingGroup() != Enum.BuildingGroup.MINE)
            return;


        if (mine.getGuild().isEmptyGuild())
            return;

        if (mine.getGuild().getNation().isEmptyGuild())
            return;

        if (mine.getTimeStamp("MineAttack") > System.currentTimeMillis())
            return;

        mine.getTimestamps().put("MineAttack", System.currentTimeMillis() + MBServerStatics.ONE_MINUTE);

        ChatManager.chatNationInfo(mine.getGuild().getNation(), mine.getName() + " in " + mine.getParentZone().getParent().getName() + " is Under attack!");
    }

    public static void loadAllMines() {

        try {

            //Load mine resources
            MineProduction.addResources();

            //pre-load all building sets
            ArrayList<Mine> serverMines = MineQueries.GET_ALL_MINES_FOR_SERVER();

            for (Mine mine : serverMines) {
                if(mine.capSize != 0) {
                    Mine.mineMap.put(mine, mine.buildingID);
                    Mine.towerMap.put(mine.buildingID, mine);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Getters
     */

    public static Mine getMineFromTower(int towerID) {
        return Mine.towerMap.get(towerID);
    }

    public static void serializeForClientMsg(Mine mine, ByteBufferWriter writer) {
        try {
            writer.putInt(mine.getObjectType().ordinal());
            writer.putInt(mine.getObjectUUID());
            writer.putInt(mine.getObjectUUID()); //actually a hash of mine
            if(mine.isStronghold){
                writer.putString("STRONGHOLD");
                writer.putString("");
            }else {
                writer.putString(mine.mineType.name);
                writer.putString(mine.capSize + " Man ");
            }
            //writer.putString(mine.zoneName + " " + mine.capSize + " Man ");

            writer.putInt(mine.production.hash);
            writer.putInt(mine.production.baseProduction);
            writer.putInt(mine.getModifiedProductionAmount()); //TODO calculate range penalty here
            writer.putInt(3600); //window in seconds

            LocalDateTime mineOpenTime = LocalDateTime.now().withHour(mine.openHour).withMinute(mine.openMinute).withSecond(0).withNano(0);

            writer.putLocalDateTime(mineOpenTime);
            writer.putLocalDateTime(mineOpenTime.plusMinutes(30));
            writer.put(mine.isActive ? (byte) 0x01 : (byte) 0x00);

            Building mineTower = BuildingManager.getBuilding(mine.buildingID);
            if (mineTower != null) {
                writer.putFloat(mineTower.getLoc().x);
                writer.putFloat(mineTower.getParentZone().getLoc().y);
                writer.putFloat(mineTower.getLoc().z);
            } else {
                writer.putFloat(mine.parentZone.getLoc().x);
                writer.putFloat(mine.parentZone.getLoc().y);
                writer.putFloat(mine.parentZone.getLoc().z);
                Logger.error("Mine Tower Was Null For Mine: " + mine.getObjectUUID());
            }

            writer.putInt(mine.isExpansion() ? mine.mineType.xpacHash : mine.mineType.hash);

            if (mine.isStronghold) {
                writer.putString("");
                GuildTag._serializeForDisplay(Guild.getErrantGuild().getGuildTag(), writer);
                writer.putString("");
                GuildTag._serializeForDisplay(Guild.getErrantGuild().getGuildTag(), writer);
            }else {
                writer.putString(mine.guildName);
                GuildTag._serializeForDisplay(mine.guildTag, writer);
                writer.putString(mine.nationName);
                GuildTag._serializeForDisplay(mine.nationTag, writer);
            }
        } catch (Exception e) {
            Logger.error("Failed TO Serialize Mine Because: " + e.getMessage());
        }
    }

    public static ArrayList<Mine> getMinesForGuild(int guildID) {

        ArrayList<Mine> mineList = new ArrayList<>();

        // Only inactive mines are returned.

        for (Mine mine : Mine.mineMap.keySet()) {
            if (mine.owningGuild.getObjectUUID() == guildID)
                mineList.add(mine);
        }
        return mineList;
    }

    /*
     * Database
     */
    public static Mine getMine(int UID) {
        return MineQueries.GET_MINE(UID);

    }

    public static ArrayList<Mine> getMines() {
        return new ArrayList<>(mineMap.keySet());
    }

    public static boolean validateClaimer(PlayerCharacter playerCharacter) {

        // Method validates that the claimer meets
        // all the requirements to claim; landed
        // guild with a warehouse, etc.

        Guild playerGuild;

        //verify the player exists

        if (playerCharacter == null)
            return false;

        //verify the player is in valid guild

        playerGuild = playerCharacter.getGuild();

        // Can't claim something if you don't have a guild!

        if (playerGuild.isEmptyGuild())
            return false;

        if (playerGuild.getNation().isEmptyGuild())
            return false;

        // Guild must own a city to hold a mine.

        City guildCity = playerGuild.getOwnedCity();

        if (guildCity == null)
            return false;

        if (guildCity.getWarehouse() == null) {
            ErrorPopupMsg.sendErrorMsg(playerCharacter, "No Warehouse exists for this claim.");
            return false;
        }

        // Number of mines is based on the rank of the nation's tree.

        City nationCapitol = playerGuild.getNation().getOwnedCity();

        Building nationCapitolTOL = nationCapitol.getTOL();

        if (nationCapitolTOL == null)
            return false;

        int treeRank = nationCapitolTOL.getRank();

        if (treeRank < 1)
            return false;

        return true;
    }

    public static ArrayList<Mine> getMinesToTeleportTo(PlayerCharacter player) {
        ArrayList<Mine> mines = new ArrayList<>();
        for(Mine mine : Mine.getMines())
            if(!mine.isActive)
                if(mine.getOwningGuild() != null)
                  if(mine.getOwningGuild().getNation().equals(player.getGuild().getNation()))
                         mines.add(mine);

        return mines;
    }

    public boolean changeProductionType(Resource resource) {
        if (!this.validForMine(resource))
            return false;
        //update resource in database;
        if (!MineQueries.CHANGE_RESOURCE(this, resource))
            return false;

        this.production = resource;
        return true;
    }

    public MineProduction getMineType() {
        return this.mineType;
    }

    public void setMineType(String type) {
        this.mineType = MineProduction.getByName(type);
    }

    public String getZoneName() {
        return this.zoneName;
    }

    public Resource getProduction() {
        return this.production;
    }

    public boolean getIsActive() {
        return this.isActive;
    }

    public Guild getOwningGuild() {
        if (this.owningGuild == null)
            return Guild.getErrantGuild();
        else
            return this.owningGuild;
    }

    public void setOwningGuild(Guild owningGuild) {
        this.owningGuild = owningGuild;
    }

    /*
     * Serialization
     */

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public Zone getParentZone() {
        return parentZone;
    }

    public GuildTag getGuildTag() {
        return guildTag;
    }

    public void setActive(boolean isAc) {

        this.isActive = isAc;
        Building building = BuildingManager.getBuildingFromCache(this.buildingID);
        if (building != null && !this.isActive)
            building.isDeranking.compareAndSet(true, false);

        if(!isAc){
            for(PlayerCharacter player : this.affectedPlayers){
                try {
                    player.ZergMultiplier = 1.0f;
                } catch(Exception e){
                    //something went wrong resetting zerg multiplier, maybe player was deleted?
                }
            }
        }
    }

    public boolean validForMine(Resource r) {
        if (this.mineType == null) {
            Logger.error("Mine Was Null Setting Resources for Mine: " + this.getObjectUUID());
            return false;
        }
        return this.mineType.validForMine(r);
    }

    public void serializeForMineProduction(ByteBufferWriter writer) {
        writer.putInt(this.getObjectType().ordinal());
        writer.putInt(this.getObjectUUID());
        writer.putInt(this.getObjectUUID()); //actually a hash of mine
        //		writer.putInt(0x215C92BB); //this.unknown1);
        writer.putString(this.mineType.name);
        writer.putString(this.zoneName);
        writer.putInt(this.production.hash);
        writer.putInt(this.getModifiedProductionAmount());
        writer.putInt(this.getModifiedProductionAmount()); //TODO calculate range penalty here
        writer.putInt(3600); //window in seconds
        writer.putInt(this.mineType.hash);
    }

    @Override
    public void updateDatabase() {
        // TODO Create update logic.
    }

    public int getBuildingID() {
        return buildingID;
    }

    public void setBuildingID(int buildingID) {
        this.buildingID = buildingID;
    }

    public void handleDestroyMine() {

        if (!this.isActive)
            return;

        //remove tags from mine

        this.guildName = "";
        this.nationName = "";
        this.owningGuild = Guild.getErrantGuild();
        this.lastClaimer = null;
        this.wasClaimed = false;

        // Update database

        DbManager.MineQueries.CHANGE_OWNER(this, 0);

        // Update mesh

        Building mineBuilding = BuildingManager.getBuildingFromCache(this.buildingID);

        if (mineBuilding == null) {
            Logger.debug("Null mine building " + this.getObjectUUID() + ". Unable to Load Building with UID " + this.buildingID);
            return;
        }

        mineBuilding.setOwner(null);
        mineBuilding.refresh(false);

        // remove hirelings

        Building building = (Building) getObject(Enum.GameObjectType.Building, this.buildingID);
        BuildingManager.cleanupHirelings(building);
    }

    public boolean claimMine(PlayerCharacter claimer) {

        if (claimer == null)
            return false;

        if (!validateClaimer(claimer))
            return false;

        if (!this.isActive) {
            ErrorPopupMsg.sendErrorMsg(claimer, "Can not for to claim inactive mine.");
            return false;
        }

        if (!updateGuildOwner(claimer))
            return false;

        // Successful claim

        this.lastClaimer = claimer;

        return true;
    }

    public boolean depositMineResources() {

        if (this.owningGuild.isEmptyGuild())
            return false;

        if (this.owningGuild.getOwnedCity() == null)
            return false;

        if (this.owningGuild.getOwnedCity().getWarehouse() == null)
            return false;

        ItemBase resourceIB = ItemBase.getItemBase(this.production.UUID);
        return this.owningGuild.getOwnedCity().getWarehouse().depositFromMine(this, resourceIB, this.getModifiedProductionAmount());
    }

    public boolean updateGuildOwner(PlayerCharacter playerCharacter) {

        Building mineBuilding = BuildingManager.getBuildingFromCache(this.buildingID);

        //should never return null, but let's check just in case.

        if (mineBuilding == null) {
            ChatManager.chatSystemError(playerCharacter, "Unable to find mine tower.");
            Logger.debug("Failed to Update Mine with UID " + this.getObjectUUID() + ". Unable to Load Building with UID " + this.buildingID);
            return false;
        }

        if (playerCharacter == null) {
            this.owningGuild = Guild.getErrantGuild();
            this.guildName = "None";
            this.guildTag = GuildTag.ERRANT;
            this.nationName = "None";
            this.nationTag = GuildTag.ERRANT;
            //Update Building.
            mineBuilding.setOwner(null);
            WorldGrid.updateObject(mineBuilding);
            return true;
        }

        Guild guild = playerCharacter.getGuild();

        if (guild.getOwnedCity() == null)
            return false;

        if (!MineQueries.CHANGE_OWNER(this, guild.getObjectUUID())) {
            Logger.debug("Database failed to Change Ownership of Mine with UID " + this.getObjectUUID());
            ChatManager.chatSystemError(playerCharacter, "Failed to claim Mine.");
            return false;
        }

        //update mine.
        this.owningGuild = guild;

        //Update Building.
        PlayerCharacter guildLeader = (PlayerCharacter) Guild.GetGL(this.owningGuild);

        if (guildLeader != null)
            mineBuilding.setOwner(guildLeader);
        WorldGrid.updateObject(mineBuilding);
        return true;
    }

    public boolean isExpansion() {
        return (this.flags & 2) != 0;
    }

    public int getModifiedProductionAmount() {
        ItemBase resourceBase = ItemBase.getItemBase(this.production.UUID);
        if(resourceBase == null)
            return 0;
        int value = resourceBase.getBaseValue();

        int amount = 0;
        switch(this.capSize){
            case 3:
                amount = 1800000;
                break;
            case 5:
                amount = 3000000;
                break;
            case 10:
                amount = 6000000;
                break;
            case 20:
                amount = 12000000;
                break;
        }
        if(this.production.UUID == 7)
            value = 1;

        amount = amount / value;

        return amount;
    }
    public void onEnter() {

        Building tower = BuildingManager.getBuildingFromCache(this.buildingID);
        if(tower == null)
            return;

        // Gather current list of players within the zone bounds

        HashSet<AbstractWorldObject> currentPlayers = WorldGrid.getObjectsInRangePartial(tower.loc, Enum.CityBoundsType.GRID.extents, MBServerStatics.MASK_PLAYER);
        HashMap<Guild,ArrayList<PlayerCharacter>> charactersByNation = new HashMap<>();
        ArrayList<Guild> updatedNations = new ArrayList<>();
        for (AbstractWorldObject playerObject : currentPlayers) {

            if (playerObject == null)
                continue;

            PlayerCharacter player = (PlayerCharacter) playerObject;

            if(this.affectedPlayers.contains(player) == false)
                this.affectedPlayers.add(player);

            if(!this._playerMemory.contains(player.getObjectUUID())){
                this._playerMemory.add(player.getObjectUUID());
            }
            Guild nation = player.guild.getNation();
            if(charactersByNation.containsKey(nation)){
                if(!charactersByNation.get(nation).contains(player)) {
                    charactersByNation.get(nation).add(player);
                    if(!updatedNations.contains(nation)){
                        updatedNations.add(nation);
                    }
                }
            }else{
                ArrayList<PlayerCharacter> players = new ArrayList<>();
                players.add(player);
                charactersByNation.put(nation,players);
                if(!updatedNations.contains(nation)){
                    updatedNations.add(nation);
                }
            }
        }
        for(Guild nation : updatedNations){
            float multiplier = ZergManager.getCurrentMultiplier(charactersByNation.get(nation).size(),this.capSize);
            for(PlayerCharacter player : charactersByNation.get(nation)){
                player.ZergMultiplier = multiplier;
            }
        }
        try
        {
            this.onExit(this._playerMemory);
        }
        catch(Exception ignored){

        }
    }

    private void onExit(HashSet<Integer> currentMemory) {

        Building tower = BuildingManager.getBuildingFromCache(this.buildingID);
        if(tower == null)
            return;
        ArrayList<Integer>toRemove = new ArrayList<>();
        HashSet<AbstractWorldObject> currentPlayers = WorldGrid.getObjectsInRangePartial(tower.loc, Enum.CityBoundsType.GRID.extents, MBServerStatics.MASK_PLAYER);
        for(Integer id : currentMemory){
            PlayerCharacter pc = PlayerCharacter.getPlayerCharacter(id);
            if(currentPlayers.contains(pc) == false){
                toRemove.add(id);
                pc.ZergMultiplier = 1.0f;
            }
        }

        // Remove players from city memory

        _playerMemory.removeAll(toRemove);
    }
    public static Building getTower(Mine mine){
        Building tower = BuildingManager.getBuildingFromCache(mine.buildingID);
        if(tower != null)
            return tower;
        else
            return null;
    }
    public static void serializeForClientMsgTeleport(Mine mine, ByteBufferWriter writer) {
        AbstractCharacter guildRuler;
        Guild rulingGuild;
        Guild rulingNation;
        java.time.LocalDateTime dateTime1900;

        // Cities aren't a mine without a TOL. Time to early exit.
        // No need to spam the log here as non-existant TOL's are indicated
        // during bootstrap routines.
        Building tower = Mine.getTower(mine);
        if (tower == null) {

            Logger.error("NULL TOWER FOR " + mine.zoneName + " mine");
            return;
        }


        // Assign mine owner

        if (tower.getOwner() != null)
            guildRuler = tower.getOwner();
        else
            guildRuler = null;

        // If is an errant tree, use errant guild for serialization.
        // otherwise we serialize the soverign guild

        if (guildRuler == null)
            rulingGuild = Guild.getErrantGuild();
        else
            rulingGuild = guildRuler.getGuild();

        rulingNation = rulingGuild.getNation();

        // Begin Serialzing soverign guild data
        writer.putInt(mine.getObjectType().ordinal());
        writer.putInt(mine.getObjectUUID());
        writer.putString(mine.zoneName + " Mine");
        writer.putInt(rulingGuild.getObjectType().ordinal());
        writer.putInt(rulingGuild.getObjectUUID());

        writer.putString(rulingGuild.getName());
        writer.putString("");
        writer.putString(rulingGuild.getLeadershipType());

        // Serialize guild ruler's name
        // If tree is abandoned blank out the name
        // to allow them a rename.

        if (guildRuler == null)
            writer.putString("");
        else
            writer.putString(guildRuler.getFirstName() + ' ' + guildRuler.getLastName());

        writer.putInt(rulingGuild.getCharter());
        writer.putInt(0); // always 00000000

        writer.put((byte)0);

        writer.put((byte) 1);
        writer.put((byte) 1);  // *** Refactor: What are these flags?
        writer.put((byte) 1);
        writer.put((byte) 1);
        writer.put((byte) 1);

        GuildTag._serializeForDisplay(rulingGuild.getGuildTag(), writer);
        GuildTag._serializeForDisplay(rulingNation.getGuildTag(), writer);

        writer.putInt(0);// TODO Implement description text

        writer.put((byte) 1);
        writer.put((byte) 0);
        writer.put((byte) 1);

        // Begin serializing nation guild info

        if (rulingNation.isEmptyGuild()) {
            writer.putInt(rulingGuild.getObjectType().ordinal());
            writer.putInt(rulingGuild.getObjectUUID());
        } else {
            writer.putInt(rulingNation.getObjectType().ordinal());
            writer.putInt(rulingNation.getObjectUUID());
        }


        // Serialize nation name

        if (rulingNation.isEmptyGuild())
            writer.putString("None");
        else
            writer.putString(rulingNation.getName());

        writer.putInt(1);

        writer.putInt(0xFFFFFFFF);

        writer.putInt(0);

        if (rulingNation.isEmptyGuild())
            writer.putString(" ");
        else
            writer.putString(Guild.GetGL(rulingNation).getFirstName() + ' ' + Guild.GetGL(rulingNation).getLastName());

        writer.putLocalDateTime(LocalDateTime.now());

        if(tower != null) {
            writer.putFloat(tower.loc.x);
            writer.putFloat(tower.loc.y);
            writer.putFloat(tower.loc.z);
        } else{
            writer.putFloat(0);
            writer.putFloat(0);
            writer.putFloat(0);
        }
        writer.putInt(0);

        writer.put((byte) 1);
        writer.put((byte) 0);
        writer.putInt(0x64);
        writer.put((byte) 0);
        writer.put((byte) 0);
        writer.put((byte) 0);
    }
}
