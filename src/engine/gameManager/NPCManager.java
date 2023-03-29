package engine.gameManager;

import engine.objects.*;
import engine.powers.EffectsBase;
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
            return;;

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
            return;;

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
}
