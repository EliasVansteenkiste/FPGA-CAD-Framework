package route.route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import route.circuit.resource.Opin;
import route.circuit.resource.RouteNode;
import route.circuit.resource.Sink;
import route.circuit.resource.Source;
import route.route.Connection;

public class Net {
	private final Set<Connection> connections;
	public final int fanout;
	
	private final short x_min;
	private final short x_max;
	private final short y_min;
	private final short y_max;
	
	private final short boundingBoxRange;
	private final short x_min_b;
	private final short x_max_b;
	private final short y_min_b;
	private final short y_max_b;
	
	public final double x_geo;
	public final double y_geo;
	
	public final int hpwl;
	
	private Opin fixedOpin;
	
	public Net(Set<Connection> net, short boundingBoxRange) {
		this.connections = net;
		this.fanout = net.size();
		
		this.boundingBoxRange = boundingBoxRange;
		
		List<Short> xCoordinatesBB = new ArrayList<>();
		List<Short> yCoordinatesBB = new ArrayList<>();
		
		double xGeomeanSum = 0.0;
		double yGeomeanSum = 0.0;
		
		//Source pin of net
		Source source = null;
		for(Connection connection : net) {
			if(source == null) {
				source = (Source) connection.sourceRouteNode;
			} else if(!source.equals((Source) connection.sourceRouteNode)) {
				throw new RuntimeException();
			}
		}
		
		if(source.xlow != source.xhigh) {
			xGeomeanSum += Math.round(0.5 * (source.xlow + source.xhigh));
			
			xCoordinatesBB.add(source.xlow);
			xCoordinatesBB.add(source.xhigh);
		} else {
			xGeomeanSum += source.xlow;
			xCoordinatesBB.add(source.xlow);
		}
		if(source.ylow != source.yhigh) {
			yGeomeanSum += Math.round(0.5 * (source.ylow + source.yhigh));
			
			yCoordinatesBB.add(source.ylow);
			yCoordinatesBB.add(source.yhigh);
		} else {
			yGeomeanSum += source.ylow;
			yCoordinatesBB.add(source.ylow);
		}

		//Sink pins of net
		for(Connection connection : net) {
			Sink sink = (Sink) connection.sinkRouteNode;
			
			if(sink.xlow != sink.xhigh) {
				xGeomeanSum += Math.round(0.5 * (sink.xlow + sink.xhigh));
				
				xCoordinatesBB.add(sink.xlow);
				xCoordinatesBB.add(sink.xhigh);
			} else {
				xGeomeanSum += sink.xlow;
				xCoordinatesBB.add(sink.xlow);
			}
			if(sink.ylow != sink.yhigh) {
				yGeomeanSum += Math.round(0.5 * (sink.ylow + sink.yhigh));
				
				yCoordinatesBB.add(sink.ylow);
				yCoordinatesBB.add(sink.yhigh);
			} else {
				yGeomeanSum += sink.ylow;
				yCoordinatesBB.add(sink.ylow);
			}
		}
		
		short x_min_temp = Short.MAX_VALUE;
		short y_min_temp = Short.MAX_VALUE;
		
		short x_max_temp = Short.MIN_VALUE;
		short y_max_temp = Short.MIN_VALUE;
		
		for(short x : xCoordinatesBB) {
			if(x < x_min_temp) {
				x_min_temp = x;
			}
			if(x > x_max_temp) {
				x_max_temp = x;
			}
		}
		for(short y : yCoordinatesBB) {
			if(y < y_min_temp) {
				y_min_temp = y;
			}
			if(y > y_max_temp) {
				y_max_temp = y;
			}
		}
		
		this.x_min = x_min_temp;
		this.y_min = y_min_temp;
		
		this.x_max = x_max_temp;
		this.y_max = y_max_temp;
		
		this.hpwl = (this.x_max - this.x_min + 1) + (this.y_max - this.y_min + 1);
		
		this.x_geo = xGeomeanSum / (1.0 + this.fanout);
		this.y_geo = yGeomeanSum / (1.0 + this.fanout);
		
		this.x_max_b = (short) (this.x_max + this.boundingBoxRange);
		this.x_min_b = (short) (this.x_min - this.boundingBoxRange);
		this.y_max_b = (short) (this.y_max + this.boundingBoxRange);
		this.y_min_b = (short) (this.y_min - this.boundingBoxRange);
		
		for(Connection connection : this.connections) {
			connection.setNet(this);
		}
		
		this.fixedOpin = null;
	}
	
	public boolean isInBoundingBoxLimit(RouteNode node) {
		return node.xlow < this.x_max_b && node.xhigh > this.x_min_b && node.ylow < this.y_max_b && node.yhigh > this.y_min_b;
	}
	
	public int wireLength() {
		int wireLength = 0;
		Set<RouteNode> routeNodes = new HashSet<>();
		for(Connection connection : this.connections) {
			routeNodes.addAll(connection.routeNodes);
		}
		for(RouteNode routeNode : routeNodes) {
			if(routeNode.isWire()) {
				wireLength += routeNode.wireLength();
			}
		}
		return wireLength;
	}
	
	public Set<Connection> getConnections() {
		return this.connections;
	}
	
	public boolean hasOpin() {
		return this.fixedOpin != null;
	}
	public Opin getOpin() {
		return this.fixedOpin;
	}
	public void setOpin(Opin opin) {
		this.fixedOpin = opin;
		this.fixedOpin.use();
	}
	
	public Opin getUniqueOpin() {
		Opin netOpin = null;
		for(Connection con : this.connections) {
			Opin opin = con.getOpin();
			if(opin != null) {
				if(netOpin == null) {
					netOpin = opin;
				} else if(opin.index != netOpin.index) {
					return null;
				}
			}
		}
		return netOpin;
	}
	public Opin getMostUsedOpin() {
		Map<Opin, Integer> opinCount = new HashMap<>();
		for(Connection con : this.connections) {
			Opin opin = con.getOpin();
			if(opin != null) {
				if(opinCount.get(opin) == null) {
					opinCount.put(opin, 1);
				} else {
					opinCount.put(opin, opinCount.get(opin) + 1);
				}
			}
		}
		Opin bestOpin = null;
		int bestCount = 0;
		for(Opin opin : opinCount.keySet()) {
			if(opinCount.get(opin) > bestCount) {
				bestOpin = opin;
				bestCount = opinCount.get(opin);
			}
		}
		return bestOpin;
	}
}
