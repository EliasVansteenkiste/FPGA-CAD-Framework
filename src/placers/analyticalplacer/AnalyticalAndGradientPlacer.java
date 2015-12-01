package placers.analyticalplacer;

import interfaces.Logger;
import interfaces.Options;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import circuit.Circuit;
import circuit.architecture.BlockCategory;
import circuit.architecture.BlockType;
import circuit.block.AbstractBlock;
import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;
import circuit.block.LeafBlock;
import circuit.block.TimingEdge;
import circuit.exceptions.PlacementException;

import placers.Placer;
import visual.PlacementVisualizer;

public abstract class AnalyticalAndGradientPlacer extends Placer {

    protected List<BlockType> blockTypes;
    protected List<Integer> blockTypeIndexStarts;
    protected final Map<GlobalBlock, Integer> blockIndexes = new HashMap<>();

    protected int numBlocks, numIOBlocks;

    protected double[] linearX, linearY;
    private int[] legalX, legalY;

    protected List<int[]> netBlockIndexes, netUniqueBlockIndexes;
    protected List<TimingEdge[]> netTimingEdges;


    public AnalyticalAndGradientPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);
    }


    protected abstract boolean isTimingDriven();

    protected abstract void solveLinear(int iteration);
    protected abstract void solveLegal(int iteration);
    protected abstract boolean stopCondition();

    protected abstract void printStatisticsHeader();
    protected abstract void printStatistics(int iteration, double time);


    @Override
    public void initializeData() {
        // Get number of blocks
        int numBlocksCounter = 0;
        for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
            numBlocksCounter += this.circuit.getBlocks(blockType).size();
        }
        this.numBlocks = numBlocksCounter;


        // Make a list of all block types, with IO blocks first
        this.blockTypes = new ArrayList<>();

        BlockType ioBlockType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        this.blockTypes.add(ioBlockType);

        for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
            if(!blockType.equals(ioBlockType)) {
                this.blockTypes.add(blockType);
            }
        }



        // Add all blocks
        this.linearX = new double[this.numBlocks];
        this.linearY = new double[this.numBlocks];
        this.legalX = new int[this.numBlocks];
        this.legalY = new int[this.numBlocks];

        this.blockTypeIndexStarts = new ArrayList<>();
        this.blockTypeIndexStarts.add(0);

        { // I want to be able to use the variable blockIndex later on as well
            int blockIndex = 0;
            for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
                for(AbstractBlock abstractBlock : this.circuit.getBlocks(blockType)) {
                    GlobalBlock block = (GlobalBlock) abstractBlock;

                    this.linearX[blockIndex] = block.getX();
                    this.linearY[blockIndex] = block.getY();
                    this.legalX[blockIndex] = block.getX();
                    this.legalY[blockIndex] = block.getY();

                    this.blockIndexes.put(block, blockIndex);
                    blockIndex++;
                }

                this.blockTypeIndexStarts.add(blockIndex);
            }
        }

        this.numIOBlocks = this.blockTypeIndexStarts.get(1);


        this.netBlockIndexes = new ArrayList<int[]>();
        this.netUniqueBlockIndexes = new ArrayList<int[]>();
        this.netTimingEdges = new ArrayList<TimingEdge[]>();


        // Add all nets
        // A net is simply a list of unique block indexes
        // If the algorithm is timing driven, we also store all the blocks in
        // a net (duplicates are allowed) and the corresponding timing edge
        boolean timingDriven = this.isTimingDriven();

        for(GlobalBlock sourceGlobalBlock : this.circuit.getGlobalBlocks()) {
            int sourceBlockIndex = this.blockIndexes.get(sourceGlobalBlock);

            for(LeafBlock sourceLeafBlock : sourceGlobalBlock.getLeafBlocks()) {
                int numSinks = sourceLeafBlock.getNumSinks();

                Set<Integer> blockIndexesSet = new HashSet<>();
                blockIndexesSet.add(sourceBlockIndex);

                int[] blockIndexes = new int[numSinks + 1];
                blockIndexes[0] = sourceBlockIndex;

                TimingEdge[] timingEdges = new TimingEdge[numSinks];

                for(int i = 0; i < numSinks; i++) {
                    GlobalBlock sinkGlobalBlock = sourceLeafBlock.getSink(i).getGlobalParent();
                    int sinkBlockIndex = this.blockIndexes.get(sinkGlobalBlock);

                    blockIndexesSet.add(sinkBlockIndex);
                    blockIndexes[i + 1] = sinkBlockIndex;

                    TimingEdge timingEdge = sourceLeafBlock.getSinkEdge(i);
                    timingEdges[i] = timingEdge;
                }


                /* Don't add nets which connect only one global block.
                 * Due to this, the WLD costcalculator is not entirely
                 * accurate, but that doesn't matter, because we use
                 * the same (inaccurate) costcalculator to calculate
                 * both the linear and legal cost, so the deviation
                 * cancels out.
                 */
                int numUniqueBlocks = blockIndexesSet.size();
                if(numUniqueBlocks > 1) {
                    int[] uniqueBlockIndexes = new int[numUniqueBlocks];
                    int i = 0;
                    for(Integer blockIndex : blockIndexesSet) {
                        uniqueBlockIndexes[i] = blockIndex;
                        i++;
                    }

                    this.netUniqueBlockIndexes.add(uniqueBlockIndexes);

                    // We only need the complete list of blocks and their
                    // timing edges if the algorithm is timing driven
                    if(timingDriven) {
                        this.netTimingEdges.add(timingEdges);
                        this.netBlockIndexes.add(blockIndexes);
                    }
                }
            }
        }
    }


    @Override
    public void place() {

        int iteration = 0;
        boolean isLastIteration = false;

        this.printStatisticsHeader();

        while(!isLastIteration) {
            double timerBegin = System.nanoTime();

            // Solve linear
            this.solveLinear(iteration);
            this.solveLegal(iteration);

            isLastIteration = this.stopCondition();

            double timerEnd = System.nanoTime();
            double time = (timerEnd - timerBegin) * 1e-9;

            this.printStatistics(iteration, time);

            // Update the visualizer
            this.visualizer.addPlacement(
                    String.format("iteration %d: linear", iteration),
                    this.blockIndexes, this.linearX, this.linearY);
            this.visualizer.addPlacement(
                    String.format("iteration %d: legal", iteration),
                    this.blockIndexes, this.legalX, this.legalY);

            iteration++;
        }

        this.logger.println();


        try {
            this.updateCircuit();
        } catch(PlacementException error) {
            this.logger.raise(error);
        }
    }



    protected void updateLegal(int[] newLegalX, int[] newLegalY) {
        int numMovableBlocks = this.numBlocks - this.numIOBlocks;

        System.arraycopy(newLegalX, this.numIOBlocks, this.legalX, this.numIOBlocks, numMovableBlocks);
        System.arraycopy(newLegalY, this.numIOBlocks, this.legalY, this.numIOBlocks, numMovableBlocks);
    }


    protected void updateCircuit() throws PlacementException {
        //Clear all previous locations
        for(GlobalBlock block : this.blockIndexes.keySet()) {
            if(block.getCategory() != BlockCategory.IO) {
                block.removeSite();
            }
        }

        // Update locations
        for(Map.Entry<GlobalBlock, Integer> blockEntry : this.blockIndexes.entrySet()) {
            GlobalBlock block = blockEntry.getKey();

            if(block.getCategory() != BlockCategory.IO) {
                int index = blockEntry.getValue();


                AbstractSite site = this.circuit.getSite(this.legalX[index], this.legalY[index], true);
                block.setSite(site);
            }
        }

        this.circuit.getTimingGraph().recalculateAllSlacksCriticalities(true);
    }


    public static double getWeight(int size) {
        switch (size) {
            case 1:
            case 2:
            case 3:  return 1;
            case 4:  return 1.0828;
            case 5:  return 1.1536;
            case 6:  return 1.2206;
            case 7:  return 1.2823;
            case 8:  return 1.3385;
            case 9:  return 1.3991;
            case 10: return 1.4493;
            case 11:
            case 12:
            case 13:
            case 14:
            case 15: return (size-10) * (1.6899-1.4493) / 5 + 1.4493;
            case 16:
            case 17:
            case 18:
            case 19:
            case 20: return (size-15) * (1.8924-1.6899) / 5 + 1.6899;
            case 21:
            case 22:
            case 23:
            case 24:
            case 25: return (size-20) * (2.0743-1.8924) / 5 + 1.8924;
            case 26:
            case 27:
            case 28:
            case 29:
            case 30: return (size-25) * (2.2334-2.0743) / 5 + 2.0743;
            case 31:
            case 32:
            case 33:
            case 34:
            case 35: return (size-30) * (2.3895-2.2334) / 5 + 2.2334;
            case 36:
            case 37:
            case 38:
            case 39:
            case 40: return (size-35) * (2.5356-2.3895) / 5 + 2.3895;
            case 41:
            case 42:
            case 43:
            case 44:
            case 45: return (size-40) * (2.6625-2.5356) / 5 + 2.5356;
            case 46:
            case 47:
            case 48:
            case 49:
            case 50: return (size-45) * (2.7933-2.6625) / 5 + 2.6625;
            default: return (size-50) * 0.02616 + 2.7933;
        }
    }
}