package circuit.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Macro {

    private List<GlobalBlock> blocks = new ArrayList<GlobalBlock>();
    private int height;

    public Macro(List<GlobalBlock> blocks) {
        /**
         * The blocks must be in the order of the carry chain ie. the
         * carry input of the first block is open, and the carry
         * output of the last block is open.
         */

        this.blocks.addAll(blocks);

        // Sort the blocks based on their y coordinate (increasing order)
        int offsetIncrease = blocks.get(0).getType().getCarryOffsetY();
        if(offsetIncrease < 0) {
            Collections.reverse(this.blocks);
            offsetIncrease = -offsetIncrease;
        }

        // Set the offset on all the blocks
        int offset = 0;
        for(GlobalBlock block : this.blocks) {
            block.setMacro(this, offset);
            offset += offsetIncrease;
        }

        this.height = offset;
    }

    public int getMinRow() {
        return this.blocks.get(0).getRow();
    }
    public int getHeight() {
        return this.height;
    }
    public int getMaxRow() {
        return this.getMinRow() + this.height;
    }
}
