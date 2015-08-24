package placers.MDP;

import java.util.HashMap;

import circuit.PackedCircuit;
import architecture.Architecture;
import architecture.HeterogeneousArchitecture;
import placers.Placer;
import placers.analyticalplacer.HeteroAnalyticalPlacerTwo;

public class MDPBasedPlacer extends Placer {

	private Architecture architecture;
	private PackedCircuit circuit;


	public MDPBasedPlacer(HeterogeneousArchitecture architecture, PackedCircuit circuit) {
		this.architecture = architecture;
		this.circuit = circuit;
	}
	
	@Override
	public void place(HashMap<String, String> options) {
		/* options: welke keys? */

		// Do a global placement using analytical placement
		HeteroAnalyticalPlacerTwo globalPlacer = new HeteroAnalyticalPlacerTwo((HeterogeneousArchitecture) architecture, circuit);
		globalPlacer.place();

		// Do a detailed placement using MDP
		MDPPlacer detailedPlacer = new MDPPlacer((HeterogeneousArchitecture) architecture, circuit);
		detailedPlacer.place();
	}
}
