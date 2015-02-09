package Visual;

//import java.awt.Color;
//import java.awt.Graphics;
import java.awt.*;
import javax.swing.*;

import architecture.RouteNode;

public class WireData extends RouteNodeData {

	double length;

	double x1;
	double y1;
	double x2;
	double y2;

	public WireData(RouteNode node) {
		super(node);

	}

	@Override
	public void draw(Graphics g, double zoom) {
		g.setColor(color);

		// g.drawLine((int)(zoom * x1), (int)(zoom * y1), (int)(zoom * x2),
		// (int)(zoom * y2));
		Graphics2D g2 = (Graphics2D) g;
		g2.setStroke(new BasicStroke(3));
		g.drawLine((int) (zoom * x1), (int) (zoom * y1), (int) (zoom * x2),
				(int) (zoom * y2)); // thick
	}

	public void setCoords(double x1, double y1, double x2, double y2) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}

}
