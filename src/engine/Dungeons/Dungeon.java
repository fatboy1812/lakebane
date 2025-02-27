package engine.Dungeons;

import engine.Enum;
import engine.InterestManagement.WorldGrid;
import engine.gameManager.PowersManager;
import engine.math.Vector3fImmutable;
import engine.objects.*;
import engine.powers.EffectsBase;
import engine.server.MBServerStatics;

import java.util.ArrayList;
import java.util.HashSet;

public class Dungeon {

    public static int NoFlyEffectID = -1733819072;
    public static int NoTeleportEffectID = -1971545187;
    public static int NoSummonEffectID = 2122002462;
    public ArrayList<PlayerCharacter> participants;
    public int maxPerGuild;
    public Vector3fImmutable entrance;
    public ArrayList<Mob> dungeon_mobs;
    public Long respawnTime = 0L;

    public Dungeon(Vector3fImmutable entrance, int maxCount){
        this.participants = new ArrayList<>();
        this.entrance = entrance;
        this.dungeon_mobs = new ArrayList<>();
        this.maxPerGuild = maxCount;
    }
    public void applyDungeonEffects(PlayerCharacter player){
        EffectsBase noFly = PowersManager.getEffectByToken(NoFlyEffectID);
        EffectsBase noTele = PowersManager.getEffectByToken(NoTeleportEffectID);
        EffectsBase noSum = PowersManager.getEffectByToken(NoSummonEffectID);

        if(noFly != null)
            player.addEffectNoTimer(noFly.getName(),noFly,40,true);

        if(noTele != null)
            player.addEffectNoTimer(noTele.getName(),noTele,40,true);

        if(noSum != null)
            player.addEffectNoTimer(noSum.getName(),noSum,40,true);
    }

    public void removeDungeonEffects(PlayerCharacter player) {
        EffectsBase noFly = PowersManager.getEffectByToken(NoFlyEffectID);
        EffectsBase noTele = PowersManager.getEffectByToken(NoTeleportEffectID);
        EffectsBase noSum = PowersManager.getEffectByToken(NoSummonEffectID);
        for (Effect eff : player.effects.values()) {
            if (noFly != null && eff.getEffectsBase().equals(noFly))
                eff.endEffect();

            if (noTele != null && eff.getEffectsBase().equals(noTele))
                eff.endEffect();

            if (noSum != null && eff.getEffectsBase().equals(noSum))
                eff.endEffect();
        }
    }
}
