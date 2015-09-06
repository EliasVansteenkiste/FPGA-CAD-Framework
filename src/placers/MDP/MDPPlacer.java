package placers.MDP;

import java.util.HashMap;
import placers.Placer;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import circuit.PackedCircuit;
import architecture.FourLutSanitized;

public class MDPPlacer extends Placer {
	
	static {
		MDPPlacer.defaultOptions.put("stopRatio", "1.001");
	}
	
	private MDPPlacement placement;
	
	
	public MDPPlacer(FourLutSanitized architecture, PackedCircuit circuit, HashMap<String, String> options) {
		super(architecture, circuit, options);
		
		this.placement = new MDPPlacement(architecture, circuit);
	}
	
	
	public void place() {
		
		double totalCost = this.calculateTotalCost();
		double prevTotalCost;
		
		double stopRatio = Double.parseDouble(this.options.get("stopRatio"));
		
		
		do {
			this.reorderCells(Axis.X);
			this.reorderCells(Axis.Y);
			
			this.placement.updateOriginalBlocks();
			prevTotalCost = totalCost;
			totalCost = this.calculateTotalCost();
			System.out.println(prevTotalCost + ", " + totalCost);
		} while(prevTotalCost / totalCost > stopRatio || true);
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
