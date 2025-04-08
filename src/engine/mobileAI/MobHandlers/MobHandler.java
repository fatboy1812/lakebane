package engine.mobileAI.MobHandlers;

import engine.InterestManagement.InterestManager;
import engine.InterestManagement.WorldGrid;
import engine.gameManager.PowersManager;
import engine.mobileAI.Threads.MobAIThread;
import engine.mobileAI.utilities.CombatUtilities;
import engine.mobileAI.utilities.MovementUtilities;
import engine.objects.*;
import engine.powers.PowersBase;
import engine.server.MBServerStatics;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class MobHandler {
    public static void run(Mob mob){

        if (!mob.isAlive()) {
            CheckForRespawn(mob);
            return;
        }

        if(mob.playerAgroMap.isEmpty())
            return;

        CheckToSendMobHome(mob);

        if(mob.combatTarget == null || !mob.combatTarget.isAlive()){
            CheckForAggro(mob);
        }
        if(mob.combatTarget != null)
            CheckToDropAggro(mob);

        if(MovementUtilities.canMove(mob))
            CheckMobMovement(mob);

        if(mob.combatTarget != null && CombatUtilities.inRangeToAttack(mob,mob.combatTarget))
            CheckToAttack(mob);
    }

    public static void CheckToDropAggro(Mob mob){
        if(mob.loc.distanceSquared(mob.combatTarget.loc) > (128f * 128f))
            mob.setCombatTarget(null);
    }

    public static void CheckForRespawn(Mob mob){
        try {

            if (mob.deathTime == 0) {
                mob.setDeathTime(System.currentTimeMillis());
                return;
            }

            //handles checking for respawn of dead mobs even when no players have mob loaded
            //Despawn Timer with Loot currently in inventory.

            if (!mob.despawned) {

                if (mob.getCharItemManager().getInventoryCount() > 0) {
                    if (System.currentTimeMillis() > mob.deathTime + MBServerStatics.DESPAWN_TIMER_WITH_LOOT) {
                        mob.despawn();
                        return;
                    }
                    //No items in inventory.
                } else if (mob.isHasLoot()) {
                    if (System.currentTimeMillis() > mob.deathTime + MBServerStatics.DESPAWN_TIMER_ONCE_LOOTED) {
                        mob.despawn();
                        return;
                    }
                    //Mob never had Loot.
                } else {
                    if (System.currentTimeMillis() > mob.deathTime + MBServerStatics.DESPAWN_TIMER_ONCE_LOOTED) {
                        mob.despawn();
                        return;
                    }
                }
                return;
            }

            if(Mob.discDroppers.contains(mob))
                return;

            if (System.currentTimeMillis() > (mob.deathTime + (mob.spawnTime * 1000L))) {
                if (!Zone.respawnQue.contains(mob)) {
                    if(mob.isSiege()){
                        mob.respawn();
                        WorldGrid.loadObject(mob);
                        //InterestManager.forceLoad(mob);
                    }else {
                        Zone.respawnQue.add(mob);
                    }
                }
            }
        } catch (Exception e) {
            //(aiAgent.getObjectUUID() + " " + aiAgent.getName() + " Failed At: CheckForRespawn" + " " + e.getMessage());
        }
    }

    public static void CheckForAggro(Mob mob){
        switch(mob.BehaviourType){
            case SimpleStandingGuard:
            case Simple:
            case None:
                return;
        }
        PlayerCharacter tar = null;
        for(int id : mob.playerAgroMap.keySet()){
            PlayerCharacter target = PlayerCharacter.getFromCache(id);

            if(target.loc.distanceSquared(mob.loc) > mob.getAggroRange() * mob.getAggroRange())
                continue;

            if(tar == null || mob.loc.distanceSquared(tar.loc) < mob.loc.distanceSquared(target.loc))
                if(MobCanAggro(mob,target))
                    tar = target;
        }
        if(tar != null)
            mob.setCombatTarget(tar);
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

        // No aggro for this race type
        if (mob.notEnemy != null && mob.notEnemy.size() > 0 && mob.notEnemy.contains(loadedPlayer.getRace().getRaceType().getMonsterType()))
            return false;

        //mob has enemies and this player race is not it
        if (mob.enemy != null && mob.enemy.size() > 0 && !mob.enemy.contains(loadedPlayer.getRace().getRaceType().getMonsterType()))
            return false;

        return true;
    }

    public static void CheckMobMovement(Mob mob){
        if(!mob.isAlive())
            return;

        mob.updateLocation();

        if(mob.combatTarget == null){
            //patrol
            Patrol(mob);
        }else{
            //combat movement
            if(CombatUtilities.inRange2D(mob,mob.combatTarget,mob.getRange())) {
                return;
            }else {
                MovementUtilities.moveToLocation(mob, mob.combatTarget.loc, mob.getRange());
            }
        }
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

            //if (mob.BehaviourType.callsForHelp)
            MobCallForHelp(mob);

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

    public static void MobCallForHelp(Mob mob) {

        try {
            HashSet<AbstractWorldObject> helpers = WorldGrid.getObjectsInRangePartial(mob.loc,mob.getAggroRange() * 2, MBServerStatics.MASK_MOB);
            for (AbstractWorldObject awo : helpers) {
                if(awo.equals(mob))
                    continue;
                Mob helper = (Mob) awo;

                if(helper.equals(mob))
                    continue;

                if(helper.combatTarget != null)
                    continue;

                helper.setCombatTarget(mob.getCombatTarget());
            }

        } catch (Exception e) {
            //(mob.getObjectUUID() + " " + mob.getName() + " Failed At: MobCallForHelp" + " " + e.getMessage());
        }
    }

    private static void Patrol(Mob mob) {

        try {

            if(mob.isMoving()) {
                mob.stopPatrolTime = System.currentTimeMillis();
                return;
            }
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

    private static void CheckToSendMobHome(Mob mob) {

        if(mob.isNecroPet())
            return;

        try {

            if (!MovementUtilities.inRangeOfBindLocation(mob)) {

                PowersBase recall = PowersManager.getPowerByToken(-1994153779);
                PowersManager.useMobPower(mob, mob, recall, 40);

                for (Map.Entry playerEntry : mob.playerAgroMap.entrySet())
                    PlayerCharacter.getFromCache((int) playerEntry.getKey()).setHateValue(0);

                mob.setCombatTarget(null);
            }
        } catch (Exception e) {
            //(mob.getObjectUUID() + " " + mob.getName() + " Failed At: CheckToSendMobHome" + " " + e.getMessage());
        }
    }

}
