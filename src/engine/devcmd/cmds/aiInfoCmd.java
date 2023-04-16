// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.devcmd.cmds;

import engine.Enum.BuildingGroup;
import engine.Enum.GameObjectType;
import engine.Enum.TargetColor;
import engine.devcmd.AbstractDevCmd;
import engine.gameManager.BuildingManager;
import engine.gameManager.SessionManager;
import engine.math.Vector3fImmutable;
import engine.objects.*;
import engine.util.StringUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author
 *
 */
public class aiInfoCmd extends AbstractDevCmd {

    public aiInfoCmd() {
        super("aiinfo");
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


        GameObjectType objType = target.getObjectType();
        int objectUUID = target.getObjectUUID();
        String output;

        if(objType != GameObjectType.Mob){
            output = "Please Select A Mob For AI Info" + newline;
        } else {
            Mob mob = (Mob) target;
            output = "Mob AI Information:" + newline;
            output += mob.getName() + newline;
            output += "BehaviourType: " + mob.BehaviourType.toString() + newline;
            output += "Behaviour Helper Type: " + mob.BehaviourType.BehaviourHelperType.toString() + newline;
            output += "Wimpy: " + mob.BehaviourType.isWimpy + newline;
            output += "Agressive: " + mob.BehaviourType.isAgressive + newline;
            output += "Can Roam: " + mob.BehaviourType.canRoam + newline;
            output += "Calls For Help: " + mob.BehaviourType.callsForHelp + newline;
            output += "Responds To Call For Help: " + mob.BehaviourType.respondsToCallForHelp + newline;
            output += "Player Aggro Map Size: " + mob.playerAgroMap.size() + newline;
            if(mob.playerAgroMap.size() > 0){
                output += "Players Loaded:" + newline;
            }
            for(Map.Entry<Integer,Boolean> entry : mob.playerAgroMap.entrySet()){
                output += "Player ID: " + entry.getKey() + "In Range To Aggro: " + entry.getValue() + newline;
            }
        }
        throwbackInfo(pc, output);
    }

    @Override
    protected String _getHelpString() {
        return "Gets AI information on a Mob.";
    }

    @Override
    protected String _getUsageString() {
        return "' /aiinfo targetID'";
    }

}