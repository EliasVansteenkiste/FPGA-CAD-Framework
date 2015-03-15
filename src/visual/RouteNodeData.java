package visual;

import java.awt.Color;
import java.awt.Graphics;

import architecture.RouteNode;

public abstract class RouteNodeData {
	protected static double nodeWidth = 0.15;

	protected Range clickRange;

	protected Boolean selected;

	RouteNode node;

	Color color;

	public RouteNodeData(RouteNode node) {
		super();
		clickRange = new Range();
		this.node = node;

		color = Color.GRAY;
		selected = false;
	}

	public boolean inClickRange(double x, double y) {
		return clickRange.inRange(x, y);
	}

	public void deselect() {
		selected = false;
	}

	public void select() {
		selected = true;
	}

	public abstract void draw(Graphics g, double zoom);

	public void setColor(Color color) {
		this.color = color;
	}

}
