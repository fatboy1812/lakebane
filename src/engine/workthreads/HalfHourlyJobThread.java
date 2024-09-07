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
import engine.gameManager.*;
import engine.net.DispatchMessage;
import engine.net.MessageDispatcher;
import engine.net.client.msg.chat.ChatSystemMsg;
import engine.objects.*;
import engine.server.world.WorldServer;
import org.pmw.tinylog.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static engine.gameManager.StrongholdManager.EndStronghold;
import static engine.server.MBServerStatics.MINE_LATE_WINDOW;

public class HalfHourlyJobThread implements Runnable {

    public HalfHourlyJobThread() {

    }


    public static void processMineWindow() {

        try {

            ArrayList<Mine> mines = Mine.getMines();
            for(Mine mine : mines){
                if (mine.isStronghold)
                    StrongholdManager.EndStronghold(mine);
            }
            for (Mine mine : mines) {
                try {
                    //handle mines opening on server reboot weird time interval
                    if (LocalDateTime.now().isAfter(LocalDateTime.now().withHour(mine.openHour).withMinute(mine.openMinute))) {
                        if (LocalDateTime.now().isBefore(LocalDateTime.now().withHour(mine.openHour).withMinute(mine.openMinute).plusMinutes(30))) {
                            HalfHourlyJobThread.mineWindowOpen(mine);
                            continue;
                        }
                    }

                    // set to the current mine window.

                    if (mine.openHour == LocalDateTime.now().getHour() && mine.openMinute == LocalDateTime.now().getMinute() && !mine.wasClaimed) {
                        HalfHourlyJobThread.mineWindowOpen(mine);
                        continue;
                    }

                    // Close the mine if it reaches this far
                    LocalDateTime openTime = LocalDateTime.now().withHour(mine.openHour).withMinute(mine.openMinute);
                    if (LocalDateTime.now().plusMinutes(1).isAfter(openTime.plusMinutes(30)))
                        mineWindowClose(mine);

                } catch (Exception e) {
                    Logger.error("mineID: " + mine.getObjectUUID(), e.toString());
                }
            }

            //StrongholdManager.processStrongholds();
        } catch (Exception e) {
            Logger.error(e.toString());
        }


    }

    public static void mineWindowOpen(Mine mine) {

        mine.setActive(true);
        ChatManager.chatSystemChannel(mine.getZoneName() + "'s Mine is now Active!");
        Logger.info(mine.getZoneName() + "'s Mine is now Active!");
    }

    public static boolean mineWindowClose(Mine mine) {

        // No need to end the window of a mine which never opened.

        if (mine.isActive == false)
            return false;

        Building mineBuilding = BuildingManager.getBuildingFromCache(mine.getBuildingID());

        if (mineBuilding == null) {
            Logger.debug("Null mine building for Mine " + mine.getObjectUUID() + " Building " + mine.getBuildingID());
            return false;
        }

        // Mine building still stands; nothing to do.
        // We can early exit here.

        if (mineBuilding.getRank() > 0) {
            mine.setActive(false);
            mine.lastClaimer = null;
            ChatSystemMsg chatMsg = new ChatSystemMsg(null, mine.guildName + " has defended the mine in " + mine.getParentZone().getParent().getName() + ". The mine is no longer active.");
            chatMsg.setMessageType(10);
            chatMsg.setChannel(Enum.ChatChannelType.SYSTEM.getChannelID());
            DispatchMessage.dispatchMsgToAll(chatMsg);
            return true;
        }

        // This mine does not have a valid claimer
        // we will therefore set it to errant
        // and keep the window open.

        if (!Mine.validateClaimer(mine.lastClaimer)) {
            mine.lastClaimer = null;
            mine.updateGuildOwner(null);
            mine.setActive(true);
            ChatSystemMsg chatMsg = new ChatSystemMsg(null, mine.getParentZone().getParent().getName() + " Was not claimed, the battle rages on!");
            chatMsg.setMessageType(10);
            chatMsg.setChannel(Enum.ChatChannelType.SYSTEM.getChannelID());
            DispatchMessage.dispatchMsgToAll(chatMsg);
            return false;
        }

        //Update ownership to map

        mine.guildName = mine.getOwningGuild().getName();
        mine.guildTag = mine.getOwningGuild().getGuildTag();
        Guild nation = mine.getOwningGuild().getNation();
        mine.nationName = nation.getName();
        mine.nationTag = nation.getGuildTag();

        mineBuilding.rebuildMine();
        WorldGrid.updateObject(mineBuilding);

        ChatSystemMsg chatMsg = new ChatSystemMsg(null, mine.lastClaimer.getName() + " has claimed the mine in " + mine.getParentZone().getParent().getName() + " for " + mine.getOwningGuild().getName() + ". The mine is no longer active.");
        chatMsg.setMessageType(10);
        chatMsg.setChannel(Enum.ChatChannelType.SYSTEM.getChannelID());
        DispatchMessage.dispatchMsgToAll(chatMsg);

        // Warehouse this claim event

        MineRecord mineRecord = MineRecord.borrow(mine, mine.lastClaimer, Enum.RecordEventType.CAPTURE);
        DataWarehouse.pushToWarehouse(mineRecord);

        mineBuilding.setRank(mineBuilding.getRank());
        mine.lastClaimer = null;
        mine.setActive(false);
        mine.wasClaimed = true;
        for(Integer id : mine._playerMemory){
            PlayerCharacter pc = PlayerCharacter.getFromCache(id);
            if(pc != null)
                pc.ZergMultiplier = 1.0f;
        }
        return true;
    }

    public void run() {

        Logger.info("Half-Hourly job is now running.");

        // Open or Close mines for the current mine window.

        processMineWindow();

    }
}
