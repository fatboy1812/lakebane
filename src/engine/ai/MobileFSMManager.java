// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.ai;

import engine.gameManager.ConfigManager;
import engine.gameManager.ZoneManager;
import engine.objects.Mob;
import engine.objects.Zone;
import engine.util.ThreadUtils;
import org.joda.time.DateTime;
import org.pmw.tinylog.Logger;

import java.time.Duration;
import java.time.Instant;


public class MobileFSMManager {

    private static final MobileFSMManager INSTANCE = new MobileFSMManager();
    public static Duration executionTime = Duration.ofNanos(1);
    public static Duration executionMax = Duration.ofNanos(1);
    //AI variables moved form mb_server_statics
    public static int AI_BASE_AGGRO_RANGE = 60;
    public static int AI_DROP_AGGRO_RANGE = 60;
    public static int AI_PULSE_MOB_THRESHOLD = 200;
    public static int AI_PATROL_DIVISOR = 15;
    public static int AI_POWER_DIVISOR = 20;
    private volatile boolean alive;
    private long timeOfKill = -1;
    private MobileFSMManager() {

        Runnable worker = new Runnable() {
            @Override
            public void run() {
                execution();
            }
        };

        alive = true;

        //assign the AI varibales base don difficulty scaling from config file:

        float difficulty = Float.parseFloat(ConfigManager.MB_AI_AGGRO_RANGE.getValue());
        AI_BASE_AGGRO_RANGE = (int) (100 * difficulty); // range at which aggressive mobs will attack you

        difficulty = Float.parseFloat(ConfigManager.MB_AI_CAST_FREQUENCY.getValue());
        AI_POWER_DIVISOR = (int) (20 * (1.5f - difficulty)); //duration between mob casts

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


    public boolean isAlive() {
        return this.alive;
    }


    private void execution() {

        //Load zone threshold once.

        long mobPulse = System.currentTimeMillis() + AI_PULSE_MOB_THRESHOLD;
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
                            Logger.error("Mob: " + mob.getName() + " UUID: " + mob.getObjectUUID() + " ERROR: " + e);
                            e.printStackTrace();
                        }
                    }
                }

                executionTime = Duration.between(startTime, Instant.now());

                if (executionTime.compareTo(executionMax) > 0)
                    executionMax = executionTime;

                mobPulse = System.currentTimeMillis() + AI_PULSE_MOB_THRESHOLD;
                Logger.info("MobileFSM cycle completed: " + DateTime.now());
            }
        }
    }

}
