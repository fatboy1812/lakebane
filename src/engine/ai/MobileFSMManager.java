// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.ai;
import engine.gameManager.ZoneManager;
import engine.objects.Mob;
import engine.objects.Zone;
import engine.server.MBServerStatics;
import engine.util.ThreadUtils;
import org.pmw.tinylog.Logger;

import java.time.Duration;
import java.time.Instant;


public class MobileFSMManager {

	private static final MobileFSMManager INSTANCE = new MobileFSMManager();

	private volatile boolean alive;
	private long timeOfKill = -1;

	public static Duration executionTime = Duration.ofNanos(1);
	public static Duration executionMax = Duration.ofNanos(1);

	private MobileFSMManager() {

		Runnable worker = new Runnable() {
			@Override
			public void run() {
				execution();
			}
		};

		alive = true;

		Thread t = new Thread(worker, "MobileFSMManager");
		t.start();
	}

	public static MobileFSMManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Stops the MobileFSMManager
	 */
	public void shutdown() {
		if (alive) {
			alive = false;
			timeOfKill = System.currentTimeMillis();
		}
	}


	public long getTimeOfKill() {
		return this.timeOfKill;
	}

	public boolean isAlive() {
		return this.alive;
	}


	private void execution() {

		//Load zone threshold once.

		long mobPulse = System.currentTimeMillis() + MBServerStatics.AI_PULSE_MOB_THRESHOLD;
		Instant startTime;

		while (alive) {

			ThreadUtils.sleep(1);

			if (System.currentTimeMillis() > mobPulse) {

				startTime = Instant.now();

				for (Zone zone : ZoneManager.getAllZones()) {

					for (Mob mob : zone.zoneMobSet) {

						try {
							if (mob != null)
								MobileFSM.DetermineAction(mob);
						} catch (Exception e) {
							Logger.error(e);
							e.printStackTrace();
						}
					}
				}

				this.executionTime = Duration.between(startTime, Instant.now());

				if (executionTime.compareTo(executionMax) > 0)
					executionMax = executionTime;

				mobPulse = System.currentTimeMillis() + MBServerStatics.AI_PULSE_MOB_THRESHOLD;
			}
		}
	}

}
