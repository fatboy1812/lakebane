package engine.mobileAI.Threads;

import engine.gameManager.ConfigManager;
import engine.mobileAI.MobAI;
import engine.gameManager.ZoneManager;
import engine.objects.Mob;
import engine.objects.Zone;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

public class MobAIThread implements Runnable{
    public static int AI_BASE_AGGRO_RANGE = 60;
    public static int AI_DROP_AGGRO_RANGE = 60;
    public static int AI_PULSE_MOB_THRESHOLD = 200;
    public static int AI_PATROL_DIVISOR = 15;
    public static float AI_CAST_FREQUENCY;
    // Thread constructor

    public MobAIThread() {
        Logger.info(" MobAIThread thread has started!");

    }

    @Override
    public void run() {
        //cache config value for mobile casting delay
        AI_CAST_FREQUENCY = Float.parseFloat(ConfigManager.MB_AI_CAST_FREQUENCY.getValue());
        AI_BASE_AGGRO_RANGE = (int)(60 * Float.parseFloat(ConfigManager.MB_AI_AGGRO_RANGE.getValue()));
        while (true) {
            for (Zone zone : ZoneManager.getAllZones()) {
                if (zone != null && zone.zoneMobSet != null) {
                    synchronized (zone.zoneMobSet) {
                        for (Mob mob : zone.zoneMobSet) {
                            try {
                                if (mob != null) {
                                    MobAI.DetermineAction(mob);
                                }
                            } catch (Exception e) {
                                Logger.error("Error processing Mob [Name: {}, UUID: {}]", mob.getName(), mob.getObjectUUID(), e);
                            }
                        }
                    }
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Logger.error("AI Thread interrupted", e);
                Thread.currentThread().interrupt();
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
