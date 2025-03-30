package engine.ZergMehcanics;

import engine.InterestManagement.WorldGrid;
import engine.gameManager.BuildingManager;
import engine.gameManager.ZergManager;
import engine.objects.*;
import engine.server.MBServerStatics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MineAntiZerg {

    public static HashMap<Mine,HashMap<PlayerCharacter,Long>> leaveTimers = new HashMap<>();
    public static HashMap<Mine,ArrayList<PlayerCharacter>> currentPlayers = new HashMap<>();

    public static void runMines(){
        for(Mine mine : Mine.getMines()){

            Building tower = BuildingManager.getBuildingFromCache(mine.getBuildingID());

            if(tower == null)
                continue;

            if(!mine.isActive)
                continue;

            logPlayersPresent(tower,mine);

            auditPlayersPresent(tower,mine);

            auditPlayers(mine);
        }
    }

    public static void logPlayersPresent(Building tower, Mine mine){
        HashSet<AbstractWorldObject> loadedPlayers = WorldGrid.getObjectsInRangePartial(tower.loc, MBServerStatics.CHARACTER_LOAD_RANGE * 3,MBServerStatics.MASK_PLAYER);

        ArrayList<PlayerCharacter> playersPresent = new ArrayList<>();
        for(AbstractWorldObject player : loadedPlayers){
            playersPresent.add((PlayerCharacter)player);
        }

        currentPlayers.put(mine,playersPresent);
    }

    public static void auditPlayersPresent(Building tower, Mine mine){
        HashSet<AbstractWorldObject> loadedPlayers = WorldGrid.getObjectsInRangePartial(tower.loc, MBServerStatics.CHARACTER_LOAD_RANGE * 3,MBServerStatics.MASK_PLAYER);

        ArrayList<PlayerCharacter> toRemove = new ArrayList<>();

        for(PlayerCharacter player : currentPlayers.get(mine)){
            if(!loadedPlayers.contains(player)){
                toRemove.add(player);
            }
        }

        currentPlayers.get(mine).removeAll(toRemove);

        for(PlayerCharacter player : toRemove){
            if(leaveTimers.containsKey(mine)){
                leaveTimers.get(mine).put(player,System.currentTimeMillis());
            }else{
                HashMap<PlayerCharacter,Long> leaveTime = new HashMap<>();
                leaveTime.put(player,System.currentTimeMillis());
                leaveTimers.put(mine,leaveTime);
            }
        }

        toRemove.clear();

        for(PlayerCharacter player : leaveTimers.get(mine).keySet()){
            long timeGone = System.currentTimeMillis() - leaveTimers.get(mine).get(player);
            if(timeGone > 180000L) {//3 minutes
                toRemove.add(player);
                player.ZergMultiplier = 1.0f;
            }
        }

        for(PlayerCharacter player : toRemove) {
            leaveTimers.get(mine).remove(player);
        }
    }

    public static void auditPlayers(Mine mine){

        HashMap<Guild,ArrayList<PlayerCharacter>> playersByNation = new HashMap<>();

        for(PlayerCharacter player : currentPlayers.get(mine)){
            if(playersByNation.containsKey(player.guild.getNation())){
                playersByNation.get(player.guild.getNation()).add(player);
            }else{
                ArrayList<PlayerCharacter> players = new ArrayList<>();
                players.add(player);
                playersByNation.put(player.guild.getNation(),players);
            }
        }

        for(PlayerCharacter player : leaveTimers.get(mine).keySet()){
            if(playersByNation.containsKey(player.guild.getNation())){
                playersByNation.get(player.guild.getNation()).add(player);
            }else{
                ArrayList<PlayerCharacter> players = new ArrayList<>();
                players.add(player);
                playersByNation.put(player.guild.getNation(),players);
            }
        }

        for(Guild nation : playersByNation.keySet()){
            for(PlayerCharacter player : playersByNation.get(nation)){
                player.ZergMultiplier = ZergManager.getCurrentMultiplier(playersByNation.get(nation).size(), mine.capSize);
            }
        }
    }
}
