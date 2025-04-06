// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.devcmd.cmds;

import engine.devcmd.AbstractDevCmd;
import engine.gameManager.HotzoneManager;
import engine.objects.AbstractGameObject;
import engine.objects.PlayerCharacter;


/**
 * @author
 */
public class zerg extends AbstractDevCmd {

    public zerg() {
        super("zerg");
    }

    @Override
    protected void _doCmd(PlayerCharacter playerCharacter, String[] words,
                          AbstractGameObject target) {

        if (playerCharacter == null)
            return;

        String newline = "\r\n ";
        String output = "Antizerg Information for Player: " + playerCharacter.getName()  + newline;
        output += "Zerg Multiplier: " + playerCharacter.ZergMultiplier + newline;
        if(playerCharacter.affectedMine != null){
            output += "Multiplier Type: MINE" + newline;
            output += newline;
            output += "Counted Players (Time Left)" + newline;
            int count = 0;
            for(int pcID : playerCharacter.affectedMine.mineAttendees.keySet()){
                PlayerCharacter counted = PlayerCharacter.getFromCache(pcID);
                if(counted == null)
                    continue;
                if(counted.guild.getNation().equals(playerCharacter.guild.getNation())) {
                    count++;
                    output += counted.getName() + "(" + playerCharacter.affectedMine.mineAttendees.get(pcID) + ")" + newline;
                }
            }
            output += "Friendly Players Counted: " + count;
        }else if(playerCharacter.affectedBane != null){
            output += "Multiplier Type: BANE" + newline;
            output += newline;
            output += "Counted Players (Time Left)" + newline;
            int count = 0;
            for(int pcID : playerCharacter.affectedBane.mineAttendees.keySet()){
                PlayerCharacter counted = PlayerCharacter.getFromCache(pcID);
                if(counted == null)
                    continue;
                if(counted.guild.getNation().equals(playerCharacter.guild.getNation())) {
                    count++;
                    output += counted.getName() + "(" + playerCharacter.affectedBane.mineAttendees.get(pcID) + ")" + newline;
                }
            }
            output += "Friendly Players Counted: " + count;
        }else{
            output += "Multiplier Type: NONE" + newline;
        }

        throwbackInfo(playerCharacter, output);
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