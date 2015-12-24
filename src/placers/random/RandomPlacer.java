package placers.random;

import interfaces.Logger;
import interfaces.Options;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import circuit.Circuit;
import circuit.architecture.BlockType;
import circuit.block.AbstractBlock;
import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;
import circuit.block.Macro;
import circuit.exceptions.FullSiteException;
import circuit.exceptions.PlacedBlockException;
import circuit.exceptions.PlacementException;

import placers.Placer;
import visual.PlacementVisualizer;

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
    public void place() throws PlacementException {

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


            boolean free = true;

            // Check if all the sites are legal and free
            for(int row = lastRow; row > firstRow; row -= blockSpace) {
                AbstractSite site = this.circuit.getSite(column, row);
                if(site == null || !blockType.equals(site.getType()) || site.isFull()) {
                    free  = false;
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

                return numBlocks;

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

    private int placeBlocks(List<AbstractBlock> blocks, List<AbstractSite> sites, int nextSiteIndex) throws PlacedBlockException, FullSiteException {

        int siteIndex = nextSiteIndex;
        for(AbstractBlock abstractBlock : blocks) {
            GlobalBlock block = (GlobalBlock) abstractBlock;

            // Skip blocks in macro's: those have already been placed
            if(!block.isInMacro()) {

                // Find a site that is empy
                AbstractSite site;
                do {
                    site = sites.get(siteIndex);
                    siteIndex++;
                } while(site.isFull());

                block.setSite(site);
            }
        }

        return blocks.size();
    }
}
