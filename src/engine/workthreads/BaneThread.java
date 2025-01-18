// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.workthreads;

import engine.Enum;
import engine.InterestManagement.WorldGrid;
import engine.db.archive.DataWarehouse;
import engine.db.archive.MineRecord;
import engine.gameManager.BuildingManager;
import engine.gameManager.ChatManager;
import engine.gameManager.StrongholdManager;
import engine.mobileAI.Threads.MobAIThread;
import engine.net.DispatchMessage;
import engine.net.client.msg.chat.ChatSystemMsg;
import engine.objects.*;
import org.pmw.tinylog.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class BaneThread implements Runnable {

    private volatile Long lastRun;
    public static final Long instancedelay = 1000L;
    public BaneThread() {
        Logger.info(" BaneThread thread has started!");
    }


    public void processBanesWindow() {

        try {
            synchronized (Bane.banes) {
                for (int baneId : Bane.banes.keySet()) {
                    Bane bane = Bane.banes.get(baneId);
                    if (bane != null && bane.getSiegePhase().equals(Enum.SiegePhase.WAR)) {
                        bane.applyZergBuffs();
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("BANE ERROR",e);
        }


    }

    public void run() {
        lastRun = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() >= lastRun + instancedelay) { // Correct condition
                this.processBanesWindow();
                lastRun = System.currentTimeMillis(); // Update lastRun after processing
            }else {
                try {
                    Thread.sleep(100); // Pause for 100ms to reduce CPU usage
                } catch (InterruptedException e) {
                    Logger.error("Thread interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    public static void startBaneThread() {
        Thread baneThread;
        baneThread = new Thread(new BaneThread());
        baneThread.setName("baneThread");
        baneThread.start();
    }
}
