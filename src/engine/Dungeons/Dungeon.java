package engine.Dungeons;

import engine.Enum;
import engine.InterestManagement.WorldGrid;
import engine.gameManager.BuildingManager;
import engine.gameManager.PowersManager;
import engine.gameManager.ZoneManager;
import engine.math.Vector3fImmutable;
import engine.net.ByteBufferWriter;
import engine.objects.*;
import engine.powers.EffectsBase;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.time.LocalDateTime;
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

    public static void serializeForClientMsgTeleport(ByteBufferWriter writer) {
        Guild rulingGuild = Guild.getErrantGuild();
        Guild rulingNation = Guild.getErrantGuild();

        Zone zone = ZoneManager.getZoneByUUID(994);
        // Begin Serialzing soverign guild data
        writer.putInt(Enum.GameObjectType.Zone.ordinal());
        writer.putInt(994);
        writer.putString("Whitehorn Citadel");
        writer.putInt(rulingGuild.getObjectType().ordinal());
        writer.putInt(rulingGuild.getObjectUUID());

        writer.putString("Whitehorn Militants"); // guild name
        writer.putString("In the Citadel, We Fight!"); // motto
        writer.putString(rulingGuild.getLeadershipType());

        // Serialize guild ruler's name
        // If tree is abandoned blank out the name
        // to allow them a rename.

        writer.putString("Kol'roth The Destroyer");//sovreign

        writer.putInt(rulingGuild.getCharter());
        writer.putInt(0); // always 00000000

        writer.put((byte)0);

        writer.put((byte) 1);
        writer.put((byte) 1);  // *** Refactor: What are these flags?
        writer.put((byte) 1);
        writer.put((byte) 1);
        writer.put((byte) 1);

        GuildTag._serializeForDisplay(rulingGuild.getGuildTag(), writer);
        GuildTag._serializeForDisplay(rulingNation.getGuildTag(), writer);

        writer.putInt(0);// TODO Implement description text

        writer.put((byte) 1);
        writer.put((byte) 0);
        writer.put((byte) 1);

        // Begin serializing nation guild info

        if (rulingNation.isEmptyGuild()) {
            writer.putInt(rulingGuild.getObjectType().ordinal());
            writer.putInt(rulingGuild.getObjectUUID());
        } else {
            writer.putInt(rulingNation.getObjectType().ordinal());
            writer.putInt(rulingNation.getObjectUUID());
        }


        // Serialize nation name

        writer.putString("Whitehorn Militants"); //nation name

        writer.putInt(-1);//city rank, -1 puts it at top of list always

        writer.putInt(0xFFFFFFFF);

        writer.putInt(0);

        writer.putString("Kol'roth The Destroyer");//nation ruler

        writer.putLocalDateTime(LocalDateTime.now());

        //location
        Vector3fImmutable loc = Vector3fImmutable.getRandomPointOnCircle(BuildingManager.getBuilding(2827951).loc,30f);

        writer.putFloat(loc.x);
        writer.putFloat(loc.y);
        writer.putFloat(loc.z);

        writer.putInt(0);

        writer.put((byte) 1);
        writer.put((byte) 0);
        writer.putInt(0x64);
        writer.put((byte) 0);
        writer.put((byte) 0);
        writer.put((byte) 0);
    }
}
