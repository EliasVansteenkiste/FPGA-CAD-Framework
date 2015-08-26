package placers.MDP;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import placers.Placer;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import circuit.Block;
import circuit.Clb;
import circuit.Input;
import circuit.PackedCircuit;
import circuit.Pin;
import architecture.Architecture;
import architecture.FourLutSanitized;
import architecture.HeterogeneousArchitecture;
import architecture.Site;
import architecture.SiteType;

public class MDPPlacer extends Placer {
	
	static {
		MDPPlacer.defaultOptions.put("stopRatio", "1.2");
	}
	
	private int width, height;
	private MDPPlacement placement;
	
	
	public MDPPlacer(FourLutSanitized architecture, PackedCircuit circuit, HashMap<String, String> options) {
		super(architecture, circuit, options);
		
		this.width = architecture.getWidth();
		this.height = architecture.getHeight();
		
		this.placement = new MDPPlacement(architecture, circuit);
	}
	
	
	
	public void place() {
		
		double totalCost = this.calculateTotalCost();
		double prevTotalCost;
		
		double stopRatio = Double.parseDouble(this.options.get("stopRatio"));
		
		
		do {
			this.reorderCells(Axis.X);
			this.reorderCells(Axis.Y);
			
			prevTotalCost = totalCost;
			totalCost = this.calculateTotalCost();
		} while(prevTotalCost / totalCost > stopRatio);
	}
	
	
	private double calculateTotalCost() {
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(this.circuit);
		return effcc.calculateTotalCost();
	}
	
	
	private void reorderCells(Axis axis) {
		int size;
		if(axis == Axis.X) {
			size = this.architecture.getHeight();
		} else {
			size = this.architecture.getWidth();
		}
		
		for(int slice = 0; slice < size; slice++) {
			reorderCellsInSlice(axis, slice);
		}
	}
	
	private void reorderCellsInSlice(Axis axis, int slice) {
		int size;
		if(axis == Axis.X) {
			size = this.architecture.getWidth();
		} else {
			size = this.architecture.getHeight();
		}
		
		
	}
}
