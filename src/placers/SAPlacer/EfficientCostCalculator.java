package placers.SAPlacer;

import placers.SAPlacer.Swap;

public interface EfficientCostCalculator
{
	
	public abstract double calculateTotalCost();
	
	public abstract double calculateAverageNetCost();

	public abstract double calculateDeltaCost(Swap swap);
	
	public abstract void recalculateFromScratch();
	
	public abstract void revert();
	
	public abstract void pushThrough();
	
}
