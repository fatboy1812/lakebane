package engine.mobileAI.MobHandlers;

import engine.Enum;
import engine.gameManager.MovementManager;
import engine.gameManager.ZoneManager;
import engine.mobileAI.utilities.CombatUtilities;
import engine.mobileAI.utilities.MovementUtilities;
import engine.objects.Building;
import engine.objects.City;
import engine.objects.Mob;
import engine.server.MBServerStatics;

public class SiegeHandler {
    public static void run(Mob engine){

        if(!engine.isAlive()) {
            check_siege_respawn(engine);
            return;
        }

        if(engine.getOwner() == null || !engine.getOwner().isAlive() || !engine.playerAgroMap.containsKey(engine.getOwner().getObjectUUID()))
            return;

        if(engine.combatTarget == null || !engine.combatTarget.getObjectType().equals(Enum.GameObjectType.Building))
            return;

        siege_attack(engine);
    }

    public static void check_siege_respawn(Mob engine){
        if(!engine.despawned) {
            engine.despawn();
        }else{
            if(engine.deathTime + MBServerStatics.FIFTEEN_MINUTES > System.currentTimeMillis()){
                engine.respawn();
            }
        }
    }

    public static void siege_attack(Mob engine){
        if(engine.getLastAttackTime() > System.currentTimeMillis())
            return;

        if(CombatUtilities.inRangeToAttack(engine,engine.combatTarget)){
            CombatUtilities.combatCycle(engine, engine.combatTarget, true, null);
            engine.setLastAttackTime(System.currentTimeMillis() + 15000);
            City playercity = ZoneManager.getCityAtLocation(engine.getLoc());
            if (playercity != null)
                for (Mob guard : playercity.getParent().zoneMobSet)
                    if (guard.isPlayerGuard && guard.combatTarget == null && guard.getGuild() != null && engine.getGuild() != null && !guard.getGuild().equals(engine.getGuild()))
                        MovementUtilities.aiMove(guard,engine.loc,false);

            MovementManager.sendRWSSMsg(engine);
        }
    }
}
