// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.workthreads;

import engine.Enum;
import engine.gameManager.SessionManager;
import engine.gameManager.SimulationManager;
import engine.objects.Bane;
import engine.objects.PlayerCharacter;
import org.pmw.tinylog.Logger;

public class UpdateThread implements Runnable {

    private volatile Long lastRun;

    public static final Long instancedelay = 1000L;
    public UpdateThread() {
        Logger.info(" UpdateThread thread has started!");
    }


    public void processPlayerUpdate() {

        try {
            for(PlayerCharacter player : SessionManager.getAllActivePlayerCharacters()){
                if (player != null) {
                    player.doRegen();
                }
            }
        } catch (Exception e) {
            Logger.error("UPDATE ERROR",e);
        }


    }

    public void run() {
        lastRun = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() >= lastRun + instancedelay) { // Correct condition
                this.processPlayerUpdate();
                lastRun = System.currentTimeMillis(); // Update lastRun after processing
            }else {
                try {
                    Thread.sleep(100); // Pause for 10ms to reduce CPU usage
                } catch (InterruptedException e) {
                    Logger.error("Thread interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    public static void startUpdateThread() {
        Thread updateThread;
        updateThread = new Thread(new UpdateThread());
        updateThread.setName("updateThread");
        updateThread.start();
    }
}
