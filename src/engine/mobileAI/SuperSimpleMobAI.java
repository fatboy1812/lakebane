package engine.mobileAI;

import engine.mobileAI.MobHandlers.MobHandler;
import engine.mobileAI.MobHandlers.PetHandler;
import engine.mobileAI.MobHandlers.PlayerGuardHandler;
import engine.mobileAI.MobHandlers.SiegeHandler;
import engine.objects.Mob;

public class SuperSimpleMobAI {

    public static void run(Mob mob){

        if(mob.isMoving())
            mob.updateLocation();

        if(mob.isPet() && !mob.isSiege()) {
            //MobAI.DetermineAction(mob);
            PetHandler.run(mob);
            return;
        }else if (mob.isSiege()) {
            //SiegeHandler.run(mob);
            MobAI.DetermineAction(mob);
            return;
        }else if(mob.isPlayerGuard()){
            PlayerGuardHandler.run(mob);
            //MobAI.DetermineAction(mob);
            return;
        }else {
            MobHandler.run(mob);
        }
    }
}
