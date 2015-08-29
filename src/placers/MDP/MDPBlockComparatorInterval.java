package placers.MDP;

import java.util.Comparator;

public class MDPBlockComparatorInterval implements Comparator<MDPBlock> {
	
	public int compare(MDPBlock b1, MDPBlock b2) {
		int c = Integer.compare(b1.optimalInterval[1], b2.optimalInterval[1]);
		if(c == 0) {
			return -Integer.compare(b1.optimalInterval[0], b2.optimalInterval[0]);
		} else {
			return c;
		}
	}
}
