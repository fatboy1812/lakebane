// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com

package engine.gameManager;

import engine.Enum;
import engine.Enum.*;
import engine.exception.MsgSendException;
import engine.job.JobContainer;
import engine.job.JobScheduler;
import engine.jobs.AttackJob;
import engine.jobs.DeferredPowerJob;
import engine.math.Vector3fImmutable;
import engine.net.DispatchMessage;
import engine.net.client.ClientConnection;
import engine.net.client.msg.*;
import engine.objects.*;
import engine.powers.DamageShield;
import engine.powers.PowersBase;
import engine.powers.effectmodifiers.AbstractEffectModifier;
import engine.powers.effectmodifiers.WeaponProcEffectModifier;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static engine.math.FastMath.sqr;

public enum CombatManager {

    COMBATMANAGER;

    public static int animation = 0;

    /**
     * Message sent by player to attack something.
     */
    public static void setAttackTarget(AttackCmdMsg msg, ClientConnection origin) throws MsgSendException {

        PlayerCharacter player;
        int targetType;
        AbstractWorldObject target;

        if (TargetedActionMsg.un2cnt == 60 || TargetedActionMsg.un2cnt == 70)
            return;

        player = SessionManager.getPlayerCharacter(origin);

        if (player == null)
            return;

        //source must match player this account belongs to

        if (player.getObjectUUID() != msg.getSourceID() || player.getObjectType().ordinal() != msg.getSourceType()) {
            Logger.error("Msg Source ID " + msg.getSourceID() + " Does not Match Player ID " + player.getObjectUUID());
            return;
        }

        targetType = msg.getTargetType();

        if (targetType == GameObjectType.PlayerCharacter.ordinal()) {
            target = PlayerCharacter.getFromCache(msg.getTargetID());
        } else if (targetType == GameObjectType.Building.ordinal()) {
            target = BuildingManager.getBuildingFromCache(msg.getTargetID());
        } else if (targetType == GameObjectType.Mob.ordinal()) {
            target = Mob.getFromCache(msg.getTargetID());
        } else {
            player.setCombatTarget(null);
            return; //not valid type to attack
        }

        // quit of the combat target is already the current combat target
        // or there is no combat target

        if (target == null)
            return;

        //set sources target

        player.setCombatTarget(target);

        //put in combat if not already

        if (!player.isCombat())
            toggleCombat(true, origin);

        //make character stand if sitting

        if (player.isSit())
            toggleSit(false, origin);

        AttackTarget(player, target);

    }

    public static void AttackTarget(PlayerCharacter playerCharacter, AbstractWorldObject target) {

        boolean swingOffhand = false;

        //check my weapon can I do an offhand attack

        Item weaponOff = playerCharacter.getCharItemManager().getEquipped().get(MBServerStatics.SLOT_OFFHAND);
        Item weaponMain = playerCharacter.getCharItemManager().getEquipped().get(MBServerStatics.SLOT_MAINHAND);

        // if you carry something in the offhand thats a weapon you get to swing it

        if (weaponOff != null)
            if (weaponOff.getItemBase().getType().equals(ItemType.WEAPON))
                swingOffhand = true;

        // if you carry  nothing in either hand you get to swing your offhand

        if (weaponOff == null && weaponMain == null)
            swingOffhand = true;


        //we always swing our mainhand if we are not on timer

        JobContainer main = playerCharacter.getTimers().get("Attack" + MBServerStatics.SLOT_MAINHAND);

        // no timers on the mainhand, lets submit a job to swing

        if (main == null)
            CombatManager.createTimer(playerCharacter, MBServerStatics.SLOT_MAINHAND, 1, true); // attack in 0.1 of a second

			/*
            only swing offhand if we have a weapon in it or are unarmed in both hands
            and no timers running
			 */

        if (swingOffhand) {

            JobContainer off = playerCharacter.getTimers().get("Attack" + MBServerStatics.SLOT_OFFHAND);

            if (off == null)
                CombatManager.createTimer(playerCharacter, MBServerStatics.SLOT_OFFHAND, 1, true); // attack in 0.1 of a second
        }

        City playerCity = ZoneManager.getCityAtLocation(playerCharacter.getLoc());

        if (playerCity != null && playerCity.getGuild().getNation().equals(playerCharacter.getGuild().getNation()) == false && playerCity.cityOutlaws.contains(playerCharacter.getObjectUUID()) == false)
            playerCity.cityOutlaws.add(playerCharacter.getObjectUUID());
    }

    public static void setAttackTarget(PetAttackMsg msg, ClientConnection origin) throws MsgSendException {

        PlayerCharacter player;
        Mob pet;
        int targetType;
        AbstractWorldObject target;

        if (TargetedActionMsg.un2cnt == 60 || TargetedActionMsg.un2cnt == 70)
            return;

        player = SessionManager.getPlayerCharacter(origin);

        if (player == null)
            return;

        pet = player.getPet();

        if (pet == null)
            return;

        targetType = msg.getTargetType();

        if (targetType == GameObjectType.PlayerCharacter.ordinal())
            target = PlayerCharacter.getFromCache(msg.getTargetID());
        else if (targetType == GameObjectType.Building.ordinal())
            target = BuildingManager.getBuildingFromCache(msg.getTargetID());
        else if (targetType == GameObjectType.Mob.ordinal())
            target = Mob.getFromCache(msg.getTargetID());
        else {
            pet.setCombatTarget(null);
            return; //not valid type to attack
        }

        if (pet.equals(target))
            return;

        // quit of the combat target is already the current combat target
        // or there is no combat target

        if (target == null || target == pet.getCombatTarget())
            return;

        //set sources target

        pet.setCombatTarget(target);

        //put in combat if not already

        if (!pet.isCombat())
            pet.setCombat(true);

        //make character stand if sitting

        if (pet.isSit())
            toggleSit(false, origin);

    }

    private static void removeAttackTimers(AbstractCharacter ac) {

        JobContainer main;
        JobContainer off;

        if (ac == null)
            return;

        main = ac.getTimers().get("Attack" + MBServerStatics.SLOT_MAINHAND);
        off = ac.getTimers().get("Attack" + MBServerStatics.SLOT_OFFHAND);

        if (main != null)
            JobScheduler.getInstance().cancelScheduledJob(main);

        ac.getTimers().remove("Attack" + MBServerStatics.SLOT_MAINHAND);

        if (off != null)
            JobScheduler.getInstance().cancelScheduledJob(off);

        ac.getTimers().remove("Attack" + MBServerStatics.SLOT_OFFHAND);

        ac.setCombatTarget(null);

    }

    /**
     * Begin Attacking
     */
    public static void doCombat(AbstractCharacter ac, int slot) {

        int ret = 0;

        if (ac == null)
            return;

        // Attempt to eat null targets until we can clean
        // up this unholy mess and refactor it into a thread.

        ret = attemptCombat(ac, slot);

        //handle pets
        if (ret < 2 && ac.getObjectType().equals(GameObjectType.Mob)) {

            Mob mob = (Mob) ac;

            if (mob.isPet())
                return;
        }

        //ret values
        //0: not valid attack, fail attack
        //1: cannot attack, wrong hand
        //2: valid attack
        //3: cannot attack currently, continue checking

        if (ret == 0 || ret == 1) {

            //Could not attack, clear timer

            ConcurrentHashMap<String, JobContainer> timers = ac.getTimers();

            if (timers != null)
                timers.remove("Attack" + slot);

            //clear combat target if not valid attack

            if (ret == 0)
                ac.setCombatTarget(null);

        } else if (ret == 3) //Failed but continue checking. reset timer
            createTimer(ac, slot, 5, false);
    }

    /**
     * Verify can attack target
     */
    private static int attemptCombat(AbstractCharacter abstractCharacter, int slot) {

        if (abstractCharacter == null)
            return 0;

        try {

            //Make sure player can attack

            PlayerBonuses bonus = abstractCharacter.getBonuses();

            if (bonus != null && bonus.getBool(ModType.ImmuneToAttack, SourceType.None))
                return 0;

            AbstractWorldObject target = abstractCharacter.getCombatTarget();

            if (target == null)
                return 0;

            //pet to assist in attacking target
            if(abstractCharacter.getObjectType().equals(GameObjectType.PlayerCharacter)){
                PlayerCharacter attacker = (PlayerCharacter)abstractCharacter;
                if(attacker.combatStats == null){
                    attacker.combatStats = new PlayerCombatStats(attacker);
                }
                if(attacker.getPet() != null){
                    Mob pet = attacker.getPet();
                    if(pet.combatTarget == null && pet.assist)
                        pet.setCombatTarget(attacker.combatTarget);
                }

            }


            //target must be valid type

            if (AbstractWorldObject.IsAbstractCharacter(target)) {

                AbstractCharacter tar = (AbstractCharacter) target;

                //must be alive, attackable and in World

                if (!tar.isAlive())
                    return 0;
                else if (tar.isSafeMode())
                    return 0;
                else if (!tar.isActive())
                    return 0;

                if (target.getObjectType().equals(GameObjectType.PlayerCharacter) && abstractCharacter.getObjectType().equals(GameObjectType.PlayerCharacter) && abstractCharacter.getTimers().get("Attack" + slot) == null) {
                    if(((PlayerCharacter)target).combatStats == null){
                        ((PlayerCharacter)target).combatStats = new PlayerCombatStats(((PlayerCharacter)target));
                    }
                    if (!((PlayerCharacter) abstractCharacter).canSee((PlayerCharacter) target))
                        return 0;
                }
                //must not be immune to all or immune to attack

                Resists res = tar.getResists();
                bonus = tar.getBonuses();

                if (bonus != null && !bonus.getBool(ModType.NoMod, SourceType.ImmuneToAttack))
                    if (res != null)
                        if (res.immuneToAll() || res.immuneToAttacks())
                            return 0;
            } else if (target.getObjectType().equals(GameObjectType.Building)) {
                Building tar = (Building) target;

                // Cannot attack an invuln building

                if (tar.isVulnerable() == false)
                    return 0;

            } else
                return 0; //only characters and buildings may be attacked

            //source must be in world and alive

            if (!abstractCharacter.isActive())
                return 0;
            else if (!abstractCharacter.isAlive())
                return 0;

            //make sure source is in combat mode

            if (!abstractCharacter.isCombat())
                return 0;

            //See if either target is in safe zone

            if (abstractCharacter.getObjectType().equals(GameObjectType.PlayerCharacter) && target.getObjectType().equals(GameObjectType.PlayerCharacter))
                if (((PlayerCharacter) abstractCharacter).inSafeZone() || ((PlayerCharacter) target).inSafeZone())
                    return 0;

            if (!(slot == MBServerStatics.SLOT_MAINHAND || slot == MBServerStatics.SLOT_OFFHAND))
                return 0;

            if (abstractCharacter.getCharItemManager() == null)
                return 0;

            //get equippment

            ConcurrentHashMap<Integer, Item> equipped = abstractCharacter.getCharItemManager().getEquipped();
            boolean hasNoWeapon = false;

            if (equipped == null)
                return 0;

            //get Weapon

            boolean isWeapon = true;
            Item weapon = equipped.get(slot);
            ItemBase wb = null;

            if (weapon == null)
                isWeapon = false;
            else {
                ItemBase ib = weapon.getItemBase();

                if (ib == null || !ib.getType().equals(ItemType.WEAPON))
                    isWeapon = false;
                else
                    wb = ib;
            }

            //no weapon, see if other hand has a weapon

            if (!isWeapon)
                if (slot == MBServerStatics.SLOT_MAINHAND) {

                    //make sure offhand has weapon, not shield

                    Item weaponOff = equipped.get(MBServerStatics.SLOT_OFFHAND);

                    if (weaponOff != null) {
                        ItemBase ib = weaponOff.getItemBase();

                        if (ib == null || !ib.getType().equals(ItemType.WEAPON))
                            hasNoWeapon = true;
                        else
                            return 1; //no need to attack with this hand

                    } else
                        hasNoWeapon = true;

                } else if (equipped.get(MBServerStatics.SLOT_MAINHAND) == null)
                    return 1; //no need to attack with this hand

            //Source can attack.
            //NOTE Don't 'return;' beyond this point until timer created

            boolean attackFailure = (wb != null) && (wb.getRange() > 35f) && abstractCharacter.isMoving();

            //Target can't attack on move with ranged weapons.
            //if not enough stamina, then skip attack

            if (wb == null) {
                if (abstractCharacter.getStamina() < 1)
                    attackFailure = true;
            } else if (abstractCharacter.getStamina() < wb.getWeight())
                attackFailure = true;

            //see if attacker is stunned. If so, stop here

            bonus = abstractCharacter.getBonuses();

            if (bonus != null && bonus.getBool(ModType.Stunned, SourceType.None))
                attackFailure = true;

            //Get Range of weapon

            float range;

            if (hasNoWeapon)
                range = MBServerStatics.NO_WEAPON_RANGE;
            else
                range = getWeaponRange(wb, bonus);

            if (abstractCharacter.getObjectType() == GameObjectType.Mob) {

                Mob minion = (Mob) abstractCharacter;

                if (minion.isSiege())
                    range = 300f;
            }

            //Range check.

            if(abstractCharacter.isMoving()){
                range += (abstractCharacter.getSpeed() * 0.1f);
            }

            if(AbstractWorldObject.IsAbstractCharacter(target)) {
                AbstractCharacter tarAc = (AbstractCharacter) target;
                if(tarAc != null && tarAc.isMoving()){
                    range += (tarAc.getSpeed() * 0.1f);
                }
            }

            range += 2;
            if (NotInRange(abstractCharacter, target, range)) {

                //target is in stealth and can't be seen by source

                if (target.getObjectType().equals(GameObjectType.PlayerCharacter) && abstractCharacter.getObjectType().equals(GameObjectType.PlayerCharacter))
                    if (!((PlayerCharacter) abstractCharacter).canSee((PlayerCharacter) target))
                        return 0;

                attackFailure = true;
            }

            //handle pet, skip timers (handled by AI)

            if (abstractCharacter.getObjectType().equals(GameObjectType.Mob)) {

                Mob mob = (Mob) abstractCharacter;

                if (mob.isPet()) {
                    attack(abstractCharacter, target, weapon, wb, slot == MBServerStatics.SLOT_MAINHAND);
                    return 2;
                }
            }

            if(abstractCharacter.getObjectType().equals(GameObjectType.PlayerCharacter)){
                PlayerCharacter pc = (PlayerCharacter)abstractCharacter;
                if(pc.isBoxed){
                    if(target.getObjectType().equals(GameObjectType.PlayerCharacter)) {
                        ChatManager.chatSystemInfo(pc, "You Are PvE Flagged: Cannot Attack Players.");
                        attackFailure = true;
                    }
                }
            }

            //TODO Verify attacker has los (if not ranged weapon).

            if (!attackFailure) {

                if (hasNoWeapon || abstractCharacter.getObjectType().equals(GameObjectType.Mob))
                    createTimer(abstractCharacter, slot, 20, true); //2 second for no weapon
                else {
                    int wepSpeed = (int) (wb.getSpeed());
                    if(abstractCharacter.getObjectType().equals(GameObjectType.PlayerCharacter)){
                        PlayerCharacter pc = (PlayerCharacter)abstractCharacter;
                        if(slot == 1){
                            wepSpeed = (int) pc.combatStats.attackSpeedHandOne;
                        }else{
                            wepSpeed = (int) pc.combatStats.attackSpeedHandTwo;
                        }
                    }else {

                        if (weapon != null && weapon.getBonusPercent(ModType.WeaponSpeed, SourceType.None) != 0f) //add weapon speed bonus
                            wepSpeed *= (1 + weapon.getBonus(ModType.WeaponSpeed, SourceType.None));

                        if (abstractCharacter.getBonuses() != null && abstractCharacter.getBonuses().getFloatPercentAll(ModType.AttackDelay, SourceType.None) != 0f) //add effects speed bonus
                            wepSpeed *= (1 + abstractCharacter.getBonuses().getFloatPercentAll(ModType.AttackDelay, SourceType.None));

                        if (wepSpeed < 10)
                            wepSpeed = 10; //Old was 10, but it can be reached lower with legit buffs,effects.
                    }
                    createTimer(abstractCharacter, slot, wepSpeed, true);
                }

                attack(abstractCharacter, target, weapon, wb, slot == MBServerStatics.SLOT_MAINHAND);
            } else
                createTimer(abstractCharacter, slot, 5, false);  // changed this to half a second to make combat attempts more aggressive than movement sync

        } catch (Exception e) {
            return 0;
        }
        return 2;
    }

    private static void createTimer(AbstractCharacter ac, int slot, int time, boolean success) {

        ConcurrentHashMap<String, JobContainer> timers = ac.getTimers();

        if (timers != null) {
            AttackJob aj = new AttackJob(ac, slot, success);
            JobContainer job;
            job = JobScheduler.getInstance().scheduleJob(aj, (time * 100));
            timers.put("Attack" + slot, job);
        } else {
            Logger.error("Unable to find Timers for Character " + ac.getObjectUUID());
        }
    }

    /**
     * Attempt to attack target
     */
    private static void attack(AbstractCharacter ac, AbstractWorldObject target, Item weapon, ItemBase wb, boolean mainHand) {

        float atr;
        int minDamage, maxDamage;
        int errorTrack = 0;

        try {

            if (ac == null)
                return;

            if (target == null)
                return;

            if(ac.getObjectType().equals(GameObjectType.PlayerCharacter)){
                PlayerCharacter pc = (PlayerCharacter) ac;
                if( pc.combatStats == null){
                    pc.combatStats = new PlayerCombatStats(pc);
                }
                pc.combatStats.calculateATR(true);
                pc.combatStats.calculateATR(false);
                if (mainHand) {
                    atr = pc.combatStats.atrHandOne;
                    minDamage = pc.combatStats.minDamageHandOne;
                    maxDamage = pc.combatStats.maxDamageHandOne;
                } else {
                    atr = pc.combatStats.atrHandTwo;
                    minDamage = pc.combatStats.minDamageHandTwo;
                    maxDamage = pc.combatStats.maxDamageHandTwo;
                }
            }else {
                if (mainHand) {
                    atr = ac.getAtrHandOne();
                    minDamage = ac.getMinDamageHandOne();
                    maxDamage = ac.getMaxDamageHandOne();
                } else {
                    atr = ac.getAtrHandTwo();
                    minDamage = ac.getMinDamageHandTwo();
                    maxDamage = ac.getMaxDamageHandTwo();
                }
            }

            boolean tarIsRat = false;

            if (target.getObjectTypeMask() == MBServerStatics.MASK_RAT)
                tarIsRat = true;
            else if (target.getObjectType() == GameObjectType.PlayerCharacter) {

                PlayerCharacter pTar = (PlayerCharacter) target;

                for (Effect eff : pTar.getEffects().values())
                    if (eff.getPowerToken() == 429513599 || eff.getPowerToken() == 429415295)
                        tarIsRat = true;
            }

            //Dont think we need to do this anymore.

            if (tarIsRat)
                if (ac.getBonuses().getFloatPercentAll(ModType.Slay, SourceType.Rat) != 0) {   //strip away current % dmg buffs then add with rat %

                    float percent = 1 + ac.getBonuses().getFloatPercentAll(ModType.Slay, SourceType.Rat);

                    minDamage *= percent;
                    maxDamage *= percent;
                }

            errorTrack = 1;

            //subtract stamina

            if (wb == null)
                ac.modifyStamina(-0.5f, ac, true);
            else {
                float stam = wb.getWeight() / 3;
                stam = (stam < 1) ? 1 : stam;
                ac.modifyStamina(-(stam), ac, true);
            }

            ac.cancelOnAttackSwing();

            errorTrack = 2;

            //set last time this player has attacked something.

            if (target.getObjectType().equals(GameObjectType.PlayerCharacter) && target.getObjectUUID() != ac.getObjectUUID() && ac.getObjectType() == GameObjectType.PlayerCharacter) {
                ac.setTimeStamp("LastCombatPlayer", System.currentTimeMillis());
                ((PlayerCharacter) target).setTimeStamp("LastCombatPlayer", System.currentTimeMillis());
            } else
                ac.setTimeStamp("LastCombatMob", System.currentTimeMillis());

            errorTrack = 3;

            //Get defense for target

            float defense;

            if (target.getObjectType().equals(GameObjectType.Building)) {

                if (BuildingManager.getBuildingFromCache(target.getObjectUUID()) == null) {
                    ac.setCombatTarget(null);
                    return;
                }

                defense = 0;

                Building building = (Building) target;

                if (building.getParentZone() != null && building.getParentZone().isPlayerCity()) {

                    if (System.currentTimeMillis() > building.getTimeStamp("CallForHelp")) {

                        building.getTimestamps().put("CallForHelp", System.currentTimeMillis() + 15000);

                        for (Mob mob : building.getParentZone().zoneMobSet) {
                            if (!mob.isPlayerGuard())
                                continue;

                            if (mob.getCombatTarget() != null)
                                continue;

                            if (mob.getGuild() != null && building.getGuild() != null)
                                if (!Guild.sameGuild(mob.getGuild().getNation(), building.getGuild().getNation()))
                                    continue;

                            if (mob.getLoc().distanceSquared2D(building.getLoc()) > sqr(300))
                                continue;

                            mob.setCombatTarget(ac);
                        }
                    }
                }
            } else {
                AbstractCharacter tar = (AbstractCharacter) target;
                if(tar.getObjectType().equals(GameObjectType.PlayerCharacter)){
                    if(((PlayerCharacter)tar).combatStats == null){
                        ((PlayerCharacter)tar).combatStats = new PlayerCombatStats((PlayerCharacter)tar);
                    }
                    ((PlayerCharacter)tar).combatStats.calculateDefense();
                    defense = ((PlayerCharacter)tar).combatStats.defense;
                }else {
                    defense = tar.getDefenseRating();
                }
                handleRetaliate(tar, ac);   //Handle target attacking back if in combat and has no other target
            }

            errorTrack = 4;

            //Get hit chance

            //int chance;
            //float dif = atr - defense;

            //if (dif > 100)
            //    chance = 94;
            //else if (dif < -100)
            //    chance = 4;
            //else
            //    chance = (int) ((0.45 * dif) + 49);

            errorTrack = 5;

            //calculate hit/miss

            DeferredPowerJob dpj = null;

            boolean hitLanded = LandHit((int)atr,(int)defense);
            if (hitLanded) {

                if (ac.getObjectType().equals(GameObjectType.PlayerCharacter))
                    updateAttackTimers((PlayerCharacter) ac, target, true);

                boolean skipPassives = false;
                PlayerBonuses bonuses = ac.getBonuses();

                if (bonuses != null && bonuses.getBool(ModType.IgnorePassiveDefense, SourceType.None))
                    skipPassives = true;

                AbstractCharacter tarAc = null;

                if (AbstractWorldObject.IsAbstractCharacter(target))
                    tarAc = (AbstractCharacter) target;

                errorTrack = 6;

                // Apply Weapon power effect if any. don't try to apply twice if
                // dual wielding. Perform after passive test for sync purposes.

                if (ac.getObjectType().equals(GameObjectType.PlayerCharacter) && (mainHand || wb.isTwoHanded())) {

                    dpj = ((PlayerCharacter) ac).getWeaponPower();

                    if (dpj != null) {

                        PlayerBonuses bonus = ac.getBonuses();
                        float attackRange = getWeaponRange(wb, bonus);

                        if(ac.isMoving()){
                            attackRange += (ac.getSpeed() * 0.1f);
                        }

                        if(AbstractWorldObject.IsAbstractCharacter(target)) {
                            //AbstractCharacter tarAc = (AbstractCharacter) target;
                            if(tarAc != null && tarAc.isMoving()){
                                attackRange += (tarAc.getSpeed() * 0.1f);
                            }
                        }

                        if(specialCaseHitRoll(dpj.getPowerToken())) {
                            if(hitLanded) {
                                dpj.attack(target, attackRange);
                            }
                        }else{
                            dpj.attack(target, attackRange);
                        }

                        if (dpj.getPower() != null && (dpj.getPowerToken() == -1851459567 || dpj.getPowerToken() == -1851489518))
                            ((PlayerCharacter) ac).setWeaponPower(dpj);
                    }
                }

                //check to apply second backstab.

                if (ac.getObjectType().equals(GameObjectType.PlayerCharacter) && !mainHand) {

                    dpj = ((PlayerCharacter) ac).getWeaponPower();

                    if (dpj != null && dpj.getPower() != null && (dpj.getPowerToken() == -1851459567 || dpj.getPowerToken() == -1851489518)) {
                        float attackRange = getWeaponRange(wb, bonuses);

                        if(ac.isMoving()){
                            attackRange += (ac.getSpeed() * 0.1f);
                        }

                        if(AbstractWorldObject.IsAbstractCharacter(target)) {
                            //AbstractCharacter tarAc = (AbstractCharacter) target;
                            if(tarAc != null && tarAc.isMoving()){
                                attackRange += (tarAc.getSpeed() * 0.1f);
                            }
                        }

                        if(specialCaseHitRoll(dpj.getPowerToken())) {
                            if(hitLanded) {
                                dpj.attack(target, attackRange);
                            }
                        }else{
                            dpj.attack(target, attackRange);
                        }
                    }
                }

                errorTrack = 7;

                //Hit, check if passive kicked in

                boolean passiveFired = false;

                if (!skipPassives && tarAc != null) {

                    if (target.getObjectType().equals(GameObjectType.PlayerCharacter)) {

                        //Handle Block passive

                        if (testPassive(ac, tarAc, "Block") && canTestBlock(ac, target)) {

                            if (!target.isAlive())
                                return;

                            sendPassiveDefenseMessage(ac, wb, target, MBServerStatics.COMBAT_SEND_BLOCK, dpj, mainHand);
                            passiveFired = true;
                        }

                        //Handle Parry passive

                        if (!passiveFired)
                            if (canTestParry(ac, target) && testPassive(ac, tarAc, "Parry")) {

                                if (!target.isAlive())
                                    return;

                                sendPassiveDefenseMessage(ac, wb, target, MBServerStatics.COMBAT_SEND_PARRY, dpj, mainHand);
                                passiveFired = true;
                            }

                    }

                    errorTrack = 8;

                    //Handle Dodge passive

                    if (!passiveFired)
                        if (testPassive(ac, tarAc, "Dodge")) {

                            if (!target.isAlive())
                                return;

                            sendPassiveDefenseMessage(ac, wb, target, MBServerStatics.COMBAT_SEND_DODGE, dpj, mainHand);
                            passiveFired = true;
                        }
                }

                //return if passive (Block, Parry, Dodge) fired

                if (passiveFired)
                    return;

                errorTrack = 9;

                //Hit and no passives
                //if target is player, set last attack timestamp

                if (target.getObjectType().equals(GameObjectType.PlayerCharacter))
                    updateAttackTimers((PlayerCharacter) target, ac, false);

                //Get damage Type

                DamageType damageType;

                if (wb != null)
                    damageType = wb.getDamageType();
                else if (ac.getObjectType().equals(GameObjectType.Mob) && ((Mob) ac).isSiege())
                    damageType = DamageType.Siege;
                else
                    damageType = DamageType.Crush;

                errorTrack = 10;

                //Get target resists

                Resists resists = null;

                if (tarAc != null)
                    resists = tarAc.getResists();
                else if (target.getObjectType().equals(GameObjectType.Building))
                    resists = ((Building) target).getResists();

                //make sure target is not immune to damage type;

                if (resists != null && resists.immuneTo(damageType)) {
                    sendCombatMessage(ac, target, 0f, wb, dpj, mainHand);
                    return;
                }

                errorTrack = 11;

                //Calculate Damage done

                float damage;

                if (wb != null)
                    damage = calculateDamage(ac, tarAc, minDamage, maxDamage, damageType, resists);
                else
                    damage = calculateDamage(ac, tarAc, minDamage, maxDamage, damageType, resists);

                if(weapon != null && weapon.effects != null){
                    float armorPierce = 0;
                    for(Effect eff : weapon.effects.values()){
                        for(AbstractEffectModifier mod : eff.getEffectModifiers()){
                            if(mod.modType.equals(ModType.ArmorPiercing)){
                                armorPierce += mod.getPercentMod() + (mod.getRamp() * eff.getTrains());
                            }
                        }
                    }
                    if(armorPierce > 0){
                        damage *= 1 + (armorPierce * 0.01f);
                    }
                }
                //Resists.handleFortitude(tarAc,damageType,damage);

                float d = 0f;

                int originalDamage = (int)damage;
                if(ac != null && ac.getObjectType().equals(GameObjectType.PlayerCharacter)){
                    damage *= ((PlayerCharacter)ac).ZergMultiplier;
                } // Health modifications are modified by the ZergMechanic

                errorTrack = 12;

                //Subtract Damage from target's health

                if(ac.getObjectType().equals(GameObjectType.PlayerCharacter)){
                    damage *= ((PlayerCharacter)ac).ZergMultiplier;
                }
                if (tarAc != null) {

                    if (tarAc.isSit())
                        damage *= 2.5f; //increase damage if sitting

                    if (tarAc.getObjectType() == GameObjectType.Mob) {
                        ac.setHateValue(damage * MBServerStatics.PLAYER_COMBAT_HATE_MODIFIER);
                        ((Mob) tarAc).handleDirectAggro(ac);
                    }
                    if (tarAc.getHealth() > 0) {
                        d = tarAc.modifyHealth(-damage, ac, false);
                        if(tarAc != null && tarAc.getObjectType().equals(GameObjectType.PlayerCharacter) && ((PlayerCharacter)ac).ZergMultiplier != 1.0f){
                            PlayerCharacter debugged = (PlayerCharacter)tarAc;
                            ChatManager.chatSystemInfo(debugged, "ZERG DEBUG: " + ac.getName() + " Hits You For: " + (int)damage + " instead of " + originalDamage);
                        }
                    }

                    tarAc.cancelOnTakeDamage();

                } else if (target.getObjectType().equals(GameObjectType.Building)) {

                    if (BuildingManager.getBuildingFromCache(target.getObjectUUID()) == null) {
                        ac.setCombatTarget(null);
                        return;
                    }

                    if (target.getHealth() > 0)
                        d = ((Building) target).modifyHealth(-damage, ac);
                }

                errorTrack = 13;

                //Test to see if any damage needs done to weapon or armor

                testItemDamage(ac, target, weapon, wb);

                // if target is dead, we got the killing blow, remove attack timers on our weapons

                if (tarAc != null && !tarAc.isAlive())
                    removeAttackTimers(ac);

                //test double death fix

                if (d != 0)
                    sendCombatMessage(ac, target, damage, wb, dpj, mainHand); //send damage message

                errorTrack = 14;

                //handle procs
                procChanceHandler(weapon,ac,tarAc);

                errorTrack = 15;

                //handle damage shields

                if (ac.isAlive() && tarAc != null && tarAc.isAlive())
                    try {
                        handleDamageShields(ac, tarAc, damage);
                    }catch(Exception e){
                        Logger.error(e.getMessage());
                    }

            } else {

                // Apply Weapon power effect if any.
                // don't try to apply twice if dual wielding.
                try {
                    if (ac.getObjectType().equals(GameObjectType.PlayerCharacter) && (mainHand || wb.isTwoHanded())) {
                        dpj = ((PlayerCharacter) ac).getWeaponPower();

                        if (dpj != null) {

                            PowersBase wp = dpj.getPower();

                            if (wp.requiresHitRoll() == false) {
                                PlayerBonuses bonus = ac.getBonuses();
                                float attackRange = getWeaponRange(wb, bonus);

                                if (ac.isMoving()) {
                                    attackRange += (ac.getSpeed() * 0.1f);
                                }

                                if (AbstractWorldObject.IsAbstractCharacter(target)) {
                                    AbstractCharacter tarAc = (AbstractCharacter) target;
                                    if (tarAc != null && tarAc.isMoving()) {
                                        attackRange += (tarAc.getSpeed() * 0.1f);
                                    }
                                }


                                if (specialCaseHitRoll(dpj.getPowerToken())) {
                                    if (hitLanded) {
                                        dpj.attack(target, attackRange);
                                    }
                                } else {
                                    dpj.attack(target, attackRange);
                                }
                            } else
                                ((PlayerCharacter) ac).setWeaponPower(null);
                        }
                    }
                }catch(Exception e) {
                    Logger.error(e.getMessage());
                }
                try {
                    if (target.getObjectType() == GameObjectType.Mob)
                        ((Mob) target).handleDirectAggro(ac);
                }catch(Exception e){
                    Logger.error(e.getMessage());
                }
                errorTrack = 17;

                //miss, Send miss message

                sendCombatMessage(ac, target, 0f, wb, dpj, mainHand);

                //if attacker is player, set last attack timestamp

                if (ac.getObjectType().equals(GameObjectType.PlayerCharacter))
                    updateAttackTimers((PlayerCharacter) ac, target, true);
            }

            errorTrack = 18;

            //cancel effects that break on attack or attackSwing
            ac.cancelOnAttack();

        } catch (Exception e) {
            Logger.error(ac.getName() + ' ' + errorTrack + ' ' + e);
        }
    }

    private static void procChanceHandler(Item weapon, AbstractCharacter ac, AbstractCharacter tarAc) {

        //no weapon means no proc
        if(weapon == null)
            return;

        //caster is dead of null, no proc
        if(ac == null || !ac.isAlive())
            return;

        //target is dead or null, no proc
        if(tarAc == null || !tarAc.isAlive())
            return;

        //no effects on weapon, skip proc
        if(weapon.effects == null || weapon.effects.isEmpty())
            return;

        for (Effect eff : weapon.effects.values()){
            for(AbstractEffectModifier mod : eff.getEffectModifiers()) {
                if (mod.modType.equals(ModType.WeaponProc)) {
                    int procChance = ThreadLocalRandom.current().nextInt(100);
                    if (procChance < MBServerStatics.PROC_CHANCE) {
                        try {
                            ((WeaponProcEffectModifier) mod).applyProc(ac, tarAc);
                            break;
                        } catch (Exception e) {
                            Logger.error(eff.getName() + " Failed To Cast Proc");
                        }
                    }
                }
            }
        }
    }

    public static boolean canTestParry(AbstractCharacter ac, AbstractWorldObject target) {

        if (ac == null || target == null || !AbstractWorldObject.IsAbstractCharacter(target))
            return false;

        AbstractCharacter tar = (AbstractCharacter) target;

        CharacterItemManager acItem = ac.getCharItemManager();
        CharacterItemManager tarItem = tar.getCharItemManager();

        if (acItem == null || tarItem == null)
            return false;

        Item acMain = acItem.getItemFromEquipped(1);
        Item acOff = acItem.getItemFromEquipped(2);
        Item tarMain = tarItem.getItemFromEquipped(1);
        Item tarOff = tarItem.getItemFromEquipped(2);

        if(target.getObjectType().equals(GameObjectType.PlayerCharacter)){
            PlayerCharacter pc = (PlayerCharacter) target;
            if(pc.getRaceID() == 1999 && !isRanged(acMain) && !isRanged(acOff))
                return true;
        }

        return !isRanged(acMain) && !isRanged(acOff) && !isRanged(tarMain) && !isRanged(tarOff);
    }

    public static boolean canTestBlock(AbstractCharacter ac, AbstractWorldObject target) {

        if (ac == null || target == null || !AbstractWorldObject.IsAbstractCharacter(target))
            return false;

        AbstractCharacter tar = (AbstractCharacter) target;

        CharacterItemManager acItem = ac.getCharItemManager();
        CharacterItemManager tarItem = tar.getCharItemManager();

        if (acItem == null || tarItem == null)
            return false;

        Item tarOff = tarItem.getItemFromEquipped(2);

        if (tarOff == null)
            return false;

        return tarOff.getItemBase().isShield() != false;
    }

    private static boolean isRanged(Item item) {

        if (item == null)
            return false;

        ItemBase ib = item.getItemBase();

        if (ib == null)
            return false;

        if (ib.getType().equals(ItemType.WEAPON) == false)
            return false;

        return ib.getRange() > MBServerStatics.RANGED_WEAPON_RANGE;

    }

    private static float calculateDamage(AbstractCharacter source, AbstractCharacter target, float minDamage, float maxDamage, DamageType damageType, Resists resists) {

        //get range between min and max

        float range = maxDamage - minDamage;

        //Damage is calculated twice to average a more central point

        float damage = ThreadLocalRandom.current().nextFloat() * range;
        damage = (damage + (ThreadLocalRandom.current().nextFloat() * range)) * .5f;

        //put it back between min and max

        damage += minDamage;

        //calculate resists in if any



        if (resists != null)
            damage = resists.getResistedDamage(source, target, damageType, damage, 0);

        return damage;
    }

    private static void sendPassiveDefenseMessage(AbstractCharacter source, ItemBase wb, AbstractWorldObject target, int passiveType, DeferredPowerJob dpj, boolean mainHand) {

        int swingAnimation = getSwingAnimation(wb, dpj, mainHand);

        if (dpj != null)
            if (PowersManager.AnimationOverrides.containsKey(dpj.getAction().getEffectID()))
                swingAnimation = PowersManager.AnimationOverrides.get(dpj.getAction().getEffectID());

        TargetedActionMsg cmm = new TargetedActionMsg(source, swingAnimation, target, passiveType);
        DispatchMessage.sendToAllInRange(target, cmm);

    }

    private static void sendCombatMessage(AbstractCharacter source, AbstractWorldObject target, float damage, ItemBase wb, DeferredPowerJob dpj, boolean mainHand) {

        int swingAnimation = getSwingAnimation(wb, dpj, mainHand);

        if (dpj != null)
            if (PowersManager.AnimationOverrides.containsKey(dpj.getAction().getEffectID()))
                swingAnimation = PowersManager.AnimationOverrides.get(dpj.getAction().getEffectID());

        if (source.getObjectType() == GameObjectType.PlayerCharacter)
            for (Effect eff : source.getEffects().values())
                if (eff.getPower() != null && (eff.getPower().getToken() == 429506943 || eff.getPower().getToken() == 429408639 || eff.getPower().getToken() == 429513599 || eff.getPower().getToken() == 429415295))
                    swingAnimation = 0;

        TargetedActionMsg cmm = new TargetedActionMsg(source, target, damage, swingAnimation);
        DispatchMessage.sendToAllInRange(target, cmm);
    }

    public static int getSwingAnimation(ItemBase wb, DeferredPowerJob dpj, boolean mainHand) {
        int token = 0;

        if (dpj != null)
            token = (dpj.getPower() != null) ? dpj.getPower().getToken() : 0;

        if (token == 563721004) //kick animation
            return 79;

        if (CombatManager.animation != 0)
            return CombatManager.animation;

        if (wb == null)
            return 75;

        if (mainHand) {
            if (wb.getAnimations().size() > 0) {

                int animation;

                int random = ThreadLocalRandom.current().nextInt(wb.getAnimations().size());

                try {
                    animation = wb.getAnimations().get(random);
                    return animation;
                } catch (Exception e) {
                    Logger.error(e.getMessage());
                    return wb.getAnimations().get(0);
                }

            } else if (wb.getOffHandAnimations().size() > 0) {

                int animation;
                int random = ThreadLocalRandom.current().nextInt(wb.getOffHandAnimations().size());

                try {
                    animation = wb.getOffHandAnimations().get(random);
                    return animation;
                } catch (Exception e) {
                    Logger.error(e.getMessage());
                    return wb.getOffHandAnimations().get(0);
                }
            }
        } else {
            if (wb.getOffHandAnimations().size() > 0) {
                int animation;
                int random = ThreadLocalRandom.current().nextInt(wb.getOffHandAnimations().size());

                try {
                    animation = wb.getOffHandAnimations().get(random);
                    return animation;
                } catch (Exception e) {
                    Logger.error(e.getMessage());
                    return wb.getOffHandAnimations().get(0);

                }
            } else if (wb.getAnimations().size() > 0) {

                int animation;
                int random = ThreadLocalRandom.current().nextInt(wb.getAnimations().size());

                try {
                    animation = wb.getAnimations().get(random);
                    return animation;
                } catch (Exception e) {
                    Logger.error(e.getMessage());
                    return wb.getAnimations().get(0);

                }

            }
        }


        String required = wb.getSkillRequired();
        String mastery = wb.getMastery();

        if (required.equals("Unarmed Combat"))
            return 75;
        else if (required.equals("Sword")) {

            if (wb.isTwoHanded())
                return 105;
            else
                return 98;

        } else if (required.equals("Staff") || required.equals("Pole Arm")) {
            return 85;
        } else if (required.equals("Spear")) {
            return 92;
        } else if (required.equals("Hammer") || required.equals("Axe")) {
            if (wb.isTwoHanded()) {
                return 105;
            } else if (mastery.equals("Throwing")) {
                return 115;
            } else {
                return 100;
            }
        } else if (required.equals("Dagger")) {
            if (mastery.equals("Throwing")) {
                return 117;
            } else {
                return 81;
            }
        } else if (required.equals("Crossbow")) {
            return 110;
        } else if (required.equals("Bow")) {
            return 109;
        } else if (wb.isTwoHanded()) {
            return 105;
        } else {
            return 100;
        }
    }

    private static boolean testPassive(AbstractCharacter source, AbstractCharacter target, String type) {

        if(target.getBonuses() != null)
            if(target.getBonuses().getBool(ModType.Stunned, SourceType.None))
                return false;

        if(source.getBonuses() != null)
            if(source.getBonuses().getBool(ModType.IgnorePassiveDefense, SourceType.None))
                return false;

        float chance = target.getPassiveChance(type, source.getLevel(), true);

        if (chance == 0f)
            return false;

        //max 75% chance of passive to fire

        if (chance > 75f)
            chance = 75f;

        int roll = ThreadLocalRandom.current().nextInt(1,100);

        return roll < chance;

    }

    private static void updateAttackTimers(PlayerCharacter pc, AbstractWorldObject target, boolean attack) {

        //Set Attack Timers

        if (target.getObjectType().equals(GameObjectType.PlayerCharacter))
            pc.setLastPlayerAttackTime();
    }

    public static float getWeaponRange(ItemBase weapon, PlayerBonuses bonus) {

        float rangeMod = 1.0f;

        if (weapon == null)
            return 0f;

        if (bonus != null)
            rangeMod += bonus.getFloatPercentAll(ModType.WeaponRange, SourceType.None);

        return weapon.getRange() * rangeMod;
    }

    public static void toggleCombat(ToggleCombatMsg msg, ClientConnection origin) {
        toggleCombat(msg.toggleCombat(), origin);
    }

    public static void toggleCombat(SetCombatModeMsg msg, ClientConnection origin) {
        toggleCombat(msg.getToggle(), origin);
    }

    private static void toggleCombat(boolean toggle, ClientConnection origin) {

        PlayerCharacter pc = SessionManager.getPlayerCharacter(origin);

        if (pc == null)
            return;

        pc.setCombat(toggle);

        if (!toggle) // toggle is move it to false so clear combat target
            pc.setCombatTarget(null); //clear last combat target

        UpdateStateMsg rwss = new UpdateStateMsg();
        rwss.setPlayer(pc);
        DispatchMessage.dispatchMsgToInterestArea(pc, rwss, DispatchChannel.PRIMARY, MBServerStatics.CHARACTER_LOAD_RANGE, false, false);
    }

    public static void toggleSit(boolean toggle, ClientConnection origin) {

        PlayerCharacter pc = SessionManager.getPlayerCharacter(origin);

        if (pc == null)
            return;

        if(pc.isFlying())
            pc.setSit(false);
        else
            pc.setSit(toggle);

        UpdateStateMsg rwss = new UpdateStateMsg();
        rwss.setPlayer(pc);
        DispatchMessage.dispatchMsgToInterestArea(pc, rwss, DispatchChannel.PRIMARY, MBServerStatics.CHARACTER_LOAD_RANGE, true, false);
    }

    public static boolean NotInRange(AbstractCharacter ac, AbstractWorldObject target, float range) {

        //Vector3fImmutable sl = ac.getLoc();
        //Vector3fImmutable tl = target.getLoc();

        //add Hitbox to range.

        //range += (calcHitBox(ac) + calcHitBox(target));

        //float magnitudeSquared = tl.distanceSquared(sl);

        //return magnitudeSquared > range * range;

        //new system without heightmaps
        float attackerAltitude = 0;
        if(ac.getObjectType().equals(GameObjectType.PlayerCharacter)){
            PlayerCharacter attacker = (PlayerCharacter)ac;
            if(attacker.isFlying()){
                attackerAltitude += attacker.getAltitude();
            }
        }
        Vector3fImmutable attackerLoc = new Vector3fImmutable(ac.loc.x,attackerAltitude,ac.loc.z);

        float targetAltitude = 0;
        if(target.getObjectType().equals(GameObjectType.PlayerCharacter)){
            PlayerCharacter pcTar = (PlayerCharacter)target;
            if(pcTar.isFlying()){
                targetAltitude += pcTar.getAltitude();
            }
        }
        Vector3fImmutable targetLoc = new Vector3fImmutable(target.loc.x,targetAltitude,target.loc.z);

        return attackerLoc.distanceSquared(targetLoc) > range * range;

    }

    public static float getCombatDistance(AbstractCharacter ac){
        AbstractWorldObject target = ac.combatTarget;
        float attackerAltitude = 0;
        if(ac.getObjectType().equals(GameObjectType.PlayerCharacter)){
            PlayerCharacter attacker = (PlayerCharacter)ac;
            if(attacker.isFlying()){
                attackerAltitude += attacker.getAltitude();
            }
        }
        Vector3fImmutable attackerLoc = new Vector3fImmutable(ac.loc.x,attackerAltitude,ac.loc.z);

        float targetAltitude = 0;
        if(target.getObjectType().equals(GameObjectType.PlayerCharacter)){
            PlayerCharacter pcTar = (PlayerCharacter)target;
            if(pcTar.isFlying()){
                targetAltitude += pcTar.getAltitude();
            }
        }
        Vector3fImmutable targetLoc = new Vector3fImmutable(target.loc.x,targetAltitude,target.loc.z);

        return attackerLoc.distance(targetLoc);
    }

    //Called when character takes damage.
    public static void handleRetaliate(AbstractCharacter tarAc, AbstractCharacter ac) {

        if (ac == null || tarAc == null)
            return;

        if (ac.equals(tarAc))
            return;

        if (tarAc.isMoving() && tarAc.getObjectType().equals(GameObjectType.PlayerCharacter))
            return;

        if (!tarAc.isAlive() || !ac.isAlive())
            return;

        boolean isCombat = tarAc.isCombat();

        //If target in combat and has no target, then attack back

        AbstractWorldObject awoCombTar = tarAc.getCombatTarget();

        if ((tarAc.isCombat() && awoCombTar == null) || (isCombat && awoCombTar != null && (!awoCombTar.isAlive() || tarAc.isCombat() && NotInRange(tarAc, awoCombTar, tarAc.getRange()))) || (tarAc != null && tarAc.getObjectType() == GameObjectType.Mob && ((Mob) tarAc).isSiege()))
            if (tarAc.getObjectType().equals(GameObjectType.PlayerCharacter)) {  // we are in combat with no valid target

                PlayerCharacter pc = (PlayerCharacter) tarAc;
                tarAc.setCombatTarget(ac);
                pc.setLastTarget(ac.getObjectType(), ac.getObjectUUID());

                if (tarAc.getTimers() != null)
                    if (!tarAc.getTimers().containsKey("Attack" + MBServerStatics.SLOT_MAINHAND))
                        CombatManager.AttackTarget((PlayerCharacter) tarAc, tarAc.getCombatTarget());
            }

        //Handle pet retaliate if assist is on and pet doesn't have a target.

        if (tarAc.getObjectType().equals(GameObjectType.PlayerCharacter)) {

            Mob pet = ((PlayerCharacter) tarAc).getPet();

            if (pet != null && pet.assist && pet.getCombatTarget() == null)
                pet.setCombatTarget(ac);
        }

        //Handle Mob Retaliate.

        if (tarAc.getObjectType() == GameObjectType.Mob) {

            Mob retaliater = (Mob) tarAc;

            if (retaliater.getCombatTarget() != null && !retaliater.isSiege())
                return;

            if (ac.getObjectType() == GameObjectType.Mob && retaliater.isSiege())
                return;

            retaliater.setCombatTarget(ac);
            if(retaliater.isPlayerGuard && (retaliater.BehaviourType.equals(MobBehaviourType.GuardMinion) || retaliater.BehaviourType.equals(MobBehaviourType.GuardCaptain))){
                for(Mob guard : retaliater.guardedCity.getParent().zoneMobSet){
                    if(guard.isPlayerGuard && guard.combatTarget == null){
                        guard.setCombatTarget(ac);
                    }
                }
            }

        }
    }

    public static void handleDamageShields(AbstractCharacter ac, AbstractCharacter target, float damage) {

        if (ac == null || target == null)
            return;

        PlayerBonuses bonuses = target.getBonuses();

        if (bonuses != null) {

            ConcurrentHashMap<AbstractEffectModifier, DamageShield> damageShields = bonuses.getDamageShields();
            float total = 0;

            for (DamageShield ds : damageShields.values()) {

                //get amount to damage back

                float amount;

                if (ds.usePercent())
                    amount = damage * ds.getAmount() / 100;
                else
                    amount = ds.getAmount();

                //get resisted damage for damagetype

                Resists resists = ac.getResists();

                if (resists != null) {
                    amount = resists.getResistedDamage(target, ac, ds.getDamageType(), amount, 0);
                }
                total += amount;
            }

            if (total > 0) {

                //apply Damage back

                ac.modifyHealth(-total, target, true);

                TargetedActionMsg cmm = new TargetedActionMsg(ac, ac, total, 0);
                DispatchMessage.sendToAllInRange(target, cmm);

            }
        }
    }

    public static float calcHitBox(AbstractWorldObject ac) {

        //TODO Figure out how Str Affects HitBox

        float hitBox = 1;

        switch (ac.getObjectType()) {
            case PlayerCharacter:
                PlayerCharacter pc = (PlayerCharacter) ac;
                if (MBServerStatics.COMBAT_TARGET_HITBOX_DEBUG) {
                    Logger.info("Hit box radius for " + pc.getFirstName() + " is " + ((int) pc.statStrBase / 20f));
                }
                hitBox = 1.5f + (int) ((PlayerCharacter) ac).statStrBase / 20f;
                break;

            case Mob:
                Mob mob = (Mob) ac;
                if (MBServerStatics.COMBAT_TARGET_HITBOX_DEBUG)
                    Logger.info("Hit box radius for " + mob.getFirstName()
                            + " is " + ((Mob) ac).getMobBase().getHitBoxRadius());

                hitBox = ((Mob) ac).getMobBase().getHitBoxRadius();
                break;
            case Building:
                Building building = (Building) ac;
                if (building.getBlueprint() == null)
                    return 32;
                hitBox = Math.max(building.getBlueprint().getBuildingGroup().getExtents().x,
                        building.getBlueprint().getBuildingGroup().getExtents().y);
                if (MBServerStatics.COMBAT_TARGET_HITBOX_DEBUG)
                    Logger.info("Hit box radius for " + building.getName() + " is " + hitBox);
                break;

        }
        return hitBox;
    }

    private static void testItemDamage(AbstractCharacter ac, AbstractWorldObject awo, Item weapon, ItemBase wb) {

        if (ac == null)
            return;

        //get chance to damage

        int chance = 4500;

        if (wb != null)
            if (wb.isGlass()) //glass used weighted so fast weapons don't break faster
                chance = 9000 / wb.getWeight();

        //test damaging attackers weapon

        int takeDamage = ThreadLocalRandom.current().nextInt(chance);

        if (takeDamage == 0 && wb != null && (ac.getObjectType().equals(GameObjectType.PlayerCharacter)))
            ac.getCharItemManager().damageItem(weapon, 1);


        //test damaging targets gear

        takeDamage = ThreadLocalRandom.current().nextInt(chance);

        if (takeDamage == 0 && awo != null && (awo.getObjectType().equals(GameObjectType.PlayerCharacter)))
            ((AbstractCharacter) awo).getCharItemManager().damageRandomArmor(1);
    }

    public static boolean LandHit(int ATR, int DEF){

        //float chance = (ATR-((ATR+DEF) * 0.315f)) / ((DEF-((ATR+DEF) * 0.315f)) + (ATR-((ATR+DEF) * 0.315f)));
        //float convertedChance = chance * 100;

        int roll = ThreadLocalRandom.current().nextInt(101);

        //if(roll <= 5)//always 5% chance to miss
        //    return false;

        //if(roll >= 95)//always 5% chance to hit
        //    return true;

        float chance = PlayerCombatStats.getHitChance(ATR,DEF);
        return chance >= roll;
    }

    public static boolean specialCaseHitRoll(int powerID){
        switch(powerID) {
            case 563200808: // Naargal's Bite
            case 563205337: // Naargal's Dart
            case 563205930: // Sword of Saint Malorn
                return true;
            default:
                return false;
        }
    }
}
