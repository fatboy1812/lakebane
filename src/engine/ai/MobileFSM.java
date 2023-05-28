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
        if (mob == null)
            return;
        if (target == null || !target.isAlive()) {
            mob.setCombatTarget(null);
            return;
        }
        if (!CombatUtilities.inRangeToAttack(mob, target))
            return;
        switch (target.getObjectType()) {
            case PlayerCharacter:
                PlayerCharacter targetPlayer = (PlayerCharacter) target;
                if (canCast(mob)) {
                    if (!MobCast(mob))
                        AttackPlayer(mob, targetPlayer);
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
                AttackMob(mob, targetMob);
                break;
        }
    }
    public static void AttackPlayer(Mob mob, PlayerCharacter target) {
        if (mob.getMobBase().getSeeInvis() < target.getHidden() || !target.isAlive()) {
            mob.setCombatTarget(null);
            return;
        }
        if (mob.BehaviourType.callsForHelp)
            MobCallForHelp(mob);
        if (!MovementUtilities.inRangeDropAggro(mob, target)) {
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
        }
    }
    public static void AttackBuilding(Mob mob, Building target) {
        if (target.getRank() == -1 || !target.isVulnerable() || BuildingManager.getBuildingFromCache(target.getObjectUUID()) == null) {
            mob.setCombatTarget(null);
            return;
        }
        City playercity = ZoneManager.getCityAtLocation(mob.getLoc());
        if (playercity != null)
            for (Mob guard : playercity.getParent().zoneMobSet)
                if (guard.BehaviourType != null && guard.BehaviourType.ordinal() == Enum.MobBehaviourType.GuardCaptain.ordinal())
                    if (guard.getCombatTarget() == null && !guard.getGuild().equals(mob.getGuild()))
                        guard.setCombatTarget(mob);
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
        } else if (mob.getWeaponItemBase(true) != null) {
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
    public static void AttackMob(Mob mob, Mob target) {
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
        } else if (mob.getWeaponItemBase(true) != null) {
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
    }
    private static void Patrol(Mob mob) {
        //make sure mob is out of combat stance
        if (mob.isCombat() && mob.getCombatTarget() == null) {
            mob.setCombat(false);
            UpdateStateMsg rwss = new UpdateStateMsg();
            rwss.setPlayer(mob);
            DispatchMessage.sendToAllInRange(mob, rwss);
        }
        int patrolDelay = ThreadLocalRandom.current().nextInt((int)(MBServerStatics.AI_PATROL_DIVISOR * 0.5f), MBServerStatics.AI_PATROL_DIVISOR) + MBServerStatics.AI_PATROL_DIVISOR;
        if (mob.stopPatrolTime + (patrolDelay * 1000) > System.currentTimeMillis())
            //early exit while waiting to patrol again
            return;
        //guard captains inherit barracks patrol points dynamically
        if (mob.BehaviourType.ordinal() == Enum.MobBehaviourType.GuardCaptain.ordinal()) {
            Building barracks = mob.building;
            if (barracks != null && barracks.patrolPoints != null && !barracks.getPatrolPoints().isEmpty()) {
                mob.patrolPoints = barracks.patrolPoints;
            } else{
                randomGuardPatrolPoint(mob);
                return;
            }
        }
            if (mob.lastPatrolPointIndex > mob.patrolPoints.size() - 1) {
                mob.lastPatrolPointIndex = 0;
            }
        mob.destination = mob.patrolPoints.get(mob.lastPatrolPointIndex);
        mob.lastPatrolPointIndex += 1;
        MovementUtilities.aiMove(mob, mob.destination, true);
        if(mob.BehaviourType.ordinal() == Enum.MobBehaviourType.GuardCaptain.ordinal()){
            for (Entry<Mob, Integer> minion : mob.siegeMinionMap.entrySet()) {
                //make sure mob is out of combat stance
                if (minion.getKey().despawned == false) {
                    if (minion.getKey().isCombat() && minion.getKey().getCombatTarget() == null) {
                        minion.getKey().setCombat(false);
                        UpdateStateMsg rwss = new UpdateStateMsg();
                        rwss.setPlayer(minion.getKey());
                        DispatchMessage.sendToAllInRange(minion.getKey(), rwss);
                    }
                    if (MovementUtilities.canMove(minion.getKey())) {
                        Vector3f minionOffset = Formation.getOffset(2, minion.getValue() + 3);
                        minion.getKey().updateLocation();
                        Vector3fImmutable formationPatrolPoint = new Vector3fImmutable(mob.destination.x + minionOffset.x, mob.destination.y, mob.destination.z + minionOffset.z);
                        MovementUtilities.aiMove(minion.getKey(), formationPatrolPoint, true);
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
            if (!mobPower.isHarmful() || mobPower.targetSelf)
                msg = PowersManager.createPowerMsg(mobPower, powerRank, mob, mob);
            else
                msg = PowersManager.createPowerMsg(mobPower, powerRank, mob, target);
            msg.setUnknown04(2);
            PowersManager.finishUseMobPower(msg, mob, 0, 0);
            // Default minimum seconds between cast = 10
                mob.nextCastTime = System.currentTimeMillis() + (MBServerStatics.AI_POWER_DIVISOR * 1000);
            return true;
        }
        return false;
    }
    public static void MobCallForHelp(Mob mob) {
        boolean callGotResponse = false;
        if (mob.nextCallForHelp == 0) {
            mob.nextCallForHelp = System.currentTimeMillis();
        }
        if (mob.nextCallForHelp < System.currentTimeMillis())
            return;
        //mob sends call for help message
        ChatManager.chatSayInfo(null, mob.getName() + " calls for help!");
        Zone mobCamp = mob.getParentZone();
        for (Mob helper : mobCamp.zoneMobSet) {
            if (helper.BehaviourType.respondsToCallForHelp && helper.BehaviourType.BehaviourHelperType.equals(mob.BehaviourType)) {
                helper.setCombatTarget(mob.getCombatTarget());
                callGotResponse = true;
            }
        }
        if (callGotResponse)
            //wait 60 seconds to call for help again
            mob.nextCallForHelp = System.currentTimeMillis() + 60000;
    }
    public static void DetermineAction(Mob mob) {
        if (mob == null)
            return;
        if (mob.despawned && mob.getMobBase().getLoadID() == 13171) {
            //trebuchet spawn handler
            CheckForRespawn(mob);
            return;
        }
        if (mob.despawned && mob.isPlayerGuard) {
            //override for guards
            if(mob.BehaviourType.ordinal() == Enum.MobBehaviourType.GuardMinion.ordinal()){
                if(mob.npcOwner.isAlive() == false || ((Mob)mob.npcOwner).despawned == true){
                    //minions don't respawn while guard captain is dead
                    if(mob.isAlive() == false) {
                        mob.deathTime = System.currentTimeMillis();
                        return;
                    }
                }
            }
            CheckForRespawn(mob);
            //check to send mob home for player guards to prevent exploit of dragging guards away and then teleporting
            CheckToSendMobHome(mob);
            return;
        }
        if (!mob.isAlive()) {
            //no need to continue if mob is dead, check for respawn and move on
            CheckForRespawn(mob);
            return;
        }
        if (mob.playerAgroMap.isEmpty() && mob.isPlayerGuard == false)
            //no players loaded, no need to proceed
            return;
        if (mob.isCombat() && mob.getCombatTarget() == null) {
            mob.setCombat(false);
            UpdateStateMsg rwss = new UpdateStateMsg();
            rwss.setPlayer(mob);
            DispatchMessage.sendToAllInRange(mob, rwss);
        }
        mob.updateLocation();
        CheckToSendMobHome(mob);
        if(mob.combatTarget != null && mob.combatTarget.isAlive() == false){
            mob.setCombatTarget(null);
        }
        switch (mob.BehaviourType) {
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
        if (!aiAgent.isAlive())
            return;
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
            if (!aiAgent.canSee(loadedPlayer))
                continue;
            // No aggro for this race type
            if (aiAgent.notEnemy.contains(loadedPlayer.getRace().getRaceType().getMonsterType()))
                continue;
            if (MovementUtilities.inRangeToAggro(aiAgent, loadedPlayer)) {
                aiAgent.setCombatTarget(loadedPlayer);
                return;
            }
        }
    }
    private static void CheckMobMovement(Mob mob) {
        if (!MovementUtilities.canMove(mob))
            return;
        switch(mob.BehaviourType){
            case Pet1:
                if (!mob.playerAgroMap.containsKey(mob.getOwner().getObjectUUID())) {
                    //mob no longer has its owner loaded, translocate pet to owner
                    MovementManager.translocate(mob, mob.getOwner().getLoc(), null);
                    return;
                }
                if (mob.getCombatTarget() == null) {
                    //move back to owner
                    if (CombatUtilities.inRange2D(mob, mob.getOwner(), 6))
                        return;
                    mob.destination = mob.getOwner().getLoc();
                    MovementUtilities.moveToLocation(mob, mob.destination, 5);
                } else
                    chaseTarget(mob);
                break;
            case GuardMinion:
                if (!mob.npcOwner.isAlive() || ((Mob)mob.npcOwner).despawned)
                    randomGuardPatrolPoint(mob);
                else{
                    if(mob.getCombatTarget() != null){
                        chaseTarget(mob);
                    }
                }
                break;
            default:
                if (mob.getCombatTarget() == null) {
                    if(!mob.isMoving()) {
                        Patrol(mob);
                    } else{
                        mob.stopPatrolTime = System.currentTimeMillis();
                    }
                }else {
                    chaseTarget(mob);
                }
                break;
        }
    }
    private static void CheckForRespawn(Mob aiAgent) {
        if (aiAgent.deathTime == 0) {
            aiAgent.setDeathTime(System.currentTimeMillis());
            return;
        }
        //handles checking for respawn of dead mobs even when no players have mob loaded
        //Despawn Timer with Loot currently in inventory.
        if (!aiAgent.despawned) {
            if (aiAgent.getCharItemManager().getInventoryCount() > 0) {
                if (System.currentTimeMillis() > aiAgent.deathTime + MBServerStatics.DESPAWN_TIMER_WITH_LOOT) {
                    aiAgent.despawn();
                    aiAgent.deathTime = System.currentTimeMillis();
                }
                //No items in inventory.
            } else {
                //Mob's Loot has been looted.
                if (aiAgent.isHasLoot()) {
                    if (System.currentTimeMillis() > aiAgent.deathTime + MBServerStatics.DESPAWN_TIMER_ONCE_LOOTED) {
                        aiAgent.despawn();
                        aiAgent.deathTime = System.currentTimeMillis();
                    }
                    //Mob never had Loot.
                } else {
                    if (System.currentTimeMillis() > aiAgent.deathTime + MBServerStatics.DESPAWN_TIMER) {
                        aiAgent.despawn();
                        aiAgent.deathTime = System.currentTimeMillis();
                    }
                }
            }
        } else if (System.currentTimeMillis() > aiAgent.deathTime + (aiAgent.spawnTime * 1000)) {
            aiAgent.respawn();
        }
    }
    public static void CheckForAttack(Mob mob) {
        //checks if mob can attack based on attack timer and range
        if (mob.isAlive() == false)
            return;
        if (mob.getCombatTarget().getObjectType().equals(Enum.GameObjectType.PlayerCharacter) && MovementUtilities.inRangeDropAggro(mob, (PlayerCharacter)mob.getCombatTarget()) == false) {
            mob.setCombatTarget(null);
            if (mob.isCombat()) {
                mob.setCombat(false);
                UpdateStateMsg rwss = new UpdateStateMsg();
                rwss.setPlayer(mob);
                DispatchMessage.sendToAllInRange(mob, rwss);
            }
            return;
        }
        if (!mob.isCombat()) {
            mob.setCombat(true);
            UpdateStateMsg rwss = new UpdateStateMsg();
            rwss.setPlayer(mob);
            DispatchMessage.sendToAllInRange(mob, rwss);
        }
        if (System.currentTimeMillis() > mob.getLastAttackTime())
            AttackTarget(mob, mob.getCombatTarget());
    }
    private static void CheckToSendMobHome(Mob mob) {
        if(mob.BehaviourType.isAgressive) {
            if(mob.isPlayerGuard())
            {
                if(mob.BehaviourType.ordinal() == Enum.MobBehaviourType.GuardCaptain.ordinal()){
                    CheckForPlayerGuardAggro(mob);
                }
            } else {
                CheckForAggro(mob);
            }
        }
        if(mob.getCombatTarget() != null && CombatUtilities.inRange2D(mob,mob.getCombatTarget(),MBServerStatics.AI_BASE_AGGRO_RANGE * 0.5f)){
            return;
        }
        if (mob.isPlayerGuard() && !mob.despawned) {
            City current = ZoneManager.getCityAtLocation(mob.getLoc());
            if (current == null || current.equals(mob.getGuild().getOwnedCity()) == false || mob.playerAgroMap.isEmpty()) {
                PowersBase recall = PowersManager.getPowerByToken(-1994153779);
                PowersManager.useMobPower(mob, mob, recall, 40);
                mob.setCombatTarget(null);
                if(mob.BehaviourType.ordinal() == Enum.MobBehaviourType.GuardCaptain.ordinal() && mob.isAlive()){
                    //guard captain pulls his minions home with him
                    for (Entry<Mob, Integer> minion : mob.siegeMinionMap.entrySet()) {
                        PowersManager.useMobPower(minion.getKey(), minion.getKey(), recall, 40);
                        minion.getKey().setCombatTarget(null);
                    }
                }
            }
        }
         else if(MovementUtilities.inRangeOfBindLocation(mob) == false) {

            PowersBase recall = PowersManager.getPowerByToken(-1994153779);
            PowersManager.useMobPower(mob, mob, recall, 40);
            mob.setCombatTarget(null);
            for (Entry playerEntry : mob.playerAgroMap.entrySet()) {
                PlayerCharacter.getFromCache((int)playerEntry.getKey()).setHateValue(0);
            }
        }
    }
    private static void chaseTarget(Mob mob) {
        mob.updateMovementState();
        if(mob.playerAgroMap.containsKey(mob.getCombatTarget().getObjectUUID()) == false){
            mob.setCombatTarget(null);
            return;
        }
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
    public static void GuardCaptainLogic(Mob mob) {
        if (mob.getCombatTarget() == null)
            CheckForPlayerGuardAggro(mob);
        CheckMobMovement(mob);
        if (mob.getCombatTarget() != null)
            CheckForAttack(mob);
    }
    public static void GuardMinionLogic(Mob mob) {
        if (!mob.npcOwner.isAlive() && mob.getCombatTarget() == null) {
            CheckForPlayerGuardAggro(mob);
        }
        if(mob.npcOwner.getCombatTarget() != null)
            mob.setCombatTarget(mob.npcOwner.getCombatTarget());
         else
            mob.setCombatTarget(null);
        CheckMobMovement(mob);
        if (mob.getCombatTarget() != null)
            CheckForAttack(mob);
    }
    public static void GuardWallArcherLogic(Mob mob) {
        if (mob.getCombatTarget() == null)
            CheckForPlayerGuardAggro(mob);
        else
            CheckForAttack(mob);
    }
    private static void PetLogic(Mob mob) {
        if (mob.getCombatTarget() != null && !mob.getCombatTarget().isAlive())
            mob.setCombatTarget(null);
        if (MovementUtilities.canMove(mob) && mob.BehaviourType.canRoam)
            CheckMobMovement(mob);
        if (mob.getCombatTarget() != null)
            CheckForAttack(mob);
    }
    private static void HamletGuardLogic(Mob mob) {
        if (mob.getCombatTarget() == null) {
            //safehold guard
            SafeGuardAggro(mob);
        } else{
            if(mob.combatTarget.isAlive() == false){
                SafeGuardAggro(mob);
            }
        }
        if (mob.getCombatTarget() != null)
            CheckForAttack(mob);
    }
    private static void DefaultLogic(Mob mob) {
        if(mob.getObjectUUID() == 40548){
            int thing = 0;
        }
        //check for players that can be aggroed if mob is agressive and has no target
        if (mob.BehaviourType.isAgressive) {
            AbstractWorldObject newTarget = ChangeTargetFromHateValue(mob);
            if (newTarget != null) {
                mob.setCombatTarget(newTarget);
            } else {
                if (mob.getCombatTarget() == null) {
                    if (mob.BehaviourType == Enum.MobBehaviourType.HamletGuard)
                        //safehold guard
                        SafeGuardAggro(mob);
                    else
                        //normal aggro
                        CheckForAggro(mob);
                }
            }
        }
        //check if mob can move for patrol or moving to target
        if (mob.BehaviourType.canRoam)
            CheckMobMovement(mob);
        //check if mob can attack if it isn't wimpy
        if (!mob.BehaviourType.isWimpy && !mob.isMoving() && mob.combatTarget != null)
            CheckForAttack(mob);
    }
    public static void CheckForPlayerGuardAggro(Mob mob) {
        //looks for and sets mobs combatTarget
        if (!mob.isAlive())
            return;
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
            if (!mob.canSee(loadedPlayer))
                continue;
            // No aggro for this player
            if (GuardCanAggro(mob, loadedPlayer) == false)
                continue;
            if (MovementUtilities.inRangeToAggro(mob, loadedPlayer)) {
                mob.setCombatTarget(loadedPlayer);
                return;
            }
        }
    }
    public static Boolean GuardCanAggro(Mob mob, PlayerCharacter target) {
        if (mob.getGuild().getNation().equals(target.getGuild().getNation()))
            return false;
        if (mob.BehaviourType.ordinal() == Enum.MobBehaviourType.GuardMinion.ordinal()) {
            if(((Mob)mob.npcOwner).building.getCity().cityOutlaws.contains(target.getObjectUUID()) == true){
                return true;
            }
        } else if(mob.building.getCity().cityOutlaws.contains(target.getObjectUUID()) == true){
            return true;
        }
        //first check condemn list for aggro allowed (allies button is checked)
        if (ZoneManager.getCityAtLocation(mob.getLoc()).getTOL().reverseKOS) {
            for (Entry<Integer, Condemned> entry : ZoneManager.getCityAtLocation(mob.getLoc()).getTOL().getCondemned().entrySet()) {
                if (entry.getValue().getPlayerUID() == target.getObjectUUID() && entry.getValue().isActive())
                    //target is listed individually
                    return false;
                if (Guild.getGuild(entry.getValue().getGuildUID()) == target.getGuild())
                    //target's guild is listed
                    return false;
                if (Guild.getGuild(entry.getValue().getGuildUID()) == target.getGuild().getNation())
                    //target's nation is listed
                    return false;
            }
            return true;
        } else{
            //allies button is not checked
            for (Entry<Integer, Condemned> entry : ZoneManager.getCityAtLocation(mob.getLoc()).getTOL().getCondemned().entrySet()) {
                if (entry.getValue().getPlayerUID() == target.getObjectUUID() && entry.getValue().isActive())
                    //target is listed individually
                    return true;
                if (Guild.getGuild(entry.getValue().getGuildUID()) == target.getGuild())
                    //target's guild is listed
                    return true;
                if (Guild.getGuild(entry.getValue().getGuildUID()) == target.getGuild().getNation())
                    //target's nation is listed
                    return true;
            }
        }
        return false;
    }
    public static void randomGuardPatrolPoint(Mob mob){
        if (mob.isMoving() == true) {
            //early exit for a mob who is already moving to a patrol point
            //while mob moving, update lastPatrolTime so that when they stop moving the 10 second timer can begin
            mob.stopPatrolTime = System.currentTimeMillis();
            return;
        }
        //wait between 10 and 15 seconds after reaching patrol point before moving
        int patrolDelay = ThreadLocalRandom.current().nextInt(10000) + 5000;
        if (mob.stopPatrolTime + patrolDelay > System.currentTimeMillis())
            //early exit while waiting to patrol again
            return;
        float xPoint = ThreadLocalRandom.current().nextInt(400) - 200;
        float zPoint = ThreadLocalRandom.current().nextInt(400) - 200;
        Vector3fImmutable TreePos = mob.getGuild().getOwnedCity().getLoc();
        mob.destination = new Vector3fImmutable(TreePos.x + xPoint,TreePos.y,TreePos.z + zPoint);
        MovementUtilities.aiMove(mob, mob.destination, true);
        if(mob.BehaviourType.ordinal() == Enum.MobBehaviourType.GuardCaptain.ordinal()){
            for (Entry<Mob, Integer> minion : mob.siegeMinionMap.entrySet()) {
                //make sure mob is out of combat stance
                if (minion.getKey().despawned == false) {
                    if (minion.getKey().isCombat() && minion.getKey().getCombatTarget() == null) {
                        minion.getKey().setCombat(false);
                        UpdateStateMsg rwss = new UpdateStateMsg();
                        rwss.setPlayer(minion.getKey());
                        DispatchMessage.sendToAllInRange(minion.getKey(), rwss);
                    }
                    if (MovementUtilities.canMove(minion.getKey())) {
                        Vector3f minionOffset = Formation.getOffset(2, minion.getValue() + 3);
                        minion.getKey().updateLocation();
                        Vector3fImmutable formationPatrolPoint = new Vector3fImmutable(mob.destination.x + minionOffset.x, mob.destination.y, mob.destination.z + minionOffset.z);
                        MovementUtilities.aiMove(minion.getKey(), formationPatrolPoint, true);
                    }
                }
            }
        }
    }
    public static AbstractWorldObject ChangeTargetFromHateValue(Mob mob){
        float CurrentHateValue = 0;
        if(mob.getCombatTarget() != null && mob.getCombatTarget().getObjectType().equals(Enum.GameObjectType.PlayerCharacter)){
            CurrentHateValue = ((PlayerCharacter)mob.getCombatTarget()).getHateValue();
        }
        AbstractWorldObject mostHatedTarget = null;
        for (Entry playerEntry : mob.playerAgroMap.entrySet()) {
            PlayerCharacter potentialTarget = PlayerCharacter.getFromCache((int)playerEntry.getKey());
            if(potentialTarget.equals(mob.getCombatTarget())){
                continue;
            }
            if(potentialTarget != null && potentialTarget.getHateValue() > CurrentHateValue && MovementUtilities.inRangeToAggro(mob, potentialTarget)){
                CurrentHateValue = potentialTarget.getHateValue();
                mostHatedTarget = potentialTarget;
            }
        }
        return mostHatedTarget;
    }
}