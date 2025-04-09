package engine.mobileAI.MobHandlers;

import engine.Enum;
import engine.gameManager.PowersManager;
import engine.gameManager.ZoneManager;
import engine.math.Vector3fImmutable;
import engine.mobileAI.Threads.MobAIThread;
import engine.mobileAI.utilities.CombatUtilities;
import engine.mobileAI.utilities.MovementUtilities;
import engine.net.client.msg.PerformActionMsg;
import engine.objects.*;
import engine.powers.ActionsBase;
import engine.powers.PowersBase;
import engine.server.MBServerStatics;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static engine.Enum.MinionClass.*;

public class PlayerGuardHandler {
    public static void run(Mob guard) {
        if(!guard.isAlive() || guard.despawned){
            CheckRespawn(guard);
            return;
        }

        CheckForRecall(guard);

        if(guard.playerAgroMap.isEmpty())
            return;

        if(guard.getName().contains("Adept")){
            runMagisterGuard(guard);
        }else if(guard.getName().contains("Archer")){
            runWallArcherGuard(guard);
        }else{
            runMeleGuard(guard);
        }
    }

    public static void runMeleGuard(Mob guard){
        try {
            if (guard.combatTarget != null)
                checkToDropGuardAggro(guard);

            if (guard.combatTarget == null)
                CheckForPlayerGuardAggro(guard);

            if(MovementUtilities.canMove(guard))
                CheckGuardMovement(guard);

            if (guard.combatTarget != null && CombatUtilities.inRangeToAttack(guard, guard.combatTarget))
                CheckToAttack(guard);

        } catch (Exception ignored) {

        }
    }

    public static void runArcherGuard(Mob guard){
        try {
            if (guard.combatTarget != null)
                checkToDropGuardAggro(guard);

            if (guard.combatTarget == null)
                CheckForPlayerGuardAggro(guard);

            if(MovementUtilities.canMove(guard))
                CheckGuardMovement(guard);

            if (guard.combatTarget != null && CombatUtilities.inRangeToAttack(guard, guard.combatTarget))
                CheckToAttack(guard);

        } catch (Exception ignored) {

        }
    }

    public static void runMagisterGuard(Mob guard){
        try {
            if (guard.combatTarget != null)
                checkToDropGuardAggro(guard);

            if (guard.combatTarget == null)
                CheckForPlayerGuardAggro(guard);

            if(MovementUtilities.canMove(guard))
                CheckMagisterMovement(guard);

            if (guard.combatTarget != null && inRangeToCast(guard, guard.combatTarget))
                GuardCast(guard);

        } catch (Exception ignored) {

        }
    }

    public static void runWallArcherGuard(Mob guard){
        try {
            if (guard.combatTarget != null)
                checkToDropGuardAggro(guard);

            if (guard.combatTarget == null)
                CheckForPlayerGuardAggro(guard);

            if (guard.combatTarget != null && CombatUtilities.inRangeToAttack(guard, guard.combatTarget))
                CheckToAttack(guard);

        } catch (Exception ignored) {

        }
    }

    public static void checkToDropGuardAggro(Mob guard){
        if(guard.combatTarget.loc.distanceSquared(guard.loc) > (128f * 128f))
            guard.setCombatTarget(null);

        if(guard.combatTarget.getObjectType().equals(Enum.GameObjectType.PlayerCharacter) && !guard.canSee((PlayerCharacter)guard.combatTarget))
            guard.setCombatTarget(null);
    }

    public static void CheckForPlayerGuardAggro(Mob guard){
        PlayerCharacter tar = null;
        for(int id : guard.playerAgroMap.keySet()){
            PlayerCharacter target = PlayerCharacter.getFromCache(id);

            float aggroRange = guard.getAggroRange();

            if(guard.getEquip().get(2) != null && guard.getEquip().get(2).getItemBase().getRange() > 15)
                aggroRange = guard.getEquip().get(2).getItemBase().getRange() * 1.5f;

            float squared = aggroRange * aggroRange;

            if(target.loc.distanceSquared(guard.loc) > squared)
                continue;

            if(tar == null || guard.loc.distanceSquared(tar.loc) < guard.loc.distanceSquared(target.loc))
                if(GuardCanAggro(guard,target))
                    tar = target;
        }
        if(tar != null)
            guard.setCombatTarget(tar);
    }

    public static Boolean GuardCanAggro(Mob guard, PlayerCharacter loadedPlayer){
        if (loadedPlayer == null)
            return false;

        //Player is Dead, Mob no longer needs to attempt to aggro. Remove them from aggro map.
        if (!loadedPlayer.isAlive())
            return false;

        //Can't see target, skip aggro.
        if (!guard.canSee(loadedPlayer))
            return false;

        if(guard.guardedCity != null && guard.guardedCity.cityOutlaws.contains(loadedPlayer.getObjectUUID()))
            return true;

        if(loadedPlayer.guild.getNation().equals(guard.guardedCity.getGuild().getNation()))
            return false;

        return !guard.guardedCity.isOpen();
    }

    public static void GuardCast(Mob mob) {

        if(mob.nextCastTime > System.currentTimeMillis())
            return;

        try {
            // Method picks a random spell from a mobile's list of powers
            // and casts it on the current target (or itself).  Validation
            // (including empty lists) is done previously within canCast();

            if(mob.combatTarget.getObjectType().equals(Enum.GameObjectType.PlayerCharacter) && !mob.canSee((PlayerCharacter)mob.combatTarget))
                return;

            int powerToken = 429757701;//mage bolt

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
            mob.nextCastTime = (long) (System.currentTimeMillis() + 15000);

            if (mobPower.requiresHitRoll)
                if (CombatUtilities.triggerDefense(mob, mob.getCombatTarget()))
                    return;

            // Cast the spell

            PerformActionMsg msg;
            PowersManager.useMobPower(mob, (AbstractCharacter) mob.combatTarget, mobPower, powerRank);
            msg = PowersManager.createPowerMsg(mobPower, powerRank, mob, (AbstractCharacter) mob.combatTarget);
            msg.setUnknown04(2);
            PowersManager.finishUseMobPower(msg, mob, 0, 0);
        } catch (Exception e) {
            ////(mob.getObjectUUID() + " " + mob.getName() + " Failed At: MobCast" + " " + e.getMessage());
        }
    }

    public static void CheckToAttack(Mob guard){
        try {

            if(guard.getLastAttackTime() > System.currentTimeMillis())
                return;

            PlayerCharacter target = (PlayerCharacter) guard.combatTarget;

            if (!guard.canSee(target)) {
                guard.setCombatTarget(null);
                return;
            }

            if (guard.isMoving() && guard.getRange() > 20)
                return;

            if(!CombatUtilities.inRangeToAttack(guard,guard.combatTarget)) {
                return;
            }

            if(target.combatStats == null)
                target.combatStats = new PlayerCombatStats(target);

            ItemBase mainHand = guard.getWeaponItemBase(true);
            ItemBase offHand = guard.getWeaponItemBase(false);

            if (mainHand == null && offHand == null) {
                CombatUtilities.combatCycle(guard, target, true, null);
                int delay = 3000;
                guard.setLastAttackTime(System.currentTimeMillis() + delay);
            } else if (guard.getWeaponItemBase(true) != null) {
                int delay = 3000;
                CombatUtilities.combatCycle(guard, target, true, guard.getWeaponItemBase(true));
                guard.setLastAttackTime(System.currentTimeMillis() + delay);
            } else if (guard.getWeaponItemBase(false) != null) {
                int attackDelay = 3000;
                CombatUtilities.combatCycle(guard, target, false, guard.getWeaponItemBase(false));
                guard.setLastAttackTime(System.currentTimeMillis() + attackDelay);
            }

            if (target.getPet() != null)
                if (target.getPet().getCombatTarget() == null && target.getPet().assist)
                    target.getPet().setCombatTarget(guard);

        } catch (Exception e) {
            ////(guard.getObjectUUID() + " " + guard.getName() + " Failed At: AttackPlayer" + " " + e.getMessage());
        }
    }

    public static void CheckGuardMovement(Mob guard){

        if(guard.isMoving())
            guard.updateLocation();

        if (guard.getCombatTarget() == null) {
            if (!guard.isMoving()) {
                Patrol(guard);
            }else {
                guard.stopPatrolTime = System.currentTimeMillis();
            }
        } else {
            if (!CombatUtilities.inRangeToAttack(guard, guard.combatTarget)) {
                MovementUtilities.moveToLocation(guard, guard.combatTarget.loc, guard.getRange());
            }
        }
    }

    private static void Patrol(Mob mob) {

        try {

            if(mob.isMoving()) {
                mob.stopPatrolTime = System.currentTimeMillis();
                return;
            }

            if(mob.building != null)
                mob.patrolPoints = mob.building.patrolPoints;
            //make sure mob is out of combat stance

            int patrolDelay = ThreadLocalRandom.current().nextInt((int) (MobAIThread.AI_PATROL_DIVISOR * 0.5f), MobAIThread.AI_PATROL_DIVISOR) + MobAIThread.AI_PATROL_DIVISOR;

            //early exit while waiting to patrol again

            if (mob.stopPatrolTime + (patrolDelay * 1000L) > System.currentTimeMillis())
                return;

            if (mob.lastPatrolPointIndex > mob.patrolPoints.size() - 1)
                mob.lastPatrolPointIndex = 0;

            mob.destination = Vector3fImmutable.getRandomPointOnCircle(mob.patrolPoints.get(mob.lastPatrolPointIndex),16f);
            mob.lastPatrolPointIndex += 1;

            MovementUtilities.aiMove(mob, mob.destination, true);

        } catch (Exception e) {
            ////(mob.getObjectUUID() + " " + mob.getName() + " Failed At: AttackTarget" + " " + e.getMessage());
        }
    }

    private static boolean inRangeToCast(Mob guard, AbstractWorldObject target){
        float castRangeSquared = 50f * 50f;
        float rangeSquared = guard.loc.distanceSquared(target.loc);
        return castRangeSquared >= rangeSquared;
    }

    public static void CheckMagisterMovement(Mob guard){
        if (guard.getCombatTarget() == null) {
            if (!guard.isMoving())
                Patrol(guard);
            else {
                guard.stopPatrolTime = System.currentTimeMillis();
            }
        } else {
            if(guard.isMoving()){
                float desiredRangeSquared = 40f * 40f;
                float distance = guard.loc.distanceSquared(guard.combatTarget.loc);
                if(distance <= desiredRangeSquared){
                    guard.stopMovement(guard.getMovementLoc());
                }
            }else {
                if(!inRangeToCast(guard,guard.combatTarget))
                    MovementUtilities.moveToLocation(guard, guard.combatTarget.loc.moveTowards(guard.loc,25), 0);
                else
                    guard.stopMovement(guard.getMovementLoc());
            }
        }
    }

    public static void CheckForRecall(Mob guard){

        if(guard.guardedCity == null){
            City guarded = ZoneManager.getCityAtLocation(guard.bindLoc);
            if( guarded != null){
                guard.guardedCity = guarded;
            }
        }
        if(guard.guardedCity != null && guard.loc.distanceSquared(guard.guardedCity.loc) > (800 * 800)) {

            PowersBase recall = PowersManager.getPowerByToken(-1994153779);
            PowersManager.useMobPower(guard, guard, recall, 40);

            for (Map.Entry playerEntry : guard.playerAgroMap.entrySet())
                PlayerCharacter.getFromCache((int) playerEntry.getKey()).setHateValue(0);

            guard.setCombatTarget(null);
        }
    }

    public static void CheckRespawn(Mob guard){
        if(!guard.despawned)
            guard.despawn();

        long respawnTime = 0L;

        Building barracks = guard.building;
        if(barracks == null){
            respawnTime = MBServerStatics.FIFTEEN_MINUTES;
        }else{
            switch(barracks.getRank()) {
                case 2:
                    respawnTime = 750000; // 12.5 minutes
                    break;
                case 3:
                    respawnTime = 660000; // 11 minutes
                    break;
                case 4:
                    respawnTime = 570000; // 9.5 minutes
                    break;
                case 5:
                    respawnTime = 480000; // 8 minutes
                    break;
                case 6:
                    respawnTime = 450000; // 6.5 minutes
                    break;
                case 7:
                    respawnTime = MBServerStatics.FIVE_MINUTES;
                    break;
                default:
                    respawnTime = MBServerStatics.FIFTEEN_MINUTES;
                    break;
            }

        }

        if(guard.deathTime + respawnTime < System.currentTimeMillis()){
            if (!Zone.respawnQue.contains(guard)) {
                Zone.respawnQue.add(guard);
            }
        }
    }
}
