package engine.gameManager;

import engine.Enum;
import engine.InterestManagement.WorldGrid;
import engine.exception.MsgSendException;
import engine.math.Vector3f;
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

        if(lastExecution + pulseDelay > System.currentTimeMillis())
            return;

        lastExecution = System.currentTimeMillis();

        while (playerQueue.size() > 1) {
            createArena();
        }
    }

    public static void joinQueue(PlayerCharacter player) {
        if (!playerQueue.contains(player)) {
            playerQueue.add(player);
        }
        for(PlayerCharacter pc : playerQueue){
            if(pc.equals(player))
                continue;
            ChatManager.chatSystemInfo(pc, player.getName() + " has joined the arena que. There are now " + playerQueue.size() + " players queued.");
        }
    }

    public static void leaveQueue(PlayerCharacter player) {
        playerQueue.remove(player);
        for(PlayerCharacter pc : playerQueue){
            if(pc.equals(player))
                continue;
            ChatManager.chatSystemInfo(pc, player.getName() + " has left the arena que. There are now " + playerQueue.size() + " players queued.");
        }
    }

    private static void createArena() {
        if (playerQueue.size() > 1) {

            Collections.shuffle(playerQueue);
            Arena newArena = new Arena();

            //set starting time
            newArena.startTime = System.currentTimeMillis();

            //decide an arena location
            newArena.loc = selectRandomArenaLocation();

            // Assign players to the arena
            newArena.player1 = playerQueue.remove(0);
            newArena.player2 = playerQueue.remove(0);

            // Teleport players to the arena location
            Zone sdr = ZoneManager.getZoneByUUID(656);
            MovementManager.translocate(newArena.player1, Vector3fImmutable.getRandomPointOnCircle(newArena.loc,75f), null);
            MovementManager.translocate(newArena.player2, Vector3fImmutable.getRandomPointOnCircle(newArena.loc,75f), null);

            // Add the new arena to the active arenas list
            activeArenas.add(newArena);
        }
    }

    public static void endArena(Arena arena, PlayerCharacter winner, PlayerCharacter loser, String condition){
        if (winner != null && loser != null) {
            Logger.info("[ARENA] The fight between {} and {} is concluded. Victor: {}",
                    arena.player1.getName(), arena.player2.getName(), winner.getName());
        } else {
            Logger.info("[ARENA] The fight between {} and {} is concluded. No Winner Declared.",
                    arena.player1.getName(), arena.player2.getName());
        }
        // Teleport players to the arena location
        Zone sdr = ZoneManager.getZoneByUUID(656);
        MovementManager.translocate(arena.player1, Vector3fImmutable.getRandomPointOnCircle(sdr.getLoc(),50f), null);
        MovementManager.translocate(arena.player2, Vector3fImmutable.getRandomPointOnCircle(sdr.getLoc(),50f), null);



        activeArenas.remove(arena);

        if(winner != null){
            ChatManager.chatPVP("[ARENA] " + winner.getName() + " has slain " + loser.getName() + " in the arena!");
            //handle prize distribution
            //ItemBase specialLoot = ItemBase.getItemBase(866);
            //Item promoted = new MobLoot(winner, specialLoot, 1, false).promoteToItem(winner);
            //promoted.setNumOfItems(21235);
            //promoted.setName("Special Banker(21235)");
            //DbManager.ItemQueries.UPDATE_NUM_ITEMS(promoted,21235);
            //winner.getCharItemManager().addItemToInventory(promoted);
            //winner.getCharItemManager().updateInventory();
        }
    }

    public static Vector3fImmutable selectRandomArenaLocation() {
        boolean locSet = false;
        Vector3fImmutable loc = Vector3fImmutable.ZERO;

        while (!locSet) {
            try {
                float x = ThreadLocalRandom.current().nextInt(114300, 123600);
                float z = ThreadLocalRandom.current().nextInt(82675, 91700);
                float y = 0; // Y coordinate is always 0

                loc = new Vector3fImmutable(x, y, z * -1);
                HashSet<AbstractWorldObject> inRange = WorldGrid.getObjectsInRangePartial(loc,500f, MBServerStatics.MASK_PLAYER);
                if(inRange.isEmpty() && !isUnderWater(loc))
                    locSet = true;
                //}
            }catch(Exception e){

            }
        }

        return loc;
    }

    public static boolean isUnderWater(Vector3fImmutable loc) {

        try {

            Zone zone = ZoneManager.findSmallestZone(loc);

            if (zone.getSeaLevel() != 0) {

                float localAltitude = loc.y;
                if (localAltitude < zone.getSeaLevel())
                    return true;
            } else {
                if (loc.y < 0)
                    return true;
            }
        } catch (Exception e) {

        }

        return false;
    }
}
