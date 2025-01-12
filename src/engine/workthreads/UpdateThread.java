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

    public Long lastRun;
    public static int instancedelay = 1000;
    public UpdateThread() {
        Logger.info(" UpdateThread thread has started!");
    }


    public void processPlayerUpdate() {

        try {
            for(PlayerCharacter player : SessionManager.getAllActivePlayerCharacters()){
                //player.update(true);
                player.doRegen();
            }
        } catch (Exception e) {
            Logger.error("UPDATE ERROR");
        }


    }

    public void run() {
        lastRun = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() >= lastRun + instancedelay) { // Correct condition
                this.processPlayerUpdate();
                lastRun = System.currentTimeMillis(); // Update lastRun after processing
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
