// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.devcmd.cmds;

import engine.Enum.GameObjectType;
import engine.devcmd.AbstractDevCmd;
import engine.gameManager.CombatManager;
import engine.gameManager.HotzoneManager;
import engine.objects.AbstractGameObject;
import engine.objects.Mob;
import engine.objects.PlayerCharacter;

import java.util.Map;


/**
 * @author
 */
public class hotzoneCmd extends AbstractDevCmd {

    public hotzoneCmd() {
        super("hotzone");
    }

    @Override
    protected void _doCmd(PlayerCharacter playerCharacter, String[] words,
                          AbstractGameObject target) {

        if (playerCharacter == null)
            return;
        HotzoneManager.create_random_hotzone();
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