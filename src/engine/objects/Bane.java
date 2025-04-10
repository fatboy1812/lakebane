// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.objects;

import engine.Enum;
import engine.Enum.ProtectionState;
import engine.Enum.SiegePhase;
import engine.Enum.SiegeResult;
import engine.InterestManagement.HeightMap;
import engine.InterestManagement.InterestManager;
import engine.InterestManagement.WorldGrid;
import engine.db.archive.BaneRecord;
import engine.db.archive.DataWarehouse;
import engine.gameManager.*;
import engine.job.JobScheduler;
import engine.jobs.ActivateBaneJob;
import engine.jobs.BaneDefaultTimeJob;
import engine.math.Vector3fImmutable;
import engine.net.Dispatch;
import engine.net.DispatchMessage;
import engine.net.client.ClientConnection;
import engine.net.client.msg.CityDataMsg;
import engine.net.client.msg.PlaceAssetMsg;
import engine.net.client.msg.chat.ChatSystemMsg;
import engine.server.MBServerStatics;
import org.joda.time.DateTime;
import org.pmw.tinylog.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import static engine.Enum.SiegeResult.CAPTURE;

public final class Bane {

    public static ConcurrentHashMap<Integer, Bane> banes = new ConcurrentHashMap<>(MBServerStatics.CHM_INIT_CAP, MBServerStatics.CHM_LOAD, MBServerStatics.CHM_THREAD_LOW);
    private final int cityUUID;
    private final int stoneUUID;
    private int ownerUUID;
    private DateTime placementDate = null;
    private DateTime liveDate = null;
    private BaneDefaultTimeJob defaultTimeJob;
    public boolean timeSet = false;
    public boolean daySet = false;
    public boolean capSet = false;
    public int capSize = 10;

    // Internal cache for banes
    private ActivateBaneJob activateBaneJob;

    public ArrayList<PlayerCharacter> affected_players;

    public HashMap<Integer, Long> mineAttendees = new HashMap<>();
    public final HashSet<Integer> _playerMemory = new HashSet<>();
    public ArrayList<PlayerCharacter> affectedPlayers = new ArrayList<>();

    /**
     * ResultSet Constructor
     */
    public Bane(ResultSet rs) throws SQLException {

        Date sqlDateTime;
        ActivateBaneJob abtj;

        this.cityUUID = rs.getInt("cityUUID");
        this.ownerUUID = rs.getInt("ownerUUID");
        this.stoneUUID = rs.getInt("stoneUUID");

        this.timeSet = rs.getInt("time_set") == 1;
        this.daySet = rs.getInt("day_set") == 1;
        this.capSet = rs.getInt("cap_set") == 1;
        this.capSize = rs.getInt("cap_size");

        sqlDateTime = rs.getTimestamp("placementDate");

        if (sqlDateTime != null)
            this.placementDate = new DateTime(sqlDateTime);

        sqlDateTime = rs.getTimestamp("liveDate");

        if (sqlDateTime != null)
            this.liveDate = new DateTime(sqlDateTime);

        switch (this.getSiegePhase()) {

            case CHALLENGE:
                break;
            case WAR:

                // cancel old job if exists

                if (this.activateBaneJob != null)
                    this.activateBaneJob.cancelJob();

                abtj = new ActivateBaneJob(cityUUID);
                JobScheduler.getInstance().scheduleJob(abtj, this.liveDate.getMillis());
                this.activateBaneJob = abtj;

                break;
            case STANDOFF:

                // cancel old job if exists

                if (this.activateBaneJob != null)
                    this.activateBaneJob.cancelJob();

                abtj = new ActivateBaneJob(cityUUID);
                JobScheduler.getInstance().scheduleJob(abtj, this.liveDate.getMillis());
                this.activateBaneJob = abtj;
                break;
        }

        //default 20 if tree count exceeds 6
        if(!this.capSet){
            int treeCount = 1;
            for(Guild sub : this.getCity().getGuild().getNation().getSubGuildList()){
                if(sub.getOwnedCity() != null)
                    treeCount ++;
            }

            if(treeCount > 6)
                this.capSize = 20;
        }
    }

    public static boolean summonBanestone(PlayerCharacter player, ClientConnection origin, int rank) {

        Guild baningGuild;
        Zone cityZone;
        City targetCity;

        ArrayList<Bane> nationBanes;

        baningGuild = player.getGuild();

        if (baningGuild.getNation().isEmptyGuild()) {
            PlaceAssetMsg.sendPlaceAssetError(origin, 55, ""); // You must be in a Nation
            return false;
        }

        if (baningGuild.getNation().isNPCGuild()) {
            PlaceAssetMsg.sendPlaceAssetError(origin, 72, ""); // Cannot be in an NPC nation
            return false;
        }

        if (GuildStatusController.isInnerCouncil(player.getGuildStatus()) == false) {
            PlaceAssetMsg.sendPlaceAssetError(origin, 10, ""); // You must be a guild leader
            return false;
        }

        // Cannot place banestone underwater;

        if (HeightMap.isLocUnderwater(player.getLoc())) {
            PlaceAssetMsg.sendPlaceAssetError(origin, 6, ""); // Cannot place underwater
            return false;
        }

        //  figure out which city zone we are standing on.

        targetCity = ZoneManager.getCityAtLocation(player.getLoc());

        if (targetCity == null) {
            PlaceAssetMsg.sendPlaceAssetError(origin, 59, ""); // No city to siege at this location!
            return false;
        }

        if (targetCity.isSafeHold()) {
            PlaceAssetMsg.sendPlaceAssetError(origin, 15, ""); // Cannot place assets in peace zone
            return false;
        }

        if (targetCity.getRank() > rank) {
            PlaceAssetMsg.sendPlaceAssetError(origin, 60, ""); // Bane rank is too low
            return false;
        }

        cityZone = targetCity.getParent();

        // Cannot place assets on a dead tree

        if ((cityZone.isPlayerCity()) &&
                (City.getCity(cityZone.getPlayerCityUUID()).getTOL().getRank() == -1)) {
            PlaceAssetMsg.sendPlaceAssetError(origin, 1, "Cannot bane a dead tree!");
            return false;
        }

        if (baningGuild.getNation() == targetCity.getGuild().getNation()) {
            PlaceAssetMsg.sendPlaceAssetError(origin, 20, ""); //Cannot bane yourself!
            return false;
        }

        if (targetCity.getTOL() == null) {
            PlaceAssetMsg.sendPlaceAssetError(origin, 65, ""); // Cannot find tree to target
            return false;
        }

        if (targetCity.getBane() != null) {
            PlaceAssetMsg.sendPlaceAssetError(origin, 23, ""); // Tree is already baned.
            return false;
        }

        if (getBaneByAttackerGuild(baningGuild) != null) {
            PlaceAssetMsg.sendPlaceAssetError(origin, 1, "Your guild has already placed a bane!");
            return false;
        }

        nationBanes = getBanesByNation(baningGuild.getNation());

        // A nation can only have 3 concurrent banes

        if (nationBanes.size() == 3) {
            PlaceAssetMsg.sendPlaceAssetError(origin, 64, ""); // Your nation is already at war and your limit has been reached
            return false;
        }

        //audit that defending nation does no have a bane on them already
        boolean defenderBaned = false;
        Guild defenderNation = targetCity.getGuild().getNation();
        if(defenderNation.getOwnedCity() != null && defenderNation.getOwnedCity().getBane() != null){
            defenderBaned = true;
        }else{
            for (Guild sub : defenderNation.getSubGuildList()) {
                if (sub.getOwnedCity() != null) {
                    if (sub.getOwnedCity().getBane() != null) {
                        defenderBaned = true;
                    }
                }
            }
        }
        if(defenderBaned){
            ChatManager.chatSystemInfo(player, "This Nation has already been baned!");
            return false;
        }

        if (targetCity.isLocationOnCityGrid(player.getLoc()) == true) {
            PlaceAssetMsg.sendPlaceAssetError(origin, 1, "Cannot place banestone on city grid.");
            return false;
        }

        Blueprint blueprint = Blueprint.getBlueprint(24300);  // Banestone

        //Let's drop a banestone!

        Vector3fImmutable localLocation = ZoneManager.worldToLocal(player.getLoc(), cityZone);

        if (localLocation == null) {
            PlaceAssetMsg.sendPlaceAssetError(player.getClientConnection(), 1, "A Serious error has occurred. Please post details for to ensure transaction integrity");
            Logger.info("Failed to Convert World coordinates to local zone coordinates");
            return false;
        }

        Building stone = DbManager.BuildingQueries.CREATE_BUILDING(
                cityZone.getObjectUUID(), player.getObjectUUID(), blueprint.getName(), blueprint.getBlueprintUUID(),
                localLocation, 1.0f, blueprint.getMaxHealth(rank), ProtectionState.PROTECTED, 0, rank,
                null, blueprint.getBlueprintUUID(), 1, 0.0f);

        if (stone == null) {
            PlaceAssetMsg.sendPlaceAssetError(player.getClientConnection(), 1, "A Serious error has occurred. Please post details for to ensure transaction integrity");
            return false;
        }

        stone.addEffectBit((1 << 19));
        stone.setMaxHitPoints(stone.getBlueprint().getMaxHealth(stone.getRank()));
        stone.setCurrentHitPoints(stone.getMaxHitPoints());
        BuildingManager.setUpgradeDateTime(stone, null, 0);

        //Make the bane

        Bane bane = makeBane(player, targetCity, stone);

        if (bane == null) {
            //delete bane stone, failed to make bane object
            DbManager.BuildingQueries.DELETE_FROM_DATABASE(stone);
            PlaceAssetMsg.sendPlaceAssetError(player.getClientConnection(), 1, "A Serious error has occurred. Please post details for to ensure transaction integrity");
            return false;
        }

        WorldGrid.addObject(stone, player);

        //Add bane effect to TOL

        targetCity.getTOL().addEffectBit((1 << 16));
        targetCity.getTOL().updateEffects();

        Vector3fImmutable movePlayerOutsideStone = player.getLoc();
        movePlayerOutsideStone = movePlayerOutsideStone.setX(movePlayerOutsideStone.x + 10);
        movePlayerOutsideStone = movePlayerOutsideStone.setZ(movePlayerOutsideStone.z + 10);
        player.teleport(movePlayerOutsideStone);

        // Notify players

        ChatSystemMsg chatMsg = new ChatSystemMsg(null, "[Bane Channel] " + targetCity.getCityName() + " has been baned by " + baningGuild.getName() + ". Standoff phase has begun!");
        chatMsg.setMessageType(4);
        chatMsg.setChannel(Enum.ChatChannelType.SYSTEM.getChannelID());

        DispatchMessage.dispatchMsgToAll(chatMsg);

        // Push this event to the DataWarehouse

        BaneRecord baneRecord = BaneRecord.borrow(bane, Enum.RecordEventType.PENDING);
        DataWarehouse.pushToWarehouse(baneRecord);

        //add bane commander NPC
        summonBaneCommander(bane);

        //check to default cap to more than 10 if tree limit exceeded
        int treeCount = 1;
        for(Guild sub : targetCity.getGuild().getNation().getSubGuildList()){
            if(sub.getOwnedCity() != null)
                treeCount ++;
        }

        if(treeCount > 6)
            bane.capSize = 20;

        try {
            //update map for all players online
            for (PlayerCharacter playerCharacter : SessionManager.getAllActivePlayerCharacters()) {
                CityDataMsg cityDataMsg = new CityDataMsg(SessionManager.getSession(playerCharacter), false);
                cityDataMsg.updateMines(true);
                cityDataMsg.updateCities(true);
                Dispatch dispatch = Dispatch.borrow(playerCharacter, cityDataMsg);
                DispatchMessage.dispatchMsgDispatch(dispatch, Enum.DispatchChannel.SECONDARY);
            }
        }catch(Exception e){

        }

        return true;
    }

    public static void summonBaneCommander(Bane bane){
        Vector3fImmutable spawnLoc = Vector3fImmutable.getRandomPointOnCircle(bane.getStone().loc,6);
        NPC baneCommander;
        int commanderuuid = DbManager.NPCQueries.BANE_COMMANDER_EXISTS(bane.getStone().getObjectUUID());

        if(commanderuuid == 0) {
            //add bane commander NPC
            int contractID = 1502042;
            baneCommander = NPC.createNPC("Bane Commander", contractID, spawnLoc, bane.getCity().getGuild(), ZoneManager.findSmallestZone(bane.getStone().loc), (short) 70, bane.getStone());
            try {
                NPCManager.slotCharacterInBuilding(baneCommander);
            }catch(Exception e){

            }
            WorldGrid.addObject(baneCommander,spawnLoc.x,spawnLoc.z);
            WorldGrid.updateObject(baneCommander);
        }
        else
        {
            baneCommander = NPC.getNPC(commanderuuid);
        }
        //try {
        //    NPCManager.slotCharacterInBuilding(baneCommander);
        //}catch (Exception e){
            //swallow it
        //}
        baneCommander.runAfterLoad();
        //baneCommander.setLoc(spawnLoc);
        InterestManager.setObjectDirty(baneCommander);

        baneCommander.updateLocation();
    }

    public static Bane getBane(int cityUUID) {

        Bane outBane;

        // Check cache first

        outBane = banes.get(cityUUID);

        // Last resort attempt to load from database

        if (outBane == null)
            outBane = DbManager.BaneQueries.LOAD_BANE(cityUUID);
        else
            return outBane;

        // As we loaded from the db, store it in the internal cache

        if (outBane != null)
            banes.put(cityUUID, outBane);

        return outBane;
    }

    public static Bane getBaneByAttackerGuild(Guild guild) {


        if (guild == null || guild.isEmptyGuild())
            return null;
        ArrayList<Bane> baneList;

        baneList = new ArrayList<>(banes.values());

        for (Bane bane : baneList) {
            if (bane.getOwner().getGuild().equals(guild))
                return bane;
        }

        return null;
    }

    public static ArrayList<Bane> getBanesByNation(Guild guild) {

        ArrayList<Bane> baneList;
        ArrayList<Bane> returnList;

        baneList = new ArrayList<>(banes.values());
        returnList = new ArrayList<>();

        for (Bane bane : baneList) {
            if (bane.getOwner().getGuild().getNation().equals(guild))
                returnList.add(bane);
        }

        return returnList;
    }

    public static void addBane(Bane bane) {

        Bane.banes.put(bane.cityUUID, bane);

    }

    public static Bane makeBane(PlayerCharacter owner, City city, Building stone) {

        Bane newBane;

        if (DbManager.BaneQueries.CREATE_BANE(city, owner, stone) == false) {
            Logger.error("Error writing to database");
            return null;
        }

        newBane = DbManager.BaneQueries.LOAD_BANE(city.getObjectUUID());

        return newBane;
    }

    //Call this to prematurely end a bane

    public SiegePhase getSiegePhase() {

        SiegePhase phase;

        if (this.liveDate == null) {
            phase = SiegePhase.CHALLENGE; //challenge
            return phase;
        }

        if (DateTime.now().isAfter(this.liveDate)) {
            phase = SiegePhase.WAR; //war
            return phase;
        }

        // If code reaches this point we are in standoff mode.
        phase = SiegePhase.STANDOFF; //standoff

        return phase;
    }

    // Cache access

    public void setDefaultTime() {

        DateTime timeToSetDefault = new DateTime(this.placementDate);
        timeToSetDefault = timeToSetDefault.plusDays(1);

        if (DateTime.now().isAfter(timeToSetDefault)){
            if(!this.capSet){
                DbManager.BaneQueries.SET_BANE_CAP_NEW(20,this.getCityUUID());
                this.capSet = true;
            }
            if(!this.daySet){
                DbManager.BaneQueries.SET_BANE_DAY_NEW(3,this.getCityUUID());
                this.daySet = true;
            }
            if(!this.timeSet){
                DbManager.BaneQueries.SET_BANE_TIME_NEW(9,this.getCityUUID());
                this.timeSet = true;
            }
        }
    }

    //Returns a guild's bane

    /*
     * Getters
     */
    public City getCity() {
        return City.getCity(this.cityUUID);
    }

    public PlayerCharacter getOwner() {
        return (PlayerCharacter) DbManager.getObject(Enum.GameObjectType.PlayerCharacter, ownerUUID);
    }

    public boolean remove() {

        Building baneStone;

        baneStone = this.getStone();

        if (baneStone == null) {
            Logger.debug("Removing bane without a stone.");
            return false;
        }

        // Reassert protection contracts

        this.getCity().protectionEnforced = true;

        // Remove visual effects on Bane and TOL.

        this.getStone().removeAllVisualEffects();
        this.getStone().updateEffects();

        this.getCity().getTOL().removeAllVisualEffects();
        this.getCity().getTOL().updateEffects();

        // Remove bane from the database

        if (DbManager.BaneQueries.REMOVE_BANE(this) == false) {
            Logger.error("Database call failed for city UUID: " + this.getCity().getObjectUUID());
            return false;
        }

        // Remove bane from ingame cache

        Bane.banes.remove(cityUUID);

        // Delete stone from database

        if (DbManager.BuildingQueries.DELETE_FROM_DATABASE(baneStone) == false) {
            Logger.error("Database error when deleting stone object");
            return false;
        }

        //Remove bane commander NPC
        if(!baneStone.getHirelings().isEmpty()) {
            NPC npc = (NPC)baneStone.getHirelings().keySet().stream().findFirst().orElse(null);
            if(npc != null) {
                DbManager.NPCQueries.DELETE_NPC(npc);
                DbManager.removeFromCache(npc);
                WorldGrid.RemoveWorldObject(npc);
                WorldGrid.removeObject(npc);
            }
        }
        // Remove object from simulation

        baneStone.removeFromCache();
        WorldGrid.RemoveWorldObject(baneStone);
        WorldGrid.removeObject(baneStone);
        return true;
    }

    public final DateTime getPlacementDate() {
        return placementDate;
    }

    public final boolean isAccepted() {

        return (this.getSiegePhase() != SiegePhase.CHALLENGE);
    }

    public final DateTime getLiveDate() {
        return liveDate;
    }

    public void setLiveDate_NEW(DateTime baneTime) {

    }
    public void setLiveDate(DateTime baneTime) {

        if (DbManager.BaneQueries.SET_BANE_TIME(baneTime, this.getCity().getObjectUUID())) {
            this.liveDate = new DateTime(baneTime);

            // Push event to warehouse

            BaneRecord.updateLiveDate(this, baneTime);

            ChatManager.chatGuildInfo(this.getOwner().getGuild(), "The bane on " + this.getCity().getGuild().getName() + " has been set! Standoff phase has begun!");
            Guild attackerNation = this.getOwner().getGuild().getNation();

            if (attackerNation != null)

                for (Guild subGuild : attackerNation.getSubGuildList()) {

                    //Don't send the message twice.
                    if (subGuild.equals(attackerNation))
                        continue;

                    ChatManager.chatGuildInfo(subGuild, "The bane on " + this.getCity().getGuild().getName() + " has been set! Standoff phase has begun!");
                }

            ChatManager.chatGuildInfo(this.getCity().getGuild(), "The bane on " + this.getCity().getGuild().getName() + " has been set! Standoff phase has begun!");

            Guild defenderNation = this.getCity().getGuild().getNation();

            if (defenderNation != null) {

                if (defenderNation != this.getCity().getGuild())
                    ChatManager.chatGuildInfo(defenderNation, "The bane on " + this.getCity().getGuild().getName() + " has been set! Standoff phase has begun!");

                for (Guild subGuild : defenderNation.getSubGuildList()) {
                    //Don't send the message twice.
                    if (subGuild == this.getCity().getGuild())
                        continue;
                    ChatManager.chatGuildInfo(subGuild, "The bane on " + this.getCity().getGuild().getName() + " has been set! Standoff phase has begun!");
                }
            }

            if (activateBaneJob != null)
                this.activateBaneJob.cancelJob();

            ActivateBaneJob abtj = new ActivateBaneJob(cityUUID);

            JobScheduler.getInstance().scheduleJob(abtj, this.liveDate.getMillis());
            this.activateBaneJob = abtj;
        } else {
            Logger.debug("error with city " + this.getCity().getName());
            ChatManager.chatGuildInfo(this.getOwner().getGuild(), "A Serious error has occurred. Please post details for to ensure transaction integrity");
            ChatManager.chatGuildInfo(this.getCity().getGuild(), "A Serious error has occurred. Please post details for to ensure transaction integrity");
        }

    }

    public final void endBane(SiegeResult siegeResult) {

        boolean baneRemoved;

        // No matter what the outcome of a bane, we re-asset
        // protection contracts at this time.  They don't quite
        // matter if the city falls, as they are invalidated.

        this.getCity().protectionEnforced = true;

        switch (siegeResult) {
            case DEFEND:

                // Push event to warehouse

                BaneRecord.updateResolution(this, Enum.RecordEventType.DEFEND);

                baneRemoved = remove();

                if (baneRemoved) {

                    // Update seieges withstood

                    this.getCity().setSiegesWithstood(this.getCity().getSiegesWithstood() + 1);

                    // Notify players

                    ChatSystemMsg msg = new ChatSystemMsg(null, "[Bane Channel]" + this.getCity().getGuild().getName() + " has rallied against " + this.getOwner().getGuild().getName() + ". The siege on " + this.getCity().getCityName() + " has been broken!");
                    msg.setMessageType(4);
                    msg.setChannel(engine.Enum.ChatChannelType.SYSTEM.getChannelID());

                    DispatchMessage.dispatchMsgToAll(msg);

                }

                break;
            case CAPTURE:

                // Push event to warehouse

                BaneRecord.updateResolution(this, Enum.RecordEventType.CAPTURE);

                baneRemoved = this.remove();

                if (baneRemoved) {

                    ChatSystemMsg msg = new ChatSystemMsg(null, "[Bane Channel]" + this.getOwner().getGuild().getName() + " have defeated " + this.getCity().getGuild().getName() + " and captured " + this.getCity().getCityName() + '!');
                    msg.setMessageType(4);
                    msg.setChannel(engine.Enum.ChatChannelType.SYSTEM.getChannelID());

                    DispatchMessage.dispatchMsgToAll(msg);
                }
                break;
            case DESTROY:

                // Push event to warehouse

                BaneRecord.updateResolution(this, Enum.RecordEventType.DESTROY);

                baneRemoved = this.remove();

                if (baneRemoved) {

                    ChatSystemMsg msg = new ChatSystemMsg(null, "[Bane Channel]" + this.getOwner().getGuild().getName() + " have defeated " + this.getCity().getGuild().getName() + " and razed " + this.getCity().getCityName() + '!');
                    msg.setMessageType(4);
                    msg.setChannel(engine.Enum.ChatChannelType.SYSTEM.getChannelID());

                    DispatchMessage.dispatchMsgToAll(msg);
                }
                break;
        }

        Zone cityZone = this.getCity().getParent();

        if (cityZone == null)
            return;

        //UNPROTECT ALL SIEGE EQUIPMENT AFTER A BANE
        for (Building toUnprotect : cityZone.zoneBuildingSet) {
            if (toUnprotect.getBlueprint() != null && toUnprotect.getBlueprint().isSiegeEquip() && toUnprotect.assetIsProtected())
                toUnprotect.setProtectionState(ProtectionState.NONE);
        }

        if(siegeResult.equals(CAPTURE)){
            for (Building toTransfer : cityZone.zoneBuildingSet) {
                toTransfer.setOwner(this.getOwner());
                WorldGrid.updateObject(toTransfer);
            }
            this.getCity().getTOL().setHealth(this.getCity().getTOL().healthMax);
            WorldGrid.updateObject(this.getCity().getTOL());

            if(this.getCity().parentZone != null && this.getCity().parentZone.zoneMobSet != null) {
                for (Mob mob : this.getCity().parentZone.zoneMobSet) {
                    mob.setGuild(this.getOwner().guild);
                    InterestManager.setObjectDirty(mob);
                    mob.setCombatTarget(null);
                    mob.teleport(mob.bindLoc);
                }
            }
        }

        if(this.affected_players != null) {
            for (PlayerCharacter affected : this.affected_players) {
                affected.ZergMultiplier = 1.0f;
                affected.affectedBane = null;
                affected.affectedMine = null;
            }
        }

    }

    public boolean isErrant() {

        boolean isErrant = true;

        if (this.getOwner() == null)
            return isErrant;


        if (this.getOwner().getGuild().isEmptyGuild() == true)
            return isErrant;

        if (this.getOwner().getGuild().getNation().isEmptyGuild() == true)
            return isErrant;

        // Bane passes validation

        isErrant = false;

        return isErrant;
    }

    /**
     * @return the stone
     */
    public Building getStone() {
        return BuildingManager.getBuilding(this.stoneUUID);
    }

    /**
     * @return the cityUUID
     */
    public int getCityUUID() {
        return cityUUID;
    }

    public void applyZergBuffs(){
        City city = this.getCity();
        if(city == null)
            return;

        this.onEnter();
    }

    public void onEnter() {

        Building tower = this.getCity().getTOL();

        // Gather current list of players within the zone bounds

        HashSet<AbstractWorldObject> currentPlayers = WorldGrid.getObjectsInRangePartial(tower.loc, Enum.CityBoundsType.SIEGEBOUNDS.extents, MBServerStatics.MASK_PLAYER);
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
                ChatManager.chatSystemInfo(player,"You Have Entered an Active Bane Area");
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

        for(Integer id : this.mineAttendees.keySet()){
            PlayerCharacter attendee = PlayerCharacter.getPlayerCharacter(id);
            if(attendee == null)
                continue;

            if(charactersByNation.containsKey(attendee.guild.getNation())){
                if(!charactersByNation.get(attendee.guild.getNation()).contains(attendee)){
                    charactersByNation.get(attendee.guild.getNation()).add(attendee);
                }
            }

        }
        Guild attackingNation = this.getOwner().getGuild().getNation();
        Guild defendingNation = this.getCity().getGuild().getNation();
        for(Guild nation : updatedNations){
            float multiplier = ZergManager.getCurrentMultiplier(charactersByNation.get(nation).size(),this.capSize);
            for(PlayerCharacter player : charactersByNation.get(nation)){
                player.ZergMultiplier = multiplier;
                player.affectedBane = this;
                player.affectedMine = null;
                if(this.capSize != 9999){
                    if(!player.guild.getNation().equals(attackingNation) && !player.guild.getNation().equals(defendingNation) && !player.getAccount().status.equals(Enum.AccountStatus.ADMIN)){
                        Building sdrTol = BuildingManager.getBuildingFromCache(27977);
                        MovementManager.translocate(player,Vector3fImmutable.getRandomPointOnCircle(sdrTol.loc,32f),null);
                        ChatManager.chatSystemInfo(player, "You Were Removed From an Active Bane Area");
                    }
                }
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

        Building tower = this.getCity().getTOL();
        if(tower == null)
            return;
        ArrayList<Integer>toRemove = new ArrayList<>();
        HashSet<AbstractWorldObject> currentPlayers = WorldGrid.getObjectsInRangePartial(tower.loc, Enum.CityBoundsType.SIEGEBOUNDS.extents, MBServerStatics.MASK_PLAYER);
        for(Integer id : currentMemory){
            PlayerCharacter pc = PlayerCharacter.getPlayerCharacter(id);
            if(!currentPlayers.contains(pc)){
                if(this.mineAttendees.containsKey(id)){
                    long timeGone = System.currentTimeMillis() - this.mineAttendees.get(id).longValue();
                    if (timeGone > 180000L) { // 3 minutes
                        toRemove.add(id); // Mark for removal
                    }
                }
                pc.ZergMultiplier = 1.0f;
                pc.affectedBane = null;
                pc.affectedMine = null;
            } else {
                this.mineAttendees.put(id,System.currentTimeMillis());
            }
        }

        // Remove players from city memory

        _playerMemory.removeAll(toRemove);
        for(int id : toRemove){
            this.mineAttendees.remove(id);
        }
    }

}
