package engine.mobileAI.MobHandlers;

import engine.Enum;
import engine.InterestManagement.InterestManager;
import engine.InterestManagement.WorldGrid;
import engine.gameManager.BuildingManager;
import engine.gameManager.MovementManager;
import engine.gameManager.NPCManager;
import engine.gameManager.ZoneManager;
import engine.mobileAI.utilities.CombatUtilities;
import engine.mobileAI.utilities.MovementUtilities;
import engine.objects.Building;
import engine.objects.City;
import engine.objects.Mob;
import engine.objects.Zone;
import engine.server.MBServerStatics;

import java.util.HashMap;

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
        try {

            if (engine.deathTime == 0) {
                engine.setDeathTime(System.currentTimeMillis());
                return;
            }

            if (System.currentTimeMillis() > (engine.deathTime + 10000)) {
                //Zone.respawnQue.add(engine);
                engine.despawned = false;
                engine.setCombatTarget(null);
                engine.setHealth(engine.healthMax);
                engine.isAlive.set(true);
                engine.deathTime = 0;

                if (engine.building != null)
                    engine.region = BuildingManager.GetRegion(engine.building, engine.bindLoc.x, engine.bindLoc.y, engine.bindLoc.z);

                MovementManager.translocate(engine, engine.bindLoc, engine.region);

                engine.updateLocation();
                InterestManager.setObjectDirty(engine);
            }
        } catch (Exception e) {
            //(aiAgent.getObjectUUID() + " " + aiAgent.getName() + " Failed At: CheckForRespawn" + " " + e.getMessage());
        }
    }

    public static void siege_attack(Mob engine){
        if(engine.getLastAttackTime() > System.currentTimeMillis())
            return;

        if(CombatUtilities.inRangeToAttack(engine,engine.combatTarget)){
            CombatUtilities.combatCycle(engine, engine.combatTarget, true, null);
            engine.setLastAttackTime(System.currentTimeMillis() + 15000);
            MovementManager.sendRWSSMsg(engine);
        }
    }
}
