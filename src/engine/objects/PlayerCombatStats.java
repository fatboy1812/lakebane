package engine.objects;

import engine.Enum;
import engine.powers.effectmodifiers.AbstractEffectModifier;

import java.util.ArrayList;
import java.util.HashMap;

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
        this.calculateATR(true);
        this.calculateATR(false);
        this.calculateMin(true);
        this.calculateMin(false);
        this.calculateMax(true);
        this.calculateMax(false);
        this.calculateAttackSpeed(true);
        this.calculateAttackSpeed(false);
        this.calculateAttackRange(true);
        this.calculateAttackRange(false);
        this.calculateRegen();
        this.calculateDefense();
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
        int primary = this.owner.statDexCurrent;
        if(weapon != null) {
            skill= weapon.getItemBase().getSkillRequired();
            mastery = weapon.getItemBase().getMastery();
            if(weapon.getItemBase().isStrBased())
                primary = this.owner.statStrCurrent;
        }

        float skillLevel = 5;
        float masteryLevel = 5;

        if(this.owner.skills.containsKey(skill))
            skillLevel = this.owner.skills.get(skill).getTotalSkillPercet();

        if(this.owner.skills.containsKey(mastery))
            masteryLevel = this.owner.skills.get(mastery).getTotalSkillPercet();

        float masteryCalc = masteryLevel * 3;
        float primaryCalc = primary * 0.5f;
        float skillCalc = skillLevel * 4;

        float atrEnchants = 0;
        float stanceValue = 0.0f;

        for(String effID : this.owner.effects.keySet()){
            if(effID.contains("Stance")){
                for(AbstractEffectModifier mod : this.owner.effects.get(effID).getEffectModifiers()){
                    if(mod.modType.equals(Enum.ModType.OCV)){
                        float percent = mod.getPercentMod();
                        int trains = this.owner.effects.get(effID).getTrains();
                        float modValue = percent + (trains * mod.getRamp());
                        stanceValue += modValue * 0.01f;
                    }
                }
            }
        }

        if(this.owner.bonuses != null){
            atrEnchants = this.owner.bonuses.getFloat(Enum.ModType.OCV, Enum.SourceType.None);
        }

        atr = primaryCalc + skillCalc + masteryCalc + atrEnchants;
        atr *= 1 + (this.owner.bonuses.getFloatPercentAll(Enum.ModType.OCV, Enum.SourceType.None) - stanceValue);
        atr *= 1 + stanceValue;
        atr = Math.round(atr);

        if(mainHand){
            this.atrHandOne = atr;
        }else{
            this.atrHandTwo = atr;
            if(this.owner.charItemManager.getEquipped(1) == null && this.owner.charItemManager.getEquipped(2) != null){
                if(!this.owner.charItemManager.getEquipped(2).getItemBase().isShield())
                    this.atrHandOne = 0.0f;
            }
        }
    }

    public void calculateMin(boolean mainHand) {
        Item weapon;
        float baseDMG = 1;
        int primaryStat = this.owner.statDexCurrent;
        int secondaryStat = this.owner.statStrCurrent;
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
                secondaryStat = this.owner.statDexCurrent;
            }
        }

        if (this.owner.skills.containsKey(skill)) {
            weaponSkill = this.owner.skills.get(skill).getTotalSkillPercet();
        }

        if (this.owner.skills.containsKey(mastery)) {
            weaponMastery = this.owner.skills.get(mastery).getTotalSkillPercet();
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

        int roundedMin = (int)Math.round(minDMG);

        if (mainHand) {
            this.minDamageHandOne = roundedMin;
        } else {
            this.minDamageHandTwo = roundedMin;
            if(this.owner.charItemManager.getEquipped(1) == null && this.owner.charItemManager.getEquipped(2) != null){
                if(!this.owner.charItemManager.getEquipped(2).getItemBase().isShield())
                    this.minDamageHandOne = 0;
            }
        }
    }

    public void calculateMax(boolean mainHand) {
        //Weapon Max DMG = BaseDMG * (0.0124*Primary Stat + 0.118*(Primary Stat -0.75)^0.5
        // + 0.0022*Secondary Stat + 0.028*(Secondary Stat-0.75)^0.5 + 0.0075*(Weapon Skill + Weapon Mastery))
        Item weapon;
        double baseDMG = 6;
        int primaryStat = this.owner.statDexCurrent;
        int secondaryStat = this.owner.statStrCurrent;
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
                secondaryStat = this.owner.statDexCurrent;
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
            maxDMG *= 1 + this.owner.bonuses.getFloatPercentAll(Enum.ModType.MeleeDamageModifier, Enum.SourceType.None);
        }

        int roundedMax = (int)Math.round(maxDMG);

        if(mainHand){
            this.maxDamageHandOne = roundedMax;
        }else{
            this.maxDamageHandTwo = roundedMax;
            if(this.owner.charItemManager.getEquipped(1) == null && this.owner.charItemManager.getEquipped(2) != null){
                if(!this.owner.charItemManager.getEquipped(2).getItemBase().isShield())
                    this.maxDamageHandOne = 0;
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

        float weaponSpecificValues = 0.0f;
        if(weapon == null) {
            speed = 20.0f;
        }else{
            speed = weapon.getItemBase().getSpeed();
            for(Effect eff : weapon.effects.values()){
                for(AbstractEffectModifier mod : eff.getEffectModifiers()){
                    if(mod.modType.equals(Enum.ModType.WeaponSpeed) || mod.modType.equals(Enum.ModType.AttackDelay)){
                        speed *= 1 + (mod.getPercentMod() * 0.01f);
                        weaponSpecificValues += (mod.getPercentMod() * 0.01f);
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

        bonusValues -= (stanceValue + weaponSpecificValues); // take away stance modifier from alac bonus values
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
        int armorDefense = 0;
        int shieldDefense = 0;
        int dexterity = this.owner.statDexCurrent;
        double armorSkill = 0;
        double blockSkill = 0;
        double weaponSkill = 0;
        double weaponMastery = 0;

        //armor defense value need to loop all equipped items and log armor defense values
        ArrayList<String> armorTypes = new ArrayList<>();
        if (this.owner.charItemManager != null) {
            for (Item equipped : this.owner.charItemManager.getEquippedList()) {
                if (equipped.getItemBase().isHeavyArmor()) {
                    armorDefense += equipped.getItemBase().getDefense();
                    if (!armorTypes.contains(equipped.getItemBase().getSkillRequired()))
                        armorTypes.add(equipped.getItemBase().getSkillRequired());
                } else if (equipped.getItemBase().isLightArmor()) {
                    armorDefense += equipped.getItemBase().getDefense();
                    if (!armorTypes.contains(equipped.getItemBase().getSkillRequired()))
                        armorTypes.add(equipped.getItemBase().getSkillRequired());
                } else if (equipped.getItemBase().isMediumArmor()) {
                    armorDefense += equipped.getItemBase().getDefense();
                    if (!armorTypes.contains(equipped.getItemBase().getSkillRequired()))
                        armorTypes.add(equipped.getItemBase().getSkillRequired());
                } else if (equipped.getItemBase().isClothArmor()) {
                    armorDefense += equipped.getItemBase().getDefense();
                } else if (equipped.getItemBase().isShield()) {
                    shieldDefense += equipped.getItemBase().getDefense();
                }
            }
        }

        //armor skill needs to calculate all trains in armor types
        for (String armorType : armorTypes) {
            if (this.owner.skills != null) {
                if (this.owner.skills.containsKey(armorType)) {
                    armorSkill += this.owner.skills.get(armorType).getModifiedAmount();
                }
            }
        }


        if (this.owner.skills.containsKey("Block")) {
            blockSkill = this.owner.skills.get("Block").getModifiedAmount();
        }

        String primarySkillName = "Unarmed Combat";
        String primaryMasteryName = "Unarmed Combat Mastery";
        Item weapon = this.owner.charItemManager.getEquipped(1);
        if (weapon == null) {
            weapon = this.owner.charItemManager.getEquipped(2);
        }
        if (weapon != null) {
            primarySkillName = weapon.getItemBase().getSkillRequired();
            primaryMasteryName = weapon.getItemBase().getMastery();
        }

        if (this.owner.skills != null) {
            if (this.owner.skills.containsKey(primarySkillName)) {
                weaponSkill = this.owner.skills.get(primarySkillName).getModifiedAmount();
            }
            if (this.owner.skills.containsKey(primaryMasteryName)) {
                weaponMastery = this.owner.skills.get(primaryMasteryName).getModifiedAmount();
            }
        }

        float stanceValue = 0.0f;
        float bonusValues = 0;
        float percentBonus = 0;
        if (this.owner.bonuses != null) {
            for (String effID : this.owner.effects.keySet()) {
                if (effID.contains("Stance")) {
                    if (this.owner.effects != null) {
                        for (AbstractEffectModifier mod : this.owner.effects.get(effID).getEffectModifiers()) {
                            if (mod.modType.equals(Enum.ModType.DCV)) {
                                float percent = mod.getPercentMod();
                                int trains = this.owner.effects.get(effID).getTrains();
                                float modValue = percent + (trains * mod.getRamp());
                                stanceValue += modValue * 0.01f;
                            }
                        }
                    }
                }
            }

            bonusValues = this.owner.bonuses.getFloat(Enum.ModType.DCV, Enum.SourceType.None);
            percentBonus = this.owner.bonuses.getFloatPercentAll(Enum.ModType.DCV, Enum.SourceType.None) - stanceValue;
        }

        double defense = (1 + armorSkill / 50.0) * armorDefense +
                (1 + blockSkill / 100.0) * shieldDefense +
                (weaponSkill / 2.0) +
                (weaponMastery / 2.0) +
                dexterity * 2.0 +
                bonusValues;
        defense *= 1.0f + percentBonus + stanceValue;
        //defense *= 1.0f + stanceValue;
        defense = Math.round(defense);
        this.defense = (int) defense;
    }
}
