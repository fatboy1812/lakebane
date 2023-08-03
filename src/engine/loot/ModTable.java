package engine.loot;

import java.util.ArrayList;

public class ModTable {
    public ArrayList<ModTableRow> rows = new ArrayList<ModTableRow>();

    public ModTableRow getRowForRange(int roll) {

        if (roll > 320)
            roll = 320;

        ModTableRow outRow = null;

        for (ModTableRow iteration : this.rows)
            if (roll >= iteration.minRoll && roll <= iteration.maxRoll)
                outRow = iteration;

        return outRow;
    }
}
