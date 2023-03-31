package engine.gameManager;

import engine.Enum;
import engine.InterestManagement.WorldGrid;
import engine.ai.MobileFSM;
import engine.math.Vector3fImmutable;
import engine.net.Dispatch;
import engine.net.DispatchMessage;
import engine.net.client.msg.PetMsg;
import engine.objects.*;
import engine.powers.EffectsBase;
import engine.server.MBServerStatics;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashMap;

public enum NPCManager {

    NPC_MANAGER;
    public static HashMap<Integer, ArrayList<EquipmentSetEntry>> _equipmentSetMap = new HashMap<>();
    public static HashMap<Integer, ArrayList<Integer>> _runeSetMap = new HashMap<>();
    public static HashMap<Integer, ArrayList<BootySetEntry>> _bootySetMap = new HashMap<>();

    public static void LoadAllEquipmentSets() {
        _equipmentSetMap = DbManager.ItemBaseQueries.LOAD_EQUIPMENT_FOR_NPC_AND_MOBS();
    }

    public static void LoadAllRuneSets() {
        _runeSetMap = DbManager.ItemBaseQueries.LOAD_RUNES_FOR_NPC_AND_MOBS();
    }

    public static void LoadAllBootySets() {
        _bootySetMap = DbManager.ItemBaseQueries.LOAD_BOOTY_FOR_MOBS();
    }

    public static void applyRuneSetEffects(Mob mob) {

        // Early exit

        if (mob.runeSetID == 0)
            return;

        //Apply all rune effects.

        if (NPCManager._runeSetMap.get(mob.runeSetID).contains(252623)) {
            mob.isPlayerGuard = true;
            mob.setNoAggro(true);
        }

        // Only captains have contracts

        if (mob.contract != null || mob.isPlayerGuard)
            applyEffectsForRune(mob, 252621);


        // Apply effects from RuneSet

        if (mob.runeSetID != 0)
            for (int runeID : _runeSetMap.get(mob.runeSetID))
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

        necroPet.state = MobileFSM.STATE.Disabled;

        necroPet.combatTarget = null;
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
                necroPet.setMob();
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
                necroPet.state = MobileFSM.STATE.Disabled;
                Logger.error(e);
            }
        }
        playerCharacter.necroPets.clear();
    }

    public static synchronized Mob createGuardMob(Mob guardCaptain, Guild guild, Zone parent, Vector3fImmutable loc, short level, String pirateName) {

        MobBase minionMobBase;
        Mob mob;
        int maxSlots = 1;

        switch (guardCaptain.getRank()) {
            case 1:
            case 2:
                maxSlots = 1;
                break;
            case 3:
                maxSlots = 2;
                break;
            case 4:
            case 5:
                maxSlots = 3;
                break;
            case 6:
                maxSlots = 4;
                break;
            case 7:
                maxSlots = 5;
                break;
            default:
                maxSlots = 1;

        }

        if (guardCaptain.siegeMinionMap.size() == maxSlots)
            return null;

        minionMobBase = guardCaptain.mobBase;

        if (minionMobBase == null)
            return null;

        mob = new Mob(minionMobBase, guild, parent, level, new Vector3fImmutable(1, 1, 1), 0, true);

        mob.despawned = true;

        mob.setLevel(level);
        //grab equipment and name from minionbase.
        if (guardCaptain.contract != null) {
            Enum.MinionType minionType = Enum.MinionType.ContractToMinionMap.get(guardCaptain.contract.getContractID());
            if (minionType != null) {
                mob.equipmentSetID = minionType.getEquipSetID();
                String rank = "";

                if (guardCaptain.getRank() < 3)
                    rank = MBServerStatics.JUNIOR;
                else if (guardCaptain.getRank() < 6)
                    rank = "";
                else if (guardCaptain.getRank() == 6)
                    rank = MBServerStatics.VETERAN;
                else
                    rank = MBServerStatics.ELITE;

                if (rank.isEmpty())
                    mob.nameOverride = pirateName + " " + minionType.getRace() + " " + minionType.getName();
                else
                    mob.nameOverride = pirateName + " " + minionType.getRace() + " " + rank + " " + minionType.getName();
            }
        }

        if (parent != null)
            mob.setRelPos(parent, loc.x - parent.absX, loc.y - parent.absY, loc.z - parent.absZ);

        mob.setObjectTypeMask(MBServerStatics.MASK_MOB | mob.getTypeMasks());

        // mob.setMob();
        mob.isPlayerGuard = true;
        mob.setParentZone(parent);
        DbManager.addToCache(mob);
        mob.runAfterLoad();


        RuneBase guardRune = RuneBase.getRuneBase(252621);

        for (MobBaseEffects mbe : guardRune.getEffectsList()) {

            EffectsBase eb = PowersManager.getEffectByToken(mbe.getToken());

            if (eb == null) {
                Logger.info("EffectsBase Null for Token " + mbe.getToken());
                continue;
            }

            //check to upgrade effects if needed.
            if (mob.effects.containsKey(Integer.toString(eb.getUUID()))) {
                if (mbe.getReqLvl() > (int) mob.level) {
                    continue;
                }

                Effect eff = mob.effects.get(Integer.toString(eb.getUUID()));

                if (eff == null)
                    continue;

                //Current effect is a higher rank, dont apply.
                if (eff.getTrains() > mbe.getRank())
                    continue;

                //new effect is of a higher rank. remove old effect and apply new one.
                eff.cancelJob();
                mob.addEffectNoTimer(Integer.toString(eb.getUUID()), eb, mbe.getRank(), true);
            } else {

                if (mbe.getReqLvl() > (int) mob.level)
                    continue;

                mob.addEffectNoTimer(Integer.toString(eb.getUUID()), eb, mbe.getRank(), true);
            }
        }

        int slot = 0;
        slot += guardCaptain.siegeMinionMap.size() + 1;

        guardCaptain.siegeMinionMap.put(mob, slot);
        mob.setInBuildingLoc(guardCaptain.building, guardCaptain);
        mob.setBindLoc(loc.add(mob.inBuildingLoc));
        mob.deathTime = System.currentTimeMillis();
        mob.spawnTime = 900;
        mob.npcOwner = guardCaptain;
        mob.state = MobileFSM.STATE.Respawn;

        return mob;
    }

    public static void removeSiegeMinions(Mob mobile) {

        for (Mob toRemove : mobile.siegeMinionMap.keySet()) {

            toRemove.state = MobileFSM.STATE.Disabled;

            if (mobile.isMoving()) {

                mobile.stopMovement(mobile.getLoc());
                mobile.state = MobileFSM.STATE.Disabled;

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
        mobile.state = MobileFSM.STATE.Disabled;

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
}
