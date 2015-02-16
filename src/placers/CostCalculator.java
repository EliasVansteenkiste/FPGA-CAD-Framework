package placers;

//import circuit.Block;
import circuit.PackedCircuit;

public interface CostCalculator {
	
	public abstract PackedCircuit getCircuit();

	public abstract double calculateTotalCost();

	public abstract double averageNetCost();
	
//	public abstract double calculateAverageNetCost();

	public abstract double calculateDeltaCost(Swap swap);

//	public abstract double calculateDeltaCostOld(Swap swap);
	
	public abstract void apply(Swap swap);
	
//	public abstract void printRoutingBlocks(Block b);

}