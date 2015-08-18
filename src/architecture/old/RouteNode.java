package architecture.old;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import architecture.Site;

public class RouteNode {
	public String name;
	public Set<RouteNode> children;
	public Set<RouteNode> parents;
	
	public double baseCost;
	
	public int usedInNet;
	public int nrOfTconOwners;
	public int nrOfConnOwners;
	public double CRfactor;//used in Negotiatied Overlap
	public int owner;//used in the clusterRouter
	public boolean sinkOrSource;
	public Vector<String> owners;
	
	public int capacity;
	public boolean target;
	public RouteNodeType type;
	
	public Site site;
	
	public int x;
	public int y;
	public int n;
	
	

	public RouteNode(String name, int capacity, int x, int y, int n, RouteNodeType t, Site site) {
		super();
		this.site = site;
		this.name = name;
		
		this.capacity = capacity;
		this.x = x;
		this.y = y;
		this.n = n;
		
		this.usedInNet = 0;
		this.nrOfTconOwners = 0;
		this.nrOfConnOwners = 0;
		this.owners = new Vector<String>();
		this.CRfactor=1;
		this.sinkOrSource = false;
		
		this.type = t;
		this.owner = -1;
		this.target = false;
		setBaseCost();
		
		children = new HashSet<RouteNode>();
		parents = new HashSet<RouteNode>();
	}
	
	public void resetDataInNode(){
		this.usedInNet = 0;
		this.nrOfTconOwners = 0;
		this.nrOfConnOwners = 0;
		this.owners.clear();
		this.CRfactor=1;
		this.sinkOrSource = false;
		
		this.owner = -1;
		this.target = false;
	}

	private void setBaseCost() {
		switch (type) {
		case SOURCE:
		case OPIN:
		case HCHAN:
		case VCHAN:
			this.baseCost = 1;
			break;
		case SINK:
			this.baseCost = 0;
			break;
		case IPIN:
			this.baseCost = 0.95;
			break;
		default:
			break;
		}
	}

	public RouteNode(String name) {
		super();
		this.name = name;
		this.owner = -1;
		children = new HashSet<RouteNode>();
		parents = new HashSet<RouteNode>();
	}
	
	public void addChild(RouteNode node) {
		children.add(node);
	}
	
	@Override
	public String toString() {
		return name;
	}

	public void addParent(RouteNode node) {
		parents.add(node);
	}

	public void setBaseCost(double baseCost) {
		this.baseCost = baseCost;
	}

	public void setCapacity(int cap) {
		capacity=cap;
		
	}

	public static void connect(Vector<RouteNode> vector, Vector<RouteNode> vector2) {
		if (vector!=null && vector2!=null)
		for (int i=0; i<vector.size();i++) {
			connect(vector.get(i),vector2.get(i));
		}
	}
	
	public static void connect(RouteNode node, Vector<RouteNode> channel) {
		for (RouteNode wire:channel) {
			connect(node, wire);
		}
	}

	public static void connect(Vector<RouteNode> channel, RouteNode node) {
		for (RouteNode wire:channel) {
			connect(wire, node);
		}
	}
	
	public static void connect(RouteNode parent, RouteNode child) {
		parent.children.add(child);
		child.parents.add(parent);//
	}

	public boolean isWire() {
		return type == RouteNodeType.HCHAN || type == RouteNodeType.VCHAN;
	}
	
}
