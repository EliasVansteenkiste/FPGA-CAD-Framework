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

import circuit.Block;

import architecture.ClbSite;
import architecture.HardBlockSite;
import architecture.HeterogeneousArchitecture;
import architecture.IoSite;
import architecture.Site;

public class HeteroArchitecturePanel extends JPanel implements MouseMotionListener
{
	
	private static final long serialVersionUID = 1L;
	private static final double CLB_WIDTH = 1.0;
	private static final double IO_WIDTH = 0.45;
	private static final double INTER_CLB_SPACE = 0.10;
	
	private double zoom;
	private Map<Site, SiteData> siteData;
	private HeterogeneousArchitecture architecture;
	private int mouseCurrentX;
	private int mouseCurrentY;
	String curClbText;
	
	public HeteroArchitecturePanel(int size, HeterogeneousArchitecture architecture)
	{
		this.architecture = architecture;
		this.zoom = size / ((architecture.getWidth() + 2) * (CLB_WIDTH + INTER_CLB_SPACE) - INTER_CLB_SPACE);
		buildData();
		addMouseMotionListener(this);
		mouseCurrentX = 0;
		mouseCurrentY = 0;
		curClbText = "";
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		setSize(getPreferredSize());

		for (SiteData data : siteData.values())
		{
			data.draw(g, zoom);
		}
		
		g.setFont(new Font("Arial", Font.BOLD, 13));
		g.setColor(Color.BLUE);
		g.drawString("CLB:", 10, 905);
		g.drawString(curClbText, 50, 905);
	}
	
	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(891, 910);
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
			if(siteX >= 1 && siteX <= architecture.getWidth() && siteY >= 1 && siteY <= architecture.getHeight())
			{
				Block block = architecture.getSite(siteX, siteY, 0).block;
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
	
	private void buildData()
	{
		siteData = new HashMap<Site, SiteData>();
		for (Site site : architecture.getSites())
		{
			siteData.put(site, new SiteData(site));
		}
		
		double tileWidth = CLB_WIDTH + INTER_CLB_SPACE;
		
		// Drawing the sites
		for (Site site : architecture.getSites())
		{
			switch (site.type)
			{
				case IO:
					if (site.x == 0)
					{
						if (site.n == 0)
						{
							double x = site.x * tileWidth + CLB_WIDTH / 2.0;
							double y = site.y * tileWidth + IO_WIDTH / 2.0;
							drawLeftIoSite(x, y, site);
						}
						else
						{
							double x = site.x * tileWidth + CLB_WIDTH / 2.0;
							double y = site.y * tileWidth + CLB_WIDTH - IO_WIDTH + IO_WIDTH / 2.0;
							drawLeftIoSite(x, y, site);
						}
					}
					if (site.x == architecture.getWidth() + 1)
					{
						if (site.n == 0)
						{
							double x = site.x * tileWidth + CLB_WIDTH / 2.0;
							double y = site.y * tileWidth + IO_WIDTH / 2.0;
							drawRightIoSite(x, y, site);
						}
						else
						{
							double x = site.x * tileWidth + CLB_WIDTH / 2.0;
							double y = site.y * tileWidth + CLB_WIDTH - IO_WIDTH + IO_WIDTH / 2.0;
							drawRightIoSite(x, y, site);
						}
					}
					if (site.y == 0)
					{
						if (site.n == 0)
						{
							double x = site.x * tileWidth + IO_WIDTH / 2.0;
							double y = site.y * tileWidth + CLB_WIDTH / 2.0;
							drawUpIoSite(x, y, site);
						}
						else
						{
							double x = site.x * tileWidth + IO_WIDTH / 2.0 + CLB_WIDTH - IO_WIDTH;
							double y = site.y * tileWidth + CLB_WIDTH / 2.0;
							drawUpIoSite(x, y, site);
						}
					}
					if (site.y == architecture.getHeight() + 1)
					{
						if (site.n == 0)
						{
							double x = site.x * tileWidth + IO_WIDTH / 2.0;
							double y = site.y * tileWidth + CLB_WIDTH / 2.0;
							drawDownIoSite(x, y, site);
						}
						else
						{
							double x = site.x * tileWidth + IO_WIDTH / 2.0 + CLB_WIDTH - IO_WIDTH;
							double y = site.y * tileWidth + CLB_WIDTH / 2.0;
							drawDownIoSite(x, y, site);
						}
					}
					break;
				case CLB:
					double xclb = site.x * tileWidth + CLB_WIDTH / 2.0;
					double yclb = site.y * tileWidth + CLB_WIDTH / 2.0;
					drawClbSite(xclb, yclb, site);
					break;
				case HARDBLOCK:
					double xhb = site.x * tileWidth + CLB_WIDTH / 2.0;
					double yhb = site.y * tileWidth + CLB_WIDTH / 2.0;
					drawHardBlockSite(xhb, yhb, site);
					break;
				default:
					break;
			}
		}
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
	
	private void drawClbSite(double x, double y, Site s)
	{
		ClbSite site = (ClbSite) s;
		SiteData data = siteData.get(site);
		data.setType(SiteType.CLB);
		data.setPosition(x, y);
	}
	
	private void drawHardBlockSite(double x, double y, Site s)
	{
		HardBlockSite site = (HardBlockSite) s;
		SiteData data = siteData.get(site);
		data.setType(SiteType.HARDBLOCK);
		data.setPosition(x,y);
	}
	
}
