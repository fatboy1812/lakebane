// • ▌ ▄ ·.  ▄▄▄·  ▄▄ • ▪   ▄▄· ▄▄▄▄·  ▄▄▄·  ▐▄▄▄  ▄▄▄ .
// ·██ ▐███▪▐█ ▀█ ▐█ ▀ ▪██ ▐█ ▌▪▐█ ▀█▪▐█ ▀█ •█▌ ▐█▐▌·
// ▐█ ▌▐▌▐█·▄█▀▀█ ▄█ ▀█▄▐█·██ ▄▄▐█▀▀█▄▄█▀▀█ ▐█▐ ▐▌▐▀▀▀
// ██ ██▌▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▌▐███▌██▄▪▐█▐█ ▪▐▌██▐ █▌▐█▄▄▌
// ▀▀  █▪▀▀▀ ▀  ▀ ·▀▀▀▀ ▀▀▀·▀▀▀ ·▀▀▀▀  ▀  ▀ ▀▀  █▪ ▀▀▀
//      Magicbane Emulator Project © 2013 - 2022
//                www.magicbane.com


package engine.workthreads;

import engine.Enum;
import engine.gameManager.*;
import engine.net.MessageDispatcher;
import engine.objects.*;
import engine.server.world.WorldServer;
import org.pmw.tinylog.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class HourlyJobThread implements Runnable {

    public HourlyJobThread() {

    }

    public void run() {

        // *** REFACTOR: TRY TRY TRY TRY {{{{{{{{{{{ OMG

        Logger.info("Hourly job is now running.");

        // Update city population values

        ConcurrentHashMap<Integer, AbstractGameObject> map = DbManager.getMap(Enum.GameObjectType.City);

        if (map != null) {

            for (AbstractGameObject ago : map.values()) {

                City city = (City) ago;

                if (city != null)
                    if (city.getGuild() != null) {
                        ArrayList<PlayerCharacter> guildList = Guild.GuildRoster(city.getGuild());
                        city.setPopulation(guildList.size());
                    }
            }
            City.lastCityUpdate = System.currentTimeMillis();
        } else {
            Logger.error("missing city map");
        }

        //run mines every day at 1:00 am CST
        if(LocalDateTime.now().getHour() == 1) {
            //produce mine resources once a day
            for (Mine mine : Mine.getMines()) {
                try {
                    mine.depositMineResources();
                } catch (Exception e) {
                    Logger.info(e.getMessage() + " for Mine " + mine.getObjectUUID());
                }
                mine.wasClaimed = false;
            }
        }

        //run maintenance every day at 2 am
        if(LocalDateTime.now().getHour() == 2) {
            MaintenanceManager.dailyMaintenance();
        }

        switch(LocalDateTime.now().getHour()){
            case 3:
            case 6:
            case 9:
            case 12:
            case 15:
            case 18:
            case 21:
            case 0:
                for(Mob mob : Mob.discDroppers)
                    if(!mob.isAlive())
                        Zone.respawnQue.add(mob);
                break;
        }

        // Log metrics to console
        Logger.info(WorldServer.getUptimeString());
        Logger.info(SimulationManager.getPopulationString());
        Logger.info(MessageDispatcher.getNetstatString());
        Logger.info(PurgeOprhans.recordsDeleted.toString() + "orphaned items deleted");

        //for (Bane bane : Bane.banes.values()){
        //    if(bane.getSiegePhase().equals(Enum.SiegePhase.CHALLENGE)){
        //        bane.setDefaultTime();
        //    }
        //}

        try{
            Logger.info("Trashing Multibox Cheaters");
            DbManager.AccountQueries.TRASH_CHEATERS();

            //disconnect all players who were banned and are still in game
            for(PlayerCharacter pc : SessionManager.getAllActivePlayers()){
                Account account = pc.getClientConnection().getAccount();
                if(account == null)
                    continue;
                try {
                    boolean banned = DbManager.AccountQueries.GET_ACCOUNT(account.getUname()).status.equals(Enum.AccountStatus.BANNED);
                    if (banned) {
                        pc.getClientConnection().forceDisconnect();
                    }
                }catch(Exception e){
                    Logger.error(e.getMessage());
                }
            }
        }catch(Exception e){
            Logger.error("Failed To Run Ban Multibox Abusers");
        }

        BaneManager.default_check();
    }
}
