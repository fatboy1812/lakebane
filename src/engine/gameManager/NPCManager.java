package engine.gameManager;

import engine.objects.EquipmentSetEntry;

import java.util.ArrayList;
import java.util.HashMap;

public enum NPCManager {
    NPC_MANAGER;
    public static HashMap<Integer, ArrayList<EquipmentSetEntry>> EquipmentSetMap = new HashMap<>();


    public static void LoadAllEquipmentSets() {
        EquipmentSetMap = DbManager.ItemBaseQueries.LOAD_EQUIPMENT_FOR_NPC_AND_MOBS();
    }
}
