package flexible_architecture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import flexible_architecture.architecture.BlockType;
import flexible_architecture.architecture.FlexibleArchitecture;
import flexible_architecture.architecture.BlockType.BlockCategory;
import flexible_architecture.block.AbstractBlock;
import flexible_architecture.site.AbstractSite;
import flexible_architecture.site.IOSite;
import flexible_architecture.site.Site;

public class Circuit {
	
	private int width, height;
	
	private NetParser netparser;
	private FlexibleArchitecture architecture;
	
	private Map<BlockType, List<AbstractBlock>> blocks;
	private AbstractSite[][] sites;
	private Map<BlockType, List<Integer>> columnsPerBlockType = new HashMap<BlockType, List<Integer>>();
	
	
	public Circuit(FlexibleArchitecture architecture, String filename) {
		this.netparser = new NetParser(filename);
		this.architecture = architecture;
	}
	
	public void parse() {
		this.blocks = this.netparser.parse();
		
		List<BlockType> blockTypes = BlockType.getBlockTypes(BlockCategory.IO);
		blockTypes.addAll(BlockType.getBlockTypes(BlockCategory.CLB));
		blockTypes.addAll(BlockType.getBlockTypes(BlockCategory.HARDBLOCK));
		
		
		
		this.calculateSizeAndColumns();
		this.createSites();
	}
	
	private void calculateSizeAndColumns() {
		BlockType ioType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
		BlockType clbType = BlockType.getBlockTypes(BlockCategory.CLB).get(0);
		List<BlockType> hardBlockTypes = BlockType.getBlockTypes(BlockCategory.HARDBLOCK);
		
		this.columnsPerBlockType.put(ioType, new ArrayList<Integer>());
		this.columnsPerBlockType.put(clbType, new ArrayList<Integer>());
		for(BlockType blockType : hardBlockTypes) {
			this.columnsPerBlockType.put(blockType, new ArrayList<Integer>());
		}
		
		
		int numClbColumns = 0;
		int[] numHardBlockColumns = new int[hardBlockTypes.size()];
		for(int i = 0; i < hardBlockTypes.size(); i++) {
			numHardBlockColumns[i] = 0;
		}
		
		List<BlockType> columns = new ArrayList<BlockType>();
		columns.add(ioType);
		int size = 2;
		
		boolean tooSmall = true;
		while(tooSmall) {
			for(int i = 0; i < hardBlockTypes.size(); i++) {
				BlockType hardBlockType = hardBlockTypes.get(i);
				int start = hardBlockType.getStart();
				int repeat = hardBlockType.getRepeat();
				
				if((size - 1 - start) % repeat == 0) {
					columns.add(hardBlockType);
					numHardBlockColumns[i]++;
					break;
				}
			}
			
			if(columns.size() < size) {
				columns.add(clbType);
				numClbColumns++;
			}
			
			size++;
			tooSmall = false;
			
			int clbCapacity = (int) ((size - 2) * numClbColumns * this.architecture.getFillGrade());
			int ioCapacity = (size - 1) * 4 * BlockType.getIoCapacity();
			if(clbCapacity < this.blocks.get(clbType).size() || ioCapacity < this.blocks.get(ioType).size()) { 
				tooSmall = true;
				continue;
			}
			
			for(int i = 0; i < hardBlockTypes.size(); i++) {
				BlockType hardBlockType = hardBlockTypes.get(i);
				
				if(!this.blocks.containsKey(hardBlockType)) {
					continue;
				}
				
				int heightPerBlock = hardBlockType.getHeight();
				int blocksPerColumn = (size - 2) / heightPerBlock;
				int capacity = numHardBlockColumns[i] * blocksPerColumn;
				
				if(capacity < this.blocks.get(hardBlockType).size()) {
					tooSmall = true;
					break;
				}
			}
		}
		
		
		columns.add(ioType);
		this.width = size;
		this.height = size;
		
		for(int i = 0; i < columns.size(); i++) {
			this.columnsPerBlockType.get(columns.get(i)).add(i);
		}
	}
	
	
	private void createSites() {
		this.sites = new AbstractSite[this.width][this.height];
		
		BlockType ioType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
		int ioCapacity = BlockType.getIoCapacity();
		
		int size = this.width;
		for(int i = 0; i < size - 1; i++) {
			this.sites[0][i] = new IOSite(0, i, ioCapacity);
			this.sites[size-1][size-1-i] = new IOSite(size-1, size-1-i, ioCapacity);
			this.sites[i][0] = new IOSite(i, 0, ioCapacity);
			this.sites[size-1-i][size-1] = new IOSite(size-1-i, size-1, ioCapacity);
		}
		
		for(Entry<BlockType, List<Integer>> columnEntry : this.columnsPerBlockType.entrySet()) {
			BlockType blockType = columnEntry.getKey();
			if(blockType.equals(ioType)) {
				continue;
			}
			
			int height = blockType.getHeight();
			List<Integer> columns = columnEntry.getValue();
			
			for(int x : columns) {
				for(int y = 1; y < size - 1; y++) {
					this.sites[x][y] = new Site(x, y, height);
				}
			}
		}
	}
	
	
	public AbstractSite getSite(int x, int y) {
		return this.sites[x][y];
	}
}
