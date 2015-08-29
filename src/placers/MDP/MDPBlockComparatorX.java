package placers.MDP;

import java.util.Comparator;

public class MDPBlockComparatorX implements Comparator<MDPBlock> {
	
	@Override
	public int compare(MDPBlock b1, MDPBlock b2) {
		return b1.coor.x - b2.coor.x;
	}
}
