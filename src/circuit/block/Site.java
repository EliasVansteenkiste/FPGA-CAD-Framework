package circuit.block;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import circuit.architecture.BlockType;


import util.Logger;

public class Site extends AbstractSite {

    private GlobalBlock block;

    public Site(int x, int y, BlockType blockType) {
        super(x, y, blockType);
    }


    public GlobalBlock getBlock() {
        return this.block;
    }

    @Override
    public GlobalBlock getRandomBlock(Random random) {
        return this.block;
    }

    @Override
    void addBlock(GlobalBlock block) {
        if(this.block == null) {
            this.block = block;
        } else {
            Logger.raise("Trying to set the block on a non-empty site");
        }
    }

    @Override
    public void removeBlock(GlobalBlock block) {
        if(block == this.block) {
            this.block = null;
        } else {
            Logger.raise("Trying to remove a block that is not present in site");
        }
    }

    @Override
    public void clear() {
        this.block = null;
    }

    @Override
    public Collection<GlobalBlock> getBlocks() {
        return Arrays.asList(this.block);
    }
}
