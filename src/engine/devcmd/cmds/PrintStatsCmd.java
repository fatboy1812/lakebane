// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.devcmd.cmds;

import engine.Enum;
import engine.devcmd.AbstractDevCmd;
import engine.objects.*;

import java.util.HashMap;

/**
 *
 */

public class PrintStatsCmd extends AbstractDevCmd {

    public PrintStatsCmd() {
        super("printstats");
        //		super("printstats", MBServerStatics.ACCESS_LEVEL_ADMIN);
    }

    public static ItemBase getWeaponBase(int slot, HashMap<Integer, MobEquipment> equip) {
        if (equip.containsKey(slot)) {
            MobEquipment item = equip.get(slot);
            if (item != null && item.getItemBase() != null) {
                return item.getItemBase();
            }
        }
        return null;
    }

    @Override
    protected void _doCmd(PlayerCharacter pc, String[] words,
                          AbstractGameObject target) {

        AbstractCharacter tar;

        if (target != null && target instanceof AbstractCharacter) {
            tar = (AbstractCharacter) target;

            if (tar instanceof PlayerCharacter) {
                printStatsPlayer(pc, (PlayerCharacter) tar);
                this.setTarget(tar); //for logging
            } else if (tar instanceof Mob)
                printStatsMob(pc, (Mob) tar);
            else if (tar instanceof NPC)
                printStatsNPC(pc, (NPC) tar);
        }
    }

    public void printStatsPlayer(PlayerCharacter pc, PlayerCharacter tar) {
        String newline = "\r\n ";

        String newOut = "Server stats for Player " + tar.getFirstName() + newline;
        newOut += "Unused Stats: " + tar.getUnusedStatPoints() + newline;
        newOut += "Stats Base (Modified)" + newline;
        newOut += "  Str: " + (int) tar.statStrBase + " (" + tar.getStatStrCurrent() + ')' + ", maxStr: " + tar.getStrMax() + newline;
        newOut += "  Dex: " + (int) tar.statDexBase + " (" + tar.getStatDexCurrent() + ')' + ", maxDex: " + tar.getDexMax() + newline;
        newOut += "  Con: " + (int) tar.statConBase + " (" + tar.getStatConCurrent() + ')' + ", maxCon: " + tar.getConMax() + newline;
        newOut += "  Int: " + (int) tar.statIntBase + " (" + tar.getStatIntCurrent() + ')' + ", maxInt: " + tar.getIntMax() + newline;
        newOut += "  Spi: " + (int) tar.statSpiBase + " (" + tar.getStatSpiCurrent() + ')' + ", maxSpi: " + tar.getSpiMax() + newline;
        newOut += "Move Speed: " + tar.getSpeed() + newline;
        newOut += "Health Regen: " + tar.combatStats.healthRegen + newline;
        newOut += "Mana Regen: " + tar.combatStats.manaRegen + newline;
        newOut += "Stamina Regen: " + tar.combatStats.staminaRegen + newline;
        newOut += "DEFENSE: " + tar.combatStats.defense + newline;
        newOut += "HAND ONE" + newline;
        newOut += "ATR: " +  tar.combatStats.atrHandOne + newline;
        newOut += "MIN: " + tar.combatStats.minDamageHandOne + newline;
        newOut += "MAX: " + " VS " + tar.combatStats.maxDamageHandOne + newline;
        newOut += "RANGE: " + tar.combatStats.rangeHandOne + newline;
        newOut += "ATTACK SPEED: " + tar.combatStats.attackSpeedHandOne + newline;
        newOut += "HAND TWO" + newline;
        newOut += "ATR: " + tar.combatStats.atrHandTwo + newline;
        newOut += "MIN: " + tar.combatStats.minDamageHandTwo + newline;
        newOut += "MAX: " + tar.combatStats.maxDamageHandTwo + newline;
        newOut += "RANGE: " + tar.combatStats.rangeHandTwo + newline;
        newOut += "ATTACK SPEED: " + tar.combatStats.attackSpeedHandTwo + newline;
        throwbackInfo(pc, newOut);
    }

    public void printStatsMob(PlayerCharacter pc, Mob tar) {
        MobBase mb = tar.getMobBase();
        if (mb == null)
            return;


        String newline = "\r\n ";
        String out = "Server stats for Mob " + mb.getFirstName() + newline;
        out += "Stats Base (Modified)" + newline;
        out += "  Str: " + tar.getStatStrCurrent() + " (" + tar.getStatStrCurrent() + ')' + newline;
        out += "  Dex: " + tar.getStatDexCurrent() + " (" + tar.getStatDexCurrent() + ')' + ", maxDex: " + tar.getStatDexCurrent() + newline;
        out += "  Con: " + tar.getStatConCurrent() + " (" + tar.getStatConCurrent() + ')' + ", maxCon: " + tar.getStatConCurrent() + newline;
        out += "  Int: " + tar.getStatIntCurrent() + " (" + tar.getStatIntCurrent() + ')' + ", maxInt: " + tar.getStatIntCurrent() + newline;
        out += "  Spi: " + tar.getStatSpiCurrent() + " (" + tar.getStatSpiCurrent() + ')' + ", maxSpi: " + tar.getStatSpiCurrent() + newline;

        out += "Health: " + tar.getHealth() + ", maxHealth: " + tar.getHealthMax() + newline;
        out += "Mana: " + tar.getMana() + ", maxMana: " + tar.getManaMax() + newline;
        out += "Stamina: " + tar.getStamina() + ", maxStamina: " + tar.getStaminaMax() + newline;
        out += "Defense: " + tar.getDefenseRating() + newline;

        //get weapons
        HashMap<Integer, MobEquipment> equip = tar.getEquip();
        ItemBase main = null;

        if (equip != null)
            main = getWeaponBase(1, equip);
        ItemBase off = null;

        if (equip != null)
            getWeaponBase(2, equip);
        if (main == null && off == null) {
            out += "Main Hand: atr: " + tar.getAtrHandOne() + ", damage: " + tar.getMinDamageHandOne() + " to " + tar.getMaxDamageHandOne() + ", speed: " + tar.getSpeedHandOne() + ", range: 6" + newline;
        } else {
            if (main != null)
                out += "Main Hand: atr: " + tar.getAtrHandOne() + ", damage: " + tar.getMinDamageHandOne() + " to " + tar.getMaxDamageHandOne() + ", speed: " + tar.getSpeedHandOne() + ", range: " + main.getRange() + newline;
            if (off != null)
                out += "Main Hand: atr: " + tar.getAtrHandTwo() + ", damage: " + tar.getMinDamageHandTwo() + " to " + tar.getMaxDamageHandTwo() + ", speed: " + tar.getSpeedHandTwo() + ", range: " + off.getRange() + newline;
        }
        out += "isAlive: " + tar.isAlive() + ", Combat: " + tar.isCombat() + newline;

        throwbackInfo(pc, out);
    }

    public void printStatsNPC(PlayerCharacter pc, NPC tar) {
        Contract contract = tar.getContract();
        if (contract == null)
            return;

        String newline = "\r\n ";

        String name;
        if (contract != null) {
            if (contract.isTrainer())
                name = tar.getName() + ", " + contract.getName();
            else
                name = tar.getName() + " the " + contract.getName();
        } else
            name = tar.getName();
        String out = "Server stats for NPC " + name + newline;
        out += "Sell Percent: " + tar.getSellPercent() + ", Buy Percent: " + tar.getBuyPercent() + newline;

        throwbackInfo(pc, out);
    }

    @Override
    protected String _getHelpString() {
        return "Returns the player's current stats";
    }

    @Override
    protected String _getUsageString() {
        return "' /printstats'";
    }

}
