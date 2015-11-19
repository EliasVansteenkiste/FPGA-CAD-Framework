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
import circuit.block.GlobalBlock;
import circuit.exceptions.PlacementException;
import circuit.pin.AbstractPin;

import placers.Placer;
import placers.analyticalplacer.linear_solver.LinearSolver;
import visual.PlacementVisualizer;

public abstract class AnalyticalAndGradientPlacer extends Placer {

    protected final Map<GlobalBlock, Integer> blockIndexes = new HashMap<>();
    protected final List<int[]> nets = new ArrayList<>();
    protected int numBlocks, numIOBlocks;

    protected double[] linearX, linearY;
    protected Legalizer legalizer;




    public AnalyticalAndGradientPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);
    }


    protected abstract Legalizer createLegalizer(List<BlockType> blockTypes, List<Integer> blockTypeIndexStarts);

    protected abstract void solveLinear(int iteration);
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
        BlockType ioBlockType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        List<BlockType> blockTypes = new ArrayList<>();
        blockTypes.add(ioBlockType);
        for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
            if(!blockType.equals(ioBlockType)) {
                blockTypes.add(blockType);
            }
        }



        // Add all blocks
        this.linearX = new double[this.numBlocks];
        this.linearY = new double[this.numBlocks];

        List<Integer> blockTypeIndexStarts = new ArrayList<>();
        int blockIndex = 0;
        blockTypeIndexStarts.add(0);

        for(BlockType blockType : blockTypes) {
            for(AbstractBlock abstractBlock : this.circuit.getBlocks(blockType)) {
                GlobalBlock block = (GlobalBlock) abstractBlock;

                this.linearX[blockIndex] = block.getX();
                this.linearY[blockIndex] = block.getY();

                this.blockIndexes.put(block, blockIndex);
                blockIndex++;
            }

            blockTypeIndexStarts.add(blockIndex);
        }

        this.numIOBlocks = blockTypeIndexStarts.get(1);



        // Add all nets
        // Loop through all net sources
        for(GlobalBlock sourceBlock : this.circuit.getGlobalBlocks()) {
            for(AbstractPin sourcePin : sourceBlock.getOutputPins()) {

                // Build a set of all blocks connected to the net
                Set<GlobalBlock> netBlocks = new HashSet<>();
                netBlocks.add((GlobalBlock) sourcePin.getOwner());

                for(AbstractPin pin : sourcePin.getSinks()) {
                    netBlocks.add((GlobalBlock) pin.getOwner());
                }

                // Make a list of the block indexes
                int numNetBlocks = netBlocks.size();

                // Ignore nets of size 1
                // Due to this, the WLD costcalculator (which uses these nets
                // for speedup) is not entirely accurate, but that doesn't
                // matter, because we use the same (inaccurate) costcalculator
                // to calculate both the linear and legal cost, so the deviation
                // cancels out.
                if(numNetBlocks > 1) {
                    int[] netBlockIndexes = new int[numNetBlocks];
                    int i = 0;
                    for(GlobalBlock netBlock : netBlocks) {
                        netBlockIndexes[i] = this.blockIndexes.get(netBlock);
                        i++;
                    }

                    // Add this "net" to the list of nets
                    this.nets.add(netBlockIndexes);
                }
            }
        }

        this.legalizer = createLegalizer(blockTypes, blockTypeIndexStarts);
    }


    @Override
    public void place() {

        int iteration = 0;
        boolean isLastIteration = false;

        // This is fixed, because making it dynamic doesn't improve results
        // But HeapLegalizer still supports other values for maxUtilization
        double maxUtilization = 1;

        this.printStatisticsHeader();

        while(!isLastIteration) {
            double timerBegin = System.nanoTime();

            // Solve linear
            this.solveLinear(iteration);

            try {
                this.legalizer.legalize(maxUtilization);
            } catch(PlacementException error) {
                this.logger.raise(error);
            }

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
                    this.blockIndexes, this.legalizer.getAnchorsX(), this.legalizer.getAnchorsY());

            iteration++;
        }

        this.logger.println();


        try {
            this.legalizer.updateCircuit();
        } catch(PlacementException error) {
            this.logger.raise(error);
        }
    }


    /*
     * Build and solve the linear system ==> recalculates linearX and linearY
     * If it is the first time we solve the linear system ==> don't take pseudonets into account
     */
    protected void solveLinearIteration(LinearSolver solver, boolean addPseudoNets) {

        // Add connections between blocks that are connected by a net
        this.processNets(solver);

        // Add pseudo connections
        if(addPseudoNets) {
            solver.addPseudoConnections(
                    this.legalizer.getAnchorsX(),
                    this.legalizer.getAnchorsY());
        }


        // Solve and save result
        solver.solve();
    }


    private void processNets(LinearSolver solver) {
        for(int[] net : this.nets) {
            solver.processNet(net);
            // TODO: add a solver.processNetTimingDriven(net), or something like that
        }
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