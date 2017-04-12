package place.placers.analytical;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import place.placers.analytical.HardblockConnectionLegalizer.Block;
import place.placers.analytical.HardblockConnectionLegalizer.Column;
import place.placers.analytical.HardblockConnectionLegalizer.Net;
import place.util.TimingTree;

public class ColumnSwap {
	private final Column[] columns;
	
	private final Set<Column> freeColumns;
	private final Set<Column> overutilizedColumns;
	
	private final Set<Block> influencedBlocks;
	
	private Map<Integer, double[]> costIncreaseTable;
	
	private TimingTree timing;
	
	ColumnSwap(Column[] columns, TimingTree timing){
		this.timing = timing;
		this.timing.start("Column Swap");
		
		this.columns = columns;
		
		this.freeColumns = this.getFreeColumns();
		this.overutilizedColumns = this.getOverutilizedColumns();
		
		this.influencedBlocks = new HashSet<Block>();
		
		this.costIncreaseTable = new HashMap<Integer, double[]>();
		
		while(!this.overutilizedColumns.isEmpty()){
			Column largestColumn = this.getLargestColumn();
			
			//Clear cost table
			this.costIncreaseTable.clear();
			
			//Make cost table
			for(Block block:largestColumn.blocks){
				int blockX = block.x;
				int blockY = block.y;

				double currentCost = block.connectionCost();
				double[] costMatrix = new double[columns.length];

				for(Column column:this.freeColumns){
					int columnX = column.coordinate;

					block.x = columnX;
					double newCost = block.connectionCost(blockX, columnX, blockY, blockY);
					block.x = blockX;
					costMatrix[column.index] = newCost - currentCost;
				}
				this.costIncreaseTable.put(block.index, costMatrix);
			}
			
			//Move blocks until the number of blocks is equal to the number of positions
			while(largestColumn.usedPos() > largestColumn.numPos()){
				
				//Find the best block based on the cost table
				Block bestBlock = null;
				Column bestColumn = null;
				double minimumIncrease = Double.MAX_VALUE;

				for(Block block:largestColumn.blocks){
					
					double[] costIncrease = this.costIncreaseTable.get(block.index);

					for(Column column:this.freeColumns){

						double increase = costIncrease[column.index];

						if(increase < minimumIncrease){
							minimumIncrease = increase;
							bestBlock = block;
							bestColumn = column;
						}
					}
				}

				//Move the best block and update bb cost of the nets 
				largestColumn.removeBlock(bestBlock);
				bestColumn.addBlock(bestBlock);
				
				for(Net net:bestBlock.nets){
					net.checkForHorizontalChange();
				}
				
				bestBlock.x = bestColumn.coordinate;
				bestBlock.updateConnectionCost(largestColumn.coordinate, bestColumn.coordinate, bestBlock.y, bestBlock.y);
				
				this.costIncreaseTable.remove(bestBlock.index);

				if(bestColumn.usedPos() == bestColumn.numPos()){
					this.freeColumns.remove(bestColumn);
				}
				
				//Update cost table
				this.influencedBlocks.clear();
				for(Net net: bestBlock.nets){
					if(net.horizontalChange){
						for(Block influencedBlock:net.blocks){
							if(largestColumn.blocks.contains(influencedBlock)){
								this.influencedBlocks.add(influencedBlock);
							}
						}
					}
				}
				for(Block block:this.influencedBlocks){
					int blockX = block.x;
					int blockY = block.y;

					double currentCost = block.connectionCost();
					double[] costMatrix = new double[columns.length];

					for(Column column:this.freeColumns){
						int columnX = column.coordinate;

						block.x = columnX;
						double newCost = block.connectionCost(blockX, columnX, blockY, blockY);
						block.x = blockX;
						costMatrix[column.index] = newCost - currentCost;
					}
					this.costIncreaseTable.put(block.index, costMatrix);
				}
			}
			this.overutilizedColumns.remove(largestColumn);
		}
		this.timing.time("Column Swap");
	}
	private Set<Column> getFreeColumns(){
		Set<Column> freeColumns = new HashSet<Column>();
		for(Column column:this.columns){
			if(column.usedPos() < column.numPos()){
				freeColumns.add(column);
			}
		}
		return freeColumns;
	}
	private Set<Column> getOverutilizedColumns(){
		Set<Column> overutilizedColumns = new HashSet<Column>();
		for(Column column:this.columns){
			if(column.usedPos() > column.numPos()){
				overutilizedColumns.add(column);
			}
		}
		return overutilizedColumns;
	}
	private Column getLargestColumn(){
		Column largestColumn = null;
		for(Column column:this.overutilizedColumns){
			if(largestColumn == null){
				largestColumn = column;
			}else if(column.usedPos() > largestColumn.usedPos()){
				largestColumn = column;
			}
		}
		return largestColumn;
	}
}
