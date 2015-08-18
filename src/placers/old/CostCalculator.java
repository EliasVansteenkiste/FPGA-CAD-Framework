package placers.old;

import placers.SAPlacer.Swap;

public interface CostCalculator {
	
	public abstract double calculateTotalCost();
	
	public abstract double calculateAverageNetCost();

	public abstract double calculateDeltaCost(Swap swap);
	
//	public abstract void apply(Swap swap);

}