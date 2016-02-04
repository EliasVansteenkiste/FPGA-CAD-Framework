package placers.analytical;

import interfaces.Logger;
import interfaces.Options;

import java.util.ArrayList;
import java.util.Arrays;
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
import circuit.block.Macro;
import circuit.block.Site;
import circuit.exceptions.PlacementException;
import circuit.timing.TimingEdge;
import circuit.timing.TimingNode;
import circuit.timing.TimingNode.Position;

import placers.Placer;
import visual.PlacementVisualizer;

public abstract class AnalyticalAndGradientPlacer extends Placer {

    protected List<BlockType> blockTypes;
    protected List<Integer> blockTypeIndexStarts;
    protected final Map<GlobalBlock, NetBlock> netBlocks = new HashMap<>();

    protected int numIOBlocks, numMovableBlocks;
    protected double startUtilization;

    protected double[] linearX, linearY;
    protected int[] legalX, legalY;
    protected int[] heights;

    private boolean[] hasNets;
    protected int numNets, numRealNets;
    protected List<Net> nets;
    protected List<TimingNet> timingNets;

    private static final String
        O_START_UTILIZATION = "start utilization";

    public static void initOptions(Options options) {
        options.add(
                O_START_UTILIZATION,
                "utilization of tiles at first legalization",
                new Double(1.0));
    }

    protected final static String
        T_INITIALIZE_DATA = "initialize data",
        T_UPDATE_CIRCUIT = "update circuit",
        T_BUILD_LINEAR = "build linear system",
        T_SOLVE_LINEAR = "solve linear system",
        T_CALCULATE_COST = "calculate cost",
        T_LEGALIZE = "legalize";


    public AnalyticalAndGradientPlacer(Circuit circuit, Options options, Random random, Logger logger, PlacementVisualizer visualizer) {
        super(circuit, options, random, logger, visualizer);

        this.startUtilization = options.getDouble(O_START_UTILIZATION);
    }


    protected abstract boolean isTimingDriven();

    protected abstract void solveLinear(int iteration);
    protected abstract void solveLegal(int iteration);
    protected abstract boolean stopCondition(int iteration);

    protected abstract void printStatistics(int iteration, double time);


    @Override
    public void initializeData() {

        this.startTimer(T_INITIALIZE_DATA);

        // Count the number of blocks
        // A macro counts as 1 block
        int numBlocks = 0;
        for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
            numBlocks += this.circuit.getBlocks(blockType).size();
        }
        for(Macro macro : this.circuit.getMacros()) {
            numBlocks -= macro.getNumBlocks() - 1;
        }


        // Make a list of all block types, with IO blocks first
        this.blockTypes = new ArrayList<>();

        BlockType ioBlockType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        this.blockTypes.add(ioBlockType);

        for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
            if(!blockType.equals(ioBlockType)) {
                this.blockTypes.add(blockType);
            }
        }



        // Add all global blocks, in the order of 'blockTypes'
        this.linearX = new double[numBlocks];
        this.linearY = new double[numBlocks];
        this.legalX = new int[numBlocks];
        this.legalY = new int[numBlocks];
        this.hasNets = new boolean[numBlocks];

        this.heights = new int[numBlocks];
        Arrays.fill(this.heights, 1);

        this.blockTypeIndexStarts = new ArrayList<>();
        this.blockTypeIndexStarts.add(0);
        List<GlobalBlock> macroBlocks = new ArrayList<>();


        int blockCounter = 0;
        for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
            for(AbstractBlock abstractBlock : this.circuit.getBlocks(blockType)) {
                GlobalBlock block = (GlobalBlock) abstractBlock;

                // Blocks that are the first block of a macro (or that aren't
                // in a macro) should get a movable position.
                if(!block.isInMacro() || block.getMacroOffsetY() == 0) {
                    int column = block.getColumn();
                    int row = block.getRow();

                    int height = block.isInMacro() ? block.getMacro().getHeight() : 1;
                    // The offset is measured in half blocks from the center of the macro
                    // For the legal position of macro's with an even number of blocks,
                    // the position of the macro is rounded down
                    float offset = (1 - height) / 2f;

                    this.linearX[blockCounter] = column;
                    this.linearY[blockCounter] = row - offset;
                    this.legalX[blockCounter] = column;
                    this.legalY[blockCounter] = row + (int) Math.floor(-offset);
                    this.heights[blockCounter] = height;

                    this.netBlocks.put(block, new NetBlock(blockCounter, offset));
                    blockCounter++;

                // The position of other blocks will be calculated
                // using the macro source.
                } else {
                    macroBlocks.add(block);
                }
            }

            this.blockTypeIndexStarts.add(blockCounter);
        }

        for(GlobalBlock block : macroBlocks) {
            GlobalBlock macroSource = block.getMacro().getBlock(0);
            int sourceIndex = this.netBlocks.get(macroSource).blockIndex;
            int macroHeight = block.getMacro().getHeight();
            int offset = (1 - macroHeight) / 2 + block.getMacroOffsetY();

            this.netBlocks.put(block, new NetBlock(sourceIndex, offset));
            blockCounter++;
        }

        this.numIOBlocks = this.blockTypeIndexStarts.get(1);



        // Add all nets
        // A net is simply a list of unique block indexes
        // If the algorithm is timing driven, we also store all the blocks in
        // a net (duplicates are allowed) and the corresponding timing edge
        this.nets = new ArrayList<Net>();
        this.timingNets = new ArrayList<TimingNet>();


        /* For each global output pin, build the net that has that pin as
         * its source. We build the following data structures:
         *   - uniqueBlockIndexes: a list of the global blocks in the net
         *     in no particular order. Duplicates are removed.
         *   - blockIndexes: a list of the blocks in the net. Duplicates
         *     are allowed if a block is connected multiple times to the
         *     same net. blockIndexes[0] is the net source.
         *   - timingEdges: the timing edges that correspond to the blocks
         *     in blockIndexes. The edge at timingEdges[i] corresponds to
         *     the block at blockIndexes[i + 1].
         */

        // Loop through all leaf blocks
        for(GlobalBlock sourceGlobalBlock : this.circuit.getGlobalBlocks()) {
            NetBlock sourceBlock = this.netBlocks.get(sourceGlobalBlock);

            for(TimingNode timingNode : sourceGlobalBlock.getTimingNodes()) {
                if(timingNode.getPosition() != Position.LEAF) {
                    this.addNet(sourceBlock, timingNode);
                }
            }
        }

        this.numRealNets = this.nets.size();

        for(NetBlock block : this.netBlocks.values()) {
            if(!this.hasNets[block.blockIndex]) {
                this.addDummyNet(block);
            }
        }

        this.numNets = this.nets.size();

        this.stopTimer(T_INITIALIZE_DATA);
    }

    private void addDummyNet(NetBlock sourceBlock) {
        // These dummy nets are needed for the analytical
        // placer. If they are not added, diagonal elements
        // exist in the matrix that are equal to 0, which
        // makes the matrix unsolvable.
        Net net = new Net(sourceBlock);
        this.nets.add(net);
    }

    private void addNet(NetBlock sourceBlock, TimingNode sourceNode) {
        int numSinks = sourceNode.getNumSinks();
        TimingNet timingNet = new TimingNet(sourceBlock, numSinks);

        boolean allFixed = this.isFixed(sourceBlock.blockIndex);

        for(int sinkIndex = 0; sinkIndex < numSinks; sinkIndex++) {
            GlobalBlock sinkGlobalBlock = sourceNode.getSink(sinkIndex).getGlobalBlock();
            NetBlock sinkBlock = this.netBlocks.get(sinkGlobalBlock);

            if(allFixed) {
                allFixed = this.isFixed(sinkBlock.blockIndex);
            }

            TimingEdge timingEdge = sourceNode.getSinkEdge(sinkIndex);

            timingNet.sinks[sinkIndex] = new TimingNetBlock(sinkBlock, timingEdge);
        }

        if(allFixed) {
            return;
        }

        Net net = new Net(timingNet);


        /* Don't add nets which connect only one global block.
         * Due to this, the WLD costcalculator is not entirely
         * accurate, but that doesn't matter, because we use
         * the same (inaccurate) costcalculator to calculate
         * both the linear and legal cost, so the deviation
         * cancels out.
         */
        int numUniqueBlocks = net.blocks.length;
        if(numUniqueBlocks > 1) {
            this.nets.add(net);

            for(NetBlock block : net.blocks) {
                this.hasNets[block.blockIndex] = true;
            }

            // We only need the complete list of blocks and their
            // timing edges if the algorithm is timing driven
            if(this.isTimingDriven()) {
                this.timingNets.add(timingNet);
            }
        }
    }


    @Override
    protected void doPlacement() {

        int iteration = 0;
        boolean isLastIteration = false;

        while(!isLastIteration) {
            double timerBegin = System.nanoTime();

            // Solve linear
            this.solveLinear(iteration);
            this.solveLegal(iteration);

            isLastIteration = this.stopCondition(iteration);

            double timerEnd = System.nanoTime();
            double time = (timerEnd - timerBegin) * 1e-9;

            this.printStatistics(iteration, time);

            // Update the visualizer
            this.visualizer.addPlacement(
                    String.format("iteration %d: linear", iteration),
                    this.netBlocks, this.linearX, this.linearY);
            this.visualizer.addPlacement(
                    String.format("iteration %d: legal", iteration),
                    this.netBlocks, this.legalX, this.legalY);

            iteration++;
        }

        this.logger.println();


        this.startTimer(T_UPDATE_CIRCUIT);
        try {
            this.updateCircuit();
        } catch(PlacementException error) {
            this.logger.raise(error);
        }
        this.stopTimer(T_UPDATE_CIRCUIT);
    }



    protected void updateLegal(int[] newLegalX, int[] newLegalY) {
        int numMovableBlocks = this.legalX.length - this.numIOBlocks;

        System.arraycopy(newLegalX, this.numIOBlocks, this.legalX, this.numIOBlocks, numMovableBlocks);
        System.arraycopy(newLegalY, this.numIOBlocks, this.legalY, this.numIOBlocks, numMovableBlocks);
    }


    protected void updateCircuit() throws PlacementException {
        // Clear all previous locations
        for(GlobalBlock block : this.netBlocks.keySet()) {
            if(block.getCategory() != BlockCategory.IO) {
                block.removeSite();
            }
        }

        // Update locations
        for(Map.Entry<GlobalBlock, NetBlock> blockEntry : this.netBlocks.entrySet()) {
            GlobalBlock block = blockEntry.getKey();

            if(block.getCategory() != BlockCategory.IO) {
                NetBlock netBlock = blockEntry.getValue();
                int index = netBlock.blockIndex;
                int offset = (int) Math.ceil(netBlock.offset);

                int column = this.legalX[index];
                int row = this.legalY[index] + offset * block.getType().getHeight();

                Site site = (Site) this.circuit.getSite(column, row, true);
                block.setSite(site);
            }
        }

        this.circuit.getTimingGraph().calculateCriticalities(true);
    }


    private boolean isFixed(int blockIndex) {
        return blockIndex < this.numIOBlocks;
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



    public class NetBlock {
        final int blockIndex;
        final float offset;

        NetBlock(int blockIndex, float offset) {
            this.blockIndex = blockIndex;
            this.offset = offset;
        }

        NetBlock(TimingNetBlock timingNetBlock) {
            this(timingNetBlock.blockIndex, timingNetBlock.offset);
        }

        public int getBlockIndex() {
            return this.blockIndex;
        }
        public float getOffset() {
            return this.offset;
        }

        @Override
        public boolean equals(Object otherObject) {
            if(!(otherObject instanceof NetBlock)) {
                return false;
            } else {
                return this.equals((NetBlock) otherObject);
            }
        }

        private boolean equals(NetBlock otherNetBlock) {
            return this.blockIndex == otherNetBlock.blockIndex && this.offset == otherNetBlock.offset;
        }

        @Override
        public int hashCode() {
            return 31 * this.blockIndex + (int) (2 * this.offset);
        }
    }

    class TimingNetBlock {
        final int blockIndex;
        final float offset;
        final TimingEdge timingEdge;

        TimingNetBlock(int blockIndex, float offset, TimingEdge timingEdge) {
            this.blockIndex = blockIndex;
            this.offset = offset;
            this.timingEdge = timingEdge;
        }

        TimingNetBlock(NetBlock block, TimingEdge timingEdge) {
            this(block.blockIndex, block.offset, timingEdge);
        }
    }

    class Net {
        final NetBlock[] blocks;

        Net(NetBlock block) {
            this.blocks = new NetBlock[2];
            this.blocks[0] = block;
            this.blocks[1] = block;
        }

        Net(TimingNet timingNet) {
            Set<NetBlock> netBlocks = new HashSet<>();
            netBlocks.add(timingNet.source);
            for(TimingNetBlock timingNetBlock : timingNet.sinks) {
                netBlocks.add(new NetBlock(timingNetBlock));
            }

            this.blocks = new NetBlock[netBlocks.size()];
            netBlocks.toArray(this.blocks);
        }
    }

    class TimingNet {
        final NetBlock source;
        final TimingNetBlock[] sinks;

        TimingNet(NetBlock source, int numSinks) {
            this.source = source;
            this.sinks = new TimingNetBlock[numSinks];
        }

        double getCriticality() {
            double maxCriticality = 0;
            for(TimingNetBlock sink : this.sinks) {
                double criticality = sink.timingEdge.getCriticality();
                if(criticality > maxCriticality) {
                    maxCriticality = criticality;
                }
            }

            return maxCriticality;
        }
    }
}