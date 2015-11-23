package placers.SAPlacer;

import java.util.Random;

import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;
import circuit.exceptions.PlacementException;



public class Swap {
    private GlobalBlock block1;
    private GlobalBlock block2;
    private AbstractSite site1;
    private AbstractSite site2;

    public Swap(GlobalBlock block, AbstractSite site, Random random) {
        this.block1 = block;
        this.site1 = block.getSite();

        this.block2 = site.getRandomBlock(random);
        this.site2 = site;
    }


    public GlobalBlock getBlock1() {
        return this.block1;
    }
    public GlobalBlock getBlock2() {
        return this.block2;
    }

    public AbstractSite getSite1() {
        return this.site1;
    }
    public AbstractSite getSite2() {
        return this.site2;
    }


    public void apply() throws PlacementException {
        this.block1.removeSite();

        if(this.block2 != null) {
            this.block2.removeSite();
            this.block2.setSite(this.site1);
        }

        this.block1.setSite(this.site2);
    }


    @Override
    public String toString() {
        return "block " + this.block1 + " on site " + this.site1
                + ", block" + this.block2 + " on site " + this.site2;
    }
}
