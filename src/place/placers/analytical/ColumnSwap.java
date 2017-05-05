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
	private Column[] columns;
	
	private final Set<Column> freeColumns;
	private final Set<Column> overutilizedColumns;
	
	private final Set<Block> influencedBlocks;
	
	private final Map<Integer, double[]> costIncreaseTable;
	
	private TimingTree timing;
	
	ColumnSwap(TimingTree timing){
		this.timing = timing;
		
		this.freeColumns = new HashSet<>();
		this.overutilizedColumns = new HashSet<>();
		this.influencedBlocks = new HashSet<>();
		this.costIncreaseTable = new HashMap<>();
	}
	public void doSwap(Column[] columns){
		this.timing.start("Column Swap");
		
		this.columns = columns;
		
		this.getFreeColumns();
		this.getOverutilizedColumns();
		
		this.influencedBlocks.clear();
		this.costIncreaseTable.clear();
		
		while(!this.overutilizedColumns.isEmpty()){
			Column largestColumn = this.getLargestColumn();
			
			//Clear cost table
			this.costIncreaseTable.clear();
			
			//Make cost table
			for(Block block:largestColumn.blocks){
				double currentCost = block.horizontalCost();
				double[] costMatrix = new double[columns.length];

				for(Column column:this.freeColumns){
					block.tryLegalX(column.coordinate);
					double newCost = block.horizontalCost();
					block.revert();
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
				
				bestBlock.setLegalX(bestColumn.coordinate);
				
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
					double currentCost = block.horizontalCost();
					double[] costMatrix = new double[columns.length];

					for(Column column:this.freeColumns){

						block.tryLegalX(column.coordinate);
						double newCost = block.horizontalCost();
						block.revert();
						costMatrix[column.index] = newCost - currentCost;
					}
					this.costIncreaseTable.put(block.index, costMatrix);
				}
			}
			this.overutilizedColumns.remove(largestColumn);
		}
		this.timing.time("Column Swap");
		
		//Update the column of each block
		for(Column column:columns){
			for(Block block:column.blocks){
				block.column = column;
			}
		}
	}
	private void getFreeColumns(){
		this.freeColumns.clear();
		for(Column column:this.columns){
			if(column.usedPos() < column.numPos()){
				this.freeColumns.add(column);
			}
		}
	}
	private void getOverutilizedColumns(){
		this.overutilizedColumns.clear();
		for(Column column:this.columns){
			if(column.usedPos() > column.numPos()){
				this.overutilizedColumns.add(column);
			}
		}
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
