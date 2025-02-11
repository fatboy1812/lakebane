package engine.AiPlayers;

import engine.Enum;
import engine.InterestManagement.InterestManager;
import engine.InterestManagement.WorldGrid;
import engine.gameManager.LootManager;
import engine.math.Vector3fImmutable;
import engine.net.client.msg.VendorDialogMsg;
import engine.objects.*;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AiPlayer {
    public Mob emulated;

    //randomized constructor
    //creates a random AI player to start at level 10 and progress throughout the game world
    public AiPlayer(){
        Mob emu = generateRandomPlayer();
        if(emu != null)
            this.emulated = emu;
    }

    public static Mob generateRandomPlayer(){
        Race race = AiPlayerManager.getRandomRace();
        if(race == null)
            return null;

        BaseClass baseClass = null;

        List<BaseClass> validClasses = race.getValidBaseClasses();
        if (!validClasses.isEmpty()) {
            Random random = new Random();
            baseClass = validClasses.get(random.nextInt(validClasses.size()));
            // Use randomClass here
        }

        if(baseClass == null)
            return null;

        City hamlet = AiPlayerManager.getRandomHamlet();
        Vector3fImmutable loc = Vector3fImmutable.getRandomPointOnCircle(hamlet.getTOL().loc,30);

        Mob guard = Mob.createStrongholdMob(race.getRaceRuneID(), loc, Guild.getErrantGuild(),true,hamlet.getParent(),null,0, AiPlayerManager.generateFirstName(),10);

        if(guard != null){
            guard.parentZone = hamlet.getParent();
            guard.bindLoc = loc;
            guard.setLoc(loc);
            guard.StrongholdGuardian = true;
            guard.runAfterLoad();
            guard.setLevel((short)10);
            guard.spawnTime = 1000000000;
            guard.setFirstName(AiPlayerManager.generateFirstName());
            guard.setLastName("Ai Player");
            InterestManager.setObjectDirty(guard);
            WorldGrid.addObject(guard,loc.x,loc.z);
            WorldGrid.updateObject(guard);
            guard.mobPowers.clear();
        }

        return guard;
    }

    public void update(){
        this.emulated.update(true);
    }


}
