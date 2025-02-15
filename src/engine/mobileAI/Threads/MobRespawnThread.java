// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.mobileAI.Threads;
import engine.gameManager.ZoneManager;
import engine.objects.Mob;
import engine.objects.Zone;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Thread blocks until MagicBane dispatch messages are
 * enqueued then processes them in FIFO order. The collection
 * is thread safe.
 * <p>
 * Any large messages not time sensitive such as load object
 * sent to more than a single individual should be spawned
 * individually on a DispatchMessageThread.
 */

public class MobRespawnThread implements Runnable {

    private volatile boolean running = true;
    private static final long RESPAWN_INTERVAL = 100; // Configurable interval

    public MobRespawnThread() {
        Logger.info("MobRespawnThread initialized.");
    }

    @Override
    public void run() {
        while (running) {
            try {
                Collection<Zone> zones = ZoneManager.getAllZones();
                if (zones != null) {
                    for (Zone zone : zones) {
                        synchronized (zone) { // Optional: Synchronize on zone
                            if (!Zone.respawnQue.isEmpty() && Zone.lastRespawn + RESPAWN_INTERVAL < System.currentTimeMillis()) {

                                Mob respawner = Zone.respawnQue.iterator().next();
                                if (respawner != null) {
                                    respawner.respawn();
                                    Zone.respawnQue.remove(respawner);
                                    Zone.lastRespawn = System.currentTimeMillis();
                                    Thread.sleep(100);
                                }
                            }
                        }
                    }
                }
                Thread.sleep(100); // Prevent busy-waiting
            } catch (Exception e) {
                Logger.error("Error in MobRespawnThread", e);
            }
        }
        Logger.info("MobRespawnThread stopped.");
    }

    public void stop() {
        running = false;
    }

    public static void startRespawnThread() {
        Thread respawnThread = new Thread(new MobRespawnThread());
        respawnThread.setName("respawnThread");
        respawnThread.start();
    }
}
