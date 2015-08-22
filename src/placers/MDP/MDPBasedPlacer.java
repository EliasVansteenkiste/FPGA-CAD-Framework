package placers.MDP;

import circuit.PackedCircuit;
import architecture.Architecture;
import architecture.FourLutSanitized;
import placers.Placer;
import placers.analyticalplacer.AnalyticalPlacerFive;

public class MDPBasedPlacer extends Placer {

	private Architecture architecture;
	private PackedCircuit circuit;


	public MDPBasedPlacer(FourLutSanitized architecture, PackedCircuit circuit) {
		this.architecture = architecture;
		this.circuit = circuit;
	}


	@Override
	public void place() {
		/* options: welke keys? */

		// Do a global placement using analytical placement
		int legalizer = 3;
		AnalyticalPlacerFive globalPlacer = new AnalyticalPlacerFive((FourLutSanitized) architecture, circuit, legalizer);
		globalPlacer.place();

		// Do a detailed placement using MDP
		MDPPlacer detailedPlacer = new MDPPlacer((FourLutSanitized) architecture, circuit);
		detailedPlacer.place();
	}
}
