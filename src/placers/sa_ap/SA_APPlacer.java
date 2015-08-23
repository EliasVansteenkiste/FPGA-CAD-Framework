package placers.sa_ap;

import java.util.HashMap;
import java.util.Random;

import circuit.PackedCircuit;
import architecture.HeterogeneousArchitecture;
import placers.Placer;
import placers.Rplace;
import placers.SAPlacer.EfficientBoundingBoxNetCC;
import placers.SAPlacer.WLD_SAPlacer;
import placers.analyticalplacer.HeteroAnalyticalPlacerTwo;

public class SA_APPlacer extends Placer {
	
	private HeterogeneousArchitecture architecture;
	private PackedCircuit circuit;
	
	private long timerBegin, timerEnd;
	
	public SA_APPlacer(HeterogeneousArchitecture architecture, PackedCircuit circuit) {
		this.architecture = architecture;
		this.circuit = circuit;
	}
	
	public void place(HashMap<String, Object> options) {
		
		// Place the circuit with SA (low effort)
		Random rand = new Random(1);
		Rplace.placeCLBsandFixedIOs(circuit, architecture, rand);
		
		WLD_SAPlacer saPlacer = new WLD_SAPlacer(this.architecture, this.circuit);
		this.startTimer();
		//saPlacer.place(1);
		this.stopTimer();
		
		
		// Print WL and timing characteristics
		this.printStatistics("SA");
		
		
		// Place the circuit with Arno's analytical placer
		HashMap<String, String> apOptions = new HashMap<String, String>();
		apOptions.put("starting_stage", "1");
		
		HeteroAnalyticalPlacerTwo apPlacer = new HeteroAnalyticalPlacerTwo(this.architecture, this.circuit);
		this.startTimer();
		apPlacer.place(apOptions);
		this.stopTimer();
		
		// Print WL and timing characteristics (again)
		this.printStatistics("AP");
		
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
