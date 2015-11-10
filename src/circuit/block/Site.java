package circuit.block;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import circuit.architecture.BlockType;
import circuit.exceptions.FullSiteException;
import circuit.exceptions.InvalidBlockException;

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
    void addBlock(GlobalBlock block) throws FullSiteException {
        if(this.block != null) {
            throw new FullSiteException();
        }

        this.block = block;
    }

    @Override
    public void removeBlock(GlobalBlock block) throws InvalidBlockException {
        if(block != this.block) {
            throw new InvalidBlockException();
        }

        this.block = null;
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
