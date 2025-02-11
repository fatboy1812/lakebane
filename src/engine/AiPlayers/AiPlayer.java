package engine.AiPlayers;

import engine.Enum;
import engine.InterestManagement.WorldGrid;
import engine.math.Vector3fImmutable;
import engine.net.client.msg.VendorDialogMsg;
import engine.objects.*;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AiPlayer {
    public PlayerCharacter emulated;

    //randomized constructor
    //creates a random AI player to start at level 10 and progress throughout the game world
    public AiPlayer(){
        PlayerCharacter emu = generateRandomPlayer();
        if(emu != null)
            this.emulated = emu;
    }

    public static PlayerCharacter generateRandomPlayer(){
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

        PromotionClass promo = null;
        while (promo == null){
            int promoId = ThreadLocalRandom.current().nextInt(2504,2526);
            PromotionClass tempPromo = PromotionClass.GetPromtionClassFromCache(promoId);
            if(tempPromo.isAllowedRune(baseClass.getToken())){
                promo = tempPromo;
            }
        }

        Account a = new Account();
        PlayerCharacter emulated = new PlayerCharacter(AiPlayerManager.generateFirstName(), "AI Player", (short) 0, (short) 0, (short) 0,
                (short) 0, (short) 0, Guild.getErrantGuild(), (byte) 0, a, race, baseClass, (byte) 1, (byte) 1,
                (byte) 1, (byte) 1, (byte) 1);

        emulated.setPromotionClass(promo.getObjectUUID());

        return emulated;
    }

    public void runAfterLoad(){
        WorldGrid.addObject(this.emulated,this.emulated.bindLoc.x,this.emulated.bindLoc.z);
        City hamlet = AiPlayerManager.getRandomHamlet();
        this.emulated.teleport(Vector3fImmutable.getRandomPointOnCircle(hamlet.getTOL().loc,30));
        WorldGrid.updateObject(this.emulated);
        this.emulated.removeEffectBySource(Enum.EffectSourceType.Invisibility,40,true);
        this.emulated.removeEffectBySource(Enum.EffectSourceType.Invulnerability,40,true);
    }

    public void update(){
        this.emulated.update(true);
    }


}
