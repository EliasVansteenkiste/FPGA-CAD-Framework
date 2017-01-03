package place.placers.random;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.circuit.block.AbstractBlock;
import place.circuit.block.AbstractSite;
import place.circuit.block.GlobalBlock;
import place.circuit.block.Macro;
import place.circuit.exceptions.FullSiteException;
import place.circuit.exceptions.PlacedBlockException;
import place.circuit.exceptions.PlacementException;
import place.interfaces.Logger;
import place.interfaces.Options;
import place.placers.Placer;
import place.visual.PlacementVisualizer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RandomPlacer extends Placer {

    @SuppressWarnings("unused")
    public static void initOptions(Options options) {
    }

    public RandomPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random ,logger, visualizer);
    }

    @Override
    public String getName() {
        return "Random placer";
    }

    @Override
    public void initializeData() {
        // Do nothing
    }

    @Override
    protected void addStatTitles(List<String> titles) {
        // Do nothing
    }

    @Override
    protected void doPlacement() throws PlacementException {

        Map<BlockType, List<AbstractSite>> sites = new HashMap<>();
        Map<BlockType, Integer> nextSiteIndexes = new HashMap<>();

        for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
            List<AbstractSite> typeSites = this.circuit.getSites(blockType);
            Collections.shuffle(typeSites, this.random);

            sites.put(blockType, typeSites);
            nextSiteIndexes.put(blockType, 0);
        }


        for(Macro macro : this.circuit.getMacros()) {
            BlockType blockType = macro.getBlock(0).getType();
            List<AbstractSite> typeSites = sites.get(blockType);
            int nextSiteIndex = nextSiteIndexes.get(blockType);

            nextSiteIndex += this.placeMacro(macro, blockType, typeSites, nextSiteIndex);
            nextSiteIndexes.put(blockType, nextSiteIndex);
        }
        
        for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
            List<AbstractBlock> typeBlocks = this.circuit.getBlocks(blockType);
            List<AbstractSite> typeSites = sites.get(blockType);
            int nextSiteIndex = nextSiteIndexes.get(blockType);
            
            nextSiteIndex += this.placeBlocks(typeBlocks, typeSites, nextSiteIndex);
            nextSiteIndexes.put(blockType, nextSiteIndex);
        }

        this.visualizer.addPlacement("Random placement");
    }

    private int placeMacro(Macro macro, BlockType blockType, List<AbstractSite> sites, int siteIndex) throws PlacedBlockException, FullSiteException {

        int blockSpace = macro.getBlockSpace();
        int numBlocks = macro.getNumBlocks();
        int numSites = sites.size();

        while(true) {
            AbstractSite firstSite = sites.get(siteIndex);
            int column = firstSite.getColumn();
            int firstRow = firstSite.getRow();
            int lastRow = firstRow + numBlocks * blockSpace;


            // Check if all the sites are legal and free
            boolean free = true;
            for(int row = firstRow; row <= lastRow; row += blockSpace) {
                if(illegalSite(blockType, column, row)) {
                    free = false;
                    break;
                }
            }

            // If this placement is possible: place all the blocks
            // in the macro and return
            if(free) {
                for(int index = 0; index < numBlocks; index++) {
                    int row = firstRow + blockSpace * index;
                    AbstractSite site = this.circuit.getSite(column, row);
                    macro.getBlock(index).setSite(site);
                }

                return 1;

            // Else, swap this block with another block in the list,
            // so that it can be used later on
            } else {
                int randomIndex = this.random.nextInt(numSites - siteIndex - 1) + siteIndex + 1;
                AbstractSite tmp = sites.get(siteIndex);
                sites.set(siteIndex, sites.get(randomIndex));
                sites.set(randomIndex, tmp);
            }
        }
    }

    private boolean illegalSite(BlockType blockType, int column, int row) {
        if(row >= this.circuit.getHeight() + 2) {
            return true;
        }
        if(column >= this.circuit.getWidth() + 2) {
            return true;
        }

        AbstractSite site = this.circuit.getSite(column, row);
        if(site == null || !site.getType().equals(blockType) || site.isFull()) {
            return true;
        }

        return false;
    }

    private int placeBlocks(List<AbstractBlock> blocks, List<AbstractSite> sites, int nextSiteIndex) throws PlacedBlockException, FullSiteException {

        int placedBlocks = 0;
        int siteIndex = nextSiteIndex;
        for(AbstractBlock abstractBlock : blocks) {
            GlobalBlock block = (GlobalBlock) abstractBlock;

            // Skip blocks in macro's: those have already been placed
            if(!block.isInMacro()) {

                // Find a site that is empty
                AbstractSite site;
                do {
                    site = sites.get(siteIndex);
                    siteIndex++;
                } while(site.isFull());

                placedBlocks += 1;
                block.setSite(site);
            }
        }

        return placedBlocks;
    }
}
