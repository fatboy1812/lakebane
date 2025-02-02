package engine.objects;

import engine.Enum;
import engine.gameManager.ChatManager;
import engine.powers.EffectsBase;
import engine.powers.effectmodifiers.AbstractEffectModifier;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCombatStats {

    public PlayerCharacter owner;
    //main hand data
    public int minDamageHandOne;
    public int maxDamageHandOne;
    public float attackSpeedHandOne;
    public float rangeHandOne;
    public float atrHandOne;
    //off hand data
    public int minDamageHandTwo;
    public int maxDamageHandTwo;
    public float attackSpeedHandTwo;
    public float rangeHandTwo;
    public float atrHandTwo;
    //defense
    public int defense;
    //regen rates
    public float healthRegen;
    public float manaRegen;
    public float staminaRegen;

    public PlayerCombatStats(PlayerCharacter pc) {
        this.owner = pc;
        this.update();
    }

    public void update() {
        try {
            this.calculateATR(true);
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE ATR FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateATR(false);
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE ATR FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateMin(true);
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Min FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateMin(false);
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Min FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateMax(true);
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Max FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateMax(false);
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Max FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateAttackSpeed(true);
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Attack Speed FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateAttackSpeed(false);
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Attack Speed FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateAttackRange(true);
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Attack Range FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateAttackRange(false);
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Attack Range FOR: " + this.owner.getObjectUUID());
        }
        //try {
            //this.calculateRegen();
        //} catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Regen FOR: " + this.owner.getObjectUUID());
        //}
        try {
            this.calculateDefense();
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Defense FOR: " + this.owner.getObjectUUID());
        }
    }

    public void calculateATR(boolean mainHand) {
        Item weapon;
        float atr;

        if(mainHand) {
            weapon = this.owner.charItemManager.getEquipped(1);
        }else {
            weapon = this.owner.charItemManager.getEquipped(2);
        }

        String skill = "Unarmed Combat";
        String mastery = "Unarmed Combat Mastery";
        int primaryStat = getDexAfterPenalty(this.owner);
        if(weapon != null) {
            skill= weapon.getItemBase().getSkillRequired();
            mastery = weapon.getItemBase().getMastery();
            if(weapon.getItemBase().isStrBased())
                primaryStat = this.owner.statStrCurrent;
        }

        float skillLevel = 0;
        float masteryLevel = 0;

        if(this.owner.skills.containsKey(skill)) {
            skillLevel = this.owner.skills.get(skill).getModifiedAmount();//calculateBuffedSkillLevel(skill,this.owner);//this.owner.skills.get(skill).getTotalSkillPercet();
        }
        if(this.owner.skills.containsKey(mastery))
            masteryLevel = this.owner.skills.get(mastery).getModifiedAmount();//calculateBuffedSkillLevel(mastery,this.owner);//this.owner.skills.get(mastery).getTotalSkillPercet();

        float stanceValue = 0.0f;
        float atrEnchants = 0;

        for(String effID : this.owner.effects.keySet()) {
            if (effID.contains("Stance")) {
                Effect effect = this.owner.effects.get(effID);
                EffectsBase eb = effect.getEffectsBase();
                if(eb.getIDString().equals("STC-H-DA"))
                    continue;
                for (AbstractEffectModifier mod : this.owner.effects.get(effID).getEffectModifiers()) {
                    if (mod.modType.equals(Enum.ModType.OCV)) {
                        float percent = mod.getPercentMod();
                        int trains = this.owner.effects.get(effID).getTrains();
                        float modValue = percent + (trains * mod.getRamp());
                        stanceValue += modValue * 0.01f;
                    }
                }
            } else {
                for (AbstractEffectModifier mod : this.owner.effects.get(effID).getEffectModifiers()) {
                    if (mod.modType.equals(Enum.ModType.OCV)) {
                        float value = mod.getMinMod();
                        int trains = this.owner.effects.get(effID).getTrains();
                        float modValue = value + (trains * mod.getRamp());
                        atrEnchants += modValue;
                    }
                }
            }
        }

        float prefixValues = 0.0f;
        if(weapon != null){
            if(this.owner.charItemManager.getEquipped(1) != null){
                for(Effect eff : this.owner.charItemManager.getEquipped(1).effects.values()){
                    for(AbstractEffectModifier mod : eff.getEffectModifiers()){
                        if(mod.modType.equals(Enum.ModType.OCV)){
                            prefixValues += mod.minMod + (eff.getTrains() * mod.getRamp());
                        }
                    }
                }
            }
        }
        if(this.owner.charItemManager.getEquipped(2) != null){
            for(Effect eff : this.owner.charItemManager.getEquipped(2).effects.values()){
                for(AbstractEffectModifier mod : eff.getEffectModifiers()){
                    if(mod.modType.equals(Enum.ModType.OCV)){
                        prefixValues += mod.minMod + (eff.getTrains() * mod.getRamp());
                    }
                }
            }
        }

        float preciseRune = 1.0f;
        for(CharacterRune rune : this.owner.runes){
            if(rune.getRuneBase().getName().equals("Precise"))
                preciseRune += 0.05f;
        }

        //if(weapon != null && weapon.getItemBase().isStrBased()){
        //    atr = (((primaryStat / 2) + (skillLevel * 4 + masteryLevel * 3) + prefixValues) * preciseRune + atrEnchants) * (1.0f + stanceValue);
        //    atr = (float) Math.round(atr);
        //}else {
            //float dexterity = getDexAfterPenalty(this.owner);
            atr = primaryStat / 2;
            atr += skillLevel * 4;
            atr += masteryLevel * 3;
            atr += prefixValues;
            atr *= preciseRune;
            atr += atrEnchants;
            atr *= 1 + (this.owner.bonuses.getFloatPercentAll(Enum.ModType.OCV, Enum.SourceType.Buff) - this.owner.bonuses.getFloatPercentAll(Enum.ModType.OCV, Enum.SourceType.DeBuff));
            atr *= 1.0f + stanceValue;
            atr = (float) Math.round(atr);
        //}



        if(mainHand){
            this.atrHandOne = atr;
        }else{
            this.atrHandTwo = atr;
            if(this.owner.charItemManager.getEquipped(1) == null && this.owner.charItemManager.getEquipped(2) != null){
                if(!this.owner.charItemManager.getEquipped(2).getItemBase().isShield())
                    this.atrHandOne = 0.0f;
            }else if(this.owner.charItemManager.getEquipped(2) == null && this.owner.charItemManager.getEquipped(1) != null){
                this.atrHandTwo = 0.0f;
            }
        }
    } //perfect DO NOT TOUCH

    public void calculateMin(boolean mainHand) {
        Item weapon;
        float specialDex = this.owner.statDexBase;
        specialDex += this.owner.bonuses.getFloat(Enum.ModType.Attr, Enum.SourceType.Dexterity);
        float baseDMG = 1;
        float primaryStat = specialDex;//getDexAfterPenalty(this.owner);
        float secondaryStat = this.owner.statStrCurrent;
        double weaponSkill = 5;
        double weaponMastery = 5;

        if (mainHand) {
            weapon = this.owner.charItemManager.getEquipped(1);
        } else {
            weapon = this.owner.charItemManager.getEquipped(2);
        }

        String skill = "Unarmed Combat";
        String mastery = "Unarmed Combat Mastery";

        if (weapon != null) {
            baseDMG = weapon.getItemBase().getMinDamage();
            skill = weapon.getItemBase().getSkillRequired();
            mastery = weapon.getItemBase().getMastery();
            if (weapon.getItemBase().isStrBased()) {
                primaryStat = this.owner.statStrCurrent;
                secondaryStat = specialDex;//getDexAfterPenalty(this.owner);
            }
            for(Effect eff : weapon.effects.values()){
                for(AbstractEffectModifier mod : eff.getEffectModifiers()){
                    if(mod.modType.equals(Enum.ModType.MinDamage)){
                        baseDMG += mod.minMod + (mod.getRamp() * eff.getTrains());
                    }
                }
            }
        }

        if (this.owner.skills.containsKey(skill)) {
            weaponSkill = calculateBaseSkillLevel(skill,this.owner);//this.owner.skills.get(skill).getTotalSkillPercet();
        }

        if (this.owner.skills.containsKey(mastery)) {
            weaponMastery = calculateBaseSkillLevel(mastery,this.owner);this.owner.skills.get(mastery).getTotalSkillPercet();
        }

        double minDMG = baseDMG * (
                0.0048 * primaryStat +
                        0.049 * Math.sqrt(primaryStat - 0.75) +
                        0.0066 * secondaryStat +
                        0.064 * Math.sqrt(secondaryStat - 0.75) +
                        0.01 * (weaponSkill + weaponMastery)
        );
        if(this.owner.bonuses != null){
            minDMG += this.owner.bonuses.getFloat(Enum.ModType.MinDamage, Enum.SourceType.None);
            minDMG *= 1 + this.owner.bonuses.getFloatPercentAll(Enum.ModType.MeleeDamageModifier, Enum.SourceType.None);
        }

        if(this.owner.charItemManager != null){
            if(this.owner.charItemManager.getEquipped(1) != null && this.owner.charItemManager.getEquipped(2) != null && !this.owner.charItemManager.getEquipped(2).getItemBase().isShield()){
                minDMG *= 0.7f;
            }
        }

        int roundedMin = (int)Math.round(minDMG);

        if (mainHand) {
            this.minDamageHandOne = roundedMin;
        } else {
            this.minDamageHandTwo = roundedMin;
            if(this.owner.charItemManager.getEquipped(1) == null && this.owner.charItemManager.getEquipped(2) != null){
                if(!this.owner.charItemManager.getEquipped(2).getItemBase().isShield())
                    this.minDamageHandOne = 0;
            }else if(this.owner.charItemManager.getEquipped(2) == null && this.owner.charItemManager.getEquipped(1) != null){
                this.minDamageHandTwo = 0;
            }
        }
    }

    public void calculateMax(boolean mainHand) {
        //Weapon Max DMG = BaseDMG * (0.0124*Primary Stat + 0.118*(Primary Stat -0.75)^0.5
        // + 0.0022*Secondary Stat + 0.028*(Secondary Stat-0.75)^0.5 + 0.0075*(Weapon Skill + Weapon Mastery))
        Item weapon;
        float specialDex = this.owner.statDexBase;
        specialDex += this.owner.bonuses.getFloat(Enum.ModType.Attr, Enum.SourceType.Dexterity);
        double baseDMG = 5;
        float primaryStat = specialDex;//getDexAfterPenalty(this.owner);
        float secondaryStat = this.owner.statStrCurrent;
        double weaponSkill = 5;
        double weaponMastery = 5;


        if (mainHand) {
            weapon = this.owner.charItemManager.getEquipped(1);
        } else {
            weapon = this.owner.charItemManager.getEquipped(2);
        }

        String skill = "Unarmed Combat";
        String mastery = "Unarmed Combat Mastery";
        if (weapon != null) {
            baseDMG = weapon.getItemBase().getMaxDamage();
            skill = weapon.getItemBase().getSkillRequired();
            mastery = weapon.getItemBase().getMastery();
            if (weapon.getItemBase().isStrBased()) {
                primaryStat = this.owner.statStrCurrent;
                secondaryStat = specialDex;//getDexAfterPenalty(this.owner);
            }
            for(Effect eff : weapon.effects.values()){
                for(AbstractEffectModifier mod : eff.getEffectModifiers()){
                    if(mod.modType.equals(Enum.ModType.MaxDamage)){
                        baseDMG += mod.minMod + (mod.getRamp() * eff.getTrains());
                    }
                }
            }
        }

        if (this.owner.skills.containsKey(skill)) {
            weaponSkill = calculateBuffedSkillLevel(skill,this.owner);//this.owner.skills.get(skill).getModifiedAmount();
        }

        if (this.owner.skills.containsKey(mastery)) {
            weaponMastery = calculateBuffedSkillLevel(mastery,this.owner);//this.owner.skills.get(mastery).getModifiedAmount();
        }

        double maxDMG = baseDMG * (
                0.0124 * primaryStat +
                        0.118 * Math.sqrt(primaryStat - 0.75) +
                        0.0022 * secondaryStat +
                        0.028 * Math.sqrt(secondaryStat - 0.75) +
                        0.0075 * (weaponSkill + weaponMastery)
        );

        if(this.owner.bonuses != null){
            maxDMG += this.owner.bonuses.getFloat(Enum.ModType.MaxDamage, Enum.SourceType.None);
            maxDMG *= 1 + this.owner.bonuses.getFloatPercentAll(Enum.ModType.MeleeDamageModifier, Enum.SourceType.None);
        }

        if(this.owner.charItemManager != null){
            if(this.owner.charItemManager.getEquipped(1) != null && this.owner.charItemManager.getEquipped(2) != null && !this.owner.charItemManager.getEquipped(2).getItemBase().isShield()){
                maxDMG *= 0.7f;
            }
        }

        int roundedMax = (int)(maxDMG);

        if(mainHand){
            this.maxDamageHandOne = roundedMax;
        }else{
            this.maxDamageHandTwo = roundedMax;
            if(this.owner.charItemManager.getEquipped(1) == null && this.owner.charItemManager.getEquipped(2) != null){
                if(!this.owner.charItemManager.getEquipped(2).getItemBase().isShield())
                    this.maxDamageHandOne = 0;
            }else if(this.owner.charItemManager.getEquipped(2) == null && this.owner.charItemManager.getEquipped(1) != null){
                this.maxDamageHandTwo = 0;
            }
        }
    }

    public void calculateAttackSpeed(boolean mainHand){
        Item weapon;
        float speed;
        if(mainHand) {
            weapon = this.owner.charItemManager.getEquipped(1);
        }else {
            weapon = this.owner.charItemManager.getEquipped(2);
        }
        float delayExtra = 0;
        if(weapon == null) {
            speed = 20.0f;
        }else{
            speed = weapon.getItemBase().getSpeed();
            for(Effect eff : weapon.effects.values()){
                for(AbstractEffectModifier mod : eff.getEffectModifiers()){
                    if(mod.modType.equals(Enum.ModType.WeaponSpeed) || mod.modType.equals(Enum.ModType.AttackDelay)){
                        float percent = mod.getPercentMod();
                        int trains = eff.getTrains();
                        float modValue = percent + (trains * mod.getRamp());
                        speed *= 1 + (modValue * 0.01f);
                    }
                }
            }
        }
        if(this.owner.charItemManager.getEquipped(1) != null){
            for(Effect eff : this.owner.charItemManager.getEquipped(1).effects.values()){
                for(AbstractEffectModifier mod : eff.getEffectModifiers()){
                    if(mod.modType.equals(Enum.ModType.AttackDelay)){
                        float percent = mod.getPercentMod();
                        int trains = eff.getTrains();
                        float modValue = percent + (trains * mod.getRamp());
                        delayExtra += modValue * 0.01f;
                    }
                }
            }
        }
        if(this.owner.charItemManager.getEquipped(2) != null){
            for(Effect eff : this.owner.charItemManager.getEquipped(2).effects.values()){
                for(AbstractEffectModifier mod : eff.getEffectModifiers()){
                    if(mod.modType.equals(Enum.ModType.AttackDelay)){
                        float percent = mod.getPercentMod();
                        int trains = eff.getTrains();
                        float modValue = percent + (trains * mod.getRamp());
                        delayExtra += modValue * 0.01f;
                    }
                }
            }
        }

        float stanceValue = 0.0f;
        for(String effID : this.owner.effects.keySet()){
            if(effID.contains("Stance")){
                if(this.owner.effects != null) {
                    for (AbstractEffectModifier mod : this.owner.effects.get(effID).getEffectModifiers()) {
                        if (mod.modType.equals(Enum.ModType.AttackDelay)) {
                            float percent = mod.getPercentMod();
                            int trains = this.owner.effects.get(effID).getTrains();
                            float modValue = percent + (trains * mod.getRamp());
                            stanceValue += modValue * 0.01f;
                        }
                    }
                }
            }
        }

        float bonusValues = 1 + this.owner.bonuses.getFloatPercentAll(Enum.ModType.AttackDelay,Enum.SourceType.None);//1.0f;
        bonusValues -= stanceValue + delayExtra; // take away stance modifier from alac bonus values
        speed *= 1 + stanceValue; // apply stance bonus
        speed *= bonusValues; // apply alac bonuses without stance mod

        if(speed < 10.0f)
            speed = 10.0f;

        if(mainHand){
            this.attackSpeedHandOne = speed;
        }else{
            this.attackSpeedHandTwo = speed;
            if(this.owner.charItemManager.getEquipped(1) == null && this.owner.charItemManager.getEquipped(2) != null){
                if(!this.owner.charItemManager.getEquipped(2).getItemBase().isShield())
                    this.attackSpeedHandOne = 0.0f;
            }else if(this.owner.charItemManager.getEquipped(2) == null && this.owner.charItemManager.getEquipped(1) != null){
                this.attackSpeedHandTwo = 0.0f;
            }
        }
    }

    public void calculateAttackRange(boolean mainHand){
        Item weapon;
        float range;
        if(mainHand) {
            weapon = this.owner.charItemManager.getEquipped(1);
        }else {
            weapon = this.owner.charItemManager.getEquipped(2);
        }

        if(weapon == null) {
            range = 6.0f;
        }else{
            range = weapon.getItemBase().getRange();
        }
        if(owner.bonuses != null){
            range *= 1 + this.owner.bonuses.getFloatPercentAll(Enum.ModType.WeaponRange, Enum.SourceType.None);
        }
        if(mainHand){
            this.rangeHandOne = range;
        }else{
            this.rangeHandTwo = range;
            if(this.owner.charItemManager.getEquipped(1) == null && this.owner.charItemManager.getEquipped(2) != null){
                if(!this.owner.charItemManager.getEquipped(2).getItemBase().isShield())
                    this.rangeHandOne = 0.0f;
            }else if(this.owner.charItemManager.getEquipped(2) == null && this.owner.charItemManager.getEquipped(1) != null){
                this.rangeHandTwo = 0.0f;
            }
        }
    }

    public void calculateRegen(){
        if(owner.bonuses != null){
            this.healthRegen = 1.0f + this.owner.bonuses.getFloatPercentAll(Enum.ModType.HealthRecoverRate, Enum.SourceType.None);
            this.manaRegen = 1.0f + this.owner.bonuses.getFloatPercentAll(Enum.ModType.ManaRecoverRate, Enum.SourceType.None);
            this.staminaRegen = 1.0f + this.owner.bonuses.getFloatPercentAll(Enum.ModType.StaminaRecoverRate, Enum.SourceType.None);

        }else{
            this.healthRegen = 1.0f;
            this.manaRegen = 1.0f;
            this.staminaRegen = 1.0f;
        }
    }

    public void calculateDefense() {
        //Defense = (1+Armor skill / 50) * Armor defense + (1 + Block skill / 100) * Shield defense + (Primary weapon skill / 2)
        // + (Weapon mastery skill/ 2) + Dexterity * 2 + Flat bonuses from rings or cloth
        float armorSkill = 0.0f;
        float armorDefense = 0.0f;
        ArrayList<String> armorsUsed = new ArrayList<>();
        for(Item equipped : this.owner.charItemManager.getEquipped().values()){
            ItemBase ib = equipped.getItemBase();
            if(ib.isHeavyArmor() || ib.isMediumArmor() || ib.isLightArmor() || ib.isClothArmor()){
                armorDefense += ib.getDefense();
                for(Effect eff : equipped.effects.values()){
                    for(AbstractEffectModifier mod : eff.getEffectModifiers()){
                        if(mod.modType.equals(Enum.ModType.DR)){
                            armorDefense += mod.minMod + (mod.getRamp() * eff.getTrains());
                        }
                    }
                }
                if(!ib.isClothArmor() && !armorsUsed.contains(ib.getSkillRequired())) {
                    armorsUsed.add(ib.getSkillRequired());
                }
            }
        }
        for(String armorUsed : armorsUsed){
            if(this.owner.skills.containsKey(armorUsed)) {
                armorSkill += calculateBuffedSkillLevel(armorUsed,this.owner);
            }
        }
        if(armorsUsed.size() > 0)
            armorSkill = armorSkill / armorsUsed.size();

        float blockSkill = 0.0f;
        if(this.owner.skills.containsKey("Block"))
            blockSkill = calculateBuffedSkillLevel("Block",this.owner);

        float shieldDefense = 0.0f;
        if(this.owner.charItemManager.getEquipped(2) != null && this.owner.charItemManager.getEquipped(2).getItemBase().isShield()){
            Item shield = this.owner.charItemManager.getEquipped(2);
            shieldDefense += shield.getItemBase().getDefense();
            for(Effect eff : shield.effects.values()){
                for(AbstractEffectModifier mod : eff.getEffectModifiers()){
                    if(mod.modType.equals(Enum.ModType.DR)){
                        shieldDefense += mod.minMod + (mod.getRamp() * eff.getTrains());
                    }
                }
            }
        }

        float weaponSkill = 0.0f;
        float masterySkill = 0.0f;
        Item weapon = this.owner.charItemManager.getEquipped(1);
        if(weapon == null){
            weapon = this.owner.charItemManager.getEquipped(2);
        }
        if(weapon != null && weapon.getItemBase().isShield())
            weapon = null;

        String skillName = "Unarmed Combat";
        String masteryName = "Unarmed Combat Mastery";

        if(weapon != null){
            skillName = weapon.getItemBase().getSkillRequired();
            masteryName = weapon.getItemBase().getMastery();
        }
        if(this.owner.skills.containsKey(skillName))
            weaponSkill = calculateBuffedSkillLevel(skillName,this.owner);//this.owner.skills.get(skillName).getModifiedAmount();//calculateModifiedSkill(skillName,this.owner);//this.owner.skills.get(skillName).getModifiedAmount();

        if(this.owner.skills.containsKey(masteryName))
            masterySkill = calculateBuffedSkillLevel(masteryName,this.owner);//this.owner.skills.get(masteryName).getModifiedAmount();//calculateModifiedSkill(masteryName,this.owner);//this.owner.skills.get(masteryName).getModifiedAmount();

        float dexterity = getDexAfterPenalty(this.owner);

        float luckyRune = 1.0f;
        for(CharacterRune rune : this.owner.runes){
            if(rune.getRuneBase().getName().equals("Lucky"))
                luckyRune += 0.05f;
        }

        float flatBonuses = 0.0f;
        float stanceMod = 1.0f;
        for(String effID : this.owner.effects.keySet()) {
            if (effID.contains("Stance")) {
                for (AbstractEffectModifier mod : this.owner.effects.get(effID).getEffectModifiers()) {
                    if (mod.modType.equals(Enum.ModType.DCV)) {
                        float percent = mod.getPercentMod();
                        int trains = this.owner.effects.get(effID).getTrains();
                        float modValue = percent + (trains * mod.getRamp());
                        stanceMod += modValue * 0.01f;
                    }
                }
            } else {
                for (AbstractEffectModifier mod : this.owner.effects.get(effID).getEffectModifiers()) {
                    if (mod.modType.equals(Enum.ModType.DCV)) {
                        float value = mod.getMinMod();
                        int trains = this.owner.effects.get(effID).getTrains();
                        float modValue = value + (trains * mod.getRamp());
                        flatBonuses += modValue;
                    }
                }
            }
        }
        if(this.owner.charItemManager.getEquipped(2) == null)
            blockSkill = 0;
        else if(this.owner.charItemManager != null && this.owner.charItemManager.getEquipped(2) != null && !this.owner.charItemManager.getEquipped(2).getItemBase().isShield())
            blockSkill = 0;

        //Defense = (1+Armor skill / 50) * Armor defense + (1 + Block skill / 100) * Shield defense
        // + (Primary weapon skill / 2) + (Weapon mastery skill/ 2) + ROUND((Dexterity-Dex penalty),0) * 2 + Flat bonuses from rings or cloth
        float defense = 0;
        for(Item equipped : this.owner.charItemManager.getEquippedList()){
            ItemBase ib = equipped.getItemBase();
            if(ib.getType().equals(Enum.ItemType.ARMOR) && !ib.isShield()){
                defense += getArmorDefense(equipped,this.owner);
            }
        }
        //float defense = (1 + armorSkill / 50) * armorDefense;
        defense += (1 + blockSkill / 100) * shieldDefense;
        defense += (weaponSkill / 2);
        defense += (masterySkill / 2);
        defense += dexterity * 2;
        defense += flatBonuses;
        defense *= luckyRune;
        defense *= stanceMod;

        defense = Math.round(defense);

        this.defense = (int) defense;
    }

    public static int getDexAfterPenalty(PlayerCharacter pc){
        if(pc.charItemManager == null)
            return pc.statDexCurrent;

        float dex = pc.statDexBase;
        if(pc.bonuses != null)
            dex += pc.bonuses.getFloat(Enum.ModType.Attr, Enum.SourceType.Dexterity);

        float penaltyFactor = 0.0f;
        for(Item equipped : pc.charItemManager.getEquipped().values()){
            ItemBase ib = equipped.getItemBase();
            if(ib.isHeavyArmor() || ib.isLightArmor() || ib.isMediumArmor()){
                penaltyFactor += ib.dexReduction;
            }
        }

        if(penaltyFactor > 0)
            penaltyFactor *= 0.01f;

        float totalPenalty = dex *  penaltyFactor;
        float returnedDex = Math.round(dex - totalPenalty);
        return (int) returnedDex;

    }

    private static float getArmorDefense(Item armor, PlayerCharacter pc) {

        if (armor == null)
            return 0;

        ItemBase ib = armor.getItemBase();

        if (ib == null)
            return 0;

        if (!ib.getType().equals(Enum.ItemType.ARMOR))
            return 0;

        if (ib.getSkillRequired().isEmpty())
            return ib.getDefense();

        CharacterSkill armorSkill = pc.skills.get(ib.getSkillRequired());
        if (armorSkill == null) {
            Logger.error("Player " + pc.getObjectUUID()
                    + " has armor equipped without the nescessary skill to equip it");
            return ib.getDefense();
        }

        float def = ib.getDefense();
        //apply item defense bonuses
        if (armor != null) {

            for(Effect eff : armor.effects.values()){
                for(AbstractEffectModifier mod : eff.getEffectModifiers()){
                    if(mod.modType.equals(Enum.ModType.DR)){
                        def += mod.minMod * (1+(eff.getTrains() * mod.getRamp()));
                    }
                }
            }


            //def += armor.getBonus(ModType.DR, SourceType.None);
            //def *= (1 + armor.getBonusPercent(ModType.DR, SourceType.None));
        }
        float skillLevel = calculateBuffedSkillLevel(armorSkill.getName(),pc);
        //return (def * (1 + ((int) armorSkill.getModifiedAmount() / 50f)));
        return (def * (1 + ((int) skillLevel / 50f)));
    }

    public static int calculateBaseSkillLevel(String skillName, PlayerCharacter pc){
        if(pc.skills.containsKey(skillName)) {
            CharacterSkill skill = pc.skills.get(skillName);
            return Math.round(skill.getModifiedAmountBeforeMods());
        }else {
            return 0;
        }
    }
    public static int calculateBuffedSkillLevel(String skillName, PlayerCharacter pc){
        if(pc.skills.containsKey(skillName)) {
            return Math.round(pc.skills.get(skillName).getTotalSkillPercet());
        }else {
            return 0;
        }
    }

    public static void PrintSkillsToClient(PlayerCharacter pc){
        for(CharacterSkill skill : pc.skills.values()){
            String name = skill.getName();
            int base = calculateBaseSkillLevel(name,pc);//calculateBaseSkillLevel(name,pc);
            int buffed = calculateBuffedSkillLevel(name,pc);//calculateBuffedSkillLevel(name,pc);
            ChatManager.chatSystemInfo(pc,name + " = " + base + " (" + buffed + ")");
        }
    }
}
