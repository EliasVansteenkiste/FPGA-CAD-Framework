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
	private final List<Connection> connections;
	public final int fanout;
	
	private final short boundingBoxRange;
	public final short x_min_b;
	public final short x_max_b;
	public final short y_min_b;
	public final short y_max_b;
	
	public final float x_geo;
	public final float y_geo;
	
	public final int hpwl;
	
	private Opin fixedOpin;
	
	public Net(List<Connection> net, short boundingBoxRange) {
		this.connections = net;
		this.fanout = net.size();
		
		this.boundingBoxRange = boundingBoxRange;
		
		List<Short> xCoordinatesBB = new ArrayList<>();
		List<Short> yCoordinatesBB = new ArrayList<>();
		
		float xGeomeanSum = 0;
		float yGeomeanSum = 0;
		
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
			xGeomeanSum += source.centerx;
			
			xCoordinatesBB.add(source.xlow);
			xCoordinatesBB.add(source.xhigh);
		} else {
			xGeomeanSum += source.xlow;
			xCoordinatesBB.add(source.xlow);
		}
		if(source.ylow != source.yhigh) {
			yGeomeanSum += source.centery;
			
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
				xGeomeanSum += sink.centerx;
				
				xCoordinatesBB.add(sink.xlow);
				xCoordinatesBB.add(sink.xhigh);
			} else {
				xGeomeanSum += sink.xlow;
				xCoordinatesBB.add(sink.xlow);
			}
			if(sink.ylow != sink.yhigh) {
				yGeomeanSum += sink.centery;
				
				yCoordinatesBB.add(sink.ylow);
				yCoordinatesBB.add(sink.yhigh);
			} else {
				yGeomeanSum += sink.ylow;
				yCoordinatesBB.add(sink.ylow);
			}
		}
		
		short x_min = Short.MAX_VALUE;
		short y_min = Short.MAX_VALUE;
		
		short x_max = Short.MIN_VALUE;
		short y_max = Short.MIN_VALUE;
		
		for(short x : xCoordinatesBB) {
			if(x < x_min) {
				x_min = x;
			}
			if(x > x_max) {
				x_max = x;
			}
		}
		for(short y : yCoordinatesBB) {
			if(y < y_min) {
				y_min = y;
			}
			if(y > y_max) {
				y_max = y;
			}
		}
		
		
		this.hpwl = (x_max - x_min + 1) + (y_max - y_min + 1);
		
		this.x_geo = xGeomeanSum / (1 + this.fanout);
		this.y_geo = yGeomeanSum / (1 + this.fanout);
		
		this.x_max_b = (short) (x_max + this.boundingBoxRange);
		this.x_min_b = (short) (x_min - this.boundingBoxRange);
		this.y_max_b = (short) (y_max + this.boundingBoxRange);
		this.y_min_b = (short) (y_min - this.boundingBoxRange);
		
		for(Connection connection : this.connections) {
			connection.setNet(this);
		}
		
		this.fixedOpin = null;
	}
	
	public boolean isInBoundingBoxLimit(RouteNode node) {
		return  node.xlow < this.x_max_b && node.xhigh > this.x_min_b && node.ylow < this.y_max_b && node.yhigh > this.y_min_b;
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
	
	public List<Connection> getConnections() {
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
