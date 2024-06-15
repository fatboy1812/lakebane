package engine.gameManager;

import engine.objects.PlayerCharacter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MultiboxManager {
    public static HashMap<String, ArrayList<PlayerCharacter>> activeCharacters = new HashMap<>();
    public static ReentrantReadWriteLock updateLock = new ReentrantReadWriteLock();

    public static void addPlayer(PlayerCharacter player){
        updateLock.writeLock().lock();
        try {

            //get the machine ID for the key of the map
            String machineID = player.getClientConnection().machineID;

            //cleanup and remove inactive players from the list
            ArrayList<PlayerCharacter> purgeList = new ArrayList<>();
            for(PlayerCharacter pc : activeCharacters.get(machineID))
                if(!pc.isEnteredWorld() || !pc.isActive())
                    purgeList.add(pc);

            activeCharacters.get(machineID).removeAll(purgeList);

            //remove empty key
            if(activeCharacters.get(machineID).size() < 1)
                activeCharacters.remove(machineID);


            if(activeCharacters.containsKey(machineID)){
                //already has an entry for this machine ID
                player.isBoxed = true;
                activeCharacters.get(machineID).add(player);
            }else{
                //does not have an entry for this machine ID
                player.isBoxed = false;
                ArrayList<PlayerCharacter> newList = new ArrayList<>();
                newList.add(player);
                activeCharacters.put(machineID,newList);
            }
        } finally {
            updateLock.writeLock().unlock();
        }
    }

    public static void removePlayer(PlayerCharacter player){

        //get the machine ID for the key of the map
        String machineID = player.getClientConnection().machineID;

        if(activeCharacters.containsKey(machineID)){
            //remove player from existing list
            activeCharacters.get(machineID).remove(player);

            //check if there are still players in the machine ID key list
            if(activeCharacters.get(machineID).size() > 1){
                //list still has characters, make one of them active
                activeCharacters.get(machineID).get(0).isBoxed = false;
            }else{
                //list is now empty, remove it from the map
                activeCharacters.remove(machineID);
            }
        }
    }
}
