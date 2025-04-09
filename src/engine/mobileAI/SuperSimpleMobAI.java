package engine.mobileAI;

import engine.mobileAI.MobHandlers.MobHandler;
import engine.mobileAI.MobHandlers.PetHandler;
import engine.mobileAI.MobHandlers.PlayerGuardHandler;
import engine.mobileAI.MobHandlers.SiegeHandler;
import engine.objects.Mob;
import org.pmw.tinylog.Logger;

public class SuperSimpleMobAI {

    public static void run(Mob mob){

        if(mob == null)
            return;

        try {
            MobType type = MobType.getMobType(mob);
            switch(type){
                case Siege:
                    SiegeHandler.run(mob);
                    break;
                case Standard:
                    MobHandler.run(mob);
                    break;
                case PlayerGuard:
                    PlayerGuardHandler.run(mob);
                    break;
                case Pet:
                    PetHandler.run(mob);
                    break;
            }

        }catch(Exception e){
            Logger.error(e.getMessage());
        }
    }
    public enum MobType{
        PlayerGuard,
        Standard,
        Pet,
        Siege;

        public static MobType getMobType(Mob mob){
            if (mob.getMobBaseID() == 13171) {
                return Siege;
            }
            if (mob.isPet() && !mob.isSiege()) {
                return Pet;
            } else if (mob.isSiege()) {
                return Siege;
            } else if (mob.isPlayerGuard()) {
                return PlayerGuard;
            } else {
                return Standard;
            }
        }
    }
}

