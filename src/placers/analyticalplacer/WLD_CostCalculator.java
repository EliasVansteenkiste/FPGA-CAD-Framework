package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import architecture.BlockType.BlockCategory;
import architecture.circuit.Circuit;
import architecture.circuit.block.GlobalBlock;
import architecture.circuit.pin.AbstractPin;


public class WLD_CostCalculator extends CostCalculator {
	
	private Circuit circuit;
	private Map<GlobalBlock, Integer> blockIndexes;
	
	WLD_CostCalculator(Circuit circuit, Map<GlobalBlock, Integer> blockIndexes) {
		this.circuit = circuit;
		this.blockIndexes = blockIndexes;
	}
	
	@Override
	boolean requiresCircuitUpdate() {
		return false;
	}
	
	@Override
	protected double calculate() {
		double cost = 0.0;
		
		for(GlobalBlock sourceBlock : this.circuit.getGlobalBlocks()) {
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
						
					} else {
						int index = this.blockIndexes.get(block);
						x = this.getX(index);
						y = this.getY(index);
					}
					
					
					if(x < minX) {
						minX = x;
					}
					if(x > maxX) {
						maxX = x;
					}
					
					if(y < minY) {
						minY = y;
					}
					if(y > maxY) {
						maxY = y;
					}
				}
				
				double weight = AnalyticalPlacer.getWeight(netBlocks.size());
				cost += ((maxX - minX) + (maxY - minY) + 2) * weight;
			}
		}
		
		return cost;
	}
}
