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
                ArrayList<MobLoot> GlassItems = new ArrayList<MobLoot>();
                ArrayList<MobLoot> Resources = new ArrayList<MobLoot>();
                ArrayList<MobLoot> Runes = new ArrayList<MobLoot>();
                ArrayList<MobLoot> Contracts = new ArrayList<MobLoot>();
                ArrayList<MobLoot> Offerings = new ArrayList<MobLoot>();
                ArrayList<MobLoot> OtherDrops = new ArrayList<MobLoot>();
                ArrayList<MobLoot>ReturnedBootyList = SimulateMobLoot(mob);
                for(MobLoot ml : ReturnedBootyList){
                    if(ml.getItemBase().isGlass() == true){
                        GlassItems.add(ml);
                        break;
                    }
                    switch(ml.getItemBase().getType().ordinal()){
                        case 20: //CONTRACT
                            Contracts.add(ml);
                            break;
                        case 33: //OFFERING
                            Offerings.add(ml);
                            break;
                        case 34: //RESOURCE
                            Resources.add(ml);
                            break;
                        case 5: //RUNE
                            Runes.add(ml);
                            break;
                        default:
                            OtherDrops.add(ml);
                            break;
                    }
                }
                output += "TOTAL ITEMS DROPPED: " + ReturnedBootyList.size();
                output += "GLASS ITEMS DROPPED: " + GlassItems.size();
                output += "RESOURCE STACKS DROPPED: " + Resources.size();
                output += "RUNES DROPPED: " + Runes.size();
                output += "CONTRACTS DROPPED: " + Contracts.size();
                output += "OFFERINGS DROPPED: " + Offerings.size();
                output += "OTHERS DROPPED: " + OtherDrops.size();
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
        //iterate the booty sets
        ArrayList<MobLoot> output1 = new ArrayList<>();
        ArrayList<MobLoot> output2 = new ArrayList<>();
        if(mob.getMobBase().bootySet != 0 && NPCManager._bootySetMap.containsKey(mob.getMobBase().bootySet)) {
            output1 = RunBootySet(NPCManager._bootySetMap.get(mob.getMobBase().bootySet), mob, multiplier, inHotzone);
        }
        if(mob.bootySet != 0) {
            output2 =RunBootySet(NPCManager._bootySetMap.get(mob.bootySet), mob, multiplier, inHotzone);
        }
        for(MobLoot lootItem : output1){
            outList.add((lootItem));
        }
        for(MobLoot lootItem : output2){
            outList.add((lootItem));
        }
        return outList;
    }
    private static ArrayList<MobLoot> RunBootySet(ArrayList<BootySetEntry> entries, Mob mob, float multiplier, boolean inHotzone) {
        ArrayList<MobLoot> outList = new ArrayList<>();
        for (BootySetEntry bse : entries) {
            //check if chance roll is good
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
                    MobLoot disc = new MobLoot(mob, ItemBase.getItemBase(bse.itemBase), true);
                    if (disc != null)
                        //mob.getCharItemManager().addItemToInventory(disc);

                    break;
            }
        }
        return outList;
    }
}
