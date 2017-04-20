package place.placers.analytical;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.architecture.BlockType;
import place.circuit.block.GlobalBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.Net;
import place.placers.analytical.AnalyticalAndGradientPlacer.NetBlock;
import place.placers.analytical.AnalyticalAndGradientPlacer.TimingNet;
import place.visual.PlacementVisualizer;

class SplittingLegalizer extends Legalizer {

    SplittingLegalizer(
            Circuit circuit,
            List<BlockType> blockTypes,
            List<Integer> blockTypeIndexStarts,
            double[] linearX,
            double[] linearY,
            int[] legalX,
            int[] legalY,
            int[] heights,
            PlacementVisualizer visualizer,
            List<Net> nets,
            List<TimingNet> timingNets,
            Map<GlobalBlock, NetBlock> netBlocks) throws IllegalArgumentException {

        super(circuit, blockTypes, blockTypeIndexStarts, linearX, linearY, legalX, legalY, heights, nets, timingNets, visualizer, netBlocks);
    }

    @Override
    protected void legalizeBlockType(int blocksStart, int blocksEnd) {
    	List<LegalizerBlock> blocks = new ArrayList<LegalizerBlock>();
    	for(int index = blocksStart; index < blocksEnd; index++) {
    		LegalizerBlock legalizerBlock = new LegalizerBlock(index, this.linearX[index], this.linearY[index]);
    		blocks.add(legalizerBlock);
    	}
    	
    	Area area = new Area(1,this.width + 1, 1, this.height + 1, blocks);
    	
    	this.legalize(area);
    }
    
    private void legalize(Area area){
    	if(!area.blocks.isEmpty()){
        	if(area.capacity() == 1){
        		LegalizerBlock legalizerBlock = area.blocks.get(0);
        		this.legalX[legalizerBlock.index] = area.left;
        		this.legalY[legalizerBlock.index] = area.bottom;
        	}else{
        		List<Area> splittedAreas = area.split();
        		for(Area newArea:splittedAreas){
        			legalize(newArea);
        		}
        	}
    	}
    }
    
    private class LegalizerBlock {
    	private int index;
    	private double x;
    	private double y;
    	
    	LegalizerBlock(int index, double x, double y){
    		this.index = index;
    		this.x = x;
    		this.y = y;
    	}
    	
        boolean inArea(Area area){
        	return (this.x >= area.left && this.x < area.right && this.y >= area.bottom && this.y < area.top);
        }
        
        @Override
        public String toString() {
            return String.format("[%.2f, %.2f]", this.x, this.y);
        }
    }
    
    private class Area {
    	int left, right, bottom, top;
        List<LegalizerBlock> blocks;
        private final double epsilon = 0.00001;

        Area(int left, int right, int bottom, int top, List<LegalizerBlock> blocks) {
            this.left = left;
            this.right = right;
            this.bottom = bottom;
            this.top = top;
            
            this.blocks = new ArrayList<LegalizerBlock>();
            
            for(LegalizerBlock block:blocks){
            	if(block.inArea(this)){
            		this.blocks.add(block);
            	}
            }
        }

        List<Area> split(){
        	if(this.width() > this.height()){
        		return this.splitHorizontal();
        	}else{
        		return this.splitVertical();
        	}
        }
        
        List<Area> splitHorizontal(){
        	int center = this.left + (int)Math.ceil((double)this.width()/2);
        	
        	Area areaLeft = new Area(this.left, center, this.bottom, this.top, this.blocks);
        	Area areaRight = new Area(center, this.right, this.bottom, this.top, this.blocks);
        	
        	while(areaLeft.utilisation() > 1){
        		LegalizerBlock block = areaLeft.getLastBlock(Comparators.HORIZONTAL);
        		block.x = areaRight.left;
        		areaLeft.blocks.remove(block);
        		areaRight.blocks.add(block);
        	}
        	while(areaRight.utilisation() > 1){
        		LegalizerBlock block = areaRight.getFirstBlock(Comparators.HORIZONTAL);
        		block.x = areaLeft.right - this.epsilon;
        		areaRight.blocks.remove(block);
        		areaLeft.blocks.add(block);
        	}
        	
        	List<Area> result = new ArrayList<Area>();
        	result.add(areaLeft);
        	result.add(areaRight);
        	
        	return result;
        	
        }
        List<Area> splitVertical(){
        	int center = this.bottom + (int)Math.ceil((double)this.height()/2);
        	Area areaBottom = new Area(this.left, this.right, this.bottom, center, this.blocks);
        	Area areaTop = new Area(this.left, this.right, center, this.top, this.blocks);
        	
        	while(areaBottom.utilisation() > 1){
        		LegalizerBlock block = areaBottom.getLastBlock(Comparators.VERTICAL);
        		block.y = areaTop.bottom;
        		areaBottom.blocks.remove(block);
        		areaTop.blocks.add(block);
        	}
        	while(areaTop.utilisation() > 1){
        		LegalizerBlock block = areaTop.getFirstBlock(Comparators.VERTICAL);
        		block.y = areaBottom.top - this.epsilon;
        		areaTop.blocks.remove(block);
        		areaBottom.blocks.add(block);
        	}
        	
        	List<Area> result = new ArrayList<Area>();
        	result.add(areaBottom);
        	result.add(areaTop);
        	
        	return result;
        }
        
        LegalizerBlock getFirstBlock(Comparator<LegalizerBlock> comparator){
        	LegalizerBlock firstBlock = this.blocks.get(0);
        	for(LegalizerBlock block: this.blocks){
        		if(comparator.compare(block, firstBlock) < 0){
        			firstBlock = block;
        		}
        	}
        	return firstBlock;
        }
        LegalizerBlock getLastBlock(Comparator<LegalizerBlock> comparator){
        	LegalizerBlock lastBlock = this.blocks.get(0);
        	for(LegalizerBlock block: this.blocks){
        		if(comparator.compare(block, lastBlock) > 0){
        			lastBlock = block;
        		}
        	}
        	return lastBlock;
        }
        
        
        int capacity(){
        	return this.area();
        }
        
        int occupation(){
        	return this.blocks.size();
        }
        
        double utilisation(){
        	return (double)this.occupation() / (double)this.capacity();
        }

        int width(){
        	return this.right - this.left;
        }
        int height(){
        	return this.top - this.bottom;
        }
        int area(){
        	return this.width() * this.height();
        }
        
        @Override
        public String toString() {
            return String.format("h: [%d, %d],\tv: [%d, %d]\t%d\t%d blocks", this.left, this.right, this.bottom, this.top, this.area(), this.blocks.size());
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
}