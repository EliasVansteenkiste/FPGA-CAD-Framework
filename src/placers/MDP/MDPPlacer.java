package placers.MDP;

import java.util.HashMap;
import java.util.Map;

import placers.Placer;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import circuit.PackedCircuit;
import architecture.FourLutSanitized;

public class MDPPlacer extends Placer {
	
	static {
		MDPPlacer.defaultOptions.put("stopRatio", "1.0");
	}
	
	private MDPPlacement placement;
	
	
	public MDPPlacer(FourLutSanitized architecture, PackedCircuit circuit, Map<String, String> options) {
		super(architecture, circuit, options);
		
		this.placement = new MDPPlacement(architecture, circuit);
	}
	
	
	public void place() {
		
		double totalCost = this.calculateTotalCost();
		double prevTotalCost;
		
		double stopRatio = Double.parseDouble(this.options.get("stopRatio"));
		
		int iteration = 0;
		
		do {
			this.reorderCells(Axis.X);
			this.reorderCells(Axis.Y);
			
			this.placement.updateOriginalBlocks();
			prevTotalCost = totalCost;
			totalCost = this.calculateTotalCost();
			
			iteration++;
			System.out.format("Iteration %d: %f\n", iteration, totalCost);
			
		} while(prevTotalCost / totalCost > stopRatio || iteration <= 5);
	}
	
	
	private double calculateTotalCost() {
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(this.circuit);
		return effcc.calculateTotalCost();
	}
	
	
	private void reorderCells(Axis axis) {
		int size = this.placement.getSize(axis);
		
		for(int slice = 1; slice <= size; slice++) {
			this.placement.reorderSlice(axis, slice);
		}
	}
}
