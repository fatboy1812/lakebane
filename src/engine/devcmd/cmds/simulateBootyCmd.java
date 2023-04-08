package engine.devcmd.cmds;
import engine.Enum;
import engine.devcmd.AbstractDevCmd;
import engine.gameManager.*;
import engine.objects.*;
import java.util.ArrayList;
public class simulateBootyCmd  extends AbstractDevCmd {
    public simulateBootyCmd() {
        super("simulatebooty");
    }
    @Override
    protected void _doCmd(PlayerCharacter pc, String[] words, AbstractGameObject target) {
        // Arg Count Check
        if (words.length != 1) {
            this.sendUsage(pc);
            return;
        }
        if (pc == null) {
            return;
        }
        int iterations = 100;
        String newline = "\r\n ";
        if(words[1].length() > 0){
            try{
                iterations = Integer.parseInt(words[1]);
            }catch(Exception ex){
                iterations = 100;
            }
        }
        boolean isZone = false;
        if(words[2].length() > 0 && words[2].toLowerCase().equals("zone")){
            isZone = true;
        }
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
            case Building:
            case PlayerCharacter:
            case NPC:
            default:
                output += "Target is Not a Mob! Please Select a Mob to Simulate Booty" + newline;
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
                    ArrayList<Item> simulatedBooty = new ArrayList<>();
                    if(isZone == false){
                        //simulate individual mob booty
                        simulatedBooty = simulateMobBooty(mob, iterations);
                    }
                    else {
                        simulatedBooty = simulateZoneBooty(mob.getParentZone(), iterations);
                    }
                    try {
                        for (Item lootItem : simulatedBooty) {
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
        return "simulates mob loot X amount of times for mob or zone";
    }
    @Override
    protected String _getUsageString() {
        return "' ./simluatebooty <ITERATIONS> <zone or blank>";
    }
    public static ArrayList<Item> simulateMobBooty(Mob mob, int iterations){
        ArrayList<Item> producedBooty = new ArrayList<>();
        for(int i = 0; i < iterations; ++i) {
            mob.loadInventory();
            for (Item lootItem : mob.getCharItemManager().getInventory()) {
                producedBooty.add(lootItem);
            }
        }
        return producedBooty;
    }
    public static ArrayList<Item> simulateZoneBooty(Zone zone, int iterations){
        ArrayList<Item> producedBooty = new ArrayList<>();
        for(Mob mob : zone.zoneMobSet) {
            for (int i = 0; i < iterations; ++i) {
                mob.loadInventory();
                for (Item lootItem : mob.getCharItemManager().getInventory()) {
                    producedBooty.add(lootItem);
                }
            }
        }
        return producedBooty;
    }
}