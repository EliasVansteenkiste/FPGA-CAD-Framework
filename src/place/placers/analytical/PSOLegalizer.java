package place.placers.analytical;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.placers.analytical.HardblockConnectionLegalizer.Block;
import place.placers.analytical.HardblockConnectionLegalizer.Crit;
import place.visual.PlacementVisualizer;

public class PSOLegalizer {
	
	private BlockType blockType;
	
	private final double[] linearX, linearY;
    private final int[] legalX, legalY;
    
    private final Block[] blocks;
	private final Net[] nets;
	private final List<Crit> crits;
	
	private final int gridWidth, gridHeigth;

	PSOLegalizer(
			double[] linearX,
			double[] linearY, 
			int[] legalX, 
			int[] legalY, 
			int[] heights,
			int gridWidth,
			int gridHeight,
			List<Net> placerNets) {
//		super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights, nets, visualizer,
//				netBlocks);
//		// TODO Auto-generated constructor stub
		this.linearX = linearX;
		this.linearY = linearY;
		this.legalX = legalX;
		this.legalY = legalY;
		
		this.blocks = new Block[this.linearX.length];
		
	}

//	@Override
	protected void legalizeBlockType(int blocksStart, int blocksEnd) {
//		// TODO Auto-generated method stub
//		
	}
	
	
	private class Block {
	  	   int index;

	  	   double linearX, linearY;
	  	  
	  	   int size;
	  	   double weight;
	  	   
	  	   boolean processed;
	      	
	  	   Block(int index, double linearX, double linearY, int width, double weight){
	  		   this.index = index;
	  		   this.linearX = linearX;
	  		   this.linearY = linearY;
	  		   this.size = width;
	  		   this.weight = weight;
	  	   }
	  	   double cost(int legalX, int legalY){
	  		   return this.weight * ((this.linearX - legalX) * (this.linearX - legalX) + (this.linearY - legalY) * (this.linearY - legalY));
	  	   }
	    }

}
