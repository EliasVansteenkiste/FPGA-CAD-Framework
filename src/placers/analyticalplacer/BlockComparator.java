package placers.analyticalplacer;

import java.util.Comparator;

public class BlockComparator implements Comparator<Integer> {
	
	private double[] coordinates;
	
	public BlockComparator(double[] coordinates) {
		this.coordinates = coordinates;
	}
	
	@Override
	public int compare(Integer index1, Integer index2) {
		return new Double(this.coordinates[index1]).compareTo(this.coordinates[index2]);
	}
}
