package placers.analyticalplacer;

public class LegalizerTwo implements Legalizer
{

	private final double UTILIZATION_FACTOR = 0.9;
	
	private int minimalX;
	private int maximalX;
	private int minimalY;
	private int maximalY;
	
	public LegalizerTwo(int minimalX, int maximalX, int minimalY, int maximalY, int nbMovableBlocks)
	{
		this.minimalX = minimalX;
		this.maximalX = maximalX;
		this.minimalY = minimalY;
		this.maximalY = maximalY;
	}
	
}
