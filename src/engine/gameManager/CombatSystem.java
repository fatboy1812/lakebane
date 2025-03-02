package engine.gameManager;

import engine.Enum;
import engine.exception.MsgSendException;
import engine.job.JobContainer;
import engine.job.JobScheduler;
import engine.jobs.AttackJob;
import engine.jobs.DeferredPowerJob;
import engine.net.DispatchMessage;
import engine.net.client.ClientConnection;
import engine.net.client.msg.AttackCmdMsg;
import engine.net.client.msg.TargetedActionMsg;
import engine.objects.*;
import engine.powers.DamageShield;
import engine.powers.effectmodifiers.AbstractEffectModifier;
import engine.powers.effectmodifiers.WeaponProcEffectModifier;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class CombatSystem {

    public static void attemptCombat(AbstractCharacter source, AbstractWorldObject target, boolean mainhand){

        //1. source or target doesn't exist, early exit
        if(source == null || target == null)
            return;

        //2. source or target is dead, early exit
        if(!source.isAlive() || !target.isAlive())
            return;

        //3. make sure if target is a building to ensure that it is damageable
        if(target.getObjectType().equals(Enum.GameObjectType.Building)){
            Building building = (Building)target;
            if(building.assetIsProtected() || building.getProtectionState().equals(Enum.ProtectionState.NPC))
                return;
        }

        //after thought: make sure target is in range of source
        if(!inRange(source,target,mainhand))
            return;

        //4. apply any weapon powers and then clear the weapon power memory for the player
        if (source.getObjectType().equals(Enum.GameObjectType.PlayerCharacter)) {
            PlayerCharacter pc = (PlayerCharacter)source;
            if(pc.getWeaponPower() != null){
                pc.getWeaponPower().attack(target,pc.getRange());
                pc.setWeaponPower(null);
            }
        }

        //5. make sure if target is AbstractCharacter to check for defense trigger and passive trigger
        if(AbstractCharacter.IsAbstractCharacter(target)) {
            int atr;
            int def;
            if (source.getObjectType().equals(Enum.GameObjectType.PlayerCharacter)) {
                PlayerCharacter pc = (PlayerCharacter)source;
                if(pc.combatStats == null)
                    pc.combatStats = new PlayerCombatStats(pc);
                atr = (int) pc.combatStats.atrHandOne;
                if(!mainhand)
                    atr =(int) pc.combatStats.atrHandTwo;

                def = pc.combatStats.defense;
            } else {
                atr = (int) ((source.getAtrHandOne() + source.getAtrHandTwo()) * 0.5f);
                def = source.defenseRating;
            }

            if(!LandHit(atr,def)) {
                createTimer(source,mainhand);
                return;
            }

            if(source.getBonuses() != null)
                if(!source.getBonuses().getBool(Enum.ModType.IgnorePassiveDefense, Enum.SourceType.None))
                    if(triggerPassive(source,target)) {
                        createTimer(source,mainhand);
                        return;
                    }
        }

        //commence actual combat management

        //6. check for any procs
        if (source.getObjectType().equals(Enum.GameObjectType.PlayerCharacter)) {
            PlayerCharacter pc = (PlayerCharacter)source;
            if(pc.getCharItemManager() != null && pc.getCharItemManager().getEquipped() != null){
                Item weapon = pc.getCharItemManager().getEquipped(1);
                if(!mainhand)
                    weapon = pc.getCharItemManager().getEquipped(2);
                if(weapon != null){
                    if(weapon.effects != null){
                        for (Effect eff : weapon.effects.values()){
                            for(AbstractEffectModifier mod : eff.getEffectModifiers()){
                                if(mod.modType.equals(Enum.ModType.WeaponProc)){
                                    int procChance = ThreadLocalRandom.current().nextInt(0,101);
                                    if (procChance <= MBServerStatics.PROC_CHANCE) {
                                        try {
                                            ((WeaponProcEffectModifier) mod).applyProc(source, target);
                                        }catch(Exception e){
                                            Logger.error(eff.getName() + " Failed To Cast Proc");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //7. configure damage amounts and type
        Enum.DamageType damageType = Enum.DamageType.Crush;
        int min = 0;
        int max = 0;
        if (source.getObjectType().equals(Enum.GameObjectType.PlayerCharacter)) {
            PlayerCharacter pc = (PlayerCharacter) source;
            if(mainhand){
                min = pc.combatStats.minDamageHandOne;
                max = pc.combatStats.maxDamageHandOne;
            }else{
                min = pc.combatStats.minDamageHandTwo;
                max = pc.combatStats.maxDamageHandTwo;
            }
        }else if (source.getObjectType().equals(Enum.GameObjectType.Mob)) {
            Mob mob = (Mob) source;
            min = (int) mob.mobBase.getDamageMin();
            max = (int) mob.mobBase.getDamageMax();
        }

        int damage = ThreadLocalRandom.current().nextInt(min,max + 1);

        if(source.getBonuses() != null){
            damage *= 1 + source.getBonuses().getFloatPercentAll(Enum.ModType.MeleeDamageModifier, Enum.SourceType.None);
        }
        if (source.getObjectType().equals(Enum.GameObjectType.PlayerCharacter)) {
            PlayerCharacter pc = (PlayerCharacter) source;
            damage *= pc.ZergMultiplier;
        }

        //8. configure the attack message to be sent to the clients
        int animation = 0;
        ItemBase wb = null;
        if(source.getCharItemManager() != null && source.getCharItemManager().getEquipped() != null) {
            Item weapon = source.getCharItemManager().getEquipped(1);
            if (!mainhand)
                weapon = source.getCharItemManager().getEquipped(2);

            if(weapon != null && weapon.getItemBase().getAnimations() != null && !weapon.getItemBase().getAnimations().isEmpty()){
                animation = weapon.getItemBase().getAnimations().get(0);
                wb = weapon.getItemBase();
                damageType = wb.getDamageType();
            }
        }

        //9. reduce damage from resists and apply damage shields
        if(AbstractCharacter.IsAbstractCharacter(target)){
            AbstractCharacter abs = (AbstractCharacter) target;
            damage = (int) abs.getResists().getResistedDamage(source, abs,damageType,damage,1);
            handleDamageShields(source,abs,damage);
        }



        sendCombatMessage(source, target, 0f, wb, null, mainhand, animation);

        //if attacker is player, set last attack timestamp
        if (source.getObjectType().equals(Enum.GameObjectType.PlayerCharacter))
            updateAttackTimers((PlayerCharacter) source, target);

        //10. cancel all effects that cancel on attack
        source.cancelOnAttack();
    }

    public static boolean LandHit(int ATR, int DEF){

        int roll = ThreadLocalRandom.current().nextInt(101);

        float chance = PlayerCombatStats.getHitChance(ATR,DEF);
        return chance >= roll;
    }

    private static void sendCombatMessage(AbstractCharacter source, AbstractWorldObject target, float damage, ItemBase wb, DeferredPowerJob dpj, boolean mainHand, int swingAnimation) {

        if (dpj != null)
            if (PowersManager.AnimationOverrides.containsKey(dpj.getAction().getEffectID()))
                swingAnimation = PowersManager.AnimationOverrides.get(dpj.getAction().getEffectID());

        if (source.getObjectType() == Enum.GameObjectType.PlayerCharacter)
            for (Effect eff : source.getEffects().values())
                if (eff.getPower() != null && (eff.getPower().getToken() == 429506943 || eff.getPower().getToken() == 429408639 || eff.getPower().getToken() == 429513599 || eff.getPower().getToken() == 429415295))
                    swingAnimation = 0;

        TargetedActionMsg cmm = new TargetedActionMsg(source, target, damage, swingAnimation);
        DispatchMessage.sendToAllInRange(target, cmm);
    }

    private static void updateAttackTimers(PlayerCharacter pc, AbstractWorldObject target) {

        //Set Attack Timers

        if (target.getObjectType().equals(Enum.GameObjectType.PlayerCharacter))
            pc.setLastPlayerAttackTime();
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

    public static boolean inRange(AbstractCharacter source, AbstractWorldObject target, boolean mainhand){

        if(source == null || target == null)
            return false;

        float distanceSquared = source.loc.distanceSquared(target.loc);

        float rangeSquared = 16.0f;

        if(source.getCharItemManager() != null && source.getCharItemManager().getEquipped() != null){
            Item weapon = source.getCharItemManager().getEquipped(1);
            if(!mainhand)
                weapon = source.getCharItemManager().getEquipped(2);
            if(weapon != null)
                rangeSquared = weapon.getItemBase().getRange() * weapon.getItemBase().getRange();
        }

        if(source.getBonuses() != null){
            rangeSquared *= 1 + source.getBonuses().getFloatPercentAll(Enum.ModType.WeaponRange, Enum.SourceType.None);
        }

        return distanceSquared <= rangeSquared;
    }

    public static boolean triggerPassive(AbstractCharacter source, AbstractWorldObject target) {
        boolean passiveFired = false;

        if (!AbstractCharacter.IsAbstractCharacter(target))
            return false;

        AbstractCharacter tarAc = (AbstractCharacter) target;
        //Handle Block passive
        if (testPassive(source, tarAc, "Block")) {
            sendPassiveDefenseMessage(source, null, target, MBServerStatics.COMBAT_SEND_DODGE, null, true);
            return true;
        }

        //Handle Parry passive
        if (testPassive(source, tarAc, "Parry")) {
            sendPassiveDefenseMessage(source, null, target, MBServerStatics.COMBAT_SEND_DODGE, null, true);
            return true;
        }

        //Handle Dodge passive
        if (testPassive(source, tarAc, "Dodge")) {
            sendPassiveDefenseMessage(source, null, target, MBServerStatics.COMBAT_SEND_DODGE, null, true);
            return true;
        }
        return false;
    }

    private static void sendPassiveDefenseMessage(AbstractCharacter source, ItemBase wb, AbstractWorldObject target, int passiveType, DeferredPowerJob dpj, boolean mainHand) {

        int swingAnimation = 75;

        if (dpj != null)
            if (PowersManager.AnimationOverrides.containsKey(dpj.getAction().getEffectID()))
                swingAnimation = PowersManager.AnimationOverrides.get(dpj.getAction().getEffectID());

        TargetedActionMsg cmm = new TargetedActionMsg(source, swingAnimation, target, passiveType);
        DispatchMessage.sendToAllInRange(target, cmm);
    }

    private static boolean testPassive(AbstractCharacter source, AbstractCharacter target, String type) {

        if(target.getBonuses() != null)
            if(target.getBonuses().getBool(Enum.ModType.Stunned, Enum.SourceType.None))
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

    private static void createTimer(AbstractCharacter source, boolean mainhand) {

        ConcurrentHashMap<String, JobContainer> timers = source.getTimers();
        int slot = 1;
        if(!mainhand)
            slot = 2;

        int time = 3000;
        if(source.getObjectType().equals(Enum.GameObjectType.PlayerCharacter)){
            PlayerCharacter pc = (PlayerCharacter)source;
            if(mainhand){
                time = (int) pc.combatStats.attackSpeedHandOne;
            }else{
                time = (int) pc.combatStats.attackSpeedHandTwo;
            }
        }

        if (timers != null) {
            AttackJob aj = new AttackJob(source, slot, true);
            JobContainer job;
            job = JobScheduler.getInstance().scheduleJob(aj, (time * 100));
            timers.put("Attack" + slot, job);
        } else {
            Logger.error("Unable to find Timers for Character " + source.getObjectUUID());
        }
    }

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

        if (targetType == Enum.GameObjectType.PlayerCharacter.ordinal()) {
            target = PlayerCharacter.getFromCache(msg.getTargetID());
        } else if (targetType == Enum.GameObjectType.Building.ordinal()) {
            target = BuildingManager.getBuildingFromCache(msg.getTargetID());
        } else if (targetType == Enum.GameObjectType.Mob.ordinal()) {
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

        boolean hasMain = false;
        boolean hasOff = false;
        if(player.getCharItemManager() != null && player.getCharItemManager().getEquipped() != null){
            if(player.getCharItemManager().getEquipped(1) != null)
                hasMain = true;
            if(player.getCharItemManager().getEquipped(2) != null && !player.getCharItemManager().getEquipped(2).getItemBase().isShield())
                hasOff = true;
        }

        if(hasMain){
            createTimer(player,true);
        }

        if(hasOff){
            createTimer(player,false);
        }

    }
}
