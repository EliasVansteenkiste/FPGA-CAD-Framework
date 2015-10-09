package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.Logger;

import flexible_architecture.Circuit;
import flexible_architecture.architecture.BlockType;
import flexible_architecture.architecture.BlockType.BlockCategory;
import flexible_architecture.block.AbstractSite;
import flexible_architecture.block.GlobalBlock;
import flexible_architecture.block.Site;
import flexible_architecture.pin.AbstractPin;

public class HeteroLegalizerSeppe {
	
	private static final double UTILIZATION_FACTOR = 0.9;
	private static enum Axis {X, Y};
	
	private Circuit circuit;
	private int width, height;
	
	private Map<GlobalBlock, Integer> blockIndexes;
	private List<BlockType> blockTypes;
	private List<Integer> blockTypeIndexStarts;
	private int numBlocks;
	
	private double tileCapacity;
	
	private double[] linearX;
	private double[] linearY;
	private int[] bestLegalX;
	private int[] bestLegalY;
	private int[] tmpLegalX;
	private int[] tmpLegalY;
	
	// These are temporary data structures
	LegalizerArea[][] areaPointers;
	List<List<List<Integer>>> blockMatrix;
	
	private double bestCost;
	boolean lastMaxUtilizationSmallerThanOne;
	boolean firstOneDone;
	
	
	HeteroLegalizerSeppe(
			Circuit circuit,
			Map<GlobalBlock, Integer> blockIndexes,
			List<BlockType> blockTypes,
			List<Integer> blockTypeIndexStarts,
			double[] linearX,
			double[] linearY) {
		
		this.circuit = circuit;
		this.width = this.circuit.getWidth();
		this.height = this.circuit.getHeight();
		
		this.blockIndexes = blockIndexes;
		this.blockTypes = blockTypes;
		this.blockTypeIndexStarts = blockTypeIndexStarts;
		
		this.linearX = linearX;
		this.linearY = linearY;
		this.numBlocks = linearX.length;
		
		this.tmpLegalX = new int[this.numBlocks];
		this.tmpLegalY = new int[this.numBlocks];
		
		this.bestCost = Double.MAX_VALUE;
	}
	
	void initializeArrays() {
		for(int index = 0; index < this.numBlocks; index++) {
			this.bestLegalX[index] = (int) Math.round(this.linearX[index]);
			this.bestLegalY[index] = (int) Math.round(this.linearY[index]);
		}
		
		this.bestCost = this.calculateLegalCost();
	}
	
	
	int[] getBestLegalX() {
		return this.bestLegalX;
	}
	int[] getBestLegalY() {
		return this.bestLegalY;
	}
	
	//TODO: should these depend on lastMaxUtilizationSmallerThanOne?
	int[] getAnchorPointsX() {
		return this.bestLegalX;
	}
	int[] getAnchorPointsY() {
		return this.bestLegalY;
	}
	
	
	void legalize(int blockTypeIndex, double tileCapacity) {
		
		System.arraycopy(this.bestLegalX, 0, this.tmpLegalX, 0, this.numBlocks);
		System.arraycopy(this.bestLegalY, 0, this.tmpLegalY, 0, this.numBlocks);
		this.tileCapacity = tileCapacity * HeteroLegalizerSeppe.UTILIZATION_FACTOR;
		
		if(blockTypeIndex == -1) {
			for(int i = 0; i < this.blockTypes.size(); i++) {
				legalizeBlockType(i);
			}
		} else {
			legalizeBlockType(blockTypeIndex);
		}
		
		this.updateBestLegal();
	}
	
	
	void legalizeBlockType(int blockTypeIndex) {
		BlockType blockType = this.blockTypes.get(blockTypeIndex);
		int startIndex = this.blockTypeIndexStarts.get(blockTypeIndex);
		int endIndex = this.blockTypeIndexStarts.get(blockTypeIndex + 1);
		
		
		// Make a matrix that contains the blocks that are closest to each position
		
		// Initialize the matrix to contain a linked list at each coordinate
		this.blockMatrix = new ArrayList<List<List<Integer>>>(this.width);
		this.blockMatrix.add(null);
		for(int x = 1; x < this.width - 1; x++) {
			List<List<Integer>> blockColumn = new ArrayList<List<Integer>>(this.height);
			blockColumn.add(null);
			for(int y = 1; y < this.height - 1; y++) {
				blockColumn.add(new LinkedList<Integer>());
			}
			this.blockMatrix.add(blockColumn);
		}
		
		// Loop through all the blocks of the correct block type and add them to their closest position
		for(int index = startIndex; index < endIndex; index++) {
			Site site = (Site) this.getClosestSite(this.linearX[index], this.linearY[index], blockType);
			int x = site.getX();
			int y = site.getY();
			
			this.blockMatrix.get(x).get(y).add(index);
		}
		
		// Build a set of disjunct areas that are not over-utilized
		this.areaPointers = new LegalizerArea[this.width][this.height];
		List<LegalizerArea> areas = new ArrayList<LegalizerArea>();
		
		for(int x = 1; x < this.width - 1; x++) {
			for(int y = 1; y < this.height; y++) {
				if(this.blockMatrix.get(x).get(y).size() > 1 && this.areaPointers[x][y] == null) {
					LegalizerArea newArea = this.newArea(blockType, x, y);
					areas.add(newArea);
				}
			}
		}
		
		// Legalize all unabsorbed areas
		for(LegalizerArea area : areas) {
			if(!area.isAbsorbed()) {
				this.legalizeArea(area);
			}
		}
	}
	
	
	private AbstractSite getClosestSite(double x, double y, BlockType blockType) {
		int width = this.circuit.getWidth();
		int height = this.circuit.getWidth();
		
		if(blockType.getCategory() == BlockCategory.CLB) {
			int row = (int) Math.round(Math.max(Math.min(y, height - 2), 1));
			
			// Get closest column
			// Not easy to do this with calculations if there are multiple hardblock types
			// So just trial and error 
			int column = (int) Math.round(x);
			int step = 1;
			int direction = (x > column) ? 1 : -1;
			
			while(true) {
				if(column > 0 && column < width-1 && this.circuit.getColumnType(column).equals(blockType)) {
					break;
				}
				
				column += direction * step;
				step++;
				direction *= -1;
			}
			
			return this.circuit.getSite(column, row);
		
			
		} else {
			int start = blockType.getStart();
			int repeat = blockType.getRepeat();
			int blockHeight = blockType.getHeight();
			
			int numRows = (int) Math.floor((height - 2) / blockHeight + 1); 
			int numColumns = (int) Math.floor((width - start - 1) / repeat + 1);
			
			int columnIndex = (int) Math.round(Math.max(Math.min((x - start) / repeat, numColumns), 0));
			int rowIndex = (int) Math.round(Math.max(Math.min((y - 1) / blockHeight, numRows), 0));
			
			return this.circuit.getSite(columnIndex * repeat + start, rowIndex * blockHeight + 1);
		}
	}
	
	
	private LegalizerArea newArea(BlockType blockType, int x, int y) {
		// left, top, right, bottom
		LegalizerArea area = new LegalizerArea(x, y, this.tileCapacity);
		area.incrementTiles();
		area.addBlockIndexes(this.blockMatrix.get(x).get(y));
		
		
		int[] desiredDirection = {0, -1};
		
		while(area.getOccupation() > area.getCapacity()) {
			// Rotate direction clockwise
			int tmp = desiredDirection[0];
			desiredDirection[0] = -desiredDirection[1];
			desiredDirection[1] = tmp;
			
			// If growing in desired direction is not possible: choose the opposite direction
			int[] realDirection = {desiredDirection[0], desiredDirection[1]};
			if(area.left + desiredDirection[0] < 1
					|| area.right + desiredDirection[0] > this.width - 2
					|| area.top + desiredDirection[1] < 1
					|| area.bottom + desiredDirection[1] > this.height - 2) {
				realDirection[0] = -realDirection[0];
				realDirection[1] = -realDirection[1];
			}
			
			
			// goalArea is the area that area should eventually cover
			LegalizerArea goalArea = new LegalizerArea(area);
			goalArea.grow(realDirection);
			
			this.growArea(blockType, area, goalArea);
		}
		
		return area;
	}
	
	private void growArea(BlockType blockType, LegalizerArea area, LegalizerArea goalArea) {
		
		int[] rows = {0, 0};
		int[] columns = {0, 0};
		
		// While goalArea is not completely covered by area
		while(true) {
			if(area.left != goalArea.left) {
				rows[0] = area.top;
				rows[1] = area.bottom;
				columns[0] = area.left - 1;
				columns[1] = area.left - 1;
				area.left--;
			
			} else if(area.right != goalArea.right) {
				rows[0] = area.top;
				rows[1] = area.bottom;
				columns[0] = area.right + 1;
				columns[1] = area.right + 1;
				area.right++;
			
			} else if(area.top != goalArea.top) {
				rows[0] = area.top - 1;
				rows[1] = area.top - 1;
				columns[0] = area.left;
				columns[1] = area.right;
				area.top--;
			
			} else if(area.bottom != goalArea.bottom) {
				rows[0] = area.bottom + 1;
				rows[1] = area.bottom + 1;
				columns[0] = area.left;
				columns[1] = area.right;
				area.bottom++;
			
			} else {
				return;
			}
			
			
			for(int y = rows[0]; y <= rows[0]; y++) {
				for(int x = columns[0]; x <= columns[1]; x++) {
					
					// If this tile is occupied by an unabsorbed area
					LegalizerArea neighbour = this.areaPointers[x][y];
					if(neighbour != null && !neighbour.isAbsorbed()) {
						neighbour.absorb();
						
						// Update the goal area to contain the absorbed area 
						goalArea.left = Math.min(goalArea.left, neighbour.left);
						goalArea.right = Math.max(goalArea.right, neighbour.right);
						goalArea.top = Math.min(goalArea.top, neighbour.top);
						goalArea.bottom = Math.max(goalArea.bottom, neighbour.bottom);
					}
					
					// Update the area pointer
					this.areaPointers[x][y] = area;
					
					// Add the blocks to the area
					area.addBlockIndexes(this.blockMatrix.get(x).get(y));
					
					// Update the capacity
					AbstractSite site = this.circuit.getSite(x, y, true);
					if(site != null && site.getType().equals(blockType)) {
						area.incrementTiles();
					}
				}
			}
		}
	}
	
	
	
	private void legalizeArea(LegalizerArea area) {
		List<Integer> blockIndexes = area.getBlockIndexes();
		int[] coordinates = {area.left, area.top, area.right, area.bottom};
		this.legalizeArea(coordinates, blockIndexes, Axis.X);
	}
	
	private void legalizeArea(int[] coordinates, List<Integer> blockIndexes, Axis axis) {
		
		// If the area is only one tile big: place all the blocks on this tile
		if(coordinates[0] == coordinates[2] && coordinates[1] == coordinates[3]) {
			if(blockIndexes.size() > this.tileCapacity) {
				Logger.raise("Too many blocks assigned to one tile");
			}
			
			for(Integer blockIndex : blockIndexes) {
				this.tmpLegalX[blockIndex] = coordinates[0];
				this.tmpLegalY[blockIndex] = coordinates[1];
			}
			
			return;
		}
		
		// Split area along axis and store ratio between the two subareas
		// Sort blocks along axis
		int[] coordinates1 = new int[4], coordinates2 = new int[4];
		System.arraycopy(coordinates, 0, coordinates1, 0, 4);
		System.arraycopy(coordinates, 0, coordinates2, 0, 4);
		
		double splitRatio;
		Axis newAxis;
		
		if(axis == Axis.X) {
			int split = (coordinates[0] + coordinates[2]) / 2;
			splitRatio = (split - coordinates[0]) / (coordinates[2] - coordinates[0]);
			
			coordinates1[2] = split;
			coordinates2[0] = split;
			
			Collections.sort(blockIndexes, new BlockComparator(this.linearX));
			
			newAxis = Axis.Y;
			
		} else {
			int split = (coordinates[1] + coordinates[3]) / 2;
			splitRatio = (split - coordinates[1]) / (coordinates[3] - coordinates[1]);
			
			coordinates1[3] = split;
			coordinates2[1] = split;
			
			Collections.sort(blockIndexes, new BlockComparator(this.linearY));
			
			newAxis = Axis.X;
		}
		
		// Split blocks in two lists with a ratio approx. equal to area split
		int split = (int) (splitRatio * blockIndexes.size());
		List<Integer> blocks1 = blockIndexes.subList(0, split);
		List<Integer> blocks2 = blockIndexes.subList(split, blockIndexes.size());
		
		this.legalizeArea(coordinates1, blocks1, newAxis);
		this.legalizeArea(coordinates2, blocks2, newAxis);
	}
	
	
	private void updateBestLegal() {
		double newCost = this.calculateTemporaryLegalCost();
		
		if(newCost < this.bestCost) {
			System.arraycopy(this.tmpLegalX, 0, this.bestLegalX, 0, this.numBlocks);
			System.arraycopy(this.tmpLegalY, 0, this.bestLegalY, 0, this.numBlocks);
			this.bestCost = newCost;
		}
	}
	
	
	double calculateLegalCost() {
		return this.bestCost;
	}
	private double calculateTemporaryLegalCost() {
		return this.calculateCost(true, true);
	}
	double calculateLinearCost() {
		return this.calculateCost(false, false);
	}
	private double calculateCost(boolean legal, boolean temp) {
		double cost = 0.0;
		
		for(Map.Entry<GlobalBlock, Integer> blockEntry : this.blockIndexes.entrySet()) {
			GlobalBlock sourceBlock = blockEntry.getKey();
			int index = blockEntry.getValue();
			
			for(AbstractPin sourcePin : sourceBlock.getOutputPins()) {
				
				Set<GlobalBlock> netBlocks = new HashSet<GlobalBlock>();
				List<AbstractPin> pins = new ArrayList<AbstractPin>();
				
				// The source pin must be added first!
				pins.add(sourcePin);
				pins.addAll(sourcePin.getSinks());
				
				double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE,
						minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
				
				
				
				for(AbstractPin pin : pins) {
					GlobalBlock block = (GlobalBlock) pin.getOwner();
					netBlocks.add(block);
					
					double x, y;
					
					if(block.getCategory() == BlockCategory.IO) {
						x = block.getX();
						y = block.getY();
						
					} else if(legal) {
						if(temp) {
							x = this.tmpLegalX[index];
							y = this.tmpLegalY[index];
						
						} else {
							x = this.bestLegalX[index];
							y = this.bestLegalY[index];
						}
					
					} else {
						x = this.linearX[index];
						y = this.linearY[index];
					}
					
					
					if(x < minX) {
						minX = x;
					} else if(x > maxX) {
						maxX = x;
					}
					
					if(y < minY) {
						minY = y;
					} else if(y > maxY) {
						maxY = y;
					}
				}
				
				double weight = HeteroAnalyticalPlacerTwo.getWeight(netBlocks.size());
				cost += ((maxX - minX) + (maxY - minY) + 2) * weight;
			}
		}
		
		return cost;
	}
}
