package engine.mobileAI.MobHandlers;

import engine.Enum;
import engine.objects.Mob;

public class SiegeHandler {
    public static void run(Mob engine){

        if(!engine.isAlive())
            return;

        if(engine.getOwner() == null || !engine.getOwner().isAlive())
            return;

        if(engine.combatTarget == null || !engine.combatTarget.getObjectType().equals(Enum.GameObjectType.Building))
            return;

    }
}
