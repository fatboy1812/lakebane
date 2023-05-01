// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.devcmd.cmds;

import engine.Enum.BuildingGroup;
import engine.devcmd.AbstractDevCmd;
import engine.gameManager.ChatManager;
import engine.gameManager.DbManager;
import engine.objects.*;
import engine.util.StringUtils;
import org.pmw.tinylog.Logger;

/**
 * Summary: Game designer utility command to add or
 * remove building slot access for contracts
 */

public class SlotNpcCmd extends AbstractDevCmd {

    public SlotNpcCmd() {
        super("slotnpc");
    }


    // AbstractDevCmd Overridden methods

    private static boolean validateUserInput(String[] userInput) {

        int stringIndex;
        BuildingGroup testGroup;

        testGroup = BuildingGroup.FORGE;

        String commandSet = "onoff";

        // incorrect number of arguments test

        if (userInput.length > 2)
            return false;


        // Test of toggle argument

        stringIndex = commandSet.indexOf(userInput[1].toLowerCase());

        if (stringIndex == -1)
            return false;

        // Validate we have a corrent building group name

        for (BuildingGroup group : BuildingGroup.values()) {
            if (group.name().equals(userInput[0].toUpperCase()))
                return true;
        }
        return false;
    }

    @Override
    protected void _doCmd(PlayerCharacter pc, String[] args,
                          AbstractGameObject target) {

        Contract contract;
        BuildingGroup buildingGroup;
        NPC npc;
        Mob mob;

        String outString;

        switch (target.getObjectType()) {

            case NPC:
                npc = (NPC) target;
                contract = npc.getContract();
                break;
            case Mob:
                mob = (Mob) target;
                contract = mob.getContract();
                break;
            default:
                throwbackInfo(pc, "NpcSlot: target must be an NPC");
                return;
        }

        // User requests list of current groups.

        if (args[0].equalsIgnoreCase("LIST")) {

            outString = "Current: " + contract.getAllowedBuildings();

            throwbackInfo(pc, outString);
            return;
        }

        if (validateUserInput(args) == false) {
            this.sendUsage(pc);
            return;
        }

        // Extract the building group flag from user input

        buildingGroup = BuildingGroup.valueOf(args[0].toUpperCase());

        switch (args[1].toUpperCase()) {

            case "ON":
                contract.getAllowedBuildings().add(buildingGroup);

                if (!DbManager.ContractQueries.updateAllowedBuildings(contract, contract.getAllowedBuildings().toLong())) {
                    Logger.error("Failed to update Database for Contract Allowed buildings");
                    ChatManager.chatSystemError(pc, "Failed to update Database for Contract Allowed buildings. " +
                            "Contact A CCR, oh wait, you are a CCR. You're Fubared.");
                    return;
                }

                throwbackInfo(pc, "SlotNpc " + buildingGroup.name() + " added to npc");
                break;
            case "OFF":
                contract.getAllowedBuildings().remove(buildingGroup);
                if (!DbManager.ContractQueries.updateAllowedBuildings(contract, contract.getAllowedBuildings().toLong())) {
                    Logger.error("Failed to update Database for Contract Allowed buildings");
                    ChatManager.chatSystemError(pc, "Failed to update Database for Contract Allowed buildings. " +
                            "Contact A CCR, oh wait, you are a CCR. You're Fubared.");
                    return;
                }

                throwbackInfo(pc, "SlotNpc " + buildingGroup.name() + " removed from npc");
                break;
        }

    }

    @Override
    protected String _getHelpString() {
        return "Sets a building slot on a targeted npc";
    }

    // Class methods

    @Override
    protected String _getUsageString() {
        String usage = "/npcslot [BuildingType] on-off \n";

        for (BuildingGroup group : BuildingGroup.values()) {
            usage += group.name() + ' ';
        }

        usage = StringUtils.wordWrap(usage, 30);

        return usage;
    }

}
