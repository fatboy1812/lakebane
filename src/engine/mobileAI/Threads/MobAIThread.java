package engine.mobileAI.Threads;

import engine.mobileAI.MobAI;
import engine.gameManager.ZoneManager;
import engine.objects.Mob;
import engine.objects.Zone;
import org.pmw.tinylog.Logger;

public class MobAIThread implements Runnable{
    public static int AI_BASE_AGGRO_RANGE = 60;
    public static int AI_DROP_AGGRO_RANGE = 60;
    public static int AI_PULSE_MOB_THRESHOLD = 200;
    public static int AI_PATROL_DIVISOR = 15;
    public static int AI_POWER_DIVISOR = 10;
    // Thread constructor

    public MobAIThread() {
        Logger.info(" MobAIThread thread has started!");

    }

    @Override
    public void run() {
        while (true) {
            for (Zone zone : ZoneManager.getAllZones()) {

                for (Mob mob : zone.zoneMobSet) {

                    try {
                        if (mob != null)
                            MobAI.DetermineAction(mob);
                    } catch (Exception e) {
                        Logger.error("Mob: " + mob.getName() + " UUID: " + mob.getObjectUUID() + " ERROR: " + e);
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    public static void startAIThread() {
        Thread aiThread;
        aiThread = new Thread(new MobAIThread());
        aiThread.setName("aiThread");
        aiThread.start();
    }
}
