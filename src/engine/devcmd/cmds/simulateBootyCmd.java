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
                for(int i = 0; i < 100; ++i) {

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
                output += "GLASS ITEMS DROPPED: " + GlassItems.size() + newline;
                output += "RESOURCE STACKS DROPPED: " + Resources.size() + newline;
                output += "RUNES DROPPED: " + Runes.size() + newline;
                output += "CONTRACTS DROPPED: " + Contracts.size() + newline;
                output += "OFFERINGS DROPPED: " + Offerings.size() + newline;
                output += "OTHERS DROPPED: " + OtherDrops.size() + newline;
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
    public static ArrayList<MobLoot> SimulateMobLoot(Mob mob){
        ArrayList<MobLoot> outList = new ArrayList<>();
        //determine if mob is in hotzone
        boolean inHotzone = ZoneManager.inHotZone(mob.getLoc());
        //get multiplier form config manager
        float multiplier = Float.parseFloat(ConfigManager.MB_NORMAL_DROP_RATE.getValue());
        if (inHotzone) {
            //if mob is inside hotzone, use the hotzone gold multiplier form the config instead
            multiplier = Float.parseFloat(ConfigManager.MB_HOTZONE_DROP_RATE.getValue());
        }
        //simulate loot 100 times
        for(int i = 0; i < 5; ++i) {
            //iterate the booty sets
            if (mob.getMobBase().bootySet != 0 && NPCManager._bootySetMap.containsKey(mob.getMobBase().bootySet)) {
                try {
                    ArrayList<MobLoot> testList = RunBootySet(NPCManager._bootySetMap.get(mob.getMobBase().bootySet), mob, multiplier, inHotzone);
                    for (MobLoot lootItem : testList) {
                        outList.add((lootItem));
                    }
                }catch(Exception ex){
                }
            }
        }
        return outList;
    }
    private static ArrayList<MobLoot> RunBootySet(ArrayList<BootySetEntry> entries, Mob mob, float multiplier, boolean inHotzone) {
        ArrayList<MobLoot> outList = new ArrayList<>();
        for (BootySetEntry bse : entries) {
            switch (bse.bootyType) {
                case "GOLD":

                    break;
                case "LOOT":
                    if (ThreadLocalRandom.current().nextInt(100) <= (bse.dropChance * multiplier)) {
                        //early exit, failed to hit minimum chance roll
                        break;
                    }
                    //iterate the booty tables and add items to mob inventory
                    MobLoot toAdd = getGenTableItem(bse.lootTable, mob);
                    if (toAdd != null) {
                        //mob.getCharItemManager().addItemToInventory(toAdd);
                        outList.add(toAdd);
                    }
                    if (inHotzone) {
                        int lootTableID = bse.lootTable + 1;
                        MobLoot toAddHZ = getGenTableItem(lootTableID, mob);
                        if (toAddHZ != null)
                            //mob.getCharItemManager().addItemToInventory(toAddHZ);
                            outList.add(toAdd);
                    }
                    break;
                case "ITEM":

                    break;
            }
        }
        return outList;
    }
}
