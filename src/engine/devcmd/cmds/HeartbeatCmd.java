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

import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

public class HeartbeatCmd extends AbstractDevCmd {

	public HeartbeatCmd() {
        super("heartbeat");
    }

	@Override
	protected void _doCmd(PlayerCharacter pc, String[] words,
			AbstractGameObject target) {

		this.throwbackInfo(pc, "Heartbeat : " + SimulationManager.HeartbeatDelta.getNano() + "ns");
		this.throwbackInfo(pc, "FSM: " + MobileFSMManager.executionTime.getNano() + "ns");

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
