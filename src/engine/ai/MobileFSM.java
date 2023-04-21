// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com
package engine.ai;
import engine.Enum;
import engine.Enum.DispatchChannel;
import engine.InterestManagement.WorldGrid;
import engine.ai.utilities.CombatUtilities;
import engine.ai.utilities.MovementUtilities;
import engine.gameManager.*;
import engine.math.Vector3f;
import engine.math.Vector3fImmutable;
import engine.net.DispatchMessage;
import engine.net.client.msg.PerformActionMsg;
import engine.net.client.msg.PowerProjectileMsg;
import engine.net.client.msg.UpdateStateMsg;
import engine.objects.*;
import engine.powers.ActionsBase;
import engine.powers.PowersBase;
import engine.server.MBServerStatics;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import static engine.math.FastMath.sqr;
public class MobileFSM {
    private static void AttackTarget(Mob mob, AbstractWorldObject target) {
        if(mob == null){
            return;
        }
        if(target == null){
            mob.setCombatTarget(null);
            return;
        }
        switch (target.getObjectType()) {
            case PlayerCharacter:
                PlayerCharacter targetPlayer = (PlayerCharacter) target;
                if (canCast(mob) == true) {
                    if (MobCast(mob) == false) {
                        AttackPlayer(mob, targetPlayer);
                    }
                } else {
                    AttackPlayer(mob, targetPlayer);
                }
                break;
            case Building:
                Building targetBuilding = (Building) target;
                AttackBuilding(mob, targetBuilding);
                break;
            case Mob:
                Mob targetMob = (Mob) target;
                AttackMob(mob,targetMob);
                break;
        }
    }
    public static void AttackPlayer(Mob mob, PlayerCharacter target){
        if (mob.getMobBase().getSeeInvis() < target.getHidden() || !target.isAlive()) {
            mob.setCombatTarget(null);
            return;
        }
        if (mob.BehaviourType.callsForHelp) {
            MobCallForHelp(mob);
        }
        if (!MovementUtilities.inRangeDropAggro(mob, target)) {
            mob.setAggroTargetID(0);
            mob.setCombatTarget(null);
            return;
        }
        if (CombatUtilities.inRange2D(mob, target, mob.getRange())) {
            //no weapons, default mob attack speed 3 seconds.
            if (System.currentTimeMillis() < mob.getLastAttackTime())
                return;
            // ranged mobs cant attack while running. skip until they finally stop.
            if (mob.isMoving())
                return;
            // add timer for last attack.
            ItemBase mainHand = mob.getWeaponItemBase(true);
            ItemBase offHand = mob.getWeaponItemBase(false);
            if (mainHand == null && offHand == null) {
                CombatUtilities.combatCycle(mob, target, true, null);
                int delay = 3000;
                if (mob.isSiege())
                    delay = 11000;
                mob.setLastAttackTime(System.currentTimeMillis() + delay);
            } else if (mob.getWeaponItemBase(true) != null) {
                int delay = 3000;
                if (mob.isSiege())
                    delay = 11000;
                CombatUtilities.combatCycle(mob, target, true, mob.getWeaponItemBase(true));
                mob.setLastAttackTime(System.currentTimeMillis() + delay);
            } else if (mob.getWeaponItemBase(false) != null) {
                int attackDelay = 3000;
                if (mob.isSiege())
                    attackDelay = 11000;
                CombatUtilities.combatCycle(mob, target, false, mob.getWeaponItemBase(false));
                mob.setLastAttackTime(System.currentTimeMillis() + attackDelay);
            }
            return;
        }
    }
    public static void AttackBuilding(Mob mob, Building target){
        if (target.getRank() == -1 || !target.isVulnerable() || BuildingManager.getBuildingFromCache(target.getObjectUUID()) == null) {
            mob.setCombatTarget(null);
            return;
        }
        City playercity = ZoneManager.getCityAtLocation(mob.getLoc());
        if(playercity != null) {
            for (Building barracks : playercity.cityBarracks) {
                for(AbstractCharacter guardCaptain : barracks.getHirelings().keySet()){
                    if(guardCaptain.getCombatTarget() == null){
                        guardCaptain.setCombatTarget(mob);
                    }
                }
            }
        }
        if (mob.isSiege())
            MovementManager.sendRWSSMsg(mob);
        ItemBase mainHand = mob.getWeaponItemBase(true);
        ItemBase offHand = mob.getWeaponItemBase(false);
        if (mainHand == null && offHand == null) {
            CombatUtilities.combatCycle(mob, target, true, null);
            int delay = 3000;
            if (mob.isSiege())
                delay = 15000;
            mob.setLastAttackTime(System.currentTimeMillis() + delay);
        } else
        if (mob.getWeaponItemBase(true) != null) {
            int attackDelay = 3000;
            if (mob.isSiege())
                attackDelay = 15000;
            CombatUtilities.combatCycle(mob, target, true, mob.getWeaponItemBase(true));
            mob.setLastAttackTime(System.currentTimeMillis() + attackDelay);
        } else if (mob.getWeaponItemBase(false) != null) {
            int attackDelay = 3000;
            if (mob.isSiege())
                attackDelay = 15000;
            CombatUtilities.combatCycle(mob, target, false, mob.getWeaponItemBase(false));
            mob.setLastAttackTime(System.currentTimeMillis() + attackDelay);
        }
        if (mob.isSiege()) {
            PowerProjectileMsg ppm = new PowerProjectileMsg(mob, target);
            ppm.setRange(50);
            DispatchMessage.dispatchMsgToInterestArea(mob, ppm, DispatchChannel.SECONDARY, MBServerStatics.CHARACTER_LOAD_RANGE, false, false);
        }
    }
    public static void AttackMob(Mob mob, Mob target){
        if (mob.getRange() >= 30 && mob.isMoving())
            return;
        //no weapons, default mob attack speed 3 seconds.
        ItemBase mainHand = mob.getWeaponItemBase(true);
        ItemBase offHand = mob.getWeaponItemBase(false);
        if (mainHand == null && offHand == null) {
            CombatUtilities.combatCycle(mob, target, true, null);
            int delay = 3000;
            if (mob.isSiege())
                delay = 11000;
            mob.setLastAttackTime(System.currentTimeMillis() + delay);
        } else
        if (mob.getWeaponItemBase(true) != null) {
            int attackDelay = 3000;
            if (mob.isSiege())
                attackDelay = 11000;
            CombatUtilities.combatCycle(mob, target, true, mob.getWeaponItemBase(true));
            mob.setLastAttackTime(System.currentTimeMillis() + attackDelay);
        } else if (mob.getWeaponItemBase(false) != null) {
            int attackDelay = 3000;
            if (mob.isSiege())
                attackDelay = 11000;
            CombatUtilities.combatCycle(mob, target, false, mob.getWeaponItemBase(false));
            mob.setLastAttackTime(System.currentTimeMillis() + attackDelay);
        }
        return;
    }
    private static void Patrol(Mob mob) {
        //make sure mob is out of combat stance
        if (mob.isCombat() && mob.getCombatTarget() == null) {
            mob.setCombat(false);
            UpdateStateMsg rwss = new UpdateStateMsg();
            rwss.setPlayer(mob);
            DispatchMessage.sendToAllInRange(mob, rwss);
        }
        if (mob.isMoving() == true) {
            //early exit for a mob who is already moving to a patrol point
            //while mob moving, update lastPatrolTime so that when they stop moving the 10 second timer can begin
            mob.stopPatrolTime = System.currentTimeMillis();
            return;
        }
        //wait between 10 and 15 seconds after reaching patrol point before moving
        int patrolDelay = ThreadLocalRandom.current().nextInt(10000) + 5000;
        if (mob.stopPatrolTime + patrolDelay > System.currentTimeMillis()) {
            //early exit while waiting to patrol again
            return;
        }
        //guard captains inherit barracks patrol points dynamically
        if (mob.contract != null && NPC.ISGuardCaptain(mob.contract.getContractID())) {
            Building barracks = mob.building;
            if (barracks != null && barracks.patrolPoints != null && barracks.getPatrolPoints().isEmpty() == false) {
                mob.patrolPoints = barracks.patrolPoints;
            }
        }
        if (MovementUtilities.canMove(mob)) {
            //get the next index of the patrol point from the patrolPoints list
            if (mob.lastPatrolPointIndex > mob.patrolPoints.size() - 1) {
                mob.lastPatrolPointIndex = 0;
            }
            mob.destination = mob.patrolPoints.get(mob.lastPatrolPointIndex);
            MovementUtilities.aiMove(mob,mob.destination,true);
            mob.lastPatrolPointIndex += 1;
            if(mob.isPlayerGuard()){
                for(Entry<Mob,Integer> minion : mob.siegeMinionMap.entrySet()){
                    //make sure mob is out of combat stance
                    if (minion.getKey().isCombat() && minion.getKey().getCombatTarget() == null) {
                        minion.getKey().setCombat(false);
                        UpdateStateMsg rwss = new UpdateStateMsg();
                        rwss.setPlayer(minion.getKey());
                        DispatchMessage.sendToAllInRange(minion.getKey(), rwss);
                    }
                    if(MovementUtilities.canMove(minion.getKey())) {
                        Vector3f minionOffset = Formation.getOffset(2, minion.getValue() + 3);
                        minion.getKey().updateLocation();
                        Vector3fImmutable formationPatrolPoint = new Vector3fImmutable(mob.destination.x + minionOffset.x, mob.destination.y,mob.destination.z + minionOffset.z);
                        //MovementUtilities.moveToLocation(minion.getKey(), formationPatrolPoint, 0);
                        MovementUtilities.aiMove(minion.getKey(),formationPatrolPoint,true);
                    }
                }
            }
        }
    }
    public static boolean canCast(Mob mob) {

        // Performs validation to determine if a
        // mobile in the proper state to cast.

        if (mob == null)
            return false;

        if (mob.mobPowers.isEmpty())
            return false;

        if (mob.nextCastTime == 0)
            mob.nextCastTime = System.currentTimeMillis();

        return mob.nextCastTime <= System.currentTimeMillis();
    }
    public static boolean MobCast(Mob mob) {

        // Method picks a random spell from a mobile's list of powers
        // and casts it on the current target (or itself).  Validation
        // (including empty lists) is done previously within canCast();

        ArrayList<Integer> powerTokens;
        ArrayList<Integer> purgeTokens;
        PlayerCharacter target = (PlayerCharacter) mob.getCombatTarget();

        if (mob.BehaviourType.callsForHelp)
            MobCallForHelp(mob);

        // Generate a list of tokens from the mob powers for this mobile.

        powerTokens = new ArrayList<>(mob.mobPowers.keySet());
        purgeTokens = new ArrayList<>();

        // If player has this effect on them currently then remove
        // this token from our list.

        for (int powerToken : powerTokens) {

            PowersBase powerBase = PowersManager.getPowerByToken(powerToken);

            for (ActionsBase actionBase : powerBase.getActions()) {

                String stackType = actionBase.stackType;

                if (target.getEffects() != null && target.getEffects().containsKey(stackType))
                    purgeTokens.add(powerToken);
            }
        }

        powerTokens.removeAll(purgeTokens);

        // Sanity check

        if (powerTokens.isEmpty())
            return false;

        // Pick random spell from our list of powers

        int powerToken = powerTokens.get(ThreadLocalRandom.current().nextInt(powerTokens.size()));
        int powerRank = mob.mobPowers.get(powerToken);
        PowersBase mobPower = PowersManager.getPowerByToken(powerToken);

        // Cast the spell

        if (CombatUtilities.inRange2D(mob, mob.getCombatTarget(), mobPower.getRange())) {

            PowersManager.useMobPower(mob, (AbstractCharacter) mob.getCombatTarget(), mobPower, powerRank);
            PerformActionMsg msg;

            if (mobPower.isHarmful() == false || mobPower.targetSelf == true)
                msg = PowersManager.createPowerMsg(mobPower, powerRank, mob, mob);
            else
                msg = PowersManager.createPowerMsg(mobPower, powerRank, mob, target);

            msg.setUnknown04(2);
            PowersManager.finishUseMobPower(msg, mob, 0, 0);

            // Default minimum seconds between cast = 10

            long coolDown = mobPower.getCooldown();

            if (coolDown < 10000)
                mob.nextCastTime = System.currentTimeMillis() + 10000 + coolDown;
            else
                mob.nextCastTime = System.currentTimeMillis() + coolDown;

            return true;
        }

        return false;
    }
    public static void MobCallForHelp(Mob mob) {
        boolean callGotResponse = false;
        if (mob.nextCallForHelp == 0) {
            mob.nextCallForHelp = System.currentTimeMillis();
        }
        if (mob.nextCallForHelp < System.currentTimeMillis()) {
            return;
        }
        //mob sends call for help message
        ChatManager.chatSayInfo(null, mob.getName() + " calls for help!");
        Zone mobCamp = mob.getParentZone();
        for (Mob helper : mobCamp.zoneMobSet) {
            if (helper.BehaviourType.respondsToCallForHelp && helper.BehaviourType.BehaviourHelperType.equals(mob.BehaviourType)) {
                helper.setCombatTarget(mob.getCombatTarget());
                callGotResponse = true;
            }
        }
        if (callGotResponse) {
            //wait 60 seconds to call for help again
            mob.nextCallForHelp = System.currentTimeMillis() + 60000;
        }
    }
    public static void run(Mob mob) {
        if (mob == null) {
            return;
        }
        if (mob.isAlive() == false) {
            //no need to continue if mob is dead, check for respawn and move on
            CheckForRespawn(mob);
            return;
        }
        if (mob.playerAgroMap.isEmpty()) {
            //no players loaded, no need to proceed
            return;
        }
        CheckToSendMobHome(mob);
        switch(mob.BehaviourType){
            case GuardCaptain:
                GuardCaptainLogic(mob);
                break;
            case GuardMinion:
                GuardMinionLogic(mob);
                break;
            case GuardWallArcher:
                GuardWallArcherLogic(mob);
                break;
            case Pet1:
                PetLogic(mob);
                break;
            case HamletGuard:
                HamletGuardLogic(mob);
                break;
            default:
                DefaultLogic(mob);
                break;
        }
    }
    private static void CheckForAggro(Mob aiAgent) {
        //looks for and sets mobs combatTarget
        if (!aiAgent.isAlive()) {
            return;
        }
        ConcurrentHashMap<Integer, Boolean> loadedPlayers = aiAgent.playerAgroMap;
        for (Entry playerEntry : loadedPlayers.entrySet()) {
            int playerID = (int) playerEntry.getKey();
            PlayerCharacter loadedPlayer = PlayerCharacter.getFromCache(playerID);
            //Player is null, let's remove them from the list.
            if (loadedPlayer == null) {
                loadedPlayers.remove(playerID);
                continue;
            }
            //Player is Dead, Mob no longer needs to attempt to aggro. Remove them from aggro map.
            if (!loadedPlayer.isAlive()) {
                loadedPlayers.remove(playerID);
                continue;
            }
            //Can't see target, skip aggro.
            if (!aiAgent.canSee(loadedPlayer)) {
                continue;
            }
            // No aggro for this race type
            if (aiAgent.notEnemy.contains(loadedPlayer.getRace().getRaceType()))
                continue;
            if (MovementUtilities.inRangeToAggro(aiAgent, loadedPlayer)) {
                aiAgent.setAggroTargetID(playerID);
                aiAgent.setCombatTarget(loadedPlayer);
                return;
            }
        }
    }
    private static void CheckMobMovement(Mob mob) {
        if(MovementUtilities.canMove(mob) == false){
            return;
        }
        mob.updateLocation();
        if (mob.isPet() == false && mob.isSummonedPet() == false && mob.isNecroPet() == false) {
            if (mob.getCombatTarget() == null) {
                Patrol(mob);
            } else {
                chaseTarget(mob);
            }
        } else {
            //pet logic
            if (mob.playerAgroMap.containsKey(mob.getOwner().getObjectUUID()) == false) {
                //mob no longer has its owner loaded, translocate pet to owner
                MovementManager.translocate(mob, mob.getOwner().getLoc(), null);
            }
            if (mob.getCombatTarget() == null) {
                //move back to owner
                if(CombatUtilities.inRange2D(mob,mob.getOwner(),11)) {
                    return;
                }
                    mob.destination = mob.getOwner().getLoc();
                    MovementUtilities.moveToLocation(mob, mob.destination, 10);
            } else{
                chaseTarget(mob);
            }
        }
    }
    private static void CheckForRespawn(Mob aiAgent) {
        if (aiAgent.deathTime == 0) {
            aiAgent.setDeathTime(System.currentTimeMillis());
        }
        //handles checking for respawn of dead mobs even when no players have mob loaded
        //Despawn Timer with Loot currently in inventory.
        if (aiAgent.getCharItemManager().getInventoryCount() > 0) {
            if (System.currentTimeMillis() > aiAgent.deathTime + MBServerStatics.DESPAWN_TIMER_WITH_LOOT) {
                aiAgent.despawn();
            }
            //No items in inventory.
        } else {
            //Mob's Loot has been looted.
            if (aiAgent.isHasLoot()) {
                if (System.currentTimeMillis() > aiAgent.deathTime + MBServerStatics.DESPAWN_TIMER_ONCE_LOOTED) {
                    aiAgent.despawn();
                }
                //Mob never had Loot.
            } else {
                if (System.currentTimeMillis() > aiAgent.deathTime + MBServerStatics.DESPAWN_TIMER) {
                    aiAgent.despawn();
                    //update time of death after mob despawns so respawn time happens after mob despawns.
                }
            }
        }
        if (System.currentTimeMillis() > aiAgent.deathTime + (aiAgent.spawnTime * 1000)) {
            aiAgent.respawn();
        }
    }
    public static void CheckForAttack(Mob mob) {
        //checks if mob can attack based on attack timer and range
        if (mob.isAlive() == false){
            return;
        }
        if (!mob.isCombat()) {
            mob.setCombat(true);
            UpdateStateMsg rwss = new UpdateStateMsg();
            rwss.setPlayer(mob);
            DispatchMessage.sendToAllInRange(mob, rwss);
        }
        if(System.currentTimeMillis() > mob.getLastAttackTime()) {
            AttackTarget(mob, mob.getCombatTarget());
        }
    }
    private static void CheckToSendMobHome(Mob mob) {


        if(mob.isPlayerGuard()){
            City current = ZoneManager.getCityAtLocation(mob.getLoc());
            if(current == null || current.equals(mob.getGuild().getOwnedCity()) == false) {
                PowersBase recall = PowersManager.getPowerByToken(-1994153779);
                PowersManager.useMobPower(mob, mob, recall, 40);
                mob.setAggroTargetID(0);
                mob.setCombatTarget(null);
            }
        }
        if (mob.getLoc().distanceSquared2D(mob.getBindLoc()) > sqr(2000)) {
            PowersBase recall = PowersManager.getPowerByToken(-1994153779);
            PowersManager.useMobPower(mob, mob, recall, 40);
            mob.setAggroTargetID(0);
            mob.setCombatTarget(null);
        }
    }
    public static void dead(Mob aiAgent) {
        //Despawn Timer with Loot currently in inventory.
        if (aiAgent.getCharItemManager().getInventoryCount() > 0) {
            if (System.currentTimeMillis() > aiAgent.deathTime + MBServerStatics.DESPAWN_TIMER_WITH_LOOT) {
                aiAgent.despawn();
                //update time of death after mob despawns so respawn time happens after mob despawns.
                aiAgent.setDeathTime(System.currentTimeMillis());
                //aiAgent.state = STATE.Respawn;
            }

            //No items in inventory.
        } else {
            //Mob's Loot has been looted.
            if (aiAgent.isHasLoot()) {
                if (System.currentTimeMillis() > aiAgent.deathTime + MBServerStatics.DESPAWN_TIMER_ONCE_LOOTED) {
                    aiAgent.despawn();
                    //update time of death after mob despawns so respawn time happens after mob despawns.
                    aiAgent.setDeathTime(System.currentTimeMillis());
                    //aiAgent.state = STATE.Respawn;
                }
                //Mob never had Loot.
            } else {
                if (System.currentTimeMillis() > aiAgent.deathTime + MBServerStatics.DESPAWN_TIMER) {
                    aiAgent.despawn();
                    //update time of death after mob despawns so respawn time happens after mob despawns.
                    aiAgent.setDeathTime(System.currentTimeMillis());
                    //aiAgent.state = STATE.Respawn;
                }
            }
        }
    }
    private static void chaseTarget(Mob mob) {
        mob.updateMovementState();
        if (CombatUtilities.inRange2D(mob, mob.getCombatTarget(), mob.getRange()) == false) {
            if (mob.getRange() > 15) {
                mob.destination = mob.getCombatTarget().getLoc();
                MovementUtilities.moveToLocation(mob, mob.destination, 0);
            } else {
                mob.destination = MovementUtilities.GetDestinationToCharacter(mob, (AbstractCharacter) mob.getCombatTarget());
                MovementUtilities.moveToLocation(mob, mob.destination, mob.getRange());
            }
        }
    }
    private static void SafeGuardAggro(Mob mob) {
        HashSet<AbstractWorldObject> awoList = WorldGrid.getObjectsInRangePartial(mob, 100, MBServerStatics.MASK_MOB);
        for (AbstractWorldObject awoMob : awoList) {
            //dont scan self.
            if (mob.equals(awoMob))
                continue;

            Mob aggroMob = (Mob) awoMob;
            //dont attack other guards
            if (aggroMob.isGuard())
                continue;
            if (mob.getLoc().distanceSquared2D(aggroMob.getLoc()) > sqr(50))
                continue;
            mob.setCombatTarget(aggroMob);
        }
    }
    public static void GuardCaptainLogic(Mob mob){
        if(mob.getCombatTarget() == null) {
            CheckForPlayerGuardAggro(mob);
        }
        CheckMobMovement(mob);
        if(mob.getCombatTarget() != null) {
            CheckForAttack(mob);
        }

    }
    public static void GuardMinionLogic(Mob mob){
        if(mob.despawned){
            if(System.currentTimeMillis() > mob.deathTime + (mob.spawnTime * 1000)){
                if(mob.getEquipmentSetID() != ((Mob)mob.npcOwner).getEquipmentSetID()){
                    mob.equipmentSetID = ((Mob)mob.npcOwner).getEquipmentSetID();
                    mob.runAfterLoad();
                }
                mob.respawn();
            }
            return;
        }
        if(mob.npcOwner.isAlive() == false && mob.getCombatTarget() == null){
            CheckForPlayerGuardAggro(mob);
            return;
        }
        CheckMobMovement(mob);
        if(mob.getCombatTarget() != null){
            CheckForAttack(mob);
        }
    }
    public static void GuardWallArcherLogic(Mob mob){
        if(mob.getCombatTarget() == null){
            CheckForPlayerGuardAggro(mob);
            return;
        }
        if(mob.getCombatTarget() != null){
            CheckForAttack(mob);
        }
    }
    private static void PetLogic(Mob mob){
        if(mob.getCombatTarget() != null && mob.getCombatTarget().isAlive() == false){
            mob.setCombatTarget(null);
        }
        if(MovementUtilities.canMove(mob)){
            CheckMobMovement(mob);
        }
        if(mob.getCombatTarget() != null) {
            CheckForAttack(mob);
        }
    }
    private static void HamletGuardLogic(Mob mob){
        if (mob.getCombatTarget() == null) {
            //safehold guard
            SafeGuardAggro(mob);
            if(mob.getCombatTarget() != null){
                CheckForAttack(mob);
            }
        }
    }
    private static void DefaultLogic(Mob mob){
        //check for players that can be aggroed if mob is agressive and has no target
        if (mob.BehaviourType.isAgressive && mob.getCombatTarget() == null) {
            if (mob.BehaviourType == Enum.MobBehaviourType.HamletGuard) {
                //safehold guard
                SafeGuardAggro(mob);
            } else {
                //normal aggro
                CheckForAggro(mob);
            }
        }
        //check if mob can move for patrol or moving to target
        if (mob.BehaviourType.canRoam) {
            CheckMobMovement(mob);
        }
        //check if mob can attack if it isn't wimpy
        if (!mob.BehaviourType.isWimpy && !mob.isMoving() && mob.combatTarget != null) {
            CheckForAttack(mob);
        }
    }
    public static void CheckForPlayerGuardAggro(Mob mob) {
        //looks for and sets mobs combatTarget
        if (!mob.isAlive()) {
            return;
        }
        ConcurrentHashMap<Integer, Boolean> loadedPlayers = mob.playerAgroMap;
        for (Entry playerEntry : loadedPlayers.entrySet()) {
            int playerID = (int) playerEntry.getKey();
            PlayerCharacter loadedPlayer = PlayerCharacter.getFromCache(playerID);
            //Player is null, let's remove them from the list.
            if (loadedPlayer == null) {
                loadedPlayers.remove(playerID);
                continue;
            }
            //Player is Dead, Mob no longer needs to attempt to aggro. Remove them from aggro map.
            if (!loadedPlayer.isAlive()) {
                loadedPlayers.remove(playerID);
                continue;
            }
            //Can't see target, skip aggro.
            if (!mob.canSee(loadedPlayer)) {
                continue;
            }
            // No aggro for this player
            if (GuardCanAggro(mob,loadedPlayer) == false)
                continue;
            if (MovementUtilities.inRangeToAggro(mob, loadedPlayer)) {
                mob.setAggroTargetID(playerID);
                mob.setCombatTarget(loadedPlayer);
                if(mob.contract != null) {
                    for (Entry<Mob, Integer> minion : mob.siegeMinionMap.entrySet()) {
                        minion.getKey().setAggroTargetID(playerID);
                        minion.getKey().setCombatTarget(loadedPlayer);
                    }
                }
                return;
            }
        }
    }
    private static Boolean GuardCanAggro(Mob mob, PlayerCharacter target){
        //first check condemn list for aggro allowed
        if(ZoneManager.getCityAtLocation(mob.getLoc()).getTOL().enforceKOS) {
            for (Entry<Integer, Condemned> entry : ZoneManager.getCityAtLocation(mob.getLoc()).getTOL().getCondemned().entrySet()) {
                if (entry.getValue().getPlayerUID() == target.getObjectUUID() && entry.getValue().isActive()) {
                    //target is listed individually
                    return true;
                }
                if(Guild.getGuild(entry.getValue().getGuildUID()) == target.getGuild()){
                    //target's guild is listed
                    return true;
                }
                if(Guild.getGuild(entry.getValue().getGuildUID()) == target.getGuild().getNation()){
                    //target's nation is listed
                    return true;
                }
            }
        }
        //next check not in same nation or allied guild/nation
        if(!mob.getGuild().getNation().equals(target.getGuild().getNation())) {
            if (!mob.getGuild().getAllyList().contains(target.getGuild()) || !mob.getGuild().getAllyList().contains(target.getGuild().getNation())) {
                return true;
            }
        }
        return false;
    }
}