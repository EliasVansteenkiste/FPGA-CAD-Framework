package Visual;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import architecture.ClbSite;
import architecture.FourLutSanitized;
import architecture.IoSite;
import architecture.RouteNode;
import architecture.Site;
import circuit.Circuit;
import circuit.Net;
import circuit.Pattern;
import circuit.Tcon;
import circuit.Connection;

public class ArchitecturePanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static double clbWidth = 1.0;
	static double ioWidth = 0.45;
	static double wireSpace = 0.05;
	static double nodeWidth = 0.08;

	private double zoom;

	private Map<RouteNode, RouteNodeData> routeNodeData;
	private Map<Site, SiteData> siteData;

	FourLutSanitized a;
	Circuit c;

	public ArchitecturePanel(int size, FourLutSanitized a, final Circuit c) {
		this.a = a;

		zoom = size
				/ ((a.width + 2)
						* (clbWidth + (a.channelWidth + 1) * wireSpace) - (a.channelWidth + 1)
						* wireSpace);

		buildRouteNodeData();

		// setBorder(BorderFactory.createLineBorder(Color.black));

	}

	public void setNodeColor(Tcon tcon, Color color) {
		for (RouteNode node : tcon.routeNodes) {
			RouteNodeData data = routeNodeData.get(node);
			data.setColor(color);
		}
	}

	public void setNodeColor(Connection con, Color color) {
		for (RouteNode node : con.routeNodes) {
			RouteNodeData data = routeNodeData.get(node);
			data.setColor(color);
		}
	}

	public void setNodeColor(Color color) {
		for (RouteNodeData data : routeNodeData.values()) {
			data.setColor(color);
		}
	}

	public void setNodeColor(Pattern scheme, Color color) {
		for (RouteNode node : scheme.routeNodes) {
			RouteNodeData data = routeNodeData.get(node);
			data.setColor(color);
		}
	}

	public void setNodeColor(Net net, Color color) {
		for (RouteNode node : net.routeNodes) {
			RouteNodeData data = routeNodeData.get(node);
			data.setColor(color);
		}
	}

	RouteNode getNode(int x, int y) {
		for (RouteNodeData data : routeNodeData.values()) {
			if (data.inClickRange(x, y)) {
				return data.node;
			}
		}
		return null;
	}

	public Dimension getPreferredSize() {
		return new Dimension(900, 900);
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		setSize(getPreferredSize());

		for (RouteNodeData data : routeNodeData.values()) {
			data.draw(g, zoom);
		}

		for (SiteData data : siteData.values()) {
			data.draw(g, zoom);
		}

	}

	public void buildRouteNodeData() {

		routeNodeData = new HashMap<RouteNode, RouteNodeData>();
		for (RouteNode node : a.routeNodeMap.values()) {
			switch (node.type) {
			case HCHAN:
			case VCHAN:
				routeNodeData.put(node, new WireData(node));
				break;
			default:
				routeNodeData.put(node, new NodeData(node));
			}

		}

		siteData = new HashMap<Site, SiteData>();
		for (Site site : a.siteMap.values()) {
			siteData.put(site, new SiteData(site));
		}

		double tileWidth = clbWidth + (a.channelWidth + 1) * wireSpace;

		// Drawing the sites
		for (Site site : a.sites()) {
			switch (site.type) {
			case IO:
				if (site.x == 0) {
					if (site.n == 0) {
						double x = site.x * tileWidth + clbWidth / 2.0;
						double y = site.y * tileWidth + ioWidth / 2.0;
						drawLeftIoSite(x, y, site);
					} else {
						double x = site.x * tileWidth + clbWidth / 2.0;
						double y = site.y * tileWidth + clbWidth - ioWidth
								+ ioWidth / 2.0;
						drawLeftIoSite(x, y, site);
					}
				}
				if (site.x == a.width + 1) {
					if (site.n == 0) {
						double x = site.x * tileWidth + clbWidth / 2.0;
						double y = site.y * tileWidth + ioWidth / 2.0;
						drawRightIoSite(x, y, site);
					} else {
						double x = site.x * tileWidth + clbWidth / 2.0;
						double y = site.y * tileWidth + clbWidth - ioWidth
								+ ioWidth / 2.0;
						drawRightIoSite(x, y, site);
					}
				}
				if (site.y == 0) {
					if (site.n == 0) {
						double x = site.x * tileWidth + ioWidth / 2.0;
						double y = site.y * tileWidth + clbWidth / 2.0;
						drawUpIoSite(x, y, site);
					} else {
						double x = site.x * tileWidth + ioWidth / 2.0
								+ clbWidth - ioWidth;
						double y = site.y * tileWidth + clbWidth / 2.0;
						drawUpIoSite(x, y, site);
					}
				}
				if (site.y == a.height + 1) {
					if (site.n == 0) {
						double x = site.x * tileWidth + ioWidth / 2.0;
						double y = site.y * tileWidth + clbWidth / 2.0;
						drawDownIoSite(x, y, site);
					} else {
						double x = site.x * tileWidth + ioWidth / 2.0
								+ clbWidth - ioWidth;
						double y = site.y * tileWidth + clbWidth / 2.0;
						drawDownIoSite(x, y, site);
					}
				}
				break;
			case CLB:
				double x = site.x * tileWidth + clbWidth / 2.0;
				double y = site.y * tileWidth + clbWidth / 2.0;
				drawClbSite(x, y, 0, site);

				break;
			default:
				break;

			}
		}

		// Drawing the channels
		double x1, x2, y1, y2;
		WireData data;
		for (RouteNode node : a.routeNodeMap.values()) {
			switch (node.type) {
			case HCHAN:
				x1 = node.x * tileWidth;
				y1 = node.y * tileWidth + clbWidth + (1 + node.n) * wireSpace;
				x2 = node.x * tileWidth + clbWidth;
				y2 = y1;
				data = (WireData) routeNodeData.get(node);
				data.setCoords(x1, y1, x2, y2);
				break;
			case VCHAN:
				x1 = node.x * tileWidth + clbWidth + (1 + node.n) * wireSpace;
				y1 = node.y * tileWidth;
				x2 = x1;
				y2 = node.y * tileWidth + clbWidth;
				data = (WireData) routeNodeData.get(node);
				data.setCoords(x1, y1, x2, y2);
				break;
			default:
			}
		}

	}

	private void drawClbSite(double x, double y, double angle, Site s) {
		ClbSite site = (ClbSite) s;

		SiteData data = siteData.get(site);
		data.setType(SiteType.CLB);
		data.setPossition(x, y);

		drawNode(site.source, x + 0.3 * clbWidth, y - 0.2 * clbWidth);
		drawNode(site.opin, x + 0.3 * clbWidth, y - 0.4 * clbWidth);
		drawNode(site.sink, x + 0 * clbWidth, y + 0 * clbWidth);
		drawNode(site.ipin.get(0), x + 0 * clbWidth, y + 0.4 * clbWidth);
		drawNode(site.ipin.get(1), x + 0.4 * clbWidth, y + 0 * clbWidth);
		drawNode(site.ipin.get(2), x + 0 * clbWidth, y - 0.4 * clbWidth);
		drawNode(site.ipin.get(3), x - 0.4 * clbWidth, y + 0 * clbWidth);

	}

	private void drawNode(RouteNode node, double x, double y) {

		NodeData data = (NodeData) routeNodeData.get(node);
		data.setPossition(x, y);
	}

	private void drawLeftIoSite(double x, double y, Site s) {
		IoSite site = (IoSite) s;

		SiteData data = siteData.get(site);
		data.setType(SiteType.IO_LEFT);
		data.setPossition(x, y);

		drawNode(site.source, x - 0.3 * clbWidth, y - 0.10 * clbWidth);
		drawNode(site.opin, x + 0.3 * clbWidth, y - 0.10 * clbWidth);
		drawNode(site.sink, x - 0.3 * clbWidth, y + 0.10 * clbWidth);
		drawNode(site.ipin, x + 0.3 * clbWidth, y + 0.10 * clbWidth);
	}

	private void drawRightIoSite(double x, double y, Site s) {
		IoSite site = (IoSite) s;

		SiteData data = siteData.get(site);
		data.setType(SiteType.IO_RIGHT);
		data.setPossition(x, y);

		drawNode(site.source, x + 0.3 * clbWidth, y - 0.10 * clbWidth);
		drawNode(site.opin, x - 0.3 * clbWidth, y - 0.10 * clbWidth);
		drawNode(site.sink, x + 0.3 * clbWidth, y + 0.10 * clbWidth);
		drawNode(site.ipin, x - 0.3 * clbWidth, y + 0.10 * clbWidth);
	}

	private void drawUpIoSite(double x, double y, Site s) {
		IoSite site = (IoSite) s;

		SiteData data = siteData.get(site);
		data.setType(SiteType.IO_UP);
		data.setPossition(x, y);

		drawNode(site.source, x - 0.1 * clbWidth, y - 0.30 * clbWidth);
		drawNode(site.opin, x - 0.1 * clbWidth, y + 0.30 * clbWidth);
		drawNode(site.sink, x + 0.1 * clbWidth, y - 0.30 * clbWidth);
		drawNode(site.ipin, x + 0.1 * clbWidth, y + 0.30 * clbWidth);
	}

	private void drawDownIoSite(double x, double y, Site s) {
		IoSite site = (IoSite) s;

		SiteData data = siteData.get(site);
		data.setType(SiteType.IO_DOWN);
		data.setPossition(x, y);

		drawNode(site.source, x - 0.1 * clbWidth, y + 0.30 * clbWidth);
		drawNode(site.opin, x - 0.1 * clbWidth, y - 0.30 * clbWidth);
		drawNode(site.sink, x + 0.1 * clbWidth, y + 0.30 * clbWidth);
		drawNode(site.ipin, x + 0.1 * clbWidth, y - 0.30 * clbWidth);
	}

}
