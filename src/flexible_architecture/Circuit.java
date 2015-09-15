package flexible_architecture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import flexible_architecture.architecture.BlockType;
import flexible_architecture.architecture.FlexibleArchitecture;
import flexible_architecture.architecture.BlockType.BlockCategory;
import flexible_architecture.architecture.PortType;
import flexible_architecture.block.AbstractBlock;
import flexible_architecture.block.GlobalBlock;
import flexible_architecture.pin.AbstractPin;
import flexible_architecture.pin.GlobalPin;
import flexible_architecture.site.AbstractSite;
import flexible_architecture.site.IOSite;
import flexible_architecture.site.Site;

public class Circuit {
	
	private int width, height;
	
	private NetParser netparser;
	private FlexibleArchitecture architecture;
	
	private Map<BlockType, List<AbstractBlock>> blocks;
	private List<GlobalBlock> globalBlockList = new ArrayList<GlobalBlock>();
	private List<BlockType> globalBlockTypes;
	
	private List<BlockType> columns = new ArrayList<BlockType>();
	private Map<BlockType, List<Integer>> columnsPerBlockType = new HashMap<BlockType, List<Integer>>();
	
	private AbstractSite[][] sites;
	
	
	public Circuit(FlexibleArchitecture architecture, String filename) {
		this.netparser = new NetParser(filename);
		this.architecture = architecture;
	}
	
	public void parse() {
		this.blocks = this.netparser.parse();
		
		this.addGlobalBlocks();
		this.calculateSizeAndColumns();
		this.createSites();
	}
	
	
	@SuppressWarnings("unchecked")
	private void addGlobalBlocks() {
		this.globalBlockTypes = BlockType.getGlobalBlockTypes();
		
		for(BlockType blockType : this.globalBlockTypes) {
			if(this.blocks.containsKey(blockType)) {
				this.globalBlockList.addAll((List<GlobalBlock>) (List<?>) this.blocks.get(blockType));
			}
		}
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
		
		this.columns.add(ioType);
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
			
			if(this.columns.size() < size) {
				this.columns.add(clbType);
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
		
		
		this.columns.add(ioType);
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
			this.sites[0][i] = new IOSite(0, i, ioType, ioCapacity);
			this.sites[size-1][size-1-i] = new IOSite(size-1, size-1-i, ioType, ioCapacity);
			this.sites[i][0] = new IOSite(i, 0, ioType, ioCapacity);
			this.sites[size-1-i][size-1] = new IOSite(size-1-i, size-1, ioType, ioCapacity);
		}
		
		for(int x = 1; x < this.columns.size() - 1; x++) {
			BlockType blockType = this.columns.get(x);
			
			int height = blockType.getHeight();
			for(int y = 1; y < size - height; y += height) {
				this.sites[x][y] = new Site(x, y, blockType);
			}
		}
	}
	
	
	
	public int getWidth() {
		return this.width;
	}
	public int getHeight() {
		return this.height;
	}
	
	public AbstractSite getSite(int x, int y) {
		return this.sites[x][y];
	}
	
	public List<AbstractBlock> getBlocks(BlockType blockType) {
		return this.blocks.get(blockType);
	}
	
	public GlobalBlock getRandomBlock(Random random) {
		int index = random.nextInt(this.globalBlockList.size());
		return this.globalBlockList.get(index);
	}
	
	public AbstractSite getRandomSite(GlobalBlock block, int distance, Random random) {
		
		if(distance < block.getType().getHeight() && distance < block.getType().getRepeat()) {
			return null;
		}
		
		AbstractSite site = block.getSite(); 
		int minX = Math.max(0, site.getX() - distance);
		int maxX = Math.min(this.width - 1, site.getX() + distance);
		int minY = Math.max(0, site.getY() - distance);
		int maxY = Math.min(this.height - 1, site.getY() + distance);
		
		while(true) {
			int x = random.nextInt(maxX - minX + 1) + minX;
			int y = random.nextInt(maxY - minY + 1) + minY;
			AbstractSite randomSite = this.getSite(x, y);
			if(block.getType().equals(randomSite.getType())) {
				return randomSite;
			}
		}
	}
	
	public List<GlobalPin> getGlobalPins(PortType portType) {
		List<GlobalPin> globalPins = new ArrayList<GlobalPin>();
		
		for(BlockType blockType : this.globalBlockTypes) {
			for(AbstractBlock block : this.getBlocks(blockType)) {
				Map<String, AbstractPin[]> allPins = block.getPins(portType);
				for(AbstractPin[] pins : allPins.values()) {
					for(int i = 0; i < pins.length; i++) {
						globalPins.add((GlobalPin) pins[i]);
					}
				}
			}
		}
		
		return globalPins;
	}
}
