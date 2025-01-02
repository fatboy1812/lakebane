package engine.gameManager;

import engine.InterestManagement.WorldGrid;
import engine.math.Vector3fImmutable;
import engine.objects.*;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ArenaManager {
    private static final List<Arena> activeArenas = new ArrayList<>();
    public static final List<PlayerCharacter> playerQueue = new ArrayList<>();
    public static Long pulseDelay = 180000L;
    public static Long lastExecution = 0L;

    public static void pulseArenas() {
        if(lastExecution == 0L){
            lastExecution = System.currentTimeMillis();
        }

        if(activeArenas.isEmpty() && playerQueue.isEmpty())
            return;

        Iterator<Arena> iterator = activeArenas.iterator();

        while (iterator.hasNext()) {
            Arena arena = iterator.next();
            if (arena.checkToComplete()) {
                iterator.remove();
            }
        }

        //if(lastExecution + pulseDelay > System.currentTimeMillis())
        //    return;

        lastExecution = System.currentTimeMillis();

        while (playerQueue.size() > 1) {
            createArena();
        }
    }

    public static void joinQueue(PlayerCharacter player) {
        if (!playerQueue.contains(player)) {
            playerQueue.add(player);
        }
    }

    public static void leaveQueue(PlayerCharacter player) {
        playerQueue.remove(player);
    }

    private static void createArena() {
        if (playerQueue.size() > 1) {
            Collections.shuffle(playerQueue);
            Arena newArena = new Arena();

            //decide an arena location
            newArena.loc = selectRandomArenaLocation();

            // Assign players to the arena
            newArena.player1 = playerQueue.remove(0);
            newArena.player2 = playerQueue.remove(0);

            // Teleport players to the arena location
            MovementManager.translocate(newArena.player1, newArena.loc, Regions.GetRegionForTeleport(newArena.loc));
            MovementManager.translocate(newArena.player2, newArena.loc, Regions.GetRegionForTeleport(newArena.loc));

            // Add the new arena to the active arenas list
            activeArenas.add(newArena);
        }
    }

    public static void endArena(Arena arena, PlayerCharacter winner, PlayerCharacter loser, String condition) {
        if (winner != null && loser != null) {
            Logger.info("[ARENA] The fight between {} and {} is concluded. Victor: {}",
                    arena.player1.getName(), arena.player2.getName(), winner.getName());
        } else {
            Logger.info("[ARENA] The fight between {} and {} is concluded. No Winner Declared.",
                    arena.player1.getName(), arena.player2.getName());
        }

        activeArenas.remove(arena);
    }

    public static Vector3fImmutable selectRandomArenaLocation() {
        boolean locSet = false;
        Vector3fImmutable loc = Vector3fImmutable.ZERO;

        while (!locSet) {
            try {
                // Generate random X and Z coordinates within the range [10,000, 90,000]
                float x = ThreadLocalRandom.current().nextInt(10000, 90000);
                float z = ThreadLocalRandom.current().nextInt(10000, 90000);
                float y = 0; // Y coordinate is always 0

                loc = new Vector3fImmutable(x, y, z);
                Zone zone = ZoneManager.findSmallestZone(loc);
                if (zone.isContinent() && !ZoneManager.getSeaFloor().equals(zone)) {
                    HashSet<AbstractWorldObject> inRange = WorldGrid.getObjectsInRangePartial(loc,250f, MBServerStatics.MASK_PLAYER);
                    if(inRange.isEmpty())
                        locSet = true;
                }
            }catch(Exception e){

            }
        }

        return loc;
    }
}
