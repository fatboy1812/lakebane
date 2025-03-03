package engine.Dungeons;

import engine.Enum;
import engine.InterestManagement.WorldGrid;
import engine.gameManager.DbManager;
import engine.gameManager.ZoneManager;
import engine.math.Vector3fImmutable;
import engine.objects.*;
import engine.powers.EffectsBase;
import engine.server.MBServerStatics;

import java.util.ArrayList;
import java.util.HashSet;

public class DungeonManager {
    public static ArrayList<Dungeon> dungeons;

    private static final float dungeonAiRange = 64f;
    private static final float maxTravel = 64f;

    public static void joinDungeon(PlayerCharacter pc, Dungeon dungeon){
        if(requestEnter(pc,dungeon)) {
            dungeon.participants.add(pc);
            dungeon.applyDungeonEffects(pc);
            translocateToDungeon(pc, dungeon);
        }
    }

    public static void leaveDungeon(PlayerCharacter pc, Dungeon dungeon){
        dungeon.participants.remove(pc);
        dungeon.removeDungeonEffects(pc);
        translocateOutOfDungeon(pc);
    }

    public static boolean requestEnter(PlayerCharacter pc, Dungeon dungeon){
        int current = 0;
        Guild nation = pc.guild.getNation();

        if(nation == null)
            return false;

        for(PlayerCharacter participant : dungeon.participants){
            if(participant.guild.getNation().equals(nation)){
                current ++;
            }
        }

        if(current >= dungeon.maxPerGuild)
            return false;

        return true;
    }

    public static void translocateToDungeon(PlayerCharacter pc, Dungeon dungeon){
        pc.teleport(dungeon.entrance);
        pc.setSafeMode();
    }

    public static void translocateOutOfDungeon(PlayerCharacter pc){
        pc.teleport(pc.bindLoc);
        pc.setSafeMode();
    }

    public static void pulse_dungeons(){
        for(Dungeon dungeon : dungeons){

            //early exit, if no players present don't waste resources
            if(dungeon.participants.isEmpty())
                continue;

            if(dungeon.respawnTime > 0 && System.currentTimeMillis() > dungeon.respawnTime){
                respawnMobs(dungeon);
            }

            //remove any players that have left
            HashSet<AbstractWorldObject> obj = WorldGrid.getObjectsInRangePartial(dungeon.entrance,4096f,MBServerStatics.MASK_PLAYER);
            for(PlayerCharacter player : dungeon.participants)
                if(!obj.contains(player))
                    leaveDungeon(player,dungeon);

            //cycle dungeon mob AI
            for(Mob mob : dungeon.dungeon_mobs)
                dungeonMobAI(mob);
        }
    }

    public static void dungeonMobAI(Mob mob){

    }

    public static void respawnMobs(Dungeon dungeon){
        for(Mob mob : dungeon.dungeon_mobs){

            if(!mob.isAlive() && mob.despawned)
                mob.respawn();

            if(!mob.isAlive() && !mob.despawned){
                mob.despawn();
                mob.respawn();
            }

        }
    }

}
