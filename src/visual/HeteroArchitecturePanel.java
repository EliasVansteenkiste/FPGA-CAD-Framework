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
	private static final Color[] COLOR_ORDER_UNOCCUPIED = {new Color(0.80f,0.98f,0.80f), new Color(0.98f,0.80f,0.80f), 
												new Color(0.97f,0.98f,0.80f), new Color(0.80f,0.80f,0.98f), Color.LIGHT_GRAY}; //hb1,hb2,hb3,clb
	private static final Color[] COLOR_ORDER_OCCUPIED = {Color.GREEN, Color.RED, Color.YELLOW, Color.BLUE, Color.GRAY}; //hb1,hb2,hb3,clb
	
	private double zoom;
	private Map<Site, SiteData> siteData;
	private HeterogeneousArchitecture heteroArchitecture;
	private int mouseCurrentX;
	private int mouseCurrentY;
	private String curClbText;
	private Block[] criticalPath;
	
	public HeteroArchitecturePanel(int size, HeterogeneousArchitecture architecture)
	{
		this.heteroArchitecture = architecture;
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
		
		if(criticalPath != null)
		{
			g.setColor(Color.RED);
			for(int i = 0; i < criticalPath.length - 1; i++)
			{
				int fromX = siteData.get(criticalPath[i].getSite()).getCenterX(zoom);
				int fromY = siteData.get(criticalPath[i].getSite()).getCenterY(zoom);
				int toX = siteData.get(criticalPath[i+1].getSite()).getCenterX(zoom);
				int toY = siteData.get(criticalPath[i+1].getSite()).getCenterY(zoom);
				g.drawLine(fromX, fromY, toX, toY);
			}
		}
		
		g.setFont(new Font("Arial", Font.BOLD, 13));
		g.setColor(Color.BLUE);
		g.drawString(curClbText, 10, 905);
	}
	
	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(891, 910);
	}
	
	public void setCriticalPath(Block[] criticalPath)
	{
		this.criticalPath = criticalPath;
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
			if(siteX >= 1 && siteX <= heteroArchitecture.getWidth() && siteY >= 1 && siteY <= heteroArchitecture.getHeight())
			{
				Site site = heteroArchitecture.getSite(siteX, siteY, 0);
				Block block = site.block;
				if(site.type == architecture.SiteType.CLB)
				{
					if(block != null)
					{
						curClbText = String.format("CLB: (%d,%d) Name: %s", siteX, siteY, block.name);
					}
					else
					{
						curClbText = String.format("CLB: (%d,%d)", siteX, siteY);
					}
				}
				else //Must be a hardblock
				{
					HardBlockSite hbSite = (HardBlockSite)site;
					String typeName = hbSite.getTypeName().toUpperCase();
					if(block != null)
					{
						curClbText = String.format("%s: (%d,%d) Name: %s", typeName, siteX, siteY, block.name);
					}
					else
					{
						curClbText = String.format("%s: (%d,%d)", typeName, siteX, siteY);
					}
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
		String[] hardBlockTypeNames = heteroArchitecture.getHardBlockTypeNames();
		
		siteData = new HashMap<Site, SiteData>();
		for (Site site : heteroArchitecture.getSites())
		{
			switch(site.type)
			{
				case CLB:
					siteData.put(site, new SiteData(site,COLOR_ORDER_UNOCCUPIED[3],COLOR_ORDER_OCCUPIED[3]));
					break;
				case IO:
					siteData.put(site,  new SiteData(site,COLOR_ORDER_UNOCCUPIED[4],COLOR_ORDER_OCCUPIED[4]));
					break;
				case HARDBLOCK:
					String typeName = ((HardBlockSite)site).getTypeName();
					int index = -1;
					for(int i = 0; i < hardBlockTypeNames.length; i++)
					{
						if(typeName.contains(hardBlockTypeNames[i]))
						{
							index = i;
							break;
						}
					}
					if(index < 0)
					{
						System.err.println("Didn't find the site typename in the architecture!");
						index = 0;
					}
					if(index > 2)
					{
						System.err.println("Found more hardblock types than allowed!");
						index = 2;
					}
					siteData.put(site, new SiteData(site,COLOR_ORDER_UNOCCUPIED[index],COLOR_ORDER_OCCUPIED[index]));
					break;
			}
			
		}
		
		double tileWidth = CLB_WIDTH + INTER_CLB_SPACE;
		
		// Drawing the sites
		for (Site site : heteroArchitecture.getSites())
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
					if (site.x == heteroArchitecture.getWidth() + 1)
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
					if (site.y == heteroArchitecture.getHeight() + 1)
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
