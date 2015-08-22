package placers.MDP;

import circuit.PackedCircuit;
import architecture.Architecture;
import architecture.HeterogeneousArchitecture;

public class MDPPlacer {
	
	private Architecture architecture;
	private PackedCircuit circuit;
	
	public MDPPlacer(HeterogeneousArchitecture architecture, PackedCircuit circuit) {
		this.architecture = architecture;
		this.circuit = circuit;
	}
	
	public void place() {
		
	}
}
