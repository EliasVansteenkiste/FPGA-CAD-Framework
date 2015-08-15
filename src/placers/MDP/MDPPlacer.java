package placers.MDP;

import circuit.PackedCircuit;
import architecture.Architecture;
import architecture.FourLutSanitized;

public class MDPPlacer {
	
	private Architecture architecture;
	private PackedCircuit circuit;
	
	public MDPPlacer(FourLutSanitized architecture, PackedCircuit circuit) {
		this.architecture = architecture;
		this.circuit = circuit;
	}
	
	public void place() {
		
	}
}
