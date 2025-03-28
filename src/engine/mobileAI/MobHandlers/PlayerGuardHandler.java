package engine.mobileAI.MobHandlers;

import engine.gameManager.PowersManager;
import engine.mobileAI.Threads.MobAIThread;
import engine.mobileAI.utilities.CombatUtilities;
import engine.mobileAI.utilities.MovementUtilities;
import engine.net.client.msg.PerformActionMsg;
import engine.objects.*;
import engine.powers.ActionsBase;
import engine.powers.PowersBase;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerGuardHandler {
    public static void run(Mob guard) {
        try {
            if (guard.combatTarget != null) {
                checkToDropGuardAggro(guard);
            }

            if (guard.combatTarget == null)
                CheckForPlayerGuardAggro(guard);

            CheckGuardMovement(guard);

            if (guard.combatTarget != null && CombatUtilities.inRangeToAttack(guard, guard.combatTarget))
                CheckToAttack(guard);

        } catch (Exception ignored) {

        }
    }

    public static void checkToDropGuardAggro(Mob guard){
        if(guard.combatTarget.loc.distanceSquared(guard.loc) > (128f * 128f))
            guard.setCombatTarget(null);
    }

    public static void CheckForPlayerGuardAggro(Mob guard){
        PlayerCharacter tar = null;
        for(int id : guard.playerAgroMap.keySet()){
            PlayerCharacter target = PlayerCharacter.getFromCache(id);

            if(target.loc.distanceSquared(guard.loc) > guard.getAggroRange() * guard.getAggroRange())
                continue;

            if(tar == null || guard.loc.distanceSquared(tar.loc) < guard.loc.distanceSquared(target.loc))
                if(MobCanAggro(guard,target))
                    tar = target;
        }
        if(tar != null)
            guard.setCombatTarget(tar);
    }

    public static Boolean MobCanAggro(Mob mob, PlayerCharacter loadedPlayer){
        if (loadedPlayer == null)
            return false;

        //Player is Dead, Mob no longer needs to attempt to aggro. Remove them from aggro map.
        if (!loadedPlayer.isAlive())
            return false;

        //Can't see target, skip aggro.
        if (!mob.canSee(loadedPlayer))
            return false;

        if(mob.guardedCity != null && mob.guardedCity.cityOutlaws.contains(loadedPlayer.getObjectUUID()))
            return true;

        if(loadedPlayer.guild.getNation().equals(mob.guardedCity.getGuild().getNation()))
            return false;

        if(mob.guardedCity.isOpen())
            return false;

        return true;
    }

    public static boolean GuardCast(Mob mob) {

        try {
            // Method picks a random spell from a mobile's list of powers
            // and casts it on the current target (or itself).  Validation
            // (including empty lists) is done previously within canCast();

            ArrayList<Integer> powerTokens;
            ArrayList<Integer> purgeTokens;
            AbstractCharacter target = (AbstractCharacter) mob.getCombatTarget();

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
                return true;
            }
        } catch (Exception e) {
            ////(mob.getObjectUUID() + " " + mob.getName() + " Failed At: MobCast" + " " + e.getMessage());
        }
        return false;
    }

    public static void CheckToAttack(Mob mob){
        try {

            if(mob.getLastAttackTime() > System.currentTimeMillis())
                return;

            PlayerCharacter target = (PlayerCharacter) mob.combatTarget;

            if (!mob.canSee(target)) {
                mob.setCombatTarget(null);
                return;
            }

            if (mob.isMoving() && mob.getRange() > 20)
                return;

            if(target.combatStats == null)
                target.combatStats = new PlayerCombatStats(target);

            ItemBase mainHand = mob.getWeaponItemBase(true);
            ItemBase offHand = mob.getWeaponItemBase(false);

            if (mainHand == null && offHand == null) {
                CombatUtilities.combatCycle(mob, target, true, null);
                int delay = 3000;
                mob.setLastAttackTime(System.currentTimeMillis() + delay);
            } else if (mob.getWeaponItemBase(true) != null) {
                int delay = 3000;
                CombatUtilities.combatCycle(mob, target, true, mob.getWeaponItemBase(true));
                mob.setLastAttackTime(System.currentTimeMillis() + delay);
            } else if (mob.getWeaponItemBase(false) != null) {
                int attackDelay = 3000;
                CombatUtilities.combatCycle(mob, target, false, mob.getWeaponItemBase(false));
                mob.setLastAttackTime(System.currentTimeMillis() + attackDelay);
            }

            if (target.getPet() != null)
                if (target.getPet().getCombatTarget() == null && target.getPet().assist)
                    target.getPet().setCombatTarget(mob);

        } catch (Exception e) {
            ////(mob.getObjectUUID() + " " + mob.getName() + " Failed At: AttackPlayer" + " " + e.getMessage());
        }
    }

    public static void CheckGuardMovement(Mob mob){
        if (mob.getCombatTarget() == null) {
            if (!mob.isMoving())
                Patrol(mob);
            else {
                mob.stopPatrolTime = System.currentTimeMillis();
            }
        } else {
            MovementUtilities.moveToLocation(mob, mob.combatTarget.loc, mob.getRange());
        }
    }

    private static void Patrol(Mob mob) {

        try {

            if(mob.isMoving())
                return;
            //make sure mob is out of combat stance

            int patrolDelay = ThreadLocalRandom.current().nextInt((int) (MobAIThread.AI_PATROL_DIVISOR * 0.5f), MobAIThread.AI_PATROL_DIVISOR) + MobAIThread.AI_PATROL_DIVISOR;

            //early exit while waiting to patrol again

            if (mob.stopPatrolTime + (patrolDelay * 1000) > System.currentTimeMillis())
                return;

            if (mob.lastPatrolPointIndex > mob.patrolPoints.size() - 1)
                mob.lastPatrolPointIndex = 0;

            mob.destination = mob.patrolPoints.get(mob.lastPatrolPointIndex);
            mob.lastPatrolPointIndex += 1;

            MovementUtilities.aiMove(mob, mob.destination, true);

        } catch (Exception e) {
            ////(mob.getObjectUUID() + " " + mob.getName() + " Failed At: AttackTarget" + " " + e.getMessage());
        }
    }
}
