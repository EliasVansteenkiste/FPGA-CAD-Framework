package placers.MDP;

import java.util.HashMap;

import placers.Placer;
import circuit.PackedCircuit;
import architecture.Architecture;
import architecture.HeterogeneousArchitecture;

public class MDPPlacer extends Placer {
	
	private Architecture architecture;
	private PackedCircuit circuit;
	
	public MDPPlacer(HeterogeneousArchitecture architecture, PackedCircuit circuit) {
		this.architecture = architecture;
		this.circuit = circuit;
	}
	
	public int place(HashMap<String, String> options) {
		return 0;
	}
}
