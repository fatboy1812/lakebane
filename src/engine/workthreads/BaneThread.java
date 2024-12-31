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

    public Long lastRun;
    public static int instancedelay = 10000;
    public BaneThread() {
        Logger.info(" BaneThread thread has started!");
    }


    public void processBanesWindow() {

        try {
            for(int baneId : Bane.banes.keySet()){
                Bane bane = Bane.banes.get(baneId);
                if(bane.getSiegePhase().equals(Enum.SiegePhase.WAR)){
                    bane.applyZergBuffs();
                }
            }
        } catch (Exception e) {
            Logger.error("BANE ERROR");
        }


    }

    public void run() {
        lastRun = System.currentTimeMillis();
        while (true) {
        if(lastRun + instancedelay < System.currentTimeMillis())
            this.processBanesWindow();
            lastRun = System.currentTimeMillis();
        }

    }

    public static void startBaneThread() {
        Thread baneThread;
        baneThread = new Thread(new BaneThread());
        baneThread.setName("baneThread");
        baneThread.start();
    }
}
