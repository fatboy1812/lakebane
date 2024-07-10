package engine.gameManager;

import engine.Enum;
import engine.InterestManagement.InterestManager;
import engine.InterestManagement.WorldGrid;
import engine.math.Vector3f;
import engine.math.Vector3fImmutable;
import engine.objects.*;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class StrongholdManager {

    public static void processStrongholds() {
        ArrayList<Mine> mines = Mine.getMines();


        //process strongholds selecting 2 randomly to become active
        int count = 0;
        while (count < 2) {
            int random = ThreadLocalRandom.current().nextInt(1, mines.size()) - 1;
            Mine mine = mines.get(random);
            if (mine != null) {
                if (!mine.isActive && !mine.isStronghold) {
                    StartStronghold(mine);
                    count++;
                }
            }
        }
    }

    public static void StartStronghold(Mine mine){

        //remove buildings
        Building tower = BuildingManager.getBuilding(mine.getBuildingID());
        if(tower == null)
            return;

        mine.isStronghold = true;
        mine.strongholdMobs = new ArrayList<>();
        mine.oldBuildings = new HashMap<>();

        Zone mineZone = ZoneManager.findSmallestZone(tower.loc);
        for(Building building : mineZone.zoneBuildingSet){
            mine.oldBuildings.put(building.getObjectUUID(),building.meshUUID);
            building.setMeshUUID(407650);
            building.setMeshScale(new Vector3f(0,0,0));
            InterestManager.setObjectDirty(building);
            WorldGrid.updateObject(building);
        }

        //update tower to become stronghold mesh
        tower.setMeshUUID(getStrongholdMeshID(mine.getParentZone()));
        tower.setMeshScale(new Vector3f(1,1,1));
        InterestManager.setObjectDirty(tower);
        WorldGrid.updateObject(tower);

        //create elite mobs
        for(int i = 0; i < mine.capSize * 2; i++){
            Vector3fImmutable loc = Vector3fImmutable.getRandomPointOnCircle(tower.loc,30);
            MobBase guardBase = MobBase.getMobBase(getStrongholdGuardianID(tower.meshUUID));
            Mob guard = Mob.createMob(guardBase.getLoadID(), loc, Guild.getErrantGuild(),true,mineZone,null,0, guardBase.getFirstName(),65);
            if(guard != null){
                guard.parentZone = mine.getParentZone();
                guard.bindLoc = loc;
                guard.setLoc(loc);
                guard.equipmentSetID = getStrongholdMobEquipSetID(guard.getMobBaseID());
                guard.runAfterLoad();
                guard.setLevel((short)65);
                guard.setResists(new Resists("Elite"));
                guard.spawnTime = 1000000000;
                guard.BehaviourType = Enum.MobBehaviourType.Aggro;
                mine.strongholdMobs.add(guard);
                LootManager.GenerateStrongholdLoot(guard,false,false);
                guard.healthMax  = 12500;
                guard.setHealth(guard.healthMax);
                guard.maxDamageHandOne = 1550;
                guard.minDamageHandOne = 750;
                guard.atrHandOne = 1800;
                guard.defenseRating = 2200;
                guard.setFirstName("Elite Guardian");
                InterestManager.setObjectDirty(guard);
                guard.StrongholdGuardian = true;
                guard.stronghold = mine;
            }
        }
        //create stronghold commander
        Vector3fImmutable loc = tower.loc;
        MobBase commanderBase = MobBase.getMobBase(getStrongholdCommanderID(tower.meshUUID));
        Mob commander = Mob.createMob(commanderBase.getLoadID(), loc,Guild.getErrantGuild(),true,mineZone,null,0, commanderBase.getFirstName(),75);
        if(commander != null){
            commander.parentZone = mine.getParentZone();
            commander.bindLoc = loc;
            commander.setLoc(loc);
            commander.equipmentSetID = getStrongholdMobEquipSetID(commander.getMobBaseID());
            commander.runAfterLoad();
            commander.setLevel((short)75);
            commander.setResists(new Resists("Elite"));
            commander.spawnTime = 1000000000;
            commander.BehaviourType = Enum.MobBehaviourType.Aggro;
            commander.mobPowers.clear();
            commander.mobPowers.put(563107033,40); //grounding shot
            commander.mobPowers.put(429032838,40); // gravechill
            commander.mobPowers.put(429413547,40); // grasp of thurin
            commander.StrongholdCommander = true;
            mine.strongholdMobs.add(commander);
            LootManager.GenerateStrongholdLoot(commander,true, false);
            commander.healthMax = 50000;
            commander.setHealth(commander.healthMax);
            commander.maxDamageHandOne = 3500;
            commander.minDamageHandOne = 1500;
            commander.atrHandOne = 3500;
            commander.defenseRating = 3500;
            commander.setFirstName("Guardian Commander");
            InterestManager.setObjectDirty(commander);
            commander.stronghold = mine;
        }

        mine.setActive(true);
        tower.setProtectionState(Enum.ProtectionState.PROTECTED);
        tower.getBounds().setRegions(tower);
        InterestManager.setObjectDirty(tower);
        WorldGrid.updateObject(tower);
        ChatManager.chatSystemChannel(mine.getZoneName() + "'s Stronghold Has Begun!");
        Logger.info(mine.getZoneName() + "'s Stronghold Has Begun!");
    }

    public static void EndStronghold(Mine mine){

        //restore the buildings
        Building tower = BuildingManager.getBuilding(mine.getBuildingID());
        if(tower == null)
            return;

        mine.isStronghold = false;

        //get rid of the mobs
        for(Mob mob : mine.strongholdMobs) {
            mob.despawn();
            mob.removeFromCache();
            DbManager.MobQueries.DELETE_MOB(mob);
        }

        //restore the buildings
        Zone mineZone = ZoneManager.findSmallestZone(tower.loc);
        for(Building building : mineZone.zoneBuildingSet){
            if(mine.oldBuildings.containsKey(building.getObjectUUID())) {
                building.setMeshUUID(mine.oldBuildings.get(building.getObjectUUID()));
                building.setMeshScale(new Vector3f(1, 1, 1));
                InterestManager.setObjectDirty(building);
                WorldGrid.updateObject(building);
            }
        }

        //update tower to become Mine Tower again
        tower.setMeshUUID(1500100);

        mine.setActive(false);
        tower.setProtectionState(Enum.ProtectionState.NPC);
        tower.getBounds().setRegions(tower);
        InterestManager.setObjectDirty(tower);
        WorldGrid.updateObject(tower);
        ChatManager.chatSystemChannel(mine.getZoneName() + "'s Stronghold Has Concluded!");
        Logger.info(mine.getZoneName() + "'s Stronghold Has Concluded!");
    }

    public static int getStrongholdMeshID(Zone parent){
        while(!parent.isMacroZone()){
            parent = parent.getParent();
            if(parent.getName().equalsIgnoreCase("seafloor")){
                return 0;
            }
        }
        switch(parent.getObjectUUID()){
            case 197:
            case 234:
            case 178:
            case 122:
                return 814000; //Frost Giant Hall (ICE)
            case 968:
            case 951:
            case 313:
            case 331:
                return 5001500; // Lich Queens Keep (UNDEAD)
            case 785:
            case 761:
            case 717:
            case 737:
                return 1306600; // Temple of the Dragon (DESERT)
            case 353:
            case 371:
            case 388:
            case 532:
                return 564600; // Undead Lord's Keep (SWAMP)
        }
        return 456100; // small stockade
    }

    public static int getStrongholdGuardianID(int ID){
        switch(ID){
            case 814000:
                return 13528; // Mountain Giant Raider Axe
            case 5001500:
                return 13643; // Vampire Spear Warrior
            case 1306600:
                return 13802; // Desert Orc Warrior
            case 564600:
                return 12728; // Kolthoss Warrior
        }
        return 13434; // human sword and board warrior
    }

    public static int getStrongholdCommanderID(int ID){
        switch(ID){
            case 814000:
                return 13515;
            case 5001500:
                return 14280;
            case 1306600:
                return 13789; // Desert Orc Xbow
            case 564600:
                return 12724; // xbow kolthoss
        }
        return 13433;
    }

    public static int getStrongholdMobEquipSetID(int mobbaseUUID){
        switch(mobbaseUUID){
            case 14280:
                return 10790;
            case 13643:
                return 6317;
            case 13515:
                return 7820;
            case 13528:
                return 5966;
            case 13802:
                return 9043;
            case 13789:
                return 9035;
            case 12728:
                return 6826;
            case 12724:
                return 9471;
            case 13434:
                return 6327;
            case 13433:
                return 6900;
        }
        return 0;
    }

    public static void CheckToEndStronghold(Mine mine) {
        if (!mine.isStronghold)
            return;

        boolean stillAlive = false;
        for (Mob mob : mine.strongholdMobs)
            if (mob.isAlive())
                stillAlive = true;

        if (!stillAlive) {
            // Epic encounter

            Building tower = BuildingManager.getBuilding(mine.getBuildingID());
            if (tower == null)
                return;

            Zone mineZone = ZoneManager.findSmallestZone(tower.loc);

            Vector3fImmutable loc = tower.loc;
            MobBase commanderBase = MobBase.getMobBase(getStrongholdCommanderID(tower.meshUUID));
            Mob commander = Mob.createMob(commanderBase.getLoadID(), loc, Guild.getErrantGuild(), true, mineZone, null, 0, commanderBase.getFirstName(), 75);
            if (commander != null) {
                commander.parentZone = mine.getParentZone();
                commander.bindLoc = loc;
                commander.setLoc(loc);
                commander.equipmentSetID = getStrongholdMobEquipSetID(commander.getMobBaseID());
                commander.runAfterLoad();
                commander.setLevel((short) 75);
                commander.setResists(new Resists("Elite"));
                commander.spawnTime = 1000000000;
                commander.BehaviourType = Enum.MobBehaviourType.Aggro;
                commander.mobPowers.clear();
                commander.mobPowers.put(563107033, 40); //grounding shot
                commander.mobPowers.put(429032838, 40); // gravechill
                commander.mobPowers.put(429413547, 40); // grasp of thurin
                mine.strongholdMobs.add(commander);
                LootManager.GenerateStrongholdLoot(commander, true, false);
                commander.healthMax = 250000;
                commander.setHealth(commander.healthMax);
                commander.maxDamageHandOne = 5000;
                commander.minDamageHandOne = 2500;
                commander.atrHandOne = 5000;
                commander.defenseRating = 3500;
                commander.setFirstName("Epic Commander");
                InterestManager.setObjectDirty(commander);
                commander.stronghold = mine;
                commander.StrongholdEpic = true;
            }
        }
    }
}
