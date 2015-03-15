package visual;

import java.awt.Color;
import java.awt.Graphics;

import architecture.Site;

public class SiteData {
	static double clbWidth = 1.0;
	static double ioWidth = 0.45;

	double x;
	double y;

	Site site;

	SiteType type;

	public SiteData(Site s) {
		site = s;
	}

	public void setPossition(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void setType(SiteType t) {
		type = t;
	}

	public void draw(Graphics g, double zoom) {
		int xCoord;
		int yCoord;
		int width;
		int hight;

		if (site.block != null)
			g.setColor(Color.BLUE);
		else
			g.setColor(Color.GRAY);

		switch (type) {
		case CLB:
			xCoord = (int) ((x - clbWidth / 2.0) * zoom);
			yCoord = (int) ((y - clbWidth / 2.0) * zoom);
			width = (int) (clbWidth * zoom);
			hight = (int) (clbWidth * zoom);

			g.drawRect(xCoord, yCoord, width, hight);
			break;
		case IO_LEFT:
		case IO_RIGHT:
			xCoord = (int) ((x - clbWidth / 2.0) * zoom);
			yCoord = (int) ((y - ioWidth / 2.0) * zoom);
			width = (int) (clbWidth * zoom);
			hight = (int) (ioWidth * zoom);

			g.drawRect(xCoord, yCoord, width, hight);
			break;
		case IO_UP:
		case IO_DOWN:
			xCoord = (int) ((x - ioWidth / 2.0) * zoom);
			yCoord = (int) ((y - clbWidth / 2.0) * zoom);
			width = (int) (ioWidth * zoom);
			hight = (int) (clbWidth * zoom);

			g.drawRect(xCoord, yCoord, width, hight);
			break;
		default:
			break;
		}

	}
}
