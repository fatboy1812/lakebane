// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


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

    // Instance variables

    // Thread constructor

    public MobRespawnThread() {
        Logger.info(" MobRespawnThread thread has started!");

    }

    @Override
    public void run() {

        while (true) {

            try {
                for (Zone zone : ZoneManager.getAllZones()) {

                    if (zone.respawnQue.isEmpty() == false && zone.lastRespawn + 100 < System.currentTimeMillis()) {

                        Mob respawner = zone.respawnQue.iterator().next();

                        if (respawner == null)
                            continue;

                        respawner.respawn();
                        zone.respawnQue.remove(respawner);
                        zone.lastRespawn = System.currentTimeMillis();
                    }
                }
            } catch (Exception e) {
                Logger.error(e);
            }

        }
    }
    public static void startRespawnThread() {
        Thread respawnThread;
        respawnThread = new Thread(new MobRespawnThread());
        respawnThread.setName("respawnThread");
        respawnThread.start();
    }
}
