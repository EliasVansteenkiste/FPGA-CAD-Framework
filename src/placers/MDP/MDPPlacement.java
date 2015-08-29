package placers.MDP;

import java.util.ArrayList;
import java.util.Arrays;
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
		this.blocks = new MDPBlock[this.width + 2][this.height + 2];
		
		this.loadBlocks();
	}
	
	
	public int getWidth() {
		return this.width;
	}
	public int getHeight() {
		return this.height;
	}
	
	
	public void reorderSlice(Axis axis, int sliceIndex) {
		MDPBlock[] slice = getSlice(axis, sliceIndex);
		
		for(int i = 0; i < slice.length; i++) {
			slice[i].calculateOptimalInterval(axis);
		}
		
		Arrays.sort(slice, comparatorInterval);
		
		
		ArrayList<MDPBlock> unmatchedBlocks = new ArrayList<MDPBlock>();
		boolean[] occupiedSlots = new boolean[slice.length];
		
		for(int blockIndex = 0; blockIndex < slice.length; blockIndex++) {
			MDPBlock block = slice[blockIndex];
			boolean matched = false;
			
			for(int slotIndex = block.optimalInterval[0]; slotIndex <= block.optimalInterval[1]; slotIndex++) {
				if(!occupiedSlots[slotIndex]) {
					occupiedSlots[slotIndex] = true;
					block.coor.set(axis, slotIndex);
					matched = true;
					break;
				}
			}
			
			if(!matched) {
				unmatchedBlocks.add(block);
			}
		}
	}
	
	private MDPBlock[] getSlice(Axis axis, int sliceIndex) {
		int length;
		if(axis == Axis.X) {
			length = this.getWidth() + 2;
		} else {
			length = this.getHeight() + 2;
		}
		
		MDPBlock[] slice = new MDPBlock[length];
		
		if(axis == Axis.X) {
			System.arraycopy(blocks[sliceIndex], 0, slice, 0, length);
		
		} else {
			for(int i = 0; i < length + 2; i++) {
				slice[i] = blocks[i][sliceIndex];
			}
		}
		
		return slice;
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
	
	
	private MDPBlock getMDPBlock(Block block) {
		GridTile tile = block.getSite().getTile();
		int x = tile.getX();
		int y = tile.getY();
		
		if(this.blocks[x][y] == null) {
			this.blocks[x][y] = new MDPBlock(block);
		}
		
		return this.blocks[x][y];
	}
	
	
	
}
