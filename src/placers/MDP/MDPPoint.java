package placers.MDP;

import java.awt.Point;

public class MDPPoint extends Point {
	
	public MDPPoint() {
		super(0, 0);
	}
	
	public MDPPoint(int x, int y) {
		super(x, y);
	}
	
	public MDPPoint(MDPPoint point) {
		super(point.x, point.y);
	}
	
	public int get(Axis axis) {
		if(axis == Axis.X) {
			return this.x;
		} else {
			return this.y;
		}
	}
	
	public int set(Axis axis, int position) {
		int oldPosition;
		if(axis == Axis.X) {
			oldPosition = this.x;
			this.x = position;
		} else {
			oldPosition = this.y;
			this.y = position;
		}
		
		return oldPosition;
	}
}
