package placers;

public interface PlacementManipulator {

	public Swap findSwap(int Rlim);
	
	public Swap findSwapInCircuit();
	
	public void swap(Swap swap);
	
	public int maxFPGAdimension();

	public double numBlocks();
	
	public void PlacementCLBsConsistencyCheck();

}
