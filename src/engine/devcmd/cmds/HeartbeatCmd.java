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
import engine.gameManager.SimulationManager;
import engine.objects.AbstractGameObject;
import engine.objects.PlayerCharacter;

public class HeartbeatCmd extends AbstractDevCmd {

	public HeartbeatCmd() {
        super("heartbeat");
    }

	@Override
	protected void _doCmd(PlayerCharacter pc, String[] words,
			AbstractGameObject target) {

		this.throwbackInfo(pc, "Heartbeat : " + SimulationManager.executionTime);
		this.throwbackInfo(pc, "Heartbeat Max: " + SimulationManager.executionMax);

		this.throwbackInfo(pc, "FSM: " + MobileFSMManager.executionTime);
		this.throwbackInfo(pc, "FSM max: " + MobileFSMManager.executionMax);

	}

	@Override
	protected String _getHelpString() {
		return "Displays simulation metrics";
	}

	@Override
	protected String _getUsageString() {
		return "' ./heartbeat";
	}

}
