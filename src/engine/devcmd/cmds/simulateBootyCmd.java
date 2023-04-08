package engine.devcmd.cmds;

import engine.Enum;
import engine.devcmd.AbstractDevCmd;
import engine.gameManager.*;
import engine.objects.*;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import static engine.loot.LootManager.getGenTableItem;

public class simulateBootyCmd  extends AbstractDevCmd {
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
        int objectUUID = target.getObjectUUID();
        String output;

        output = "Loot Simulation:" + newline;

        switch (objType) {
            case Building:

                break;
            case PlayerCharacter:

                break;

            case NPC:

                break;

            case Mob:
                Mob mob = (Mob) target;
                ArrayList<Item> GlassItems = new ArrayList<Item>();
                ArrayList<Item> Resources = new ArrayList<Item>();
                ArrayList<Item> Runes = new ArrayList<Item>();
                ArrayList<Item> Contracts = new ArrayList<Item>();
                ArrayList<Item> Offerings = new ArrayList<Item>();
                ArrayList<Item> OtherDrops = new ArrayList<Item>();
                int failures = 0;
                for (int i = 0; i < 100; ++i) {

                    try {
                        mob.loadInventory();
                        for (Item lootItem : mob.getCharItemManager().getInventory()) {
                            ItemBase ib = lootItem.getItemBase();
                            int ordinal = ib.getType().ordinal();
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
                                    if (lootItem.getItemBase().isGlass()) {
                                        GlassItems.add(lootItem);
                                    } else {
                                        OtherDrops.add(lootItem);
                                    }
                                    break;
                                default:
                                    OtherDrops.add(lootItem);
                                    break;
                            }
                        }
                    } catch (Exception ex) {
                        failures++;
                    }
                }
                output += "OFFERINGS DROPPED: " + Offerings.size() + newline;
                output += "OTHERS DROPPED: " + OtherDrops.size() + newline;
                output += "FAILED ROLLS: " + failures + newline;
                output += "Time Required To Gain Simulated Booty: " + mob.getMobBase().getSpawnTime() * 100 + " Seconds" + newline;
                output += "Glass Drops:" + GlassItems.size() + newline;
                for(Item glassItem : GlassItems){
                    output += glassItem.getName() + newline;
                }
                output += "Rune Drops:" + Runes.size() + newline;
                for(Item runeItem : Runes){
                    output += runeItem.getName() + newline;
                }
                output += "Contract Drops:" + Contracts.size() + newline;
                for(Item contractItem : Contracts){
                    output += contractItem.getName() + newline;
                }
                output += "Resource Drops:" + Resources.size() + newline;
                for(Item resourceItem : Contracts){
                    output += resourceItem.getName() + newline;
                }
                output += "OFFERINGS DROPPED: " + Offerings.size() + newline;
                output += "OTHER ITEMS DROPPED: " + OtherDrops.size() + newline;
                output += "FAILED ROLLS: " + failures + newline;
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
