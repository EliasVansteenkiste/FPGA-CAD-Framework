package placers.analyticalplacer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
				
				List<AbstractPin> pins = new ArrayList<AbstractPin>();
				pins.add(sourcePin);
				pins.addAll(sourcePin.getSinks());
				
				
				double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY,
						minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
				
				for(AbstractPin pin : pins) {
					GlobalBlock block = (GlobalBlock) pin.getOwner();
					
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
				
				cost += ((maxX - minX) + (maxY - minY) + 2) * AnalyticalPlacer.getWeight(pins.size());
			}
		}
		
		return cost;
	}
}
