package engine.gameManager;

import engine.Enum;
import engine.InterestManagement.WorldGrid;
import engine.math.Quaternion;
import engine.math.Vector3f;
import engine.math.Vector3fImmutable;
import engine.net.Dispatch;
import engine.net.DispatchMessage;
import engine.net.client.msg.PetMsg;
import engine.objects.*;
import engine.powers.EffectsBase;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

import static engine.math.FastMath.acos;

public enum NPCManager {

    NPC_MANAGER;
    public static HashMap<Integer, ArrayList<Integer>> _runeSetMap = new HashMap<>();

    public static void LoadAllRuneSets() {
        _runeSetMap = DbManager.ItemBaseQueries.LOAD_RUNES_FOR_NPC_AND_MOBS();
    }

    public static void LoadAllBootySets() {
        LootManager._bootySetMap = DbManager.LootQueries.LOAD_BOOTY_TABLES();
    }

    public static void applyRuneSetEffects(Mob mob) {

        // Early exit

        if (mob.runeSet == 0)
            return;

        //Apply all rune effects.

        if (NPCManager._runeSetMap.get(mob.runeSet).contains(252623)) {
            mob.isPlayerGuard = true;
        }

        // Only captains have contracts

        if (mob.contract != null || mob.isPlayerGuard)
            applyEffectsForRune(mob, 252621);


        // Apply effects from RuneSet

        if (mob.runeSet != 0)
            for (int runeID : _runeSetMap.get(mob.runeSet))
                applyEffectsForRune(mob, runeID);

        // Not sure why but apply Warrior effects for some reason?

        applyEffectsForRune(mob, 2518);
    }

    public static void applyEffectsForRune(AbstractCharacter character, int runeID) {

        EffectsBase effectsBase;
        RuneBase sourceRune = RuneBase.getRuneBase(runeID);

        // Race runes are in the runeset but not in runebase for some reason

        if (sourceRune == null)
            return;

        for (MobBaseEffects mbe : sourceRune.getEffectsList()) {

            effectsBase = PowersManager.getEffectByToken(mbe.getToken());

            if (effectsBase == null) {
                Logger.info("Mob: " + character.getObjectUUID() + "  EffectsBase Null for Token " + mbe.getToken());
                continue;
            }

            //check to upgrade effects if needed.
            if (character.effects.containsKey(Integer.toString(effectsBase.getUUID()))) {

                if (mbe.getReqLvl() > (int) character.level)
                    continue;

                Effect eff = character.effects.get(Integer.toString(effectsBase.getUUID()));

                if (eff == null)
                    continue;

                //Current effect is a higher rank, dont apply.
                if (eff.getTrains() > mbe.getRank())
                    continue;

                //new effect is of a higher rank. remove old effect and apply new one.
                eff.cancelJob();
                character.addEffectNoTimer(Integer.toString(effectsBase.getUUID()), effectsBase, mbe.getRank(), true);
            } else {

                if (mbe.getReqLvl() > (int) character.level)
                    continue;

                character.addEffectNoTimer(Integer.toString(effectsBase.getUUID()), effectsBase, mbe.getRank(), true);
            }

        }
    }

    public static void dismissNecroPet(Mob necroPet, boolean updateOwner) {

        necroPet.setCombatTarget(null);
        necroPet.hasLoot = false;

        if (necroPet.parentZone != null)
            necroPet.parentZone.zoneMobSet.remove(necroPet);

        try {
            necroPet.clearEffects();
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
        necroPet.playerAgroMap.clear();
        WorldGrid.RemoveWorldObject(necroPet);

        DbManager.removeFromCache(necroPet);

        PlayerCharacter petOwner = necroPet.getOwner();

        if (petOwner != null) {
            necroPet.setOwner(null);
            petOwner.setPet(null);

            if (updateOwner == false)
                return;

            PetMsg petMsg = new PetMsg(5, null);
            Dispatch dispatch = Dispatch.borrow(petOwner, petMsg);
            DispatchMessage.dispatchMsgDispatch(dispatch, Enum.DispatchChannel.PRIMARY);
        }
    }

    public static void auditNecroPets(PlayerCharacter player) {
        int removeIndex = 0;
        while (player.necroPets.size() >= 10) {


            if (removeIndex == player.necroPets.size())
                break;

            Mob necroPet = player.necroPets.get(removeIndex);

            if (necroPet == null) {
                removeIndex++;
                continue;
            }
            dismissNecroPet(necroPet, true);
            player.necroPets.remove(necroPet);
            removeIndex++;


        }
    }

    public static void resetNecroPets(PlayerCharacter player) {

        for (Mob necroPet : player.necroPets)
            if (necroPet.isPet())
                necroPet.agentType = Enum.AIAgentType.MOBILE;
    }

    public static void spawnNecroPet(PlayerCharacter playerCharacter, Mob mob) {

        if (mob == null)
            return;

        if (mob.getMobBaseID() != 12021 && mob.getMobBaseID() != 12022)
            return;

        auditNecroPets(playerCharacter);
        resetNecroPets(playerCharacter);

        playerCharacter.necroPets.add(mob);
    }

    public static void dismissNecroPets(PlayerCharacter playerCharacter) {


        if (playerCharacter.necroPets.isEmpty())
            return;

        for (Mob necroPet : playerCharacter.necroPets) {

            try {
                dismissNecroPet(necroPet, true);
            } catch (Exception e) {
                Logger.error(e);
            }
        }
        playerCharacter.necroPets.clear();
    }


    public static void removeSiegeMinions(Mob mobile) {

        for (Mob toRemove : mobile.siegeMinionMap.keySet()) {

            if (mobile.isMoving()) {

                mobile.stopMovement(mobile.getLoc());

                if (toRemove.parentZone != null)
                    toRemove.parentZone.zoneMobSet.remove(toRemove);
            }

            try {
                toRemove.clearEffects();
            } catch (Exception e) {
                Logger.error(e.getMessage());
            }

            if (toRemove.parentZone != null)
                toRemove.parentZone.zoneMobSet.remove(toRemove);

            WorldGrid.RemoveWorldObject(toRemove);
            WorldGrid.removeObject(toRemove);
            DbManager.removeFromCache(toRemove);

            PlayerCharacter petOwner = toRemove.getOwner();

            if (petOwner != null) {

                petOwner.setPet(null);
                toRemove.setOwner(null);

                PetMsg petMsg = new PetMsg(5, null);
                Dispatch dispatch = Dispatch.borrow(petOwner, petMsg);
                DispatchMessage.dispatchMsgDispatch(dispatch, Enum.DispatchChannel.PRIMARY);
            }
        }
    }

    public static boolean removeMobileFromBuilding(Mob mobile, Building building) {

        // Remove npc from it's building

        try {
            mobile.clearEffects();
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }

        if (mobile.parentZone != null)
            mobile.parentZone.zoneMobSet.remove(mobile);

        if (building != null) {
            building.getHirelings().remove(mobile);
            removeSiegeMinions(mobile);
        }

        // Delete npc from database

        if (DbManager.MobQueries.DELETE_MOB(mobile) == 0)
            return false;

        // Remove npc from the simulation

        mobile.removeFromCache();
        DbManager.removeFromCache(mobile);
        WorldGrid.RemoveWorldObject(mobile);
        WorldGrid.removeObject(mobile);
        return true;
    }

    public static void loadAllPirateNames() {

        DbManager.NPCQueries.LOAD_PIRATE_NAMES();
    }

    public static String getPirateName(int mobBaseID) {

        ArrayList<String> nameList = null;

        // If we cannot find name for this mobbase then
        // fallback to human male

        if (NPC._pirateNames.containsKey(mobBaseID))
            nameList = NPC._pirateNames.get(mobBaseID);
        else
            nameList = NPC._pirateNames.get(2111);

        if (nameList == null) {
            Logger.error("Null name list for 2111!");
        }

        return nameList.get(ThreadLocalRandom.current().nextInt(nameList.size()));

    }

    public static ArrayList<Building> getProtectedBuildings(NPC npc) {

        ArrayList<Building> protectedBuildings = new ArrayList<>();

        if (npc.building == null)
            return protectedBuildings;

        if (npc.building.getCity() == null)
            return protectedBuildings;

        for (Building b : npc.building.getCity().getParent().zoneBuildingSet) {

            if (b.getBlueprint() == null)
                continue;

            if (b.getProtectionState().equals(Enum.ProtectionState.CONTRACT))
                protectedBuildings.add(b);

            if (b.getProtectionState().equals(Enum.ProtectionState.PENDING))
                protectedBuildings.add(b);
        }

        return protectedBuildings;
    }

    public static int slotCharacterInBuilding(AbstractCharacter abstractCharacter) {

        int buildingSlot;

        if (abstractCharacter.building == null)
            return -1;

        // Get next available slot for this NPC and use it
        // to add the NPC to the building's hireling list
        // Account for R8's having slots reversed.

        if (abstractCharacter.building.getBlueprint() != null && abstractCharacter.building.getBlueprint().getBuildingGroup().equals(Enum.BuildingGroup.TOL) && abstractCharacter.building.getRank() == 8)
            buildingSlot = BuildingManager.getLastAvailableSlot(abstractCharacter.building);
        else
            buildingSlot = BuildingManager.getAvailableSlot(abstractCharacter.building);

        //if (buildingSlot == -1)
            //Logger.error("No available slot for NPC: " + abstractCharacter.getObjectUUID());

        abstractCharacter.building.getHirelings().put(abstractCharacter, buildingSlot);

        // Override bind and location for  this npc derived
        // from BuildingManager slot location data.

        Vector3fImmutable slotLocation = BuildingManager.getSlotLocation(abstractCharacter.building, buildingSlot).getLocation();

        abstractCharacter.bindLoc = abstractCharacter.building.getLoc().add(slotLocation);

        // Rotate slot position by the building rotation

        if (abstractCharacter != null && abstractCharacter.building != null && abstractCharacter.bindLoc != null
                && abstractCharacter.building.getLoc() != null && abstractCharacter.building.getBounds() != null
                && abstractCharacter.building.getBounds().getQuaternion() != null) {
            abstractCharacter.bindLoc = Vector3fImmutable.rotateAroundPoint(
                    abstractCharacter.building.getLoc(),
                    abstractCharacter.bindLoc,
                    abstractCharacter.building.getBounds().getQuaternion().angleY
            );
        } else {
            Logger.error("Null value detected in abstractCharacter or its properties. Skipping rotation logic.");
            // Handle the case where one or more objects are null, if needed
        }

        abstractCharacter.loc = new Vector3fImmutable(abstractCharacter.bindLoc);

        // Rotate NPC rotation by the building's rotation

        Quaternion slotRotation = new Quaternion().fromAngles(0, acos(abstractCharacter.getRot().y) * 2, 0);
        slotRotation = slotRotation.mult(abstractCharacter.building.getBounds().getQuaternion());
        abstractCharacter.setRot(new Vector3f(0, slotRotation.y, 0));

        // Configure region and floor/level for this NPC

        abstractCharacter.region = BuildingManager.GetRegion(abstractCharacter.building, abstractCharacter.bindLoc.x, abstractCharacter.bindLoc.y, abstractCharacter.bindLoc.z);

        return buildingSlot;
    }
}
