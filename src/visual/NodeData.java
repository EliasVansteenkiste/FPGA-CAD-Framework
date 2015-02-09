package Visual;

import java.awt.Color;
import java.awt.Graphics;

import architecture.RouteNode;

public class NodeData extends RouteNodeData {

	protected double x;
	protected double y;

	public NodeData(RouteNode node) {
		super(node);
	}

	public void draw(Graphics g, double zoom) {

		g.setColor(color);

		int yCoord = (int) (zoom * (y - nodeWidth / 2));
		int xCoord = (int) (zoom * (x - nodeWidth / 2));
		int width = (int) (zoom * (nodeWidth));
		int hight = (int) (zoom * (nodeWidth));

		g.fillOval(xCoord, yCoord, width, hight);

		clickRange.setRange(xCoord, yCoord, width, hight);
	}

	public void setPossition(double x, double y) {
		this.x = x;
		this.y = y;

	}

}
