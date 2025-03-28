package engine.mobileAI.Threads;

import engine.gameManager.ConfigManager;
import engine.gameManager.ZoneManager;
import engine.mobileAI.MobAI;
import engine.mobileAI.SuperSimpleMobAI;
import engine.objects.Mob;
import engine.objects.Zone;
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
                if (zone != null) {
                    synchronized (zone.zoneMobSet) {
                        for (Mob mob : zone.zoneMobSet) {
                            try {
                                if (mob != null) {
                                    MobAI.DetermineAction(mob);
                                    //if(mob.isSiege() || mob.isPet() || mob.isPlayerGuard()){
                                    //    SuperSimpleMobAI.run(mob);
                                    //    return;
                                    //}
                                    //boolean override;
                                    //switch (mob.BehaviourType) {
                                    //    case GuardCaptain:
                                    //    case GuardMinion:
                                    //    case GuardWallArcher:
                                    //    case Pet1:
                                    //    case HamletGuard:
                                    //        override = false;
                                    //        break;
                                     //   default:
                                    //        override = true;
                                    //        break;
                                    //}

                                   // if(mob.isSiege())
                                    //    override = false;

                                    //if(mob.isPet())
                                    //    override = false;

                                    //if(override){
                                     //   SuperSimpleMobAI.run(mob);
                                    //    return;
                                    //}
                                }
                            } catch (Exception e) {
                                Logger.error("Error processing Mob [Name: {}, UUID: {}]", mob.getName(), mob.getObjectUUID(), e);
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
    }
    public static void startAIThread() {
        Thread aiThread;
        aiThread = new Thread(new MobAIThread());
        aiThread.setName("aiThread");
        aiThread.start();
    }
}
