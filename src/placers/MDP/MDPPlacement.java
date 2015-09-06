package placers.MDP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

import circuit.Block;
import circuit.Clb;
import circuit.Input;
import circuit.Net;
import circuit.PackedCircuit;
import circuit.Pin;
import architecture.Architecture;
import architecture.GridTile;
import architecture.Site;

public class MDPPlacement {
	
	private Architecture architecture;
	private PackedCircuit circuit;
	
	private int width, height;
	private MDPBlock[][] blocks;
	
	private MDPBlockComparatorInterval comparatorInterval = new MDPBlockComparatorInterval();
	
	
	public MDPPlacement(Architecture architecture, PackedCircuit circuit) {
		this.architecture = architecture;
		this.circuit = circuit;
		
		this.width = architecture.getWidth();
		this.height = architecture.getHeight();
		
		// + 2 is for 2 rows and 2 colums of IO blocks
		this.blocks = new MDPBlock[this.height + 2][this.width + 2];
		
		this.loadBlocks();
	}
	
	
	public int getWidth() {
		return this.width;
	}
	public int getHeight() {
		return this.height;
	}
	public int getSize(Axis axis) {
		if(axis == Axis.X) {
			return this.getWidth();
		} else {
			return this.getHeight();
		}
	}
	
	
	public void reorderSlice(Axis axis, int sliceIndex) {
		MDPBlock[] slice = this.getSlice(axis, sliceIndex);
		slice[0] = null;
		slice[slice.length-1] = null;
		
		int blocksInSlice = 0;
		for(int i = 1; i < slice.length - 1; i++) {
			if(slice[i] != null) {
				slice[i].calculateOptimalInterval(axis);
				blocksInSlice++;
			}
		}
		
		Arrays.sort(slice, this.comparatorInterval);
		
		
		ArrayList<MDPBlock> unmatchedBlocks = new ArrayList<MDPBlock>();
		MDPBlock[] newSlice = new MDPBlock[slice.length];
		
		// Interval bipartite matching
		for(int blockIndex = 0; blockIndex < blocksInSlice; blockIndex++) {
			MDPBlock block = slice[blockIndex];
			boolean matched = false;
			
			for(int slotIndex = block.optimalInterval[0]; slotIndex <= block.optimalInterval[1]; slotIndex++) {
				if(newSlice[slotIndex] == null) {
					newSlice[slotIndex] = block;
					block.optimalPosition = slotIndex;
					matched = true;
					break;
				}
			}
			
			if(!matched) {
				unmatchedBlocks.add(block);
			}
		}
		
		// Construct Min-cost graphs
		LinkedList<int[]> partitions = this.getMinCostPartitions(unmatchedBlocks, newSlice);
		
		
		for(int i = 1; i < newSlice.length - 1; i++) {
			if(newSlice[i] != null) {
				this.moveBlock(newSlice[i], axis, i);
			}
		}
	}
	
	
	private LinkedList<int[]> getMinCostPartitions(List<MDPBlock> unmatchedBlocks, MDPBlock[] newSlice) {
		LinkedList<int[]> partitions = new LinkedList<int[]>();
		
		if(unmatchedBlocks.size() == 0) {
			return partitions;
		}
		
		// Match all the unmatched blocks to a position.
		// In the process: get an ordered list of minimal partitions each containing
		// exactly one previously unmatched block. Partitions are allowed to overlap
		// (for now).
		for(MDPBlock block : unmatchedBlocks) {
			int left = block.optimalInterval[0];
			int right = block.optimalInterval[1];
			int step = 0, direction = 0, startPosition = 0, endPosition = 0;
			
			while(direction == 0) {
				step += 1;
				if(left - step >= 1 && newSlice[left - step] == null) {
					direction = 1;
					startPosition = left - step;
					endPosition = left;
				} else if(right + step < newSlice.length - 1 && newSlice[right + step] == null) {
					direction = -1;
					startPosition = right + step;
					endPosition = right;
				}
			}
			
			for(int position = startPosition; position != endPosition; position += direction) {
				newSlice[position] = newSlice[position + direction];
			}
			
			newSlice[endPosition] = block;
			block.optimalPosition = endPosition;
			
			
			// Get the partition containing this unmatched block.
			int[] partition = {1, newSlice.length - 1};
			if(direction == 1) {
				partition[0] = Math.max(partition[0], Math.min(left - 2, startPosition));
				partition[1] = Math.min(partition[1], left + 2);
			} else {
				partition[0] = Math.max(partition[0], right - 2);
				partition[1] = Math.min(partition[1], Math.max(right + 2, endPosition));
			}
			
			partitions.add(partition);
		}
		
		
		// Sort the partitions based on their smallest bound
		Collections.sort(partitions, new Comparator<int[]>() {
			public int compare(int[] p1, int[] p2) {
				return p1[0] - p2[0];
			}
		});
		
		
		// Merge overlapping partitions
		ListIterator<int[]> li = partitions.listIterator();
		int[] previousPartition = li.next();
		
		while(li.hasNext()) {
			int[] partition = li.next();
			
			if(partition[0] < previousPartition[1]) {
				previousPartition[1] = partition[1];
				li.remove();
			} else {
				previousPartition = partition;
			}
		}
		
		return partitions;
	}
	
	
	private MDPBlock[] getSlice(Axis axis, int sliceIndex) {
		int length = this.getSize(axis) + 2;
		
		MDPBlock[] slice = new MDPBlock[length];
		
		if(axis == Axis.X) {
			System.arraycopy(blocks[sliceIndex], 0, slice, 0, length);
		
		} else {
			for(int i = 0; i < length; i++) {
				slice[i] = blocks[i][sliceIndex];
			}
		}
		
		return slice;
	}

	
	private void moveBlock(MDPBlock block, Axis axis, int position) {
		block.move(axis, position);
		this.blocks[block.coor.y][block.coor.x] = block;
	}
	
	private void loadBlocks() {
		for(Net originalNet : this.circuit.getNets().values()) {
			ArrayList<MDPBlock> blocks = new ArrayList<>();
			
			blocks.add(this.getMDPBlock(originalNet.source.owner));
			for(Pin sink : originalNet.sinks) {
				blocks.add(this.getMDPBlock(sink.owner));
			}
			
			
			MDPNet net = new MDPNet(originalNet, blocks);
			
			
			for(MDPBlock block : blocks) {
				block.addNet(net);
			}
		}
	}
	
	public void updateOriginalBlocks() {
		for(int y = 1; y < this.height - 1; y++) {
			for(int x = 1; x < this.width - 1; x++) {
				if(this.blocks[y][x] != null) {
					Block block = this.blocks[y][x].getOriginalBlock();
					Site site = this.architecture.getSite(x, y, 0);
					
					block.setSite(site);
					site.setBlock(block);
				}
			}
		}
	}
	
	
	private MDPBlock getMDPBlock(Block block) {
		GridTile tile = block.getSite().getTile();
		int x = tile.getX();
		int y = tile.getY();
		
		//TODO: allow multiple IO blocks on one tile
		if(this.blocks[y][x] == null) {
			this.blocks[y][x] = new MDPBlock(block);
		} else if(this.blocks[y][x].originalBlock != block) {
			System.out.println("ok");
		}
		
		return this.blocks[y][x];
	}
}
