// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com

package engine.mobileAI;

import engine.Enum;
import engine.Enum.DispatchChannel;
import engine.InterestManagement.WorldGrid;
import engine.gameManager.*;
import engine.math.Vector3f;
import engine.math.Vector3fImmutable;
import engine.mobileAI.Threads.MobAIThread;
import engine.mobileAI.utilities.CombatUtilities;
import engine.mobileAI.utilities.MovementUtilities;
import engine.net.DispatchMessage;
import engine.net.client.msg.PerformActionMsg;
import engine.net.client.msg.PowerProjectileMsg;
import engine.objects.*;
import engine.powers.ActionsBase;
import engine.powers.PowersBase;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static engine.math.FastMath.sqr;

public class MobAI {


    private static void AttackTarget(Mob mob, AbstractWorldObject target) {

        try {

            if (mob == null)
                return;

            if (target == null || !target.isAlive()) {
                mob.setCombatTarget(null);
                return;
            }

            if (target.getObjectType() == Enum.GameObjectType.PlayerCharacter && canCast(mob)) {

                if (mob.isPlayerGuard() == false && MobCast(mob)) {
                    mob.updateLocation();
                    return;
                }

                if (mob.isPlayerGuard() == true && GuardCast(mob)) {
                    mob.updateLocation();
                    return;
                }
            }

            if (!CombatUtilities.inRangeToAttack(mob, target))
                return;

            switch (target.getObjectType()) {
                case PlayerCharacter:
                    PlayerCharacter targetPlayer = (PlayerCharacter) target;
                    AttackPlayer(mob, targetPlayer);
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

            mob.updateLocation();

        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: AttackTarget" + " " + e.getMessage());
        }
    }

    public static void AttackPlayer(Mob mob, PlayerCharacter target) {

        try {

            if (!mob.canSee(target)) {
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

                if (mob.isMoving() && mob.getRange() > 20)
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

            if (target.getPet() != null)
                if (target.getPet().getCombatTarget() == null && target.getPet().assist == true)
                    target.getPet().setCombatTarget(mob);

        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: AttackPlayer" + " " + e.getMessage());
        }

    }

    public static void AttackBuilding(Mob mob, Building target) {

        try {

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

        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: AttackBuilding" + " " + e.getMessage());
        }
    }

    public static void AttackMob(Mob mob, Mob target) {

        try {

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
                if (target.getCombatTarget() == null) {
                    target.setCombatTarget(mob);
                }
            }
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: AttackMob" + " " + e.getMessage());
        }
    }

    private static void Patrol(Mob mob) {

        try {

            //make sure mob is out of combat stance

            int patrolDelay = ThreadLocalRandom.current().nextInt((int) (MobAIThread.AI_PATROL_DIVISOR * 0.5f), MobAIThread.AI_PATROL_DIVISOR) + MobAIThread.AI_PATROL_DIVISOR;

            //early exit while waiting to patrol again

            if (mob.stopPatrolTime + (patrolDelay * 1000) > System.currentTimeMillis())
                return;

            //guard captains inherit barracks patrol points dynamically

            if (mob.BehaviourType.ordinal() == Enum.MobBehaviourType.GuardCaptain.ordinal()) {

                Building barracks = mob.building;

                if (barracks != null && barracks.patrolPoints != null && !barracks.getPatrolPoints().isEmpty()) {
                    mob.patrolPoints = barracks.patrolPoints;
                } else {
                    randomGuardPatrolPoint(mob);
                    return;
                }
            }

            if (mob.lastPatrolPointIndex > mob.patrolPoints.size() - 1)
                mob.lastPatrolPointIndex = 0;

            mob.destination = mob.patrolPoints.get(mob.lastPatrolPointIndex);
            mob.lastPatrolPointIndex += 1;

            MovementUtilities.aiMove(mob, mob.destination, true);

            if (mob.BehaviourType.equals(Enum.MobBehaviourType.GuardCaptain))
                for (Entry<Mob, Integer> minion : mob.siegeMinionMap.entrySet())

                    //make sure mob is out of combat stance

                    if (minion.getKey().despawned == false) {
                        if (MovementUtilities.canMove(minion.getKey())) {
                            Vector3f minionOffset = Formation.getOffset(2, minion.getValue() + 3);
                            minion.getKey().updateLocation();
                            Vector3fImmutable formationPatrolPoint = new Vector3fImmutable(mob.destination.x + minionOffset.x, mob.destination.y, mob.destination.z + minionOffset.z);
                            MovementUtilities.aiMove(minion.getKey(), formationPatrolPoint, true);
                        }
                    }
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: AttackTarget" + " " + e.getMessage());
        }
    }

    public static boolean canCast(Mob mob) {

        try {

            // Performs validation to determine if a
            // mobile in the proper state to cast.

            if (mob == null)
                return false;

            if(mob.isPlayerGuard == true){

                int contractID;

                if(mob.BehaviourType.equals(Enum.MobBehaviourType.GuardMinion))
                    contractID = mob.npcOwner.contract.getContractID();
                 else
                    contractID = mob.contract.getContractID();

                if(Enum.MinionType.ContractToMinionMap.get(contractID).isMage() == false)
                    return false;
            }

            if (mob.mobPowers.isEmpty())
                return false;

            if (!mob.canSee((PlayerCharacter) mob.getCombatTarget())) {
                mob.setCombatTarget(null);
                return false;
            }
            if (mob.nextCastTime == 0)
                mob.nextCastTime = System.currentTimeMillis();

            return mob.nextCastTime <= System.currentTimeMillis();

        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: canCast" + " " + e.getMessage());
        }
        return false;
    }

    public static boolean MobCast(Mob mob) {

        try {
            // Method picks a random spell from a mobile's list of powers
            // and casts it on the current target (or itself).  Validation
            // (including empty lists) is done previously within canCast();

            ArrayList<Integer> powerTokens;
            ArrayList<Integer> purgeTokens;
            AbstractCharacter target = (AbstractCharacter) mob.getCombatTarget();

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

            //check for hit-roll

            if (mobPower.requiresHitRoll)
                if (CombatUtilities.triggerDefense(mob, mob.getCombatTarget()))
                    return false;

            // Cast the spell

            if (CombatUtilities.inRange2D(mob, mob.getCombatTarget(), mobPower.getRange())) {

                PerformActionMsg msg;

                if (!mob.StrongholdCommander && (!mobPower.isHarmful() || mobPower.targetSelf)) {
                    PowersManager.useMobPower(mob, mob, mobPower, powerRank);
                    msg = PowersManager.createPowerMsg(mobPower, powerRank, mob, mob);
                } else {
                    PowersManager.useMobPower(mob, target, mobPower, powerRank);
                    msg = PowersManager.createPowerMsg(mobPower, powerRank, mob, target);
                }

                msg.setUnknown04(2);

                PowersManager.finishUseMobPower(msg, mob, 0, 0);
                long randomCooldown = (long)((ThreadLocalRandom.current().nextInt(10,15) * 1000) * MobAIThread.AI_CAST_FREQUENCY);

                mob.nextCastTime = System.currentTimeMillis() + randomCooldown;
                return true;
            }
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: MobCast" + " " + e.getMessage());
        }
        return false;
    }

    public static boolean GuardCast(Mob mob) {

        try {
            // Method picks a random spell from a mobile's list of powers
            // and casts it on the current target (or itself).  Validation
            // (including empty lists) is done previously within canCast();

            ArrayList<Integer> powerTokens;
            ArrayList<Integer> purgeTokens;
            AbstractCharacter target = (AbstractCharacter) mob.getCombatTarget();

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

            int powerToken = 0;
            int nukeRoll = ThreadLocalRandom.current().nextInt(1,100);

            if (nukeRoll < 55) {

                //use direct damage spell
                powerToken = powerTokens.get(powerTokens.size() - 1);

            } else {
                //use random spell
                powerToken = powerTokens.get(ThreadLocalRandom.current().nextInt(powerTokens.size()));
            }

            int powerRank = 1;

            switch(mob.getRank()){
                case 1:
                    powerRank = 10;
                    break;
                case 2:
                    powerRank = 15;
                    break;
                case 3:
                    powerRank = 20;
                    break;
                case 4:
                    powerRank = 25;
                    break;
                case 5:
                    powerRank = 30;
                    break;
                case 6:
                    powerRank = 35;
                    break;
                case 7:
                    powerRank = 40;
                    break;
            }

            PowersBase mobPower = PowersManager.getPowerByToken(powerToken);

            //check for hit-roll

            if (mobPower.requiresHitRoll)
                if (CombatUtilities.triggerDefense(mob, mob.getCombatTarget()))
                    return false;

            // Cast the spell

            if (CombatUtilities.inRange2D(mob, mob.getCombatTarget(), mobPower.getRange())) {

                PerformActionMsg msg;

                if (!mobPower.isHarmful() || mobPower.targetSelf) {

                    if (mobPower.category.equals("DISPEL")) {
                        PowersManager.useMobPower(mob, target, mobPower, powerRank);
                        msg = PowersManager.createPowerMsg(mobPower, powerRank, mob, target);
                    } else {
                        PowersManager.useMobPower(mob, mob, mobPower, powerRank);
                        msg = PowersManager.createPowerMsg(mobPower, powerRank, mob, mob);
                    }
                } else {
                    PowersManager.useMobPower(mob, target, mobPower, powerRank);
                    msg = PowersManager.createPowerMsg(mobPower, powerRank, mob, target);
                }

                msg.setUnknown04(2);

                PowersManager.finishUseMobPower(msg, mob, 0, 0);

                long randomCooldown = (long)((ThreadLocalRandom.current().nextInt(10,15) * 1000) * MobAIThread.AI_CAST_FREQUENCY);
                mob.nextCastTime = System.currentTimeMillis() + randomCooldown;
                return true;
            }
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: MobCast" + " " + e.getMessage());
        }
        return false;
    }

    public static void MobCallForHelp(Mob mob) {

        try {

            boolean callGotResponse = false;

            if (mob.nextCallForHelp == 0)
                mob.nextCallForHelp = System.currentTimeMillis();

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

            //wait 60 seconds to call for help again

            if (callGotResponse)
                mob.nextCallForHelp = System.currentTimeMillis() + 60000;

        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: MobCallForHelp" + " " + e.getMessage());
        }
    }

    public static void DetermineAction(Mob mob) {

        try {

            //always check the respawn que, respawn 1 mob max per second to not flood the client

            if (mob == null)
                return;

            if (mob.getTimestamps().containsKey("lastExecution") == false)
                mob.getTimestamps().put("lastExecution", System.currentTimeMillis());

            if (System.currentTimeMillis() < mob.getTimeStamp("lastExecution"))
                return;

            mob.getTimestamps().put("lastExecution", System.currentTimeMillis() + MobAIThread.AI_PULSE_MOB_THRESHOLD);

            //trebuchet spawn handler

            if (mob.despawned && mob.getMobBase().getLoadID() == 13171) {
                CheckForRespawn(mob);
                return;
            }

            //override for guards

            if (mob.despawned && mob.isPlayerGuard) {

                if (mob.BehaviourType.ordinal() == Enum.MobBehaviourType.GuardMinion.ordinal()) {
                    if (mob.npcOwner.isAlive() == false || ((Mob) mob.npcOwner).despawned == true) {

                        //minions don't respawn while guard captain is dead

                        if (mob.isAlive() == false) {
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

            //no need to continue if mob is dead, check for respawn and move on

            if (!mob.isAlive()) {
                CheckForRespawn(mob);
                return;
            }

            //no players loaded, no need to proceed

            if (mob.playerAgroMap.isEmpty()) {
                if(mob.getCombatTarget() != null)
                    mob.setCombatTarget(null);
                return;
            }

            if (mob.getCombatTarget() != null) {

                if (mob.getCombatTarget().isAlive() == false) {
                    mob.setCombatTarget(null);
                    return;
                }

                if (mob.getCombatTarget().getObjectTypeMask() == MBServerStatics.MASK_PLAYER) {

                    PlayerCharacter target = (PlayerCharacter) mob.getCombatTarget();

                    if (mob.playerAgroMap.containsKey(target.getObjectUUID()) == false) {
                        mob.setCombatTarget(null);
                        return;
                    }

                    if (mob.canSee((PlayerCharacter) mob.getCombatTarget()) == false) {
                        mob.setCombatTarget(null);
                        return;
                    }

                }
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
            if(mob.isAlive())
                RecoverHealth(mob);
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: DetermineAction" + " " + e.getMessage());
        }
    }

    private static void CheckForAggro(Mob aiAgent) {

        try {

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

                if (aiAgent.notEnemy.size() > 0 && aiAgent.notEnemy.contains(loadedPlayer.getRace().getRaceType().getMonsterType()) == true)
                    continue;

                //mob has enemies and this player race is not it

                if (aiAgent.enemy.size() > 0 && aiAgent.enemy.contains(loadedPlayer.getRace().getRaceType().getMonsterType()) == false)
                    continue;

                if (MovementUtilities.inRangeToAggro(aiAgent, loadedPlayer)) {
                    aiAgent.setCombatTarget(loadedPlayer);
                    return;
                }

            }

            if (aiAgent.getCombatTarget() == null) {

                //look for pets to aggro if no players found to aggro

                HashSet<AbstractWorldObject> awoList = WorldGrid.getObjectsInRangePartial(aiAgent, MobAIThread.AI_BASE_AGGRO_RANGE, MBServerStatics.MASK_PET);

                for (AbstractWorldObject awoMob : awoList) {

                    // exclude self.

                    if (aiAgent.equals(awoMob))
                        continue;

                    Mob aggroMob = (Mob) awoMob;
                    aiAgent.setCombatTarget(aggroMob);
                    return;
                }
            }
        } catch (Exception e) {
            Logger.info(aiAgent.getObjectUUID() + " " + aiAgent.getName() + " Failed At: CheckForAggro" + " " + e.getMessage());
        }
    }

    private static void CheckMobMovement(Mob mob) {

        try {

            if (!MovementUtilities.canMove(mob))
                return;

            mob.updateLocation();

            switch (mob.BehaviourType) {

                case Pet1:
                    if (mob.getOwner() == null)
                        return;

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
                    if (!mob.npcOwner.isAlive() && mob.getCombatTarget() == null)
                        randomGuardPatrolPoint(mob);
                    else {
                        if (mob.getCombatTarget() != null) {
                            chaseTarget(mob);
                        }
                    }
                    break;
                default:
                    if (mob.getCombatTarget() == null) {
                        if (!mob.isMoving())
                            Patrol(mob);
                        else {
                            mob.stopPatrolTime = System.currentTimeMillis();
                        }
                    } else {
                        chaseTarget(mob);
                    }
                    break;

            }
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: CheckMobMovement" + " " + e.getMessage());
        }
    }

    private static void CheckForRespawn(Mob aiAgent) {

        try {

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
                        return;
                    }
                    //No items in inventory.
                } else {
                    //Mob's Loot has been looted.
                    if (aiAgent.isHasLoot()) {
                        if (System.currentTimeMillis() > aiAgent.deathTime + MBServerStatics.DESPAWN_TIMER_ONCE_LOOTED) {
                            aiAgent.despawn();
                            aiAgent.deathTime = System.currentTimeMillis();
                            return;
                        }
                        //Mob never had Loot.
                    } else {
                        if (System.currentTimeMillis() > aiAgent.deathTime + MBServerStatics.DESPAWN_TIMER) {
                            aiAgent.despawn();
                            aiAgent.deathTime = System.currentTimeMillis();
                            return;
                        }
                    }
                }
            } else if (System.currentTimeMillis() > (aiAgent.deathTime + (aiAgent.spawnTime * 1000L))) {

                if(Mob.discDroppers.contains(aiAgent))
                    return;

                if (!Zone.respawnQue.contains(aiAgent)) {
                    Zone.respawnQue.add(aiAgent);
                }
            }
        } catch (Exception e) {
            Logger.info(aiAgent.getObjectUUID() + " " + aiAgent.getName() + " Failed At: CheckForRespawn" + " " + e.getMessage());
        }
    }

    public static void CheckForAttack(Mob mob) {
        try {

            //checks if mob can attack based on attack timer and range

            if (mob.isAlive() == false)
                return;

            if (mob.getCombatTarget() == null)
                return;

            if (mob.getCombatTarget().getObjectType().equals(Enum.GameObjectType.PlayerCharacter) && MovementUtilities.inRangeDropAggro(mob, (PlayerCharacter) mob.getCombatTarget()) == false && mob.BehaviourType.ordinal() != Enum.MobBehaviourType.Pet1.ordinal()) {

                mob.setCombatTarget(null);
                return;
            }
            if (System.currentTimeMillis() > mob.getLastAttackTime())
                AttackTarget(mob, mob.getCombatTarget());

        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: CheckForAttack" + " " + e.getMessage());
        }
    }

    private static void CheckToSendMobHome(Mob mob) {

        if(mob.BehaviourType.equals(Enum.MobBehaviourType.Pet1)){
            if(mob.loc.distanceSquared(mob.getOwner().loc) > 60 * 60)
                mob.teleport(mob.getOwner().loc);
            return;
        }
        try {
            if (mob.BehaviourType.isAgressive) {

                if (mob.isPlayerGuard()) {
                    if (mob.BehaviourType.ordinal() == Enum.MobBehaviourType.GuardCaptain.ordinal())
                        CheckForPlayerGuardAggro(mob);
                } else {
                    CheckForAggro(mob);
                }
            }

            if (mob.isPlayerGuard() && !mob.despawned) {

                City current = ZoneManager.getCityAtLocation(mob.getLoc());

                if (current == null || current.equals(mob.getGuild().getOwnedCity()) == false) {

                    PowersBase recall = PowersManager.getPowerByToken(-1994153779);
                    PowersManager.useMobPower(mob, mob, recall, 40);
                    mob.setCombatTarget(null);

                    if (mob.BehaviourType.ordinal() == Enum.MobBehaviourType.GuardCaptain.ordinal() && mob.isAlive()) {

                        //guard captain pulls his minions home with him

                        for (Entry<Mob, Integer> minion : mob.siegeMinionMap.entrySet()) {
                            PowersManager.useMobPower(minion.getKey(), minion.getKey(), recall, 40);
                            minion.getKey().setCombatTarget(null);
                        }
                    }
                }
            } else if (MovementUtilities.inRangeOfBindLocation(mob) == false) {

                PowersBase recall = PowersManager.getPowerByToken(-1994153779);
                PowersManager.useMobPower(mob, mob, recall, 40);
                mob.setCombatTarget(null);

                for (Entry playerEntry : mob.playerAgroMap.entrySet())
                    PlayerCharacter.getFromCache((int) playerEntry.getKey()).setHateValue(0);
            }
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: CheckToSendMobHome" + " " + e.getMessage());
        }
    }

    private static void chaseTarget(Mob mob) {

        try {

            float rangeSquared = mob.getRange() * mob.getRange();
            float distanceSquared = mob.getLoc().distanceSquared2D(mob.getCombatTarget().getLoc());

            if(mob.isMoving() == true && distanceSquared < rangeSquared - 50) {
                mob.destination = mob.getLoc();
                MovementUtilities.moveToLocation(mob, mob.destination, 0);
            } else if (CombatUtilities.inRange2D(mob, mob.getCombatTarget(), mob.getRange()) == false) {
                if (mob.getRange() > 15) {
                    mob.destination = mob.getCombatTarget().getLoc();
                    MovementUtilities.moveToLocation(mob, mob.destination, 0);
                } else {

                    //check if building

                    switch (mob.getCombatTarget().getObjectType()) {
                        case PlayerCharacter:
                        case Mob:
                            mob.destination = MovementUtilities.GetDestinationToCharacter(mob, (AbstractCharacter) mob.getCombatTarget());
                            MovementUtilities.moveToLocation(mob, mob.destination, mob.getRange() + 1);
                            break;
                        case Building:
                            mob.destination = mob.getCombatTarget().getLoc();
                            MovementUtilities.moveToLocation(mob, mob.getCombatTarget().getLoc(), 0);
                            break;
                    }
                }
            }
            mob.updateMovementState();
            mob.updateLocation();
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: chaseTarget" + " " + e.getMessage());
        }
    }

    private static void SafeGuardAggro(Mob mob) {
        try {

            HashSet<AbstractWorldObject> awoList = WorldGrid.getObjectsInRangePartial(mob, 100, MBServerStatics.MASK_MOB);

            for (AbstractWorldObject awoMob : awoList) {

                //dont scan self.

                if (mob.equals(awoMob) || (mob.agentType.equals(Enum.AIAgentType.GUARD)) || (mob.agentType.equals(Enum.AIAgentType.PET)))
                    continue;

                Mob aggroMob = (Mob) awoMob;

                //don't attack other guards

                if ((aggroMob.agentType.equals(Enum.AIAgentType.GUARD)))
                    continue;

                if(aggroMob.BehaviourType.equals(Enum.MobBehaviourType.Pet1))
                    continue;

                if (mob.getLoc().distanceSquared2D(aggroMob.getLoc()) > sqr(50))
                    continue;

                mob.setCombatTarget(aggroMob);
            }
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: SafeGuardAggro" + " " + e.getMessage());
        }
    }

    public static void GuardCaptainLogic(Mob mob) {

        try {
            if (mob.getCombatTarget() == null)
                CheckForPlayerGuardAggro(mob);

            AbstractWorldObject newTarget = ChangeTargetFromHateValue(mob);

            if (newTarget != null) {

                if (newTarget.getObjectType().equals(Enum.GameObjectType.PlayerCharacter)) {
                    if (GuardCanAggro(mob, (PlayerCharacter) newTarget))
                        mob.setCombatTarget(newTarget);
                } else
                    mob.setCombatTarget(newTarget);

            }
            CheckMobMovement(mob);
            CheckForAttack(mob);
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: GuardCaptainLogic" + " " + e.getMessage());
        }
    }

    public static void GuardMinionLogic(Mob mob) {

        try {
            boolean isComanded = mob.npcOwner.isAlive();
            if (!isComanded) {
                GuardCaptainLogic(mob);
            }else {
                if (mob.npcOwner.getCombatTarget() != null)
                    mob.setCombatTarget(mob.npcOwner.getCombatTarget());
                else
                    if (mob.getCombatTarget() != null)
                        mob.setCombatTarget(null);
            }
            CheckMobMovement(mob);
            CheckForAttack(mob);
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: GuardMinionLogic" + " " + e.getMessage());
        }
    }

    public static void GuardWallArcherLogic(Mob mob) {

        try {
            if (mob.getCombatTarget() == null)
                CheckForPlayerGuardAggro(mob);
            else
                CheckForAttack(mob);
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: GuardWallArcherLogic" + " " + e.getMessage());
        }
    }

    private static void PetLogic(Mob mob) {

        try {

            if (mob.getOwner() == null && mob.isNecroPet() == false && mob.isSiege() == false)
                if (ZoneManager.getSeaFloor().zoneMobSet.contains(mob))
                    mob.killCharacter("no owner");

            if (MovementUtilities.canMove(mob) && mob.BehaviourType.canRoam)
                CheckMobMovement(mob);

            CheckForAttack(mob);
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: PetLogic" + " " + e.getMessage());
        }
    }

    private static void HamletGuardLogic(Mob mob) {

        try {
            //safehold guard

            if (mob.getCombatTarget() == null)
                SafeGuardAggro(mob);
            else if (mob.getCombatTarget().isAlive() == false)
                SafeGuardAggro(mob);

            CheckForAttack(mob);
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: HamletGuardLogic" + " " + e.getMessage());
        }
    }

    private static void DefaultLogic(Mob mob) {

        try {

            //check for players that can be aggroed if mob is agressive and has no target

            if (mob.getCombatTarget() != null && mob.playerAgroMap.containsKey(mob.getCombatTarget().getObjectUUID()) == false)
                mob.setCombatTarget(null);

            if (mob.BehaviourType.isAgressive) {

                AbstractWorldObject newTarget = ChangeTargetFromHateValue(mob);

                if (newTarget != null)
                    mob.setCombatTarget(newTarget);
                else {
                    if (mob.getCombatTarget() == null) {
                        if (mob.BehaviourType == Enum.MobBehaviourType.HamletGuard)
                            SafeGuardAggro(mob);  //safehold guard
                        else
                            CheckForAggro(mob);   //normal aggro
                    }
                }
            }

            //check if mob can move for patrol or moving to target

            if (mob.BehaviourType.canRoam)
                CheckMobMovement(mob);

            //check if mob can attack if it isn't wimpy

            if (!mob.BehaviourType.isWimpy && mob.getCombatTarget() != null)
                CheckForAttack(mob);

        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: DefaultLogic" + " " + e.getMessage());
        }
    }

    public static void CheckForPlayerGuardAggro(Mob mob) {

        try {

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

                if (MovementUtilities.inRangeToAggro(mob, loadedPlayer) && mob.getCombatTarget() == null) {
                    mob.setCombatTarget(loadedPlayer);
                    return;
                }
            }
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: CheckForPlayerGuardAggro" + e.getMessage());
        }
    }

    public static Boolean GuardCanAggro(Mob mob, PlayerCharacter target) {

        try {

            if (mob.getGuild().getNation().equals(target.getGuild().getNation()))
                return false;

            if (mob.BehaviourType.ordinal() == Enum.MobBehaviourType.GuardMinion.ordinal()) {
                if (((Mob) mob.npcOwner).building.getCity().cityOutlaws.contains(target.getObjectUUID()) == true) {
                    return true;
                }
            } else if (mob.building.getCity().cityOutlaws.contains(target.getObjectUUID()) == true) {
                return true;
            }

            //first check condemn list for aggro allowed (allies button is checked)

            if (ZoneManager.getCityAtLocation(mob.getLoc()).getTOL().reverseKOS) {
                for (Entry<Integer, Condemned> entry : ZoneManager.getCityAtLocation(mob.getLoc()).getTOL().getCondemned().entrySet()) {

                    //target is listed individually

                    if (entry.getValue().getPlayerUID() == target.getObjectUUID() && entry.getValue().isActive())
                        return false;

                    //target's guild is listed

                    if (Guild.getGuild(entry.getValue().getGuildUID()) == target.getGuild())
                        return false;

                    //target's nation is listed

                    if (Guild.getGuild(entry.getValue().getGuildUID()) == target.getGuild().getNation())
                        return false;
                }
                return true;
            } else {

                //allies button is not checked

                for (Entry<Integer, Condemned> entry : ZoneManager.getCityAtLocation(mob.getLoc()).getTOL().getCondemned().entrySet()) {

                    //target is listed individually

                    if (entry.getValue().getPlayerUID() == target.getObjectUUID() && entry.getValue().isActive())
                        return true;

                    //target's guild is listed

                    if (Guild.getGuild(entry.getValue().getGuildUID()) == target.getGuild())
                        return true;

                    //target's nation is listed

                    if (Guild.getGuild(entry.getValue().getGuildUID()) == target.getGuild().getNation())
                        return true;
                }
            }
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: GuardCanAggro" + " " + e.getMessage());
        }
        return false;
    }

    public static void randomGuardPatrolPoint(Mob mob) {

        try {

            //early exit for a mob who is already moving to a patrol point
            //while mob moving, update lastPatrolTime so that when they stop moving the 10 second timer can begin

            if (mob.isMoving() == true) {
                mob.stopPatrolTime = System.currentTimeMillis();
                return;
            }

            //wait between 10 and 15 seconds after reaching patrol point before moving

            int patrolDelay = ThreadLocalRandom.current().nextInt(10000) + 5000;

            //early exit while waiting to patrol again

            if (mob.stopPatrolTime + patrolDelay > System.currentTimeMillis())
                return;

            float xPoint = ThreadLocalRandom.current().nextInt(400) - 200;
            float zPoint = ThreadLocalRandom.current().nextInt(400) - 200;
            Vector3fImmutable TreePos = mob.getGuild().getOwnedCity().getLoc();
            mob.destination = new Vector3fImmutable(TreePos.x + xPoint, TreePos.y, TreePos.z + zPoint);

            MovementUtilities.aiMove(mob, mob.destination, true);

            if (mob.BehaviourType.ordinal() == Enum.MobBehaviourType.GuardCaptain.ordinal()) {
                for (Entry<Mob, Integer> minion : mob.siegeMinionMap.entrySet()) {

                    //make sure mob is out of combat stance

                    if (minion.getKey().despawned == false) {
                        if (MovementUtilities.canMove(minion.getKey())) {
                            Vector3f minionOffset = Formation.getOffset(2, minion.getValue() + 3);
                            minion.getKey().updateLocation();
                            Vector3fImmutable formationPatrolPoint = new Vector3fImmutable(mob.destination.x + minionOffset.x, mob.destination.y, mob.destination.z + minionOffset.z);
                            MovementUtilities.aiMove(minion.getKey(), formationPatrolPoint, true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: randomGuardPatrolPoints" + " " + e.getMessage());
        }
    }

    public static AbstractWorldObject ChangeTargetFromHateValue(Mob mob) {

        try {

            float CurrentHateValue = 0;

            if (mob.getCombatTarget() != null && mob.getCombatTarget().getObjectType().equals(Enum.GameObjectType.PlayerCharacter))
                CurrentHateValue = ((PlayerCharacter) mob.getCombatTarget()).getHateValue();

            AbstractWorldObject mostHatedTarget = null;

            for (Entry playerEntry : mob.playerAgroMap.entrySet()) {

                PlayerCharacter potentialTarget = PlayerCharacter.getFromCache((int) playerEntry.getKey());

                if (potentialTarget.equals(mob.getCombatTarget()))
                    continue;

                if (potentialTarget != null && potentialTarget.getHateValue() > CurrentHateValue && MovementUtilities.inRangeToAggro(mob, potentialTarget)) {
                    CurrentHateValue = potentialTarget.getHateValue();
                    mostHatedTarget = potentialTarget;
                }

            }
            return mostHatedTarget;
        } catch (Exception e) {
            Logger.info(mob.getObjectUUID() + " " + mob.getName() + " Failed At: ChangeTargetFromMostHated" + " " + e.getMessage());
        }
        return null;
    }

    public static void RecoverHealth(Mob mob){
        //recover health

        if (mob.getTimestamps().containsKey("HEALTHRECOVERED") == false)
            mob.getTimestamps().put("HEALTHRECOVERED", System.currentTimeMillis());

        if (mob.isSit() && mob.getTimeStamp("HEALTHRECOVERED") < System.currentTimeMillis() + 3000)
            if (mob.getHealth() < mob.getHealthMax()) {

                float recoveredHealth = mob.getHealthMax() * ((1 + mob.getBonuses().getFloatPercentAll(Enum.ModType.HealthRecoverRate, Enum.SourceType.None)) * 0.01f);
                mob.setHealth(mob.getHealth() + recoveredHealth);
                mob.getTimestamps().put("HEALTHRECOVERED", System.currentTimeMillis());

                if (mob.getHealth() > mob.getHealthMax())
                    mob.setHealth(mob.getHealthMax());
            }
    }
}