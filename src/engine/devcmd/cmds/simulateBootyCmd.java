package engine.devcmd.cmds;

import engine.Enum;
import engine.devcmd.AbstractDevCmd;
import engine.gameManager.BuildingManager;
import engine.gameManager.LootManager;
import engine.gameManager.NPCManager;
import engine.gameManager.ZoneManager;
import engine.objects.*;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class simulateBootyCmd extends AbstractDevCmd {
    public simulateBootyCmd() {
        super("simulatebooty");
    }

    @Override
    protected void _doCmd(PlayerCharacter pc, String[] words,
                          AbstractGameObject target) {
        // Arg Count Check
        if (words.length != 1) {
            this.sendUsage(pc);
            return;
        }
        if (pc == null) {
            return;
        }

        String newline = "\r\n ";

        try {
            int targetID = Integer.parseInt(words[0]);
            Building b = BuildingManager.getBuilding(targetID);
            if (b == null)
                throwbackError(pc, "Building with ID " + targetID
                        + " not found");
            else
                target = b;
        } catch (Exception e) {
        }

        if (target == null) {
            throwbackError(pc, "Target is unknown or of an invalid type."
                    + newline + "Type ID: 0x"
                    + pc.getLastTargetType().toString()
                    + "   Table ID: " + pc.getLastTargetID());
            return;
        }


        Enum.GameObjectType objType = target.getObjectType();
        String output;

        output = "Booty Simulation:" + newline;

        switch (objType) {
            case Mob:
                Mob mob = (Mob) target;
                output += "Name: " + mob.getName() + newline;
                output += "Special Loot:" + newline;
                if (mob.bootySet != 0) {
                    for (BootySetEntry entry : NPCManager._bootySetMap.get(mob.bootySet)) {
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
                ArrayList<Item> Offerings = new ArrayList<Item>();
                ArrayList<Item> OtherDrops = new ArrayList<Item>();
                ArrayList<Item> EquipmentDrops = new ArrayList<Item>();
                int failures = 0;
                int goldAmount = 0;
                for (int i = 0; i < 100; ++i) {

                    try {
                        mob.loadInventory();
                        for (Item lootItem : mob.getCharItemManager().getInventory()) {
                            switch (lootItem.getItemBase().getType()) {
                                case CONTRACT: //CONTRACT
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
                for (BootySetEntry entry : NPCManager._bootySetMap.get(mob.getMobBase().bootySet)) {
                    output += "NORMAL TABLE [" + entry.bootyType + "] " + entry.lootTable + newline;
                }
                if(ZoneManager.inHotZone(mob.getLoc())){
                    for (BootySetEntry entry : NPCManager._bootySetMap.get(mob.getMobBase().bootySet)) {
                        if(LootManager.generalItemTables.containsKey(entry.lootTable + 1) == true)
                            output += "HOTZONE TABLE [" + entry.bootyType + "] " + entry.lootTable + 1 + newline;
                    }
                }
                output += "GLASS DROPS: " + GlassItems.size() + newline;
                output += "RUNE DROPS: " + Runes.size() + newline;
                output += "CONTRACTS DROPS: " + Contracts.size() + newline;
                output += "RESOURCE DROPS: " + Resources.size() + newline;
                output += "OFFERINGS DROPPED: " + Offerings.size() + newline;
                output += "ENCHANTED ITEMS DROPPED: " + OtherDrops.size() + newline;
                output += "TOTAL GOLD DROPPED: " + goldAmount + newline;
                output += "EQUIPMENT DROPPED: " + EquipmentDrops.size() + newline;
                output += "FAILED ROLLS: " + failures + newline;
                break;
            default:
                break;
        }
        throwbackInfo(pc, output);
    }

    @Override
    protected String _getHelpString() {
        return "Gets information on an Object.";
    }

    @Override
    protected String _getUsageString() {
        return "' /info targetID'";
    }
}
