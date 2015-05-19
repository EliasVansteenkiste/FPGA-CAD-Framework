package visual;

import java.awt.Color;
import java.awt.Graphics;

import architecture.Site;

public class SiteData {
	
	private static final double CLB_WIDTH = 1.0;
	private static final double IO_WIDTH = 0.45;

	private double x;
	private double y;
	private Site site;
	private SiteType type;

	public SiteData(Site s)
	{
		site = s;
	}

	public void setPosition(double x, double y)
	{
		this.x = x;
		this.y = y;
	}

	public void setType(SiteType t)
	{
		type = t;
	}

	public void draw(Graphics g, double zoom)
	{
		int xCoord;
		int yCoord;
		int width;
		int hight;

		if (site.block != null)
		{
			g.setColor(Color.BLUE);
		}
		else
		{
			g.setColor(Color.GRAY);
		}

		switch (type)
		{
			case CLB:
			case HARDBLOCK:
				xCoord = (int) ((x - CLB_WIDTH / 2.0) * zoom);
				yCoord = (int) ((y - CLB_WIDTH / 2.0) * zoom);
				width = (int) (CLB_WIDTH * zoom);
				hight = (int) (CLB_WIDTH * zoom);
				g.drawRect(xCoord, yCoord, width, hight);
				break;
			case IO_LEFT:
			case IO_RIGHT:
				xCoord = (int) ((x - CLB_WIDTH / 2.0) * zoom);
				yCoord = (int) ((y - IO_WIDTH / 2.0) * zoom);
				width = (int) (CLB_WIDTH * zoom);
				hight = (int) (IO_WIDTH * zoom);
				g.drawRect(xCoord, yCoord, width, hight);
				break;
			case IO_UP:
			case IO_DOWN:
				xCoord = (int) ((x - IO_WIDTH / 2.0) * zoom);
				yCoord = (int) ((y - CLB_WIDTH / 2.0) * zoom);
				width = (int) (IO_WIDTH * zoom);
				hight = (int) (CLB_WIDTH * zoom);
				g.drawRect(xCoord, yCoord, width, hight);
				break;
			default:
				break;
		}
	}
}
