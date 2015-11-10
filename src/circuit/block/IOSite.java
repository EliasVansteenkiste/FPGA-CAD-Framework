package circuit.block;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import circuit.architecture.BlockType;
import circuit.exceptions.FullSiteException;
import circuit.exceptions.InvalidBlockException;




public class IOSite extends AbstractSite {

    private int capacity;
    private Set<GlobalBlock> blocks;

    public IOSite(int x, int y, BlockType blockType, int capacity) {
        super(x, y, blockType);
        this.capacity = capacity;
        this.blocks = new HashSet<GlobalBlock>(capacity);
    }



    @Override
    public GlobalBlock getRandomBlock(Random random) {
        int size = this.blocks.size();
        if(size == 0) {
            return null;
        }

        int index = random.nextInt(size);
        Iterator<GlobalBlock> iter = this.blocks.iterator();
        for(int i = 0; i < index; i++) {
            iter.next();
        }

        return iter.next();
    }

    @Override
    void addBlock(GlobalBlock block) throws FullSiteException {
        if(this.blocks.size() == this.capacity) {
            throw new FullSiteException();
        }

        this.blocks.add(block);
    }

    @Override
    public void removeBlock(GlobalBlock block) throws InvalidBlockException {
        boolean success = this.blocks.remove(block);
        if(!success) {
            throw new InvalidBlockException();
        }
    }

    @Override
    public void clear() {
        this.blocks.clear();
    }


    @Override
    public Collection<GlobalBlock> getBlocks() {
        return this.blocks;
    }
}
