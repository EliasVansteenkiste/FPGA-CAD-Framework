package placers.cascaded;

import java.util.HashMap;
import java.util.Random;

import circuit.PackedCircuit;
import architecture.HeterogeneousArchitecture;
import placers.Placer;
import placers.Rplace;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import placers.SAPlacer.WLD_SAPlacer;
import placers.analyticalplacer.HeteroAnalyticalPlacerTwo;

public class CascadedPlacer extends Placer {
	
	private HeterogeneousArchitecture architecture;
	private PackedCircuit circuit;
	
	private long timerBegin, timerEnd;
	
	public CascadedPlacer(HeterogeneousArchitecture architecture, PackedCircuit circuit) {
		this.architecture = architecture;
		this.circuit = circuit;
	}
	
	public void place(HashMap<String, String> options) {
		
		// Initialize with random placement
		Random rand = new Random(1);
		Rplace.placeCLBsandFixedIOs(circuit, architecture, rand);
		
		// Print WL and timing characteristics before placement
		this.printStatistics("before placement");
		
		
		
		// Place the circuit with SA (low effort)
		WLD_SAPlacer saPlacer = new WLD_SAPlacer(this.architecture, this.circuit);
		this.startTimer();
		saPlacer.place(1);
		this.stopTimer();
		
		// Print WL and timing characteristics
		this.printStatistics("after SA");
		
		
		
		// Place the circuit with Arno's analytical placer
		options.put("starting_stage", "1");
		
		HeteroAnalyticalPlacerTwo apPlacer = new HeteroAnalyticalPlacerTwo(this.architecture, this.circuit);
		this.startTimer();
		apPlacer.place(options);
		this.stopTimer();
		
		// Print WL and timing characteristics
		this.printStatistics("after AP");
		
	}
	
	private void startTimer() {
		this.timerBegin = System.nanoTime();
	}
	private void stopTimer() {
		this.timerEnd = System.nanoTime();
	}
	private double getTimer() {
		return (this.timerEnd - this.timerBegin) * 1e-12;
	}
	
	private void printStatistics(String prefix) {
		
		System.out.println();
		double placeTime = this.getTimer();
		System.out.format("%s %15s: %fs\n", prefix, "place time", placeTime);
		
		EfficientBoundingBoxNetCC effcc = new EfficientBoundingBoxNetCC(this.circuit);
		double totalCost = effcc.calculateTotalCost();
		System.out.format("%s %15s: %f\n", prefix, "total cost", totalCost);
	}
}
