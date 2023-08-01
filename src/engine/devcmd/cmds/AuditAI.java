// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.devcmd.cmds;

import engine.ai.MobileFSMManager;
import engine.devcmd.AbstractDevCmd;
import engine.objects.AbstractGameObject;
import engine.objects.PlayerCharacter;

/**
 * ./auditai                      <- display the current state of mob AI thread
 */

public class AuditAI extends AbstractDevCmd {

    public AuditAI() {
        super("auditai");
    }

    @Override
    protected void _doCmd(PlayerCharacter playerCharacter, String[] words, AbstractGameObject target) {
        throwbackInfo(playerCharacter, MobileFSMManager.getInstance().getFSMState());
    }

    @Override
    protected String _getHelpString() {
        return "displays the current state and mob running thought he AI system";
    }

    @Override
    protected String _getUsageString() {
        return "'./auditai";
    }


}

