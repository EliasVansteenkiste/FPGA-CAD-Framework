package route.route;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import route.circuit.resource.RouteNode;
import route.circuit.resource.Sink;
import route.circuit.resource.Source;
import route.route.Connection;

public class Net {
	public final Set<Connection> connections;
	public final int fanout;
	
	public final short x_min;
	public final short x_max;
	public final short y_min;
	public final short y_max;
	
	public final int hpwl;
	
	public final short boundingBoxRange;
	
	public final double x_geo;
	public final double y_geo;
	
	public Net(Set<Connection> net, short boundingBoxRange) {
		this.connections = net;
		this.fanout = net.size();
		
		this.boundingBoxRange = boundingBoxRange;
		
		List<Short> xCoordinatesBB = new ArrayList<>();
		List<Short> yCoordinatesBB = new ArrayList<>();
		
		List<Short> xCoordinatesGeoMean = new ArrayList<>();
		List<Short> yCoordinatesGeoMean = new ArrayList<>();
		
		short x_val;
		short y_val;
		
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
			x_val = (short) Math.round(0.5 * (source.xlow + source.xhigh));
			
			xCoordinatesBB.add(source.xlow);
			xCoordinatesBB.add(source.xhigh);
		} else {
			x_val = source.xlow;
			xCoordinatesBB.add(source.xlow);
		}
		if(source.ylow != source.yhigh) {
			y_val = (short) Math.round(0.5 * (source.ylow + source.yhigh));
			yCoordinatesBB.add(source.ylow);
			yCoordinatesBB.add(source.yhigh);
		} else {
			y_val = source.ylow;
			yCoordinatesBB.add(source.ylow);
		}
		
		xCoordinatesGeoMean.add(x_val);
		yCoordinatesGeoMean.add(y_val);

		//Sink pins of net
		for(Connection connection : net) {
			Sink sink = (Sink) connection.sinkRouteNode;
			
			if(sink.xlow != sink.xhigh) {
				x_val = (short) Math.round(0.5 * (sink.xlow + sink.xhigh));
				
				xCoordinatesBB.add(sink.xlow);
				xCoordinatesBB.add(sink.xhigh);
			} else {
				x_val = sink.xlow;
				xCoordinatesBB.add(sink.xlow);
			}
			if(sink.ylow != sink.yhigh) {
				y_val = (short) Math.round(0.5 * (sink.ylow + sink.yhigh));
				
				yCoordinatesBB.add(sink.ylow);
				yCoordinatesBB.add(sink.yhigh);
			} else {
				y_val = sink.ylow;
				yCoordinatesBB.add(sink.ylow);
			}

			xCoordinatesGeoMean.add(x_val);
			yCoordinatesGeoMean.add(y_val);
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
		
		short sum_x = 0;
		for(short x : xCoordinatesGeoMean) {
			sum_x += x;
		}
		short sum_y = 0;
		for(short y : yCoordinatesGeoMean) {
			sum_y += y;
		}
		
		this.x_geo = ((double) sum_x) / (1 + this.fanout);
		this.y_geo = ((double) sum_y) / (1 + this.fanout);
		
		for(Connection connection : this.connections) {
			connection.setNet(this);
		}
	}
	
	public boolean isInBoundingBoxLimit(RouteNode node) {
		if(
			node.xlow < (this.x_max + this.boundingBoxRange) 
			&& node.xhigh > (this.x_min - this.boundingBoxRange) 
			&& node.ylow < (this.y_max + this.boundingBoxRange) 
			&& node.yhigh > (this.y_min - this.boundingBoxRange)
		) {
			return true;
		}
		
		return false;
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
}
