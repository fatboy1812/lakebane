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


package engine.workthreads;

import engine.Enum.DispatchChannel;
import engine.gameManager.ZoneManager;
import engine.net.Dispatch;
import engine.objects.Mob;
import engine.objects.Zone;
import org.pmw.tinylog.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;

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
        for (Zone zone : ZoneManager.getAllZones()) {
            if (zone.respawnQue.isEmpty() == false && zone.lastRespawn + 100 < System.currentTimeMillis()) {
                if (zone.respawnQue.iterator().next() != null) {
                    Mob respawner = zone.respawnQue.iterator().next();
                    respawner.respawn();
                    zone.respawnQue.remove(respawner);
                    zone.lastRespawn = System.currentTimeMillis();
                }
            }
        }
    }
    public static void startRespawnThread() {

        Thread respawnThread;
        respawnThread = new Thread(new PurgeOprhans());

        respawnThread.setName("respawnThread");
        respawnThread.start();
    }
}
