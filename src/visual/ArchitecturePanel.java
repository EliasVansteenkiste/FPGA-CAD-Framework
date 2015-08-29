package visual;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import architecture.ClbSite;
import architecture.FourLutSanitized;
import architecture.IoSite;
import architecture.RouteNode;
import architecture.Site;
import circuit.Block;
import circuit.Net;
import circuit.Connection;

public class ArchitecturePanel extends JPanel implements MouseMotionListener
{

	private static final long serialVersionUID = 1L;
	static final double CLB_WIDTH = 1.0;
	static final double IO_WIDTH = 0.45;
	private static final double INTER_CLB_SPACE = 0.10;

	private double zoom;

	private Map<RouteNode, RouteNodeData> routeNodeData;
	private Map<Site, SiteData> siteData;

	FourLutSanitized a;
	
	private int mouseCurrentX;
	private int mouseCurrentY;
	String curClbText;

	public ArchitecturePanel(int size, FourLutSanitized a, boolean drawRouteNodes)
	{
		this.a = a;

		zoom = size
				/ ((a.getWidth() + 2)
						* (CLB_WIDTH + INTER_CLB_SPACE) - INTER_CLB_SPACE);

		buildData();

		// setBorder(BorderFactory.createLineBorder(Color.black));
		
		addMouseMotionListener(this);
		mouseCurrentX = 0;
		mouseCurrentY = 0;
		curClbText = "";
	}

	public void setNodeColor(Connection con, Color color)
	{
		for (RouteNode node : con.routeNodes)
		{
			RouteNodeData data = routeNodeData.get(node);
			data.setColor(color);
		}
	}

	public void setNodeColor(Color color)
	{
		for (RouteNodeData data : routeNodeData.values())
		{
			data.setColor(color);
		}
	}

	public void setNodeColor(Net net, Color color)
	{
		for (RouteNode node : net.routeNodes)
		{
			RouteNodeData data = routeNodeData.get(node);
			data.setColor(color);
		}
	}

	public RouteNode getNode(int x, int y)
	{
		for (RouteNodeData data : routeNodeData.values())
		{
			if (data.inClickRange(x, y))
			{
				return data.node;
			}
		}
		return null;
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(891, 910);
	}

	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		setSize(getPreferredSize());

		for (RouteNodeData data : routeNodeData.values())
		{
			data.draw(g, zoom);
		}

		for (SiteData data : siteData.values())
		{
			data.draw(g, zoom);
		}
		
		g.setFont(new Font("Arial", Font.BOLD, 13));
		g.setColor(Color.BLUE);
		g.drawString("CLB:", 10, 905);
		g.drawString(curClbText, 50, 905);
	}

	private void buildData()
	{
		siteData = new HashMap<Site, SiteData>();
		for (Site site : a.getSites())
		{
			siteData.put(site, new SiteData(site, Color.LIGHT_GRAY, Color.GRAY));
		}

		double tileWidth = CLB_WIDTH + INTER_CLB_SPACE;

		// Drawing the sites
		for (Site site : a.getSites())
		{
			switch (site.getType())
			{
			case IO:
				if (site.getX() == 0)
				{
					if (site.getZ() == 0)
					{
						double x = site.getX() * tileWidth + CLB_WIDTH / 2.0;
						double y = site.getY() * tileWidth + IO_WIDTH / 2.0;
						drawLeftIoSite(x, y, site);
					} else
					{
						double x = site.getX() * tileWidth + CLB_WIDTH / 2.0;
						double y = site.getY() * tileWidth + CLB_WIDTH - IO_WIDTH
								+ IO_WIDTH / 2.0;
						drawLeftIoSite(x, y, site);
					}
				}
				if (site.getX() == a.getWidth() + 1)
				{
					if (site.getZ() == 0)
					{
						double x = site.getX() * tileWidth + CLB_WIDTH / 2.0;
						double y = site.getY() * tileWidth + IO_WIDTH / 2.0;
						drawRightIoSite(x, y, site);
					} else
					{
						double x = site.getX() * tileWidth + CLB_WIDTH / 2.0;
						double y = site.getY() * tileWidth + CLB_WIDTH - IO_WIDTH
								+ IO_WIDTH / 2.0;
						drawRightIoSite(x, y, site);
					}
				}
				if (site.getY() == 0)
				{
					if (site.getZ() == 0)
					{
						double x = site.getX() * tileWidth + IO_WIDTH / 2.0;
						double y = site.getY() * tileWidth + CLB_WIDTH / 2.0;
						drawUpIoSite(x, y, site);
					} else
					{
						double x = site.getX() * tileWidth + IO_WIDTH / 2.0
								+ CLB_WIDTH - IO_WIDTH;
						double y = site.getY() * tileWidth + CLB_WIDTH / 2.0;
						drawUpIoSite(x, y, site);
					}
				}
				if (site.getY() == a.getHeight() + 1)
				{
					if (site.getZ() == 0)
					{
						double x = site.getX() * tileWidth + IO_WIDTH / 2.0;
						double y = site.getY() * tileWidth + CLB_WIDTH / 2.0;
						drawDownIoSite(x, y, site);
					} else
					{
						double x = site.getX() * tileWidth + IO_WIDTH / 2.0
								+ CLB_WIDTH - IO_WIDTH;
						double y = site.getY() * tileWidth + CLB_WIDTH / 2.0;
						drawDownIoSite(x, y, site);
					}
				}
				break;
			case CLB:
				double x = site.getX() * tileWidth + CLB_WIDTH / 2.0;
				double y = site.getY() * tileWidth + CLB_WIDTH / 2.0;
				drawClbSite(x, y, 0, site);

				break;
			default:
				break;

			}
		}
	}

	private void drawClbSite(double x, double y, double angle, Site s)
	{
		ClbSite site = (ClbSite) s;

		SiteData data = siteData.get(site);
		data.setType(SiteType.CLB);
		data.setPosition(x, y);
	}

	private void drawLeftIoSite(double x, double y, Site s)
	{
		IoSite site = (IoSite) s;

		SiteData data = siteData.get(site);
		data.setType(SiteType.IO_LEFT);
		data.setPosition(x, y);
	}

	private void drawRightIoSite(double x, double y, Site s)
	{
		IoSite site = (IoSite) s;

		SiteData data = siteData.get(site);
		data.setType(SiteType.IO_RIGHT);
		data.setPosition(x, y);
	}

	private void drawUpIoSite(double x, double y, Site s)
	{
		IoSite site = (IoSite) s;

		SiteData data = siteData.get(site);
		data.setType(SiteType.IO_UP);
		data.setPosition(x, y);
	}

	private void drawDownIoSite(double x, double y, Site s)
	{
		IoSite site = (IoSite) s;

		SiteData data = siteData.get(site);
		data.setType(SiteType.IO_DOWN);
		data.setPosition(x, y);
	}
	
	@Override
	public void mouseDragged(MouseEvent e)
	{
		
	}
	
	@Override
	public void mouseMoved(MouseEvent e)
	{
		int xPos = e.getX();
		int yPos = e.getY();
		
		double tileWidth = CLB_WIDTH + INTER_CLB_SPACE;
		int siteX = (int)((double)xPos / tileWidth / zoom);
		int siteY = (int)((double)yPos / tileWidth / zoom);
		
		if(siteX != mouseCurrentX || siteY != mouseCurrentY)
		{
			//System.out.println("X: " + siteX + ", Y: " + siteY);
			mouseCurrentX = siteX;
			mouseCurrentY = siteY;
			if(siteX >= 1 && siteX <= a.getWidth() && siteY >= 1 && siteY <= a.getHeight())
			{
				Block block = a.getSite(siteX, siteY, 0).getBlock();
				if(block != null)
				{
					curClbText = String.format("(%d,%d) Name: %s", siteX, siteY, block.name);
				}
				else
				{
					curClbText = String.format("(%d,%d)", siteX, siteY);
				}
			}
			else
			{
				curClbText = String.format("(%d,%d)", siteX, siteY);
			}
			repaint(5, 893, 875, 16);
		}
	}

}
