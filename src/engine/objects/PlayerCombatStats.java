package engine.objects;

import engine.Enum;
import engine.powers.EffectsBase;
import engine.powers.effectmodifiers.AbstractEffectModifier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlayerCombatStats {

    public PlayerCharacter owner;
    //main hand data
    public int minDamageHandOne;
    public int maxDamageHandOne;
    public float attackSpeedHandOne;
    public float rangeHandOne;
    public float atrHandOne;
    //offhand data
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
    public static final Map<Float, Float> HIT_VALUE_MAP = new HashMap<>();

    static {
        HIT_VALUE_MAP.put(0.40f, 0f);
        HIT_VALUE_MAP.put(0.41f, 1f);
        HIT_VALUE_MAP.put(0.42f, 1f);
        HIT_VALUE_MAP.put(0.43f, 1f);
        HIT_VALUE_MAP.put(0.44f, 2f);
        HIT_VALUE_MAP.put(0.45f, 2f);
        HIT_VALUE_MAP.put(0.46f, 2f);
        HIT_VALUE_MAP.put(0.47f, 3f);
        HIT_VALUE_MAP.put(0.48f, 3f);
        HIT_VALUE_MAP.put(0.49f, 4f);
        HIT_VALUE_MAP.put(0.50f, 4f);
        HIT_VALUE_MAP.put(0.51f, 5f);
        HIT_VALUE_MAP.put(0.52f, 5f);
        HIT_VALUE_MAP.put(0.53f, 6f);
        HIT_VALUE_MAP.put(0.54f, 7f);
        HIT_VALUE_MAP.put(0.55f, 7f);
        HIT_VALUE_MAP.put(0.56f, 8f);
        HIT_VALUE_MAP.put(0.57f, 9f);
        HIT_VALUE_MAP.put(0.58f, 9f);
        HIT_VALUE_MAP.put(0.59f, 10f);
        HIT_VALUE_MAP.put(0.60f, 11f);
        HIT_VALUE_MAP.put(0.61f, 11f);
        HIT_VALUE_MAP.put(0.62f, 12f);
        HIT_VALUE_MAP.put(0.63f, 13f);
        HIT_VALUE_MAP.put(0.64f, 13f);
        HIT_VALUE_MAP.put(0.65f, 14f);
        HIT_VALUE_MAP.put(0.66f, 15f);
        HIT_VALUE_MAP.put(0.67f, 15f);
        HIT_VALUE_MAP.put(0.68f, 16f);
        HIT_VALUE_MAP.put(0.69f, 17f);
        HIT_VALUE_MAP.put(0.70f, 17f);
        HIT_VALUE_MAP.put(0.71f, 18f);
        HIT_VALUE_MAP.put(0.72f, 19f);
        HIT_VALUE_MAP.put(0.73f, 20f);
        HIT_VALUE_MAP.put(0.74f, 20f);
        HIT_VALUE_MAP.put(0.75f, 21f);
        HIT_VALUE_MAP.put(0.76f, 22f);
        HIT_VALUE_MAP.put(0.77f, 23f);
        HIT_VALUE_MAP.put(0.78f, 24f);
        HIT_VALUE_MAP.put(0.79f, 24f);
        HIT_VALUE_MAP.put(0.80f, 25f);
        HIT_VALUE_MAP.put(0.81f, 26f);
        HIT_VALUE_MAP.put(0.82f, 27f);
        HIT_VALUE_MAP.put(0.83f, 28f);
        HIT_VALUE_MAP.put(0.84f, 29f);
        HIT_VALUE_MAP.put(0.85f, 30f);
        HIT_VALUE_MAP.put(0.86f, 31f);
        HIT_VALUE_MAP.put(0.87f, 32f);
        HIT_VALUE_MAP.put(0.88f, 33f);
        HIT_VALUE_MAP.put(0.89f, 34f);
        HIT_VALUE_MAP.put(0.90f, 35f);
        HIT_VALUE_MAP.put(0.91f, 36f);
        HIT_VALUE_MAP.put(0.92f, 37f);
        HIT_VALUE_MAP.put(0.93f, 38f);
        HIT_VALUE_MAP.put(0.94f, 39f);
        HIT_VALUE_MAP.put(0.95f, 40f);
        HIT_VALUE_MAP.put(0.96f, 42f);
        HIT_VALUE_MAP.put(0.97f, 44f);
        HIT_VALUE_MAP.put(0.98f, 46f);
        HIT_VALUE_MAP.put(0.99f, 48f);
        HIT_VALUE_MAP.put(1.00f, 50f);
        HIT_VALUE_MAP.put(1.01f, 52f);
        HIT_VALUE_MAP.put(1.02f, 54f);
        HIT_VALUE_MAP.put(1.03f, 56f);
        HIT_VALUE_MAP.put(1.04f, 58f);
        HIT_VALUE_MAP.put(1.05f, 60f);
        HIT_VALUE_MAP.put(1.06f, 61f);
        HIT_VALUE_MAP.put(1.07f, 62f);
        HIT_VALUE_MAP.put(1.08f, 63f);
        HIT_VALUE_MAP.put(1.09f, 64f);
        HIT_VALUE_MAP.put(1.10f, 65f);
        HIT_VALUE_MAP.put(1.11f, 66f);
        HIT_VALUE_MAP.put(1.12f, 67f);
        HIT_VALUE_MAP.put(1.13f, 68f);
        HIT_VALUE_MAP.put(1.14f, 69f);
        HIT_VALUE_MAP.put(1.15f, 70f);
        HIT_VALUE_MAP.put(1.16f, 70f);
        HIT_VALUE_MAP.put(1.17f, 71f);
        HIT_VALUE_MAP.put(1.18f, 71f);
        HIT_VALUE_MAP.put(1.19f, 72f);
        HIT_VALUE_MAP.put(1.20f, 72f);
        HIT_VALUE_MAP.put(1.21f, 73f);
        HIT_VALUE_MAP.put(1.22f, 73f);
        HIT_VALUE_MAP.put(1.23f, 74f);
        HIT_VALUE_MAP.put(1.24f, 74f);
        HIT_VALUE_MAP.put(1.25f, 75f);
        HIT_VALUE_MAP.put(1.26f, 75f);
        HIT_VALUE_MAP.put(1.27f, 76f);
        HIT_VALUE_MAP.put(1.28f, 76f);
        HIT_VALUE_MAP.put(1.29f, 77f);
        HIT_VALUE_MAP.put(1.30f, 77f);
        HIT_VALUE_MAP.put(1.31f, 78f);
        HIT_VALUE_MAP.put(1.32f, 78f);
        HIT_VALUE_MAP.put(1.33f, 79f);
        HIT_VALUE_MAP.put(1.34f, 79f);
        HIT_VALUE_MAP.put(1.35f, 80f);
        HIT_VALUE_MAP.put(1.36f, 80f);
        HIT_VALUE_MAP.put(1.37f, 81f);
        HIT_VALUE_MAP.put(1.38f, 81f);
        HIT_VALUE_MAP.put(1.39f, 81f);
        HIT_VALUE_MAP.put(1.40f, 82f);
        HIT_VALUE_MAP.put(1.41f, 82f);
        HIT_VALUE_MAP.put(1.42f, 82f);
        HIT_VALUE_MAP.put(1.43f, 83f);
        HIT_VALUE_MAP.put(1.44f, 83f);
        HIT_VALUE_MAP.put(1.45f, 83f);
        HIT_VALUE_MAP.put(1.46f, 84f);
        HIT_VALUE_MAP.put(1.47f, 84f);
        HIT_VALUE_MAP.put(1.48f, 84f);
        HIT_VALUE_MAP.put(1.49f, 85f);
        HIT_VALUE_MAP.put(1.50f, 85f);
        HIT_VALUE_MAP.put(1.51f, 85f);
        HIT_VALUE_MAP.put(1.52f, 86f);
        HIT_VALUE_MAP.put(1.53f, 86f);
        HIT_VALUE_MAP.put(1.54f, 86f);
        HIT_VALUE_MAP.put(1.55f, 86f);
        HIT_VALUE_MAP.put(1.56f, 87f);
        HIT_VALUE_MAP.put(1.57f, 87f);
        HIT_VALUE_MAP.put(1.58f, 87f);
        HIT_VALUE_MAP.put(1.59f, 87f);
        HIT_VALUE_MAP.put(1.60f, 88f);
        HIT_VALUE_MAP.put(1.61f, 88f);
        HIT_VALUE_MAP.put(1.62f, 88f);
        HIT_VALUE_MAP.put(1.63f, 88f);
        HIT_VALUE_MAP.put(1.64f, 89f);
        HIT_VALUE_MAP.put(1.65f, 89f);
        HIT_VALUE_MAP.put(1.66f, 89f);
        HIT_VALUE_MAP.put(1.67f, 89f);
        HIT_VALUE_MAP.put(1.68f, 90f);
        HIT_VALUE_MAP.put(1.69f, 90f);
        HIT_VALUE_MAP.put(1.70f, 90f);
        HIT_VALUE_MAP.put(1.71f, 90f);
        HIT_VALUE_MAP.put(1.72f, 91f);
        HIT_VALUE_MAP.put(1.73f, 91f);
        HIT_VALUE_MAP.put(1.74f, 91f);
        HIT_VALUE_MAP.put(1.75f, 91f);
        HIT_VALUE_MAP.put(1.76f, 91f);
        HIT_VALUE_MAP.put(1.77f, 92f);
        HIT_VALUE_MAP.put(1.78f, 92f);
        HIT_VALUE_MAP.put(1.79f, 92f);
        HIT_VALUE_MAP.put(1.80f, 92f);
        HIT_VALUE_MAP.put(1.81f, 92f);
        HIT_VALUE_MAP.put(1.82f, 93f);
        HIT_VALUE_MAP.put(1.83f, 93f);
        HIT_VALUE_MAP.put(1.84f, 93f);
        HIT_VALUE_MAP.put(1.85f, 93f);
        HIT_VALUE_MAP.put(1.86f, 93f);
        HIT_VALUE_MAP.put(1.87f, 93f);
        HIT_VALUE_MAP.put(1.88f, 94f);
        HIT_VALUE_MAP.put(1.89f, 94f);
        HIT_VALUE_MAP.put(1.90f, 94f);
        HIT_VALUE_MAP.put(1.91f, 94f);
        HIT_VALUE_MAP.put(1.92f, 94f);
        HIT_VALUE_MAP.put(1.93f, 94f);
        HIT_VALUE_MAP.put(1.94f, 95f);
        HIT_VALUE_MAP.put(1.95f, 95f);
        HIT_VALUE_MAP.put(1.96f, 95f);
        HIT_VALUE_MAP.put(1.97f, 95f);
        HIT_VALUE_MAP.put(1.98f, 95f);
        HIT_VALUE_MAP.put(1.99f, 95f);
        HIT_VALUE_MAP.put(2.00f, 96f);
        HIT_VALUE_MAP.put(2.01f, 96f);
        HIT_VALUE_MAP.put(2.02f, 96f);
        HIT_VALUE_MAP.put(2.03f, 96f);
        HIT_VALUE_MAP.put(2.04f, 96f);
        HIT_VALUE_MAP.put(2.05f, 96f);
        HIT_VALUE_MAP.put(2.06f, 96f);
        HIT_VALUE_MAP.put(2.07f, 96f);
        HIT_VALUE_MAP.put(2.08f, 97f);
        HIT_VALUE_MAP.put(2.09f, 97f);
        HIT_VALUE_MAP.put(2.10f, 97f);
        HIT_VALUE_MAP.put(2.11f, 97f);
        HIT_VALUE_MAP.put(2.12f, 97f);
        HIT_VALUE_MAP.put(2.13f, 97f);
        HIT_VALUE_MAP.put(2.14f, 97f);
        HIT_VALUE_MAP.put(2.15f, 97f);
        HIT_VALUE_MAP.put(2.16f, 97f);
        HIT_VALUE_MAP.put(2.17f, 97f);
        HIT_VALUE_MAP.put(2.18f, 98f);
        HIT_VALUE_MAP.put(2.19f, 98f);
        HIT_VALUE_MAP.put(2.20f, 98f);
        HIT_VALUE_MAP.put(2.21f, 98f);
        HIT_VALUE_MAP.put(2.22f, 98f);
        HIT_VALUE_MAP.put(2.23f, 98f);
        HIT_VALUE_MAP.put(2.24f, 98f);
        HIT_VALUE_MAP.put(2.25f, 98f);
        HIT_VALUE_MAP.put(2.26f, 98f);
        HIT_VALUE_MAP.put(2.27f, 98f);
        HIT_VALUE_MAP.put(2.28f, 98f);
        HIT_VALUE_MAP.put(2.29f, 98f);
        HIT_VALUE_MAP.put(2.30f, 98f);
        HIT_VALUE_MAP.put(2.31f, 98f);
        HIT_VALUE_MAP.put(2.32f, 99f);
        HIT_VALUE_MAP.put(2.33f, 99f);
        HIT_VALUE_MAP.put(2.34f, 99f);
        HIT_VALUE_MAP.put(2.35f, 99f);
        HIT_VALUE_MAP.put(2.36f, 99f);
        HIT_VALUE_MAP.put(2.37f, 99f);
        HIT_VALUE_MAP.put(2.38f, 99f);
        HIT_VALUE_MAP.put(2.39f, 99f);
        HIT_VALUE_MAP.put(2.40f, 99f);
        HIT_VALUE_MAP.put(2.41f, 99f);
        HIT_VALUE_MAP.put(2.42f, 99f);
        HIT_VALUE_MAP.put(2.43f, 99f);
        HIT_VALUE_MAP.put(2.44f, 99f);
        HIT_VALUE_MAP.put(2.45f, 99f);
        HIT_VALUE_MAP.put(2.46f, 99f);
        HIT_VALUE_MAP.put(2.47f, 99f);
        HIT_VALUE_MAP.put(2.48f, 99f);
        HIT_VALUE_MAP.put(2.49f, 99f);
        HIT_VALUE_MAP.put(2.50f, 100f);
    }

    public PlayerCombatStats(PlayerCharacter pc) {
        this.owner = pc;
        this.update();
    }

    public void update() {
        try {
            this.calculateATR(true);
            this.owner.atrHandOne = (int) this.atrHandOne;
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE ATR FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateATR(false);
            this.owner.atrHandTwo = (int) this.atrHandTwo;
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE ATR FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateMin(true);
            this.owner.minDamageHandOne = this.minDamageHandOne;
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Min FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateMin(false);
            this.owner.minDamageHandTwo = this.minDamageHandTwo;
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Min FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateMax(true);
            this.owner.maxDamageHandOne = this.maxDamageHandOne;
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Max FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateMax(false);
            this.owner.maxDamageHandTwo = this.maxDamageHandTwo;
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Max FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateAttackSpeed(true);
            this.owner.speedHandOne = this.attackSpeedHandOne;
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Attack Speed FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateAttackSpeed(false);
            this.owner.speedHandTwo = this.attackSpeedHandTwo;
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Attack Speed FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateAttackRange(true);
            this.owner.rangeHandOne = this.rangeHandOne;
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Attack Range FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateAttackRange(false);
            this.owner.rangeHandTwo = this.rangeHandTwo;
        } catch (Exception e) {
            //Logger.error("FAILED TO CALCULATE Attack Range FOR: " + this.owner.getObjectUUID());
        }
        try {
            this.calculateDefense();
            this.owner.defenseRating = this.defense;
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
        int primaryStat = this.owner.statDexCurrent;
        if(weapon != null) {
            skill= weapon.getItemBase().getSkillRequired();
            mastery = weapon.getItemBase().getMastery();
            if(weapon.getItemBase().isStrBased())
                primaryStat = this.owner.statStrCurrent;
        }

        if(weapon == null)
            primaryStat = this.owner.statStrCurrent;

        float skillLevel = 0;
        float masteryLevel = 0;

        if(this.owner.skills.containsKey(skill)) {
            skillLevel = this.owner.skills.get(skill).getModifiedAmount();
        }
        if(this.owner.skills.containsKey(mastery))
            masteryLevel = this.owner.skills.get(mastery).getModifiedAmount();

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
                        if(mod.getPercentMod() == 0) {
                            float value = mod.getMinMod();
                            int trains = this.owner.effects.get(effID).getTrains();
                            float modValue = value + (trains * mod.getRamp());
                            atrEnchants += modValue;
                        }
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

            atr = primaryStat / 2.0f;
            atr += skillLevel * 4;
            atr += masteryLevel * 3;
            atr += prefixValues;
            atr *= preciseRune;
            atr += atrEnchants;

            atr *= 1.0f + stanceValue;
        if(this.owner.bonuses != null) {
            float positivePercentBonuses = this.owner.bonuses.getFloatPercentPositive(Enum.ModType.OCV, Enum.SourceType.None);
            float negativePercentBonuses = this.owner.bonuses.getFloatPercentNegative(Enum.ModType.OCV, Enum.SourceType.None);
            float modifier = 1 + (positivePercentBonuses + negativePercentBonuses);
            if(preciseRune > 1.0f)
                modifier -= 0.05f;
            if(stanceValue > 1.0f){
                modifier -= (stanceValue - 1.0f);
            }
            atr *= modifier;
        }
            atr = (float) Math.round(atr);

        if(atr < 0)
            atr = 0;

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
    } //PERFECT DO NOT TOUCH

    public void calculateMin(boolean mainHand) {
        Item weapon;
        float specialDex = this.owner.statDexBase;
        specialDex += this.owner.bonuses.getFloat(Enum.ModType.Attr, Enum.SourceType.Dexterity);
        float baseDMG = 1;
        float primaryStat = specialDex;
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
                //secondaryStat = specialDex;//getDexAfterPenalty(this.owner);
                primaryStat = this.owner.statStrCurrent;
                secondaryStat = this.owner.statDexCurrent;
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
            weaponSkill = this.owner.skills.get(skill).getModifiedAmount();
        }

        if (this.owner.skills.containsKey(mastery)) {
            weaponMastery = this.owner.skills.get(mastery).getModifiedAmount();
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
            minDMG *= 1 + this.owner.bonuses.getFloatPercentAll(Enum.ModType.MeleeDamageModifier, Enum.SourceType.None, this.owner);
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
            if(this.owner.charItemManager != null) {
                if (this.owner.charItemManager.getEquipped(1) == null && this.owner.charItemManager.getEquipped(2) != null) {
                    if (!this.owner.charItemManager.getEquipped(2).getItemBase().isShield())
                        this.minDamageHandOne = 0;
                } else if (this.owner.charItemManager.getEquipped(2) == null && this.owner.charItemManager.getEquipped(1) != null) {
                    this.minDamageHandTwo = 0;
                }
            }
        }
    }

    public void calculateMax(boolean mainHand) {
        Item weapon;
        double baseDMG = 5;
        float primaryStat = this.owner.statDexCurrent;
        float secondaryStat = this.owner.statStrCurrent;
        double weaponSkill = 5;
        double weaponMastery = 5;


        if (mainHand) {
            weapon = this.owner.charItemManager.getEquipped(1);
        } else {
            weapon = this.owner.charItemManager.getEquipped(2);
        }

        int extraDamage = 3;
        String skill = "Unarmed Combat";
        String mastery = "Unarmed Combat Mastery";
        if (weapon != null) {
            baseDMG = weapon.getItemBase().getMaxDamage();
            skill = weapon.getItemBase().getSkillRequired();
            mastery = weapon.getItemBase().getMastery();
            if (weapon.getItemBase().isStrBased()) {
                primaryStat = this.owner.statStrCurrent;
                secondaryStat = this.owner.statDexCurrent;
                //extraDamage = 3;
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
            weaponSkill = this.owner.skills.get(skill).getModifiedAmount();
        }

        if (this.owner.skills.containsKey(mastery)) {
            weaponMastery = this.owner.skills.get(mastery).getModifiedAmount();
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
            maxDMG *= 1 + this.owner.bonuses.getFloatPercentAll(Enum.ModType.MeleeDamageModifier, Enum.SourceType.None, this.owner);
        }

        if(this.owner.charItemManager != null){
            if(this.owner.charItemManager.getEquipped(1) != null && this.owner.charItemManager.getEquipped(2) != null && !this.owner.charItemManager.getEquipped(2).getItemBase().isShield()){
                maxDMG *= 0.7f;
            }
        }

        int roundedMax = (int) (Math.round(maxDMG) + extraDamage);

        if(mainHand){
            this.maxDamageHandOne = roundedMax;
        }else {
            if (this.owner.charItemManager != null) {
                this.maxDamageHandTwo = roundedMax;
                if (this.owner.charItemManager.getEquipped(1) == null && this.owner.charItemManager.getEquipped(2) != null) {
                    if (!this.owner.charItemManager.getEquipped(2).getItemBase().isShield())
                        this.maxDamageHandOne = 0;
                } else if (this.owner.charItemManager.getEquipped(2) == null && this.owner.charItemManager.getEquipped(1) != null) {
                    this.maxDamageHandTwo = 0;
                }
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

        float bonusValues = 1 + this.owner.bonuses.getFloatPercentAll(Enum.ModType.AttackDelay,Enum.SourceType.None, null);//1.0f;
        bonusValues -= stanceValue + delayExtra; // take away stance modifier from alacrity bonus values
        speed *= 1 + stanceValue; // apply stance bonus
        speed *= bonusValues; // apply alacrity bonuses without stance mod

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
            range *= 1 + this.owner.bonuses.getFloatPercentAll(Enum.ModType.WeaponRange, Enum.SourceType.None, null);
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

    public void calculateDefense() {
        //Defense = (1+Armor skill / 50) * Armor defense + (1 + Block skill / 100) * Shield defense + (Primary weapon skill / 2)
        // + (Weapon mastery skill/ 2) + Dexterity * 2 + Flat bonuses from rings or cloth
        float armorSkill = 0.0f;
        float armorDefense = 0.0f;
        ArrayList<String> armorsUsed = new ArrayList<>();
        int itemDef;
        for(Item equipped : this.owner.charItemManager.getEquipped().values()){
            ItemBase ib = equipped.getItemBase();
            if(ib.isHeavyArmor() || ib.isMediumArmor() || ib.isLightArmor() || ib.isClothArmor()){
                itemDef = ib.getDefense();

                for(Effect eff : equipped.effects.values()){
                    for(AbstractEffectModifier mod : eff.getEffectModifiers()){
                        if(mod.modType.equals(Enum.ModType.DR)){
                            itemDef += mod.minMod + (mod.getRamp() * eff.getTrains());
                        }
                    }
                }
                if(!ib.isClothArmor() && !armorsUsed.contains(ib.getSkillRequired())) {
                    armorsUsed.add(ib.getSkillRequired());
                }
                armorDefense += itemDef;
            }
        }
        for(String armorUsed : armorsUsed){
            if(this.owner.skills.containsKey(armorUsed)) {
                armorSkill += this.owner.skills.get(armorUsed).getModifiedAmount();//calculateBuffedSkillLevel(armorUsed,this.owner);
            }
        }
        if(armorsUsed.size() > 0)
            armorSkill = armorSkill / armorsUsed.size();

        float blockSkill = 0.0f;
        if(this.owner.skills.containsKey("Block"))
            blockSkill = this.owner.skills.get("Block").getModifiedAmount();

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
            weaponSkill = this.owner.skills.get(skillName).getModifiedAmount();//calculateBuffedSkillLevel(skillName,this.owner);//this.owner.skills.get(skillName).getModifiedAmount();//calculateModifiedSkill(skillName,this.owner);//this.owner.skills.get(skillName).getModifiedAmount();

        if(this.owner.skills.containsKey(masteryName))
            masterySkill = this.owner.skills.get(masteryName).getModifiedAmount();//calculateBuffedSkillLevel(masteryName,this.owner);//this.owner.skills.get(masteryName).getModifiedAmount();//calculateModifiedSkill(masteryName,this.owner);//this.owner.skills.get(masteryName).getModifiedAmount();

        float dexterity = this.owner.statDexCurrent;//getDexAfterPenalty(this.owner);

        float luckyRune = 1.0f;
        for(CharacterRune rune : this.owner.runes){
            if(rune.getRuneBase().getName().equals("Lucky")) {
                luckyRune += 0.05f;
                break;
            }
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
                        if(mod.getPercentMod() == 0) {
                            float value = mod.getMinMod();
                            int trains = this.owner.effects.get(effID).getTrains();
                            float modValue = value + (trains * mod.getRamp());
                            flatBonuses += modValue;
                        }
                    }
                }
            }
        }
        if(this.owner.charItemManager.getEquipped(2) == null)
            blockSkill = 0;
        else if(this.owner.charItemManager != null && this.owner.charItemManager.getEquipped(2) != null && !this.owner.charItemManager.getEquipped(2).getItemBase().isShield())
            blockSkill = 0;

        float defense = (1 + armorSkill / 50) * armorDefense;
        defense += (1 + blockSkill / 100) * shieldDefense;
        defense += (weaponSkill / 2);
        defense += (masterySkill / 2);
        defense += dexterity * 2;
        defense += flatBonuses;
        defense *= luckyRune;
        defense *= stanceMod;

        if(this.owner.bonuses != null) {
            float positivePercentBonuses = this.owner.bonuses.getFloatPercentPositive(Enum.ModType.DCV, Enum.SourceType.None);
            float negativePercentBonuses = this.owner.bonuses.getFloatPercentNegative(Enum.ModType.DCV, Enum.SourceType.None);
            float modifier = 1 + (positivePercentBonuses + negativePercentBonuses - (luckyRune - 1.0f) - (stanceMod - 1.0f));
            defense *= modifier;
        }
        defense = Math.round(defense);

        if(defense < 0)
            defense = 0;

        this.defense = (int) defense;
    } // PERFECT DO NOT TOUCH

    public static float getHitChance(int atr,int def){
        if(atr == 0)
            return 0.0f;
        if(def == 0)
            return 100.0f;

        float key = ((float)atr / def);
        BigDecimal bd = new BigDecimal(key).setScale(2, RoundingMode.HALF_UP);
        key = bd.floatValue(); // handles rounding for mandatory 2 decimal places
        if(key < 0.40f)
            return 0.0f;
        if(key > 2.50f)
            return 100.0f;
        return HIT_VALUE_MAP.get(key);
    }

}
