package engine.mobileAI.MobHandlers;

import engine.Enum;
import engine.gameManager.MovementManager;
import engine.gameManager.ZoneManager;
import engine.mobileAI.utilities.CombatUtilities;
import engine.mobileAI.utilities.MovementUtilities;
import engine.objects.City;
import engine.objects.Mob;
import engine.objects.PlayerCharacter;

public class PetHandler {

    public static void run(Mob pet){
        PlayerCharacter owner = pet.getOwner();

        if(owner == null)
            return;

        if(!pet.isAlive()){
            return;
        }

        if(!owner.isAlive()) {
            owner.dismissPet();
            return;
        }

        pet.updateLocation();

        if(pet.combatTarget == null){
            //follow owner
            if(!CombatUtilities.inRangeToAttack(pet,owner)) {
                MovementUtilities.moveToLocation(pet, owner.loc, pet.getRange());
            }
        }else{

            if(!pet.combatTarget.isAlive()) {
                pet.setCombatTarget(null);
                return;
            }
            if (pet.combatTarget.equals(pet)) {
                pet.setCombatTarget(null);
                return;
            }
            if(pet.combatTarget != null && pet.combatTarget.getObjectType().equals(Enum.GameObjectType.PlayerCharacter)){
                if(pet.canSee((PlayerCharacter) pet.combatTarget)){
                    pet.setCombatTarget(null);
                    return;
                }
            }

            //ensure boxed characters' pets cannot attack anything except mobs
            if(pet.getOwner() != null){
                if(pet.getOwner().isBoxed){
                    if(!pet.combatTarget.getObjectType().equals(Enum.GameObjectType.Mob)){
                        pet.setCombatTarget(null);
                        return;
                    }
                }
            }

            //chase target
            if(!CombatUtilities.inRange2D(pet,pet.combatTarget,pet.getRange())) {
                MovementUtilities.moveToLocation(pet, pet.combatTarget.loc, pet.getRange());
            }else{
                if(pet.getLastAttackTime() > System.currentTimeMillis())
                    return;

                pet.setLastAttackTime(System.currentTimeMillis() + 3000);

                //attack target
                if(pet.combatTarget.getObjectType().equals(Enum.GameObjectType.Building)){
                    //attacking building
                    City playercity = ZoneManager.getCityAtLocation(pet.getLoc());
                    if (playercity != null)
                        for (Mob guard : playercity.getParent().zoneMobSet)
                            if (guard.combatTarget == null && guard.getGuild() != null && pet.getGuild() != null && !guard.getGuild().equals(pet.getGuild()))
                                MovementUtilities.aiMove(guard,pet.loc,false);
                }

                CombatUtilities.combatCycle(pet,pet.combatTarget,true,null);
            }
        }
    }
}
