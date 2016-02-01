package circuit.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Macro {

    private List<GlobalBlock> blocks = new ArrayList<GlobalBlock>();
    private int blockSpace;
    private int height;

    public Macro(List<GlobalBlock> blocks) {
        /**
         * The blocks must be in the order of the carry chain ie. the
         * carry input of the first block is open, and the carry
         * output of the last block is open.
         */

        this.blocks.addAll(blocks);

        // Sort the blocks based on their y coordinate (increasing order)
        this.blockSpace = blocks.get(0).getType().getCarryOffsetY();
        if(this.blockSpace < 0) {
            Collections.reverse(this.blocks);
            this.blockSpace = -this.blockSpace;
        }

        // Set the offset on all the blocks
        int offset = 0;
        for(GlobalBlock block : this.blocks) {
            block.setMacro(this, offset);
            offset += this.blockSpace;
        }

        this.height = offset;
    }

    public int getMinRow() {
        return this.blocks.get(0).getRow();
    }
    public int getMaxRow() {
        return this.getMinRow() + this.height;
    }

    public int getBlockSpace() {
        return this.blockSpace;
    }
    public int getHeight() {
        return this.height;
    }


    public int getNumBlocks() {
        return this.blocks.size();
    }
    public List<GlobalBlock> getBlocks() {
        return this.blocks;
    }
    public GlobalBlock getBlock(int index) {
        return this.blocks.get(index);
    }
}
