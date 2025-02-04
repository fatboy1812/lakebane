package engine.devcmd.cmds;

import engine.Enum;
import engine.devcmd.AbstractDevCmd;
import engine.gameManager.LootManager;
import engine.gameManager.ZoneManager;
import engine.loot.BootySetEntry;
import engine.objects.*;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class SimulateBootyCmd extends AbstractDevCmd {

    public int simCount = 250;
    public SimulateBootyCmd() {
        super("bootysim");
    }

    @Override
    protected void _doCmd(PlayerCharacter playerCharacter, String[] words,
                          AbstractGameObject target) {

        if (playerCharacter == null)
            return;

        String newline = "\r\n ";

        String output;
        if(target.getObjectType().equals(Enum.GameObjectType.PlayerCharacter)){
            int ATR = Integer.parseInt(words[0]);
            int DEF = Integer.parseInt(words[1]);
            int attacks = Integer.parseInt(words[2]);

            int hits = 0;
            int misses = 0;
            int defaultHits = 0;
            int defualtMisses = 0;

            float chance = (ATR-((ATR+DEF) * 0.315f)) / ((DEF-((ATR+DEF) * 0.315f)) + (ATR-((ATR+DEF) * 0.315f)));
            float convertedChance = chance * 100;
            output = "" + newline;
            output += "DEF VS ATR SIMULATION: " + attacks + " ATTACKS SIMULATED" + newline;
            output += "DEF = " + DEF + newline;
            output += "ATR = " + ATR + newline;
            output += "CHANCE TO LAND HIT: " + convertedChance + "%" + newline;
            if(convertedChance < 5){
                output += "CHANCE ADJUSTED TO 5.0%" + newline;
                convertedChance = 5.0f;
            }
            if(convertedChance > 95){
                output += "CHANCE ADJUSTED TO 95.0%" + newline;
                convertedChance = 95.0f;
            }
            for(int i = 0; i < attacks; i++){
                int roll = ThreadLocalRandom.current().nextInt(101);

                if(roll <= convertedChance){
                    hits += 1;
                }else{
                    misses += 1;
                }
            }

            float totalHits = defaultHits + hits;
            float totalMisses = defualtMisses + misses;
            float hitPercent = Math.round(totalHits / attacks * 100);
            float missPercent = Math.round(totalMisses / attacks * 100);

            output += "HITS LANDED: " + (defaultHits + hits) + "(" + Math.round(hitPercent) + "%)" + newline;
            output += "HITS MISSED: " + (defualtMisses + misses) + "(" + Math.round(missPercent) + "%)";

            throwbackInfo(playerCharacter,output);
            return;
        }
        try
        {
            simCount = Integer.parseInt(words[0]);
        }catch(Exception e)
        {

        }

        output = "Booty Simulation: Rolls:" + simCount + newline;

        Mob mob = (Mob) target;
        output += "Name: " + mob.getName() + newline;
        output += "Special Loot:" + newline;

        if (mob.bootySet != 0) {
            for (BootySetEntry entry : LootManager._bootySetMap.get(mob.bootySet)) {
                ItemBase item = ItemBase.getItemBase(entry.itemBase);
                if (item != null) {
                    output += "[" + entry.bootyType + "] " + item.getName() + " [Chance] " + entry.dropChance + newline;
                }
            }
        }

        ArrayList<Item> GlassItems = new ArrayList<Item>();
        ArrayList<Item> Resources = new ArrayList<Item>();
        ArrayList<Item> Runes = new ArrayList<Item>();
        ArrayList<Item> Contracts = new ArrayList<Item>();
        ArrayList<Item> GuardContracts = new ArrayList<Item>();
        ArrayList<Item> Offerings = new ArrayList<Item>();
        ArrayList<Item> OtherDrops = new ArrayList<Item>();
        ArrayList<Item> EquipmentDrops = new ArrayList<Item>();

        int failures = 0;
        int goldAmount = 0;

        for (int i = 0; i < simCount; ++i) {

            try {
                mob.loadInventory();
                for (Item lootItem : mob.getCharItemManager().getInventory()) {
                    switch (lootItem.getItemBase().getType()) {
                        case CONTRACT: //CONTRACT
                            if(lootItem.getName().contains("Captain"))
                                GuardContracts.add(lootItem);
                            else
                                Contracts.add(lootItem);
                            break;
                        case OFFERING: //OFFERING
                            Offerings.add(lootItem);
                            break;
                        case RESOURCE: //RESOURCE
                            Resources.add(lootItem);
                            break;
                        case RUNE: //RUNE
                            Runes.add(lootItem);
                            break;
                        case WEAPON: //WEAPON
                            if (lootItem.getItemBase().isGlass())
                                GlassItems.add(lootItem);
                            else
                                OtherDrops.add(lootItem);
                            break;
                        case GOLD:
                            goldAmount += lootItem.getNumOfItems();
                            break;
                        default:
                            OtherDrops.add(lootItem);
                            break;
                    }
                }
            } catch (Exception ex) {
                failures++;
            }
            if (mob.getEquip() != null) {
                for (MobEquipment me : mob.getEquip().values()) {

                    if (me.getDropChance() == 0)
                        continue;

                    float equipmentRoll = ThreadLocalRandom.current().nextInt(99) + 1;
                    float dropChance = me.getDropChance() * 100;

                    if (equipmentRoll > (dropChance))
                        continue;

                    MobLoot ml = new MobLoot(mob, me.getItemBase(), false);

                    if (ml != null)
                        EquipmentDrops.add(ml);
                }
            }
        }
        output += "MobBase BootySet: " + mob.getMobBase().bootySet + newline;
        output += "Mob BootySet: " + mob.bootySet + newline;
        output += "Tables Rolled On: " + newline;

        boolean hotZoneRan = false;
        float dropRate = 1.0f;

        if (ZoneManager.inHotZone(mob.getLoc()))
            dropRate = LootManager.HOTZONE_DROP_RATE;
        else
            dropRate = LootManager.NORMAL_DROP_RATE;

        for (BootySetEntry entry : LootManager._bootySetMap.get(mob.getMobBase().bootySet)) {

            if (entry.bootyType.equals("GOLD"))
                output += "NORMAL TABLE [" + entry.bootyType + "] " + entry.genTable + ": " + entry.dropChance + newline;
            else
                output += "NORMAL TABLE [" + entry.bootyType + "] " + entry.genTable + ": " + entry.dropChance * dropRate + newline;

            if (hotZoneRan == false && ZoneManager.inHotZone(mob.getLoc()) && LootManager._genTables.containsKey(entry.genTable + 1)) {
                output += "HOTZONE TABLE [" + entry.bootyType + "] " + (entry.genTable + 1) + ": " + entry.dropChance * dropRate + newline;
                hotZoneRan = true;
            }
        }

        int baseBound = 100000;
        int levelPenalty = (int) (Math.max(0, Math.abs(50 - mob.level)) * 0.01 * 100000);
        int totalRange = baseBound + levelPenalty;
        if(mob.level >= 50){
            totalRange = baseBound;
        }
        output += "TOTAL ROLL POTENTIAL: " + totalRange + newline;
        output += "GLASS DROPS: " + GlassItems.size() + newline;
        output += "RUNE DROPS: " + Runes.size() + newline;
        output += "CONTRACTS DROPS: " + Contracts.size() + newline;
        output += "GUARD CONTRACTS DROPS: " + GuardContracts.size() + newline;
        output += "RESOURCE DROPS: " + Resources.size() + newline;
        output += "OFFERINGS DROPPED: " + Offerings.size() + newline;
        output += "ENCHANTED ITEMS DROPPED: " + OtherDrops.size() + newline;
        output += "TOTAL GOLD DROPPED: " + goldAmount + newline;
        output += "EQUIPMENT DROPPED: " + EquipmentDrops.size() + newline;
        output += "FAILED ROLLS: " + failures + newline;

        throwbackInfo(playerCharacter, output);
    }

    @Override
    protected String _getHelpString() {
        return "Simulates loot drops";
    }

    @Override
    protected String _getUsageString() {
        return "'/bootysim targetID'";
    }
}
