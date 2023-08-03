package engine.loot;

import java.util.ArrayList;

public class ModTypeTable {
    public ArrayList<ModTypeTableRow> rows = new ArrayList<>();

    public ModTypeTableRow getRowForRange(int roll) {

        ModTypeTableRow outRow = null;

        for (ModTypeTableRow iteration : this.rows)
            if (roll >= iteration.minRoll && roll <= iteration.maxRoll)
                return iteration;

        return outRow;
    }
}
