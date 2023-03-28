package engine.gameManager;

import engine.objects.EquipmentSetEntry;

import java.util.ArrayList;
import java.util.HashMap;

public enum NPCManager {
    NPC_MANAGER;
    public static HashMap<Integer, ArrayList<EquipmentSetEntry>> _equipmentSetMap = new HashMap<>();
    public static HashMap<Integer, ArrayList<Integer>> _runeSetMap = new HashMap<>();


    public static void LoadAllEquipmentSets() {
        _equipmentSetMap = DbManager.ItemBaseQueries.LOAD_EQUIPMENT_FOR_NPC_AND_MOBS();
    }

    public static void LoadAllRuneSets() {
        _runeSetMap = DbManager.ItemBaseQueries.LOAD_RUNES_FOR_NPC_AND_MOBS();
    }
}
