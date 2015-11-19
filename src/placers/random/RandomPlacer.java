package placers.random;

import interfaces.Logger;
import interfaces.Options;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import circuit.Circuit;
import circuit.architecture.BlockType;
import circuit.architecture.BlockCategory;
import circuit.block.AbstractBlock;
import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;
import circuit.exceptions.FullSiteException;
import circuit.exceptions.PlacedBlockException;

import placers.Placer;
import visual.PlacementVisualizer;

public class RandomPlacer extends Placer {

    public static void initOptions(Options options) {
        options.add("categories", "comma-separated list of block categories that must be placed", "");
    }

    private final Set<BlockCategory> categories = new HashSet<>();

    public RandomPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random ,logger, visualizer);

        String categoriesString = this.options.getString("categories");
        Set<String> categoriesStrings = new HashSet<>(Arrays.asList(categoriesString.split(",")));

        for(BlockCategory category : BlockCategory.values()) {
            if(categoriesString.length() == 0 || categoriesStrings.contains(category.toString())) {
                this.categories.add(category);
            }
        }
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
    public void place() {
        List<BlockType> blockTypes = this.circuit.getGlobalBlockTypes();
        for(BlockType blockType : blockTypes) {

            if(this.categories.size() > 0 && !this.categories.contains(blockType.getCategory())) {
                continue;
            }

            // Get all possible blocks and sites for this type
            List<AbstractBlock> blocks = this.circuit.getBlocks(blockType);
            List<AbstractSite> sites = this.circuit.getSites(blockType);

            // Permutate sites
            Collections.shuffle(sites, this.random);

            // Assign each block to a site
            int siteIndex = 0;
            for(AbstractBlock abstractBlock : blocks) {
                AbstractSite site = sites.get(siteIndex);
                GlobalBlock block = (GlobalBlock) abstractBlock;

                try {
                    block.setSite(site);
                } catch(PlacedBlockException | FullSiteException error) {
                    this.logger.raise(error);
                }

                siteIndex++;
            }
        }
    }


}
