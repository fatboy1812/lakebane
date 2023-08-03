package engine.loot;

import java.util.ArrayList;

public class GenTable {
    public ArrayList<GenTableRow> rows = new ArrayList<GenTableRow>();

    public GenTableRow getRowForRange(int roll) {

        GenTableRow outRow = null;

        for (GenTableRow iteration : this.rows)
            if (roll >= iteration.minRoll && roll <= iteration.maxRoll)
                outRow = iteration;

        return outRow;
    }
}
