package visual;

import java.io.PrintStream;
import java.util.Collection;

import architecture.ClbSite;
import architecture.IoSite;
import architecture.Site;
import architecture.old.FourLutSanitized;
import architecture.old.RouteNode;

public class SvgGenerator {
	static int clbWidth = 100;
	static int ioWidth = 45;
	static int wireSpace = 10;

	public static void go(FourLutSanitized a, Collection<RouteNode> nodes,
			PrintStream stream) {
		int tileWidth = clbWidth + (a.getChannelWidth() + 1) * wireSpace;
		String standaardLineStyle = "stroke:grey;stroke-width:2";
		String highlightLineStyle = "stroke:rgb(255,0,0);stroke-width:2";

		stream.println("<?xml version=\"1.0\" standalone=\"no\"?>");
		stream.println("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">");
		stream.println("<svg width=\"100%\" height=\"100%\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\">");

		stream.println("<g transform=\"matrix(1,0,0,-1,0,"
				+ ((a.getHeight() + 1) * tileWidth + clbWidth) + ")\">");
		// stream.println("<g transform=\"matrix(1,0,0,-1,0,0)\">");

		stream.println("<rect x=\"0\" y=\"0\" width=\""
				+ ((a.getWidth() + 1) * tileWidth + clbWidth) + "\" height=\""
				+ ((a.getHeight() + 1) * tileWidth + clbWidth)
				+ "\" style=\"fill:rgb(255,255,255)\" />");

		// Drawing the sites
		for (Site site : a.getSites()) {
			switch (site.type) {
			case IO:
				if (site.x == 0) {
					if (site.n == 0) {
						double x = site.x * tileWidth + clbWidth / 2.0;
						double y = site.y * tileWidth + ioWidth / 2.0;
						drawIoSite(stream, x, y, 0, site, nodes);
					} else {
						double x = site.x * tileWidth + clbWidth / 2.0;
						double y = site.y * tileWidth + clbWidth - ioWidth
								+ ioWidth / 2.0;
						drawIoSite(stream, x, y, 0, site, nodes);
					}
				}
				if (site.x == a.getWidth() + 1) {
					if (site.n == 0) {
						double x = site.x * tileWidth + clbWidth / 2.0;
						double y = site.y * tileWidth + ioWidth / 2.0;
						drawIoSite(stream, x, y, 180, site, nodes);
					} else {
						double x = site.x * tileWidth + clbWidth / 2.0;
						double y = site.y * tileWidth + clbWidth - ioWidth
								+ ioWidth / 2.0;
						drawIoSite(stream, x, y, 180, site, nodes);
					}
				}
				if (site.y == 0) {
					if (site.n == 0) {
						double x = site.x * tileWidth + ioWidth / 2.0;
						double y = site.y * tileWidth + clbWidth / 2.0;
						drawIoSite(stream, x, y, 90, site, nodes);
					} else {
						double x = site.x * tileWidth + ioWidth / 2.0
								+ clbWidth - ioWidth;
						double y = site.y * tileWidth + clbWidth / 2.0;
						drawIoSite(stream, x, y, 90, site, nodes);
					}
				}
				if (site.y == a.getHeight() + 1) {
					if (site.n == 0) {
						double x = site.x * tileWidth + ioWidth / 2.0;
						double y = site.y * tileWidth + clbWidth / 2.0;
						drawIoSite(stream, x, y, 270, site, nodes);
					} else {
						double x = site.x * tileWidth + ioWidth / 2.0
								+ clbWidth - ioWidth;
						double y = site.y * tileWidth + clbWidth / 2.0;
						drawIoSite(stream, x, y, 270, site, nodes);
					}
				}
				break;
			case CLB:
				double x = site.x * tileWidth + clbWidth / 2.0;
				double y = site.y * tileWidth + clbWidth / 2.0;
				drawClbSite(stream, x, y, 0, site, nodes);

				break;
			default:
				break;

			}
		}

		// Drawing the channels
		int x1, x2, y1, y2;
		String lineStyle;
		for (RouteNode node : a.getRouteNodes()) {
			if (nodes.contains(node))
				lineStyle = highlightLineStyle;
			else
				lineStyle = standaardLineStyle;

			switch (node.type) {
			case HCHAN:
				x1 = node.x * tileWidth;
				y1 = node.y * tileWidth + clbWidth + (1 + node.n) * wireSpace;
				x2 = node.x * tileWidth + clbWidth;
				y2 = y1;
				stream.println("<line x1=\"" + x1 + "\" y1=\"" + y1
						+ "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" style=\""
						+ lineStyle + "\"/>");
				break;
			case VCHAN:
				x1 = node.x * tileWidth + clbWidth + (1 + node.n) * wireSpace;
				y1 = node.y * tileWidth;
				x2 = x1;
				y2 = node.y * tileWidth + clbWidth;
				stream.println("<line x1=\"" + x1 + "\" y1=\"" + y1
						+ "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" style=\""
						+ lineStyle + "\"/>");
				break;
			default:
				switch (node.site.type) {
				case IO:
					switch (node.type) {
					case SOURCE:
						break;
					case OPIN:
						break;
					case SINK:
						break;
					case IPIN:
						break;
					case VCHAN:
						break;
					case HCHAN:
						break;
					}
					break;
				case CLB:

					break;
				default:
					break;
				}
				break;
			}
		}
		stream.println("</g>");
		stream.println("</svg>");
	}

	private static void drawClbSite(PrintStream stream, double x, double y,
			double angle, Site s, Collection<RouteNode> nodes) {
		ClbSite site = (ClbSite) s;
		stream.println("<g transform=\"translate(" + x + "," + y + ")\">");
		stream.println("<g transform=\"rotate(" + angle + ")\">");
		stream.println("<rect x=\"-50\" y=\"-50\" width=\"" + clbWidth
				+ "\" height=\"" + clbWidth + "\" fill=\"gray\"/>");

		if (nodes.contains(site.source))
			stream.println("<circle cx=\"30\" cy=\"-20\" r=\"7\" fill=\"blue\"/>"); // source
		else
			stream.println("<circle cx=\"30\" cy=\"-20\" r=\"7\" fill=\"black\"/>");
		if (nodes.contains(site.opin))
			stream.println("<circle cx=\"30\" cy=\"-40\" r=\"7\" fill=\"red\"/>"); // opin
		else
			stream.println("<circle cx=\"30\" cy=\"-40\" r=\"7\" fill=\"black\"/>"); // opin
		if (nodes.contains(site.sink))
			stream.println("<circle cx=\"0\" cy=\"0\" r=\"7\" fill=\"red\"/>"); // sink
		else
			stream.println("<circle cx=\"0\" cy=\"0\" r=\"7\" fill=\"black\"/>"); // sink
		if (nodes.contains(site.ipin.get(0)))
			stream.println("<circle cx=\"0\" cy=\"40\" r=\"7\" fill=\"red\"/>"); // ipin
		else
			stream.println("<circle cx=\"0\" cy=\"40\" r=\"7\" fill=\"black\"/>"); // ipin
		if (nodes.contains(site.ipin.get(1)))
			stream.println("<circle cx=\"40\" cy=\"0\" r=\"7\" fill=\"red\"/>"); // ipin
		else
			stream.println("<circle cx=\"40\" cy=\"0\" r=\"7\" fill=\"black\"/>"); // ipin
		if (nodes.contains(site.ipin.get(2)))
			stream.println("<circle cx=\"0\" cy=\"-40\" r=\"7\" fill=\"red\"/>"); // ipin
		else
			stream.println("<circle cx=\"0\" cy=\"-40\" r=\"7\" fill=\"black\"/>"); // ipin
		if (nodes.contains(site.ipin.get(3)))
			stream.println("<circle cx=\"-40\" cy=\"0\" r=\"7\" fill=\"red\"/>"); // ipin
		else
			stream.println("<circle cx=\"-40\" cy=\"0\" r=\"7\" fill=\"black\"/>"); // ipin

		stream.println("</g>");
		stream.println("</g>");
	}

	private static void drawIoSite(PrintStream stream, double x, double y,
			int angle, Site s, Collection<RouteNode> nodes) {
		IoSite site = (IoSite) s;
		stream.println("<g transform=\"translate(" + x + "," + y + ")\">");
		stream.println("<g transform=\"rotate(" + angle + ")\">");
		stream.println("<rect x=\"-50\" y=\"-22.5\" width=\"100\" height=\"45\" fill=\"gray\"/>");
		if (nodes.contains(site.source))
			stream.println("<circle cx=\"-30\" cy=\"-10.5\" r=\"7\" fill=\"blue\"/>"); // source
		else
			stream.println("<circle cx=\"-30\" cy=\"-10.5\" r=\"7\" fill=\"black\"/>"); // source

		if (nodes.contains(site.opin))
			stream.println("<circle cx=\"30\" cy=\"-10.5\" r=\"7\" fill=\"red\"/>"); // opin
		else
			stream.println("<circle cx=\"30\" cy=\"-10.5\" r=\"7\" fill=\"black\"/>"); // opin

		if (nodes.contains(site.sink))
			stream.println("<circle cx=\"-30\" cy=\"+10.5\" r=\"7\" fill=\"red\"/>"); // sink
		else
			stream.println("<circle cx=\"-30\" cy=\"+10.5\" r=\"7\" fill=\"black\"/>"); // sink

		if (nodes.contains(site.ipin))
			stream.println("<circle cx=\"30\" cy=\"+10.5\" r=\"7\" fill=\"red\"/>"); // ipin
		else
			stream.println("<circle cx=\"30\" cy=\"+10.5\" r=\"7\" fill=\"black\"/>"); // ipin
		stream.println("</g>");
		stream.println("</g>");
	}

}
