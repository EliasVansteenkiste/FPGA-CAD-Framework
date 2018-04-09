package place.placers.analytical;

import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;

class LinearSolverGradient {

    private double[] coordinatesX, coordinatesY;
    private int[] netBlockIndexes;
    private float[] netBlockOffsets;

    private DimensionSolverGradient solverX, solverY;

    LinearSolverGradient(
            double[] coordinatesX,
            double[] coordinatesY,
            int[] netBlockIndexes,
            float[] netBlockOffsets,
            double maxConnectionLength,
            boolean[] fixed,
            double beta1,
            double beta2,
            double eps) {

        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;

        this.netBlockIndexes = netBlockIndexes;
        this.netBlockOffsets = netBlockOffsets;

        this.solverX = new DimensionSolverGradient(coordinatesX, maxConnectionLength, fixed, beta1, beta2, eps);
        this.solverY = new DimensionSolverGradient(coordinatesY, maxConnectionLength, fixed, beta1, beta2, eps);
    }

    public void initializeIteration(double pseudoWeight, double learningRate) {
        this.solverX.initializeIteration(pseudoWeight, learningRate);
        this.solverY.initializeIteration(pseudoWeight, learningRate);
    }


    void addPseudoConnections(int[] legalX, int[] legalY) {
        this.solverX.setLegal(legalX);
        this.solverY.setLegal(legalY);
    }
    void processNetNew(Net net, boolean[] fixed) {
        int numNetBlocks = net.blocks.length;////
        double weight = AnalyticalAndGradientPlacer.getWeight(numNetBlocks);

        // Nets with 2 blocks are common and can be processed very quick
        if(numNetBlocks == 2) {

            double coordinateX1, coordinateX2;
            double coordinateY1, coordinateY2;
          
            NetBlock block1 = net.blocks[0];
            NetBlock block2 = net.blocks[1];
            int blockIndex1 = block1.getBlockIndex();
            int blockIndex2 = block2.getBlockIndex();
            boolean block1Fixed = fixed[blockIndex1];
            boolean block2Fixed = fixed[blockIndex2];
            
//            System.out.println("block\t" + blockIndex1 + "\t" + blockIndex2);
            
            coordinateY1 = block1Fixed?(block1.legalY + block1.offset):(block1.linearY + block1.offset);
            coordinateY2 = block2Fixed?(block2.legalY + block2.offset):(block2.linearY + block2.offset);
//            System.out.println("y\t" + coordinateY1 + "\t" + coordinateY2);
            this.solverY.processConnection(blockIndex1, blockIndex2, coordinateY2 - coordinateY1, weight, false);
            
            coordinateX1 = block1Fixed?block1.legalX:block1.linearX;
            coordinateX2 = block2Fixed?block1.legalX:block2.linearX;
//            System.out.println("x\t" + coordinateX1 + "\t" + coordinateX2);
            this.solverX.processConnection(blockIndex1, blockIndex2, coordinateX2 - coordinateX1, weight, false);
            
            return;
            
        }

        // For bigger nets, we have to find the min and max block      
        NetBlock firstBlock = net.blocks[0];
        int firstIndex = firstBlock.getBlockIndex();
        int minXIndex = firstIndex;
        int maxXIndex = firstIndex;
        int minYIndex = firstIndex;
        int maxYIndex = firstIndex;
        boolean firstBlockFixed = fixed[firstIndex];
        
        double minX = firstBlockFixed?firstBlock.legalX : firstBlock.linearX;
        double maxX = firstBlockFixed?firstBlock.legalX : firstBlock.linearX;
        double minY = firstBlockFixed?(firstBlock.legalY + firstBlock.offset):(firstBlock.linearY + firstBlock.offset);
        double maxY = firstBlockFixed?(firstBlock.legalY + firstBlock.offset ):(firstBlock.linearY + firstBlock.offset);
        
        for(NetBlock block:net.blocks){
        	int index = block.getBlockIndex();
        	boolean blockFixed = fixed[index];
        	
        	double x = blockFixed?block.legalX : block.linearX;
        	double y = blockFixed?(block.legalY + block.offset):(block.linearY + block.offset);
        	
        	if(x < minX){
        		minX = x;
        		minXIndex = index;
        	}else if(x > maxX){
        		maxX = x;
        		maxXIndex = index;
        	}
        	if(y < minY){
        		minY = y;
        		minYIndex = index;
        	}else if(y > maxY){
        		maxY = y;
        		maxYIndex = index;
        	}
        }
        

        // Add connections between the min and max block
//        System.out.println("x\t" + minX + "\t" + maxX);
//        System.out.println("y\t" + minY + "\t" + maxY);
        this.solverX.processConnection(minXIndex, maxXIndex, maxX - minX, weight, false);
        this.solverY.processConnection(minYIndex, maxYIndex, maxY - minY, weight, false);
    }

    void processNet(int netStart, int netEnd) {
        int numNetBlocks = netEnd - netStart;
        double weight = AnalyticalAndGradientPlacer.getWeight(numNetBlocks);

        // Nets with 2 blocks are common and can be processed very quick
        if(numNetBlocks == 2) {
            int blockIndex1 = this.netBlockIndexes[netStart],
                blockIndex2 = this.netBlockIndexes[netStart + 1];
            
//            System.out.println("block\t" + blockIndex1 + "\t" + blockIndex2);

            double coordinate1, coordinate2;
            
            coordinate1 = this.coordinatesY[blockIndex1] + this.netBlockOffsets[netStart];
            coordinate2 = this.coordinatesY[blockIndex2] + this.netBlockOffsets[netStart + 1];
//            System.out.println("y\t" + coordinate1 + "\t" + coordinate2);
            this.solverY.processConnection(blockIndex1, blockIndex2, coordinate2 - coordinate1, weight, false);

            coordinate1 = this.coordinatesX[blockIndex1];
            coordinate2 = this.coordinatesX[blockIndex2];
//            System.out.println("x\t" + coordinate1 + "\t" + coordinate2);
            this.solverX.processConnection(blockIndex1, blockIndex2, coordinate2 - coordinate1, weight, false);

            return;
        }


        // For bigger nets, we have to find the min and max block
        int minXIndex = this.netBlockIndexes[netStart],
            maxXIndex = this.netBlockIndexes[netStart],
            minYIndex = this.netBlockIndexes[netStart],
            maxYIndex = this.netBlockIndexes[netStart];

        double minX = this.coordinatesX[minXIndex],
               maxX = this.coordinatesX[maxXIndex],
               minY = this.coordinatesY[minYIndex] + this.netBlockOffsets[netStart],
               maxY = this.coordinatesY[maxYIndex] + this.netBlockOffsets[netStart];

        for(int i = netStart + 1; i < netEnd; i++) {
            int blockIndex = this.netBlockIndexes[i];
            double x = this.coordinatesX[blockIndex],
                   y = this.coordinatesY[blockIndex] + this.netBlockOffsets[i];

            if(x < minX) {
                minX = x;
                minXIndex = blockIndex;
            } else if(x > maxX) {
                maxX = x;
                maxXIndex = blockIndex;
            }

            if(y < minY) {
                minY = y;
                minYIndex = blockIndex;
            } else if(y > maxY) {
                maxY = y;
                maxYIndex = blockIndex;
            }
        }

        // Add connections between the min and max block
        this.solverX.processConnection(minXIndex, maxXIndex, maxX - minX, weight, false);
        this.solverY.processConnection(minYIndex, maxYIndex, maxY - minY, weight, false);
    }

    void processConnection(int blockIndex1, int blockIndex2, float offset, float weight, boolean critical) {
        double x1 = this.coordinatesX[blockIndex1],
               x2 = this.coordinatesX[blockIndex2],
               y1 = this.coordinatesY[blockIndex1],
               y2 = this.coordinatesY[blockIndex2];

        this.solverX.processConnection(blockIndex1, blockIndex2, x2 - x1, weight, critical);
        this.solverY.processConnection(blockIndex1, blockIndex2, y2 - y1 + offset, weight, critical);
    }

    void solve() {
        this.solverX.solve();
        this.solverY.solve();
    }
    
    double[] getCoordinatesX(){
    	return this.coordinatesX;
    }
    double[] getCoordinatesY(){
    	return this.coordinatesY;
    }
}