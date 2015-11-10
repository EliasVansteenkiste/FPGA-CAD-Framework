package circuit.block;

import java.util.Collection;
import java.util.Random;

import circuit.architecture.BlockType;
import circuit.exceptions.FullSiteException;
import circuit.exceptions.InvalidBlockException;



public abstract class AbstractSite {

    private int x, y;
    private BlockType blockType;

    public AbstractSite(int x, int y, BlockType blockType) {
        this.x = x;
        this.y = y;
        this.blockType = blockType;
    }

    public int getX() {
        return this.x;
    }
    public int getY() {
        return this.y;
    }
    public BlockType getType() {
        return this.blockType;
    }


    public abstract GlobalBlock getRandomBlock(Random random);
    abstract void addBlock(GlobalBlock block) throws FullSiteException;
    public abstract void removeBlock(GlobalBlock block) throws InvalidBlockException;
    public abstract void clear();

    public abstract Collection<GlobalBlock> getBlocks();


    @Override
    public int hashCode() {
        // 10007 is a prime number that is larger than any y
        return 10007 * this.x + this.y;
    }

    @Override
    public String toString() {
        return "[" + this.x + ", " + this.y + "]";
    }
}
