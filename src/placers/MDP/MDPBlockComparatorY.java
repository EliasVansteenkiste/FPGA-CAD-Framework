package placers.MDP;

import java.util.Comparator;

public class MDPBlockComparatorY implements Comparator<MDPBlock> {
	
	@Override
	public int compare(MDPBlock b1, MDPBlock b2) {
		return b1.coor.y - b2.coor.y;
	}
}
