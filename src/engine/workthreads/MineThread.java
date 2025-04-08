// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.workthreads;

import engine.gameManager.BaneManager;
import engine.gameManager.HotzoneManager;
import engine.objects.Mine;
import org.pmw.tinylog.Logger;

public class MineThread implements Runnable {

    public MineThread() {
        Logger.info(" BaneThread thread has started!");
    }


    public void run() {
        while (true) {
            try {
                for(Mine mine : Mine.getMines()){
                    if(mine != null && mine.isActive)
                        mine.onEnter();
                }
                Thread.sleep(5000); // Pause for 100ms to reduce CPU usage
            } catch (InterruptedException e) {
                Logger.error("Thread interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }


    public static void startMineThread() {
        Thread baneThread;
        baneThread = new Thread(new MineThread());
        baneThread.setName("mineThread");
        baneThread.start();
    }
}
