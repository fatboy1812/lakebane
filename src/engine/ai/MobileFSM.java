// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com
package engine.ai;
import engine.Enum.DispatchChannel;
import engine.InterestManagement.WorldGrid;
import engine.ai.utilities.CombatUtilities;
import engine.ai.utilities.MovementUtilities;
import engine.gameManager.*;
import engine.net.DispatchMessage;
import engine.net.client.msg.PerformActionMsg;
import engine.net.client.msg.PowerProjectileMsg;
import engine.net.client.msg.UpdateStateMsg;
import engine.objects.*;
import engine.powers.ActionsBase;
import engine.powers.PowersBase;
import engine.server.MBServerStatics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import static engine.math.FastMath.sqr;
public class MobileFSM {
    public enum MobBehaviourType {
        //Power
        Power(null, false, true, true, true, false),
        PowerHelpee(Power, false, true, true, false, true),
        PowerHelpeeWimpy(Power, true, false, true, false, false),
        PowerGrouperWimpy(Power, true, false, true, false, false),
        PowerAggro(Power, false, true, true, false, true),
        PowerAggroHelpee(Power, false, true, true, false, true),
        //Aggro
        Aggro(null, false, true, true, true, false),
        AggroHelpee(Aggro, false, true, true, false, true),
        AggroHelpeeWimpy(Aggro, true, false, true, false, false),
        AggroGrouperWimpy(Aggro, true, false, true, false, false),
        //Spell
        Spell(null, false, true, true, true, false),
        SpellHelpee(Spell, false, true, true, false, true),
        SpellHelpeeWimpy(Spell, true, false, true, false, false),
        SpellGrouperWimpy(Spell, true, false, true, false, false),
        SpellAggro(Spell, false, true, true, false, true),
        SpellAggroHelpee(Spell, false, true, true, false, true),
        SpellAggroHelpeeWimpy(Spell, true, false, true, false, false),
        SpellAggroHelpeeEpic(Spell, false, true, true, false, true),
        SpellAggroGrouperWimpy(Spell, true, false, true, false, false),
        //Independent Types
        SimpleStandingGuard(null, false, false, false, false, false),
        Pet1(null, false, false, true, false, false),
        Simple(null, false, false, true, false, false),
        Helpee(null, false, true, true, false, true),
        HelpeeWimpy(null, true, false, true, false, false),
        None(null, false, false, false, false, false),
        GuardCaptain(null, false, true, true, true, false),
        GuardMinion(GuardCaptain, false, true, true, false, true);

        private static HashMap<Integer, MobBehaviourType> _behaviourTypes = new HashMap<>();
        public MobBehaviourType BehaviourHelperType;
        public boolean isWimpy;
        public boolean isAgressive;
        public boolean canRoam;
        public boolean callsForHelp;
        public boolean respondsToCallForHelp;

        MobBehaviourType(MobBehaviourType helpeebehaviourType, boolean wimpy, boolean agressive, boolean canroam, boolean callsforhelp, boolean respondstocallforhelp) {
            this.BehaviourHelperType = helpeebehaviourType;
            this.isWimpy = wimpy;
            this.isAgressive = agressive;
            this.canRoam = canroam;
            this.callsForHelp = callsforhelp;
            this.respondsToCallForHelp = respondstocallforhelp;
        }
    }

    private static void mobAttack(Mob aiAgent) {

        AbstractGameObject target = aiAgent.getCombatTarget();
        if (target == null) {
            return;
        }
        switch (target.getObjectType()) {
            case PlayerCharacter:
                PlayerCharacter player = (PlayerCharacter) target;
                if (!player.isActive()) {
                    aiAgent.setCombatTarget(null);
                    CheckMobMovement(aiAgent);
                    return;
                }
                if (aiAgent.isNecroPet() && player.inSafeZone()) {
                    aiAgent.setCombatTarget(null);
                    return;
                }
                if (canCast(aiAgent) == true) {
                    if (MobCast(aiAgent) == false) {
                        handlePlayerAttackForMob(aiAgent, player);
                    }
                } else {
                    handlePlayerAttackForMob(aiAgent, player);
                }
                break;
            case Building:
                Building building = (Building) target;
                petHandleBuildingAttack(aiAgent, building);
                break;
            case Mob:
                Mob mob = (Mob) target;
                handleMobAttackForMob(aiAgent, mob);
        }
    }

    private static void petHandleBuildingAttack(Mob aiAgent, Building building) {

        int buildingHitBox = (int) CombatManager.calcHitBox(building);

        if (building.getRank() == -1) {
            aiAgent.setCombatTarget(null);
            return;
        }

        if (!building.isVulnerable()) {
            aiAgent.setCombatTarget(null);
            return;
        }

        if (BuildingManager.getBuildingFromCache(building.getObjectUUID()) == null) {
            aiAgent.setCombatTarget(null);
            return;
        }

        if (building.getParentZone() != null && building.getParentZone().isPlayerCity()) {

            for (Mob mob : building.getParentZone().zoneMobSet) {

                if (!mob.isPlayerGuard())
                    continue;

                if (mob.getCombatTarget() != null)
                    continue;

                if (mob.getGuild() != null && building.getGuild() != null)
                    if (!Guild.sameGuild(mob.getGuild().getNation(), building.getGuild().getNation()))
                        continue;

                mob.setCombatTarget(aiAgent);
            }
        }

        if (CombatUtilities.inRangeToAttack(aiAgent, building)) {
            //not time to attack yet.

            if (!CombatUtilities.RunAIRandom())
                return;

            if (System.currentTimeMillis() < aiAgent.getLastAttackTime())
                return;

            if (aiAgent.getRange() >= 30 && aiAgent.isMoving())
                return;

            //reset attack animation
            if (aiAgent.isSiege())
                MovementManager.sendRWSSMsg(aiAgent);

            //			Fire siege balls
            //			 TODO: Fix animations not following stone

            //no weapons, defualt mob attack speed 3 seconds.
            ItemBase mainHand = aiAgent.getWeaponItemBase(true);
            ItemBase offHand = aiAgent.getWeaponItemBase(false);

            if (mainHand == null && offHand == null) {

                CombatUtilities.combatCycle(aiAgent, building, true, null);
                int delay = 3000;

                if (aiAgent.isSiege())
                    delay = 15000;

                aiAgent.setLastAttackTime(System.currentTimeMillis() + delay);
            } else
                //TODO set offhand attack time.
                if (aiAgent.getWeaponItemBase(true) != null) {

                    int attackDelay = 3000;

                    if (aiAgent.isSiege())
                        attackDelay = 15000;

                    CombatUtilities.combatCycle(aiAgent, building, true, aiAgent.getWeaponItemBase(true));
                    aiAgent.setLastAttackTime(System.currentTimeMillis() + attackDelay);

                } else if (aiAgent.getWeaponItemBase(false) != null) {

                    int attackDelay = 3000;

                    if (aiAgent.isSiege())
                        attackDelay = 15000;

                    CombatUtilities.combatCycle(aiAgent, building, false, aiAgent.getWeaponItemBase(false));
                    aiAgent.setLastAttackTime(System.currentTimeMillis() + attackDelay);
                }

            if (aiAgent.isSiege()) {
                PowerProjectileMsg ppm = new PowerProjectileMsg(aiAgent, building);
                ppm.setRange(50);
                DispatchMessage.dispatchMsgToInterestArea(aiAgent, ppm, DispatchChannel.SECONDARY, MBServerStatics.CHARACTER_LOAD_RANGE, false, false);
            }
            return;
        }

        //Outside of attack Range, Move to players predicted loc.

        if (!aiAgent.isMoving())
            if (MovementUtilities.canMove(aiAgent))
                MovementUtilities.moveToLocation(aiAgent, building.getLoc(), aiAgent.getRange() + buildingHitBox);
    }

    private static void handlePlayerAttackForMob(Mob aiAgent, PlayerCharacter player) {

        if (aiAgent.getMobBase().getSeeInvis() < player.getHidden()) {
            aiAgent.setCombatTarget(null);
            return;
        }

        if (!player.isAlive()) {
            aiAgent.setCombatTarget(null);
            return;
        }

        if (aiAgent.BehaviourType.callsForHelp) {
            MobCallForHelp(aiAgent);
        }
        if (!MovementUtilities.inRangeDropAggro(aiAgent, player)) {
            aiAgent.setAggroTargetID(0);
            aiAgent.setCombatTarget(null);
            MovementUtilities.moveToLocation(aiAgent, aiAgent.getTrueBindLoc(), 0);
            return;
        }
        if (CombatUtilities.inRange2D(aiAgent, player, aiAgent.getRange())) {

            //no weapons, defualt mob attack speed 3 seconds.

            if (System.currentTimeMillis() < aiAgent.getLastAttackTime())
                return;

            //if (!CombatUtilities.RunAIRandom())
            //    return;

            // ranged mobs cant attack while running. skip until they finally stop.
            //if (aiAgent.getRange() >= 30 && aiAgent.isMoving())
            if (aiAgent.isMoving())
                return;

            // add timer for last attack.
            //	player.setTimeStamp("LastCombatPlayer", System.currentTimeMillis());
            ItemBase mainHand = aiAgent.getWeaponItemBase(true);
            ItemBase offHand = aiAgent.getWeaponItemBase(false);

            if (mainHand == null && offHand == null) {

                CombatUtilities.combatCycle(aiAgent, player, true, null);
                int delay = 3000;

                if (aiAgent.isSiege())
                    delay = 11000;

                aiAgent.setLastAttackTime(System.currentTimeMillis() + delay);

            } else
                //TODO set offhand attack time.
                if (aiAgent.getWeaponItemBase(true) != null) {

                    int delay = 3000;

                    if (aiAgent.isSiege())
                        delay = 11000;

                    CombatUtilities.combatCycle(aiAgent, player, true, aiAgent.getWeaponItemBase(true));
                    aiAgent.setLastAttackTime(System.currentTimeMillis() + delay);
                } else if (aiAgent.getWeaponItemBase(false) != null) {

                    int attackDelay = 3000;

                    if (aiAgent.isSiege())
                        attackDelay = 11000;
                    if (aiAgent.BehaviourType.callsForHelp) {
                        MobCallForHelp(aiAgent);
                    }
                    CombatUtilities.combatCycle(aiAgent, player, false, aiAgent.getWeaponItemBase(false));
                    aiAgent.setLastAttackTime(System.currentTimeMillis() + attackDelay);
                }
            return;
        }

        if (!MovementUtilities.updateMovementToCharacter(aiAgent, player))
            return;

        if (!MovementUtilities.canMove(aiAgent))
            return;

        //this stops mobs from attempting to move while they are underneath a player.
        if (CombatUtilities.inRangeToAttack2D(aiAgent, player))
            return;

    }

    private static void handleMobAttackForMob(Mob aiAgent, Mob mob) {


        if (!mob.isAlive()) {
            aiAgent.setCombatTarget(null);
            return;
        }

        if (CombatUtilities.inRangeToAttack(aiAgent, mob)) {
            //not time to attack yet.
            if (System.currentTimeMillis() < aiAgent.getLastAttackTime()) {
                return;
            }

            if (!CombatUtilities.RunAIRandom())
                return;

            if (aiAgent.getRange() >= 30 && aiAgent.isMoving())
                return;
            //no weapons, defualt mob attack speed 3 seconds.
            ItemBase mainHand = aiAgent.getWeaponItemBase(true);
            ItemBase offHand = aiAgent.getWeaponItemBase(false);

            if (mainHand == null && offHand == null) {

                CombatUtilities.combatCycle(aiAgent, mob, true, null);
                int delay = 3000;

                if (aiAgent.isSiege())
                    delay = 11000;

                aiAgent.setLastAttackTime(System.currentTimeMillis() + delay);
            } else
                //TODO set offhand attack time.
                if (aiAgent.getWeaponItemBase(true) != null) {

                    int attackDelay = 3000;

                    if (aiAgent.isSiege())
                        attackDelay = 11000;

                    CombatUtilities.combatCycle(aiAgent, mob, true, aiAgent.getWeaponItemBase(true));
                    aiAgent.setLastAttackTime(System.currentTimeMillis() + attackDelay);

                } else if (aiAgent.getWeaponItemBase(false) != null) {

                    int attackDelay = 3000;

                    if (aiAgent.isSiege())
                        attackDelay = 11000;

                    CombatUtilities.combatCycle(aiAgent, mob, false, aiAgent.getWeaponItemBase(false));
                    aiAgent.setLastAttackTime(System.currentTimeMillis() + attackDelay);
                }
            return;
        }

        //use this so mobs dont continue to try to move if they are underneath a flying target. only use 2D range check.
        if (CombatUtilities.inRangeToAttack2D(aiAgent, mob))
            return;

        if (!MovementUtilities.updateMovementToCharacter(aiAgent, mob))
            return;
    }

    private static void patrol(Mob mob) {
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
        //make sure mob is out of combat stance
        if (mob.isCombat() && mob.getCombatTarget() == null) {
            mob.setCombat(false);
            UpdateStateMsg rwss = new UpdateStateMsg();
            rwss.setPlayer(mob);
            DispatchMessage.sendToAllInRange(mob, rwss);
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
            MovementUtilities.aiMove(mob, mob.patrolPoints.get(mob.lastPatrolPointIndex), true);
            mob.lastPatrolPointIndex += 1;
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
        if(mob.BehaviourType.ordinal() == MobBehaviourType.GuardCaptain.ordinal()){
            //this is a player slotted guard captain
            GuardCaptainLogic(mob);
            return;
        }
        if (mob.isPet() == false && mob.isSummonedPet() == false && mob.isNecroPet() == false) {
            if (mob.isAlive() == false) {
                //no need to continue if mob is dead, check for respawn and move on
                CheckForRespawn(mob);
                return;
            }
            //check to see if mob has wandered too far from his bind loc
            CheckToSendMobHome(mob);
            //check to see if players have mob loaded
            if (mob.playerAgroMap.isEmpty()) {
                //no players loaded, no need to proceed
                return;
            }
            //check for players that can be aggroed if mob is agressive and has no target
            if (mob.BehaviourType.isAgressive && mob.getCombatTarget() == null && mob.BehaviourType != MobBehaviourType.SimpleStandingGuard) {
                //normal aggro
                CheckForAggro(mob);
            } else if (mob.BehaviourType == MobBehaviourType.SimpleStandingGuard) {
                //safehold guard
                SafeGuardAggro(mob);
            }
            //check if mob can move for patrol or moving to target
            if (mob.BehaviourType.canRoam) {
                CheckMobMovement(mob);
            }
            //check if mob can attack if it isn't wimpy
            if (!mob.BehaviourType.isWimpy && !mob.isMoving() && mob.combatTarget != null) {
                CheckForAttack(mob);
            }
        } else {
            CheckMobMovement(mob);
            CheckForAttack(mob);
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
        mob.updateLocation();
        if (mob.isPet() == false && mob.isSummonedPet() == false && mob.isNecroPet() == false) {

            if (mob.getCombatTarget() == null) {
                patrol(mob);
            } else {
                chaseTarget(mob);
            }
        } else {
            //pet logic
            if (mob.playerAgroMap.containsKey(mob.getOwner().getObjectUUID()) == false) {
                //mob no longer has its owner loaded, translocate pet to owner
                MovementManager.translocate(mob, mob.getOwner().getLoc(), null);
            }
            if (mob.getCombatTarget() == null || mob.combatTarget.isAlive() == false) {
                //move back to owner
                if (CombatUtilities.inRange2D(mob, mob.getOwner(), 10) == false) {
                    mob.destination = mob.getOwner().getLoc();
                    MovementUtilities.moveToLocation(mob, mob.destination, 5);
                } else {
                    chaseTarget(mob);
                }
            }
        }
    }

    private static void CheckForRespawn(Mob aiAgent) {
        //handles checking for respawn of dead mobs even when no players have mob loaded
        //Despawn Timer with Loot currently in inventory.
        if (aiAgent.getCharItemManager().getInventoryCount() > 0) {
            if (System.currentTimeMillis() > aiAgent.deathTime + MBServerStatics.DESPAWN_TIMER_WITH_LOOT) {
                aiAgent.despawn();
                //update time of death after mob despawns so respawn time happens after mob despawns.
                if (aiAgent.deathTime != 0) {
                    aiAgent.setDeathTime(System.currentTimeMillis());
                }
                respawn(aiAgent);
            }

            //No items in inventory.
        } else {
            //Mob's Loot has been looted.
            if (aiAgent.isHasLoot()) {
                if (System.currentTimeMillis() > aiAgent.deathTime + MBServerStatics.DESPAWN_TIMER_ONCE_LOOTED) {
                    aiAgent.despawn();
                    //update time of death after mob despawns so respawn time happens after mob despawns.
                    if (aiAgent.deathTime != 0) {
                        aiAgent.setDeathTime(System.currentTimeMillis());
                    }
                    respawn(aiAgent);
                }
                //Mob never had Loot.
            } else {
                if (System.currentTimeMillis() > aiAgent.deathTime + MBServerStatics.DESPAWN_TIMER) {
                    aiAgent.despawn();
                    //update time of death after mob despawns so respawn time happens after mob despawns.
                    if (aiAgent.deathTime != 0) {
                        aiAgent.setDeathTime(System.currentTimeMillis());
                    }
                    respawn(aiAgent);
                }
            }
        }

    }

    public static void CheckForAttack(Mob mob) {
        //checks if mob can attack based on attack timer and range
        if (mob.isAlive())
            mob.updateLocation();
        if (!mob.isCombat()) {
            mob.setCombat(true);
            UpdateStateMsg rwss = new UpdateStateMsg();
            rwss.setPlayer(mob);
            DispatchMessage.sendToAllInRange(mob, rwss);
        }
        mobAttack(mob);
    }

    private static void CheckToSendMobHome(Mob mob) {
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

    private static void respawn(Mob aiAgent) {

        if (!aiAgent.canRespawn())
            return;

        long spawnTime = aiAgent.getSpawnTime();

        if (aiAgent.isPlayerGuard() && aiAgent.npcOwner != null && !aiAgent.npcOwner.isAlive())
            return;

        if (System.currentTimeMillis() > aiAgent.deathTime + spawnTime) {
            aiAgent.respawn();
            aiAgent.setCombatTarget(null);
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
        if (mob.playerAgroMap.isEmpty()) {
            //no players loaded, no need to proceed
            return;
        }
        if (mob.isAlive() == false) {
            //no need to continue if mob is dead, check for respawn and move on
            CheckForRespawn(mob);
            return;
        }
        CheckToSendMobHome(mob);
        CheckForPlayerGuardAggro(mob);
        CheckMobMovement(mob);
        CheckForAttack(mob);

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
                return;
            }
        }
    }
    private static Boolean GuardCanAggro(Mob mob, PlayerCharacter target){
        //first check condemn list for aggro allowed
        if(ZoneManager.getCityAtLocation(mob.building.getLoc()).getTOL().enforceKOS) {
            for (Entry<Integer, Condemned> entry : ZoneManager.getCityAtLocation(mob.building.getLoc()).getTOL().getCondemned().entrySet()) {
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
        if(!mob.building.getGuild().getNation().equals(target.getGuild().getNation())) {
            if (!mob.building.getGuild().getAllyList().contains(target.getGuild()) || !mob.building.getGuild().getAllyList().contains(target.getGuild().getNation())) {
                return true;
            }
        }
        return false;
    }
}