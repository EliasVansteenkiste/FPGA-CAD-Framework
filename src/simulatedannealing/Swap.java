package simulatedannealing;

import java.util.ArrayList;
import java.util.List;

import circuit.Circuit;
import circuit.block.GlobalBlock;
import circuit.block.Macro;
import circuit.block.Site;
import circuit.exceptions.PlacementException;

public class Swap {

    private boolean applied = false;
    private List<Site> sites1 = new ArrayList<>(), sites2 = new ArrayList<>();

    public Swap(Circuit circuit, GlobalBlock block, Site site) {
        if(block.isInMacro()) {
            // block has to be the FIRST block in the macro
            Macro macro = block.getMacro();

            int column1 = block.getColumn();
            int column2 = site.getColumn();

            int minRow1 = block.getRow();
            int minRow2 = site.getRow();

            int macroHeight = macro.getHeight();
            int blockSpace = macro.getBlockSpace();
            for(int offset = 0; offset < macroHeight; offset += blockSpace) {
                this.sites1.add((Site) circuit.getSite(column1, minRow1 + offset));
                this.sites2.add((Site) circuit.getSite(column2, minRow2 + offset));
            }

        } else {
            this.sites1.add((Site) block.getSite());
            this.sites2.add(site);
        }
    }

    public int getNumBlocks() {
        return this.sites1.size();
    }

    public GlobalBlock getBlock1(int index) {
        return this.sites1.get(index).getBlock();
    }
    public GlobalBlock getBlock2(int index) {
        return this.sites2.get(index).getBlock();
    }

    public Site getSite1(int index) {
        return this.sites1.get(index);
    }
    public Site getSite2(int index) {
        return this.sites2.get(index);
    }


    public void apply() throws PlacementException {
        if(!this.applied) {
            this.swap();
            this.applied = true;
        }
    }

    public void undoApply() throws PlacementException {
        if(this.applied) {
            this.swap();
            this.applied = false;
        }
    }


    private void swap() throws PlacementException {
        int numBlocks = this.getNumBlocks();
        for(int i = 0; i < numBlocks; i++) {
            Site site1 = this.sites1.get(i);
            GlobalBlock block1 = site1.getBlock();

            Site site2 = this.sites2.get(i);
            GlobalBlock block2 = site2.getBlock();


            if(block1 != null) {
                block1.removeSite();
            }

            if(block2 != null) {
                block2.removeSite();
                block2.setSite(site1);
            }

            if(block1 != null) {
                block1.setSite(site2);
            }
        }
    }
}
