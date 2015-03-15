package visual;

public class Range {
	double x;
	double y;
	double w;
	double h;

	public void setRange(double d, double e, double nodeWidth, double nodeWidth2) {
		this.x = d;
		this.y = e;
		this.w = nodeWidth;
		this.h = nodeWidth2;
	}

	public boolean inRange(double x2, double y2) {
		return (x2 >= x) && (x2 <= x + w) && (y2 >= y) && (y2 <= y + h);
	}

}
