package place.placers.analytical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.visual.PlacementVisualizer;

class SimpleLegalizer extends Legalizer {
	private int numRow = this.height + 2;
	private ArrayList<ArrayList<Cluster>> clustersLists = new ArrayList<ArrayList<Cluster>>(numRow);
	private int[] tmpLegalX = new int[this.linearX.length];
	private int[] tmpLegalY = new int[this.linearY.length];
	SimpleLegalizer(
            Circuit circuit,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            double[] linearX,
            double[] linearY,
            int[] legalX,
            int[] legalY,
            int[] heights,
            PlacementVisualizer visualizer,
            Map<GlobalBlock, NetBlock> blockIndexes) throws IllegalArgumentException {

        	super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights, visualizer, blockIndexes);
     }

    @Override
    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
    	ArrayList<LegalizerBlock> blocks = new ArrayList<LegalizerBlock>();  	
    	for(int id = blocksStart; id < blocksEnd; id++) {
    		LegalizerBlock legalizerBlock = new LegalizerBlock(id, this.linearX[id], this.linearY[id], 1, 1);
    		blocks.add(legalizerBlock);
    	}
    	Collections.sort(blocks, Comparators.HORIZONTAL);
    	
    	int maxCost = (this.width + this.height)*(blocksEnd - blocksStart);
    	ArrayList<Row> blockRows = new ArrayList<Row>(numRow);
    	for(int row = 0; row < this.numRow; row++){
    		blockRows.add(new Row(row, new ArrayList<LegalizerBlock>()));
    	}
    	for(int row = 0; row < this.numRow; row++){
    		this.clustersLists.add(new ArrayList<Cluster>());
    	}
    	for(LegalizerBlock block : blocks){
    		int bestRow = 1;
    		double bestCost = maxCost;
    		for (int row = 1; row < this.numRow-1; row++){
    			blockRows.get(row).addBlock(block);
    			blockRows.get(row).placeRow();//block should be inserted into this row firstly and removed at the end
//    			double cost = blockRows.get(row).cost();
    			double cost = calculateMD(this.linearX[block.getId()], this.linearY[block.getId()], this.tmpLegalX[block.getId()], this.tmpLegalY[block.getId()]);
    			if(cost < bestCost){
    				bestCost = cost;
    				bestRow = row;
    			}
    			blockRows.get(row).removeBlock();
    		}
        	//insert block to the best row best .add(block)
        	block.x = this.tmpLegalX[block.getId()];
//        	block.y = bestRow;
        	blockRows.get(bestRow).addBlock(block);
        	blockRows.get(bestRow).placeRow();
    	}
    	String name = "tmpLegal";
    	this.addVisual(name, this.tmpLegalX, this.tmpLegalY);
    	for(LegalizerBlock blocki : blocks){
    		this.legalX[blocki.getId()] = this.tmpLegalX[blocki.getId()];
    		this.legalY[blocki.getId()] = this.tmpLegalY[blocki.getId()];
    	}
    }
    private class Row{
    	int row;
    	ArrayList<LegalizerBlock> blockRow;
    	Row(int row, ArrayList<LegalizerBlock> blockRow){
    		this.row = row;
    		this.blockRow = blockRow;
    	}
    	private void addBlock(LegalizerBlock block){
    		block.y = this.row;
    		this.blockRow.add(block);
    	}
    	private double cost(){
    		double cost = totalMovement(this.blockRow); 
    		return cost;
    	}    	
    	private void removeBlock(){
    		this.blockRow.remove(this.blockRow.size()-1);
    	}
    	//input: one row with N cells
    	//output tmplegalXs
    	private void placeRow(){
    		Cluster lastCluster = null;
    		for (int i = 0; i < this.blockRow.size(); i++){
    			if((i == 0)){
    				Cluster cluster = new Cluster(lastCluster, new ArrayList<LegalizerBlock>(), 0, 0, 0, 0);
    					cluster.addBlock(this.blockRow.get(i));
    					cluster.optimalX = (int)Math.round(this.blockRow.get(i).getX());
    					clustersLists.get(this.row).add(cluster);
    					lastCluster = cluster;
    			}else{
    				if(lastCluster.optimalX + lastCluster.width < this.blockRow.get(i).getX()){
    					Cluster cluster = new Cluster(lastCluster, new ArrayList<LegalizerBlock>(), 0, 0, 0, 0);
    					cluster.addBlock( this.blockRow.get(i));
    					cluster.optimalX = (int)Math.round(this.blockRow.get(i).getX());
    					clustersLists.get(this.row).add(cluster);
    					lastCluster = cluster;
    				}else{
    					lastCluster.addBlock(this.blockRow.get(i));
    					collapse(this.row, lastCluster);	
    				}	
    			}
    			optimiseBlocks(this.row, lastCluster);//place cells
    		}
    	}
	}
    private class Cluster{
	   Cluster previous;
	   ArrayList<LegalizerBlock> clustersBlock;
	   int optimalX;
	   int width;
	   int weight;
	   double qc;
	   Cluster(Cluster previous, ArrayList<LegalizerBlock> clustersBlock,int optimalX, int width, int weight, int qc) {
		   this.previous = previous;
		   this.clustersBlock = clustersBlock;
		   this.optimalX = optimalX;
		   this.width = width;
		   this.weight = weight;
		   this.qc = qc;
	   }
	   private void addBlock(LegalizerBlock block){
		   this.clustersBlock.add(block);
		   this.weight += block.weight;
		   this.qc += block.weight * (block.getX() - this.weight);//how it comes to be minus? too much overlapped?
		   this.width += block.width;
	   }	   
   }
   private void mergeCluster(Cluster lastCluster, Cluster cluster){
	   lastCluster.clustersBlock = mergeTwoLists(lastCluster.clustersBlock, cluster.clustersBlock);
	   lastCluster.weight += cluster.weight;
	   lastCluster.width += cluster.width;
	   lastCluster.qc += cluster.qc;
   }
   private ArrayList<LegalizerBlock> mergeTwoLists(ArrayList<LegalizerBlock> list1, ArrayList<LegalizerBlock> list2){
	   list1.addAll(list2);
	   return list1;	   
   }
    private void collapse(int row, Cluster cluster){
    	//place cluster
    	cluster.optimalX = (int)Math.round(cluster.qc / cluster.weight);//integer division
    	//limit position in the row	
    	if(cluster.optimalX > this.width - cluster.width){
//    		System.out.println("collapse outside the right of the row " + cluster.optimalX + " " + cluster.width);
    		cluster.optimalX = this.width - cluster.width;
    	}
    	if(cluster.optimalX < 1){
//    		System.out.println("collapse outside the left of the row " + cluster.optimalX+ " " + cluster.width);
    		cluster.optimalX = 1;
    		
    	}
    	if(cluster.previous!=null){//if cluster's predecessor exists
    		if(cluster.previous.optimalX + cluster.previous.width > cluster.optimalX){
        		mergeCluster(cluster.previous, cluster);
        		this.clustersLists.get(row).remove(cluster);
        		collapse(row, cluster.previous);
        	}
    	}
    }
    //transform cluster position to cell positions
    private void optimiseBlocks(int row, Cluster cluster){   	
    	int x = cluster.optimalX;
    	for(LegalizerBlock block : cluster.clustersBlock){
    		this.tmpLegalX[block.getId()] = x;
    		this.tmpLegalY[block.getId()] = row;
    		x++;
    	}
    }
    private double totalMovement(List<LegalizerBlock> blocks){
		double movement = 0;
			for(LegalizerBlock block : blocks){
				movement += calculateMD(this.linearX[block.getId()], this.linearY[block.getId()], this.tmpLegalX[block.getId()], this.tmpLegalY[block.getId()]);
	    	}
	    	return movement;
    }

	private double calculateMD( double x1, double y1, int x, int y ){
		double movement = Math.abs(x1 - x)  + Math.abs(y1 - y);
		return movement;
	}
  
    private class LegalizerBlock {
    	int id;
    	double x;
    	double y;
    	int width;
    	int weight;   	
    	
    	LegalizerBlock(int id, double x, double y, int width, int weight){
    		this.id = id;
    		this.x = x;
    		this.y = y;
    		this.width = width;
    		this.weight = weight;
    	}
    	private double getX(){
    		return this.x;
    	}
       	private int getId(){
       		return this.id;
       	}
        
        @Override
        public String toString() {
            return String.format("[%.2f, %.2f]", this.x, this.y);
        }
    }    
    
    public static class Comparators {
        public static Comparator<LegalizerBlock> HORIZONTAL = new Comparator<LegalizerBlock>() {
            @Override
            public int compare(LegalizerBlock b1, LegalizerBlock b2) {
                return Double.compare(b1.x, b2.x);
            }
        };
        public static Comparator<LegalizerBlock> VERTICAL = new Comparator<LegalizerBlock>() {
            @Override
            public int compare(LegalizerBlock b1, LegalizerBlock b2) {
                return Double.compare(b1.y, b2.y);
            }
        };
    }
    
    @Override
    protected void initializeLegalizationAreas(){
    	return;
    }

    @Override
    protected HashMap<BlockType,ArrayList<int[]>> getLegalizationAreas(){
    	return new HashMap<BlockType,ArrayList<int[]>>();
    }
}