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

    public static void initializeStaticEffects(Mob mob) {

        EffectsBase eb = null;
        for (MobBaseEffects mbe : mob.mobBase.getRaceEffectsList()) {

            eb = PowersManager.getEffectByToken(mbe.getToken());

            if (eb == null) {
                Logger.info("EffectsBase Null for Token " + mbe.getToken());
                continue;
            }

            //check to upgrade effects if needed.
            if (mob.effects.containsKey(Integer.toString(eb.getUUID()))) {
                if (mbe.getReqLvl() > (int) mob.level)
                    continue;

                Effect eff = mob.effects.get(Integer.toString(eb.getUUID()));

                if (eff == null)
                    continue;

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

        //Apply all rune effects.
        // Only Captains have contracts
        if (mob.contract != null || mob.isPlayerGuard) {
            RuneBase guardRune = RuneBase.getRuneBase(252621);
            for (MobBaseEffects mbe : guardRune.getEffectsList()) {

                eb = PowersManager.getEffectByToken(mbe.getToken());

                if (eb == null) {
                    Logger.info("Mob: " + mob.getObjectUUID() + "  EffectsBase Null for Token " + mbe.getToken());
                    continue;
                }

                //check to upgrade effects if needed.
                if (mob.effects.containsKey(Integer.toString(eb.getUUID()))) {

                    if (mbe.getReqLvl() > (int) mob.level)
                        continue;

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

            RuneBase WarriorRune = RuneBase.getRuneBase(2518);
            for (MobBaseEffects mbe : WarriorRune.getEffectsList()) {

                eb = PowersManager.getEffectByToken(mbe.getToken());

                if (eb == null) {
                    Logger.info("EffectsBase Null for Token " + mbe.getToken());
                    continue;
                }

                //check to upgrade effects if needed.
                if (mob.effects.containsKey(Integer.toString(eb.getUUID()))) {

                    if (mbe.getReqLvl() > (int) mob.level)
                        continue;

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
        }

        // Apply effects from RuneSet

        if (mob.runeSetID != 0)
        for (int runeID : _runeSetMap.get(mob.runeSetID)) {

            RuneBase rune = RuneBase.getRuneBase(runeID);

            if (rune != null)
                for (MobBaseEffects mbe : rune.getEffectsList()) {

                    eb = PowersManager.getEffectByToken(mbe.getToken());
                    if (eb == null) {
                        Logger.info("EffectsBase Null for Token " + mbe.getToken());
                        continue;
                    }

                    //check to upgrade effects if needed.
                    if (mob.effects.containsKey(Integer.toString(eb.getUUID()))) {
                        if (mbe.getReqLvl() > (int) mob.level)
                            continue;

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
            }
        }
}
