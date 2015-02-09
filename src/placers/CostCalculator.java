package placers;

//import circuit.Block;
import circuit.Circuit;

public interface CostCalculator {
	
	public abstract Circuit getCircuit();

	public abstract double calculateTotalCost();

	public abstract double averageNetCost();
	
//	public abstract double calculateAverageNetCost();

	public abstract double calculateDeltaCost(Swap swap);

//	public abstract double calculateDeltaCostOld(Swap swap);
	
	public abstract void apply(Swap swap);
	
//	public abstract void printRoutingBlocks(Block b);

}