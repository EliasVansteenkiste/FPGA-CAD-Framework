package route.circuit.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import route.circuit.Circuit;
import route.circuit.architecture.Architecture;
import route.circuit.architecture.BlockCategory;
import route.circuit.architecture.BlockType;
import route.circuit.resource.Site;
import route.circuit.resource.RouteNode;

public class ResourceGraph {
	private final Circuit circuit;
	private final Architecture architecture;
	
	private final int width, height;
    
	private final List<Site> sites;
	private final Site[][] siteArray;
	
	private final List<RouteNode> routeNodes;
	private final Map<RouteNodeType, List<RouteNode>> routeNodeMap;
	
    public ResourceGraph(Circuit circuit) {
    	this.circuit = circuit;
    	this.architecture = this.circuit.getArchitecture();
    	
    	this.width = this.architecture.getWidth();
    	this.height = this.architecture.getHeight();
    	
		this.sites = new ArrayList<>();
		this.siteArray = new Site[this.width+2][this.height+2];
		
		this.routeNodes = new ArrayList<>();
		this.routeNodeMap = new HashMap<>();
		for(RouteNodeType routeNodeType: RouteNodeType.values()){
			List<RouteNode> temp = new ArrayList<>();
			this.routeNodeMap.put(routeNodeType, temp);
		}
    }
    
    public void build(){
        this.createSites();
        
		try {
			this.generateRRG(this.architecture.getRRGFile());
		} catch (IOException e) {
			System.err.println("Problem in generating RRG: " + e.getMessage());
			e.printStackTrace();
		}
		
		this.assignNamesToSourceAndSink();
		this.connectSourceAndSinkToSite();
		
		//this.analyzeSites();
		//this.analyzeRRG();
		//this.testRRG();
		
		//this.printRoutingGraph();
    }
    
    private void createSites() {
        BlockType ioType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        int ioCapacity = this.architecture.getIoCapacity();
        int ioHeight = ioType.getHeight();
        
        //IO Sites
        for(int i = 1; i < this.height + 1; i++) {
        	this.addSite(new Site(0, i, ioHeight, ioType, ioCapacity));
            this.addSite(new Site(this.width + 1, i, ioHeight, ioType, ioCapacity));
        }
        for(int i = 1; i < this.width + 1; i++) {
        	this.addSite(new Site(i, 0, ioHeight, ioType, ioCapacity));
            this.addSite(new Site(i, this.height + 1, ioHeight, ioType, ioCapacity));
        }
        
        for(int column = 1; column < this.width + 1; column++) {
            BlockType blockType = this.circuit.getColumnType(column);
            int blockHeight = blockType.getHeight();
            for(int row = 1; row < this.height + 2 - blockHeight; row += blockHeight) {
            	this.addSite(new Site(column, row, blockHeight, blockType, 1));
            }
        }
    }
    public void addSite(Site site) {
    	this.siteArray[site.getColumn()][site.getRow()] = site;
    	this.sites.add(site);
    }
    
    /**
     * Return the site at coordinate (x, y). If allowNull is false,
     * return the site that overlaps coordinate (x, y) but possibly
     * doesn't start at that position.
     */
    public Site getSite(int column, int row) {
        return this.getSite(column, row, false);
    }
    public Site getSite(int column, int row, boolean allowNull) {
        if(allowNull) {
            return this.siteArray[column][row];
        } else {
            Site site = null;
            int topY = row;
            while(site == null) {
                site = this.siteArray[column][topY];
                topY--;
            }
            
            return site;
        }
    }
    public List<Site> getSites(BlockType blockType) {
        BlockType ioType = BlockType.getBlockTypes(BlockCategory.IO).get(0);
        List<Site> sites;
        
        if(blockType.equals(ioType)) {
            int ioCapacity = this.architecture.getIoCapacity();
            sites = new ArrayList<Site>((this.width + this.height) * 2 * ioCapacity);
            
            for(int n = 0; n < ioCapacity; n++) {
                for(int i = 1; i < this.height + 1; i++) {
                    sites.add(this.siteArray[0][i]);
                    sites.add(this.siteArray[this.width + 1][i]);
                }
                
                for(int i = 1; i < this.width + 1; i++) {
                    sites.add(this.siteArray[i][0]);
                    sites.add(this.siteArray[i][this.height + 1]);
                }
            }
        } else {
            List<Integer> columns = this.circuit.getColumnsPerBlockType(blockType);
            int blockHeight = blockType.getHeight();
            sites = new ArrayList<Site>(columns.size() * this.height);
            
            for(Integer column : columns) {
                for(int row = 1; row < this.height + 2 - blockHeight; row += blockHeight) {
                    sites.add(this.siteArray[column][row]);
                }
            }
        }
    
        return sites;
    }
    public List<Site> getSites(){
    	return this.sites;
    }
	
    
    /**********************
     * GENERATE THE RRG   *
     * READ FROM RRG FILE *
     * DUMPED BY VPR      *
     **********************/
	private void generateRRG(File rrgFile) throws IOException {
		System.out.println("Process RRG");
		BufferedReader reader = new BufferedReader(new FileReader(rrgFile));
		
		String line;
		String[] words;
		RouteNode routeNode = null;
	
		int lineType = -1;
		
		String currentPort = null;
		int portIndex = -1;
		
        while ((line = reader.readLine()) != null) {
            //Clean up line
        	line = line.trim();
        	
        	if(line.length() > 0){
                //Process line
                if(line.contains("Node;Type;Name")){
                	//System.out.println("Node mode");
                	//System.out.printf("%8s   %6s   %12s   %5s   %5s   %5s   %5s   %6s   %3s   %4s   %6s   %10s\n", "Node", "Type", "Name", "xlow", "xhigh", "ylow", "yhigh", "PtcNum", "Cap", "Cost", "R", "C");
                	//System.out.printf("%8s   %6s   %12s   %5s   %5s   %5s   %5s   %6s   %3s   %4s   %6s   %10s\n", "--------", "------", "------------", "-----", "-----", "-----", "-----", "------", "---", "----", "------", "----------");
                	lineType = 1;
                }else if(line.contains("EDGES => Node")) {
                	//System.out.println("Edges mode");
                	lineType = 2;
                }else if(line.contains("RR Switch types")) {
                	//System.out.println("Switch type mode");
                	lineType = 3;
            	}else if(lineType == 1) {
            		words = line.split(";");
            		
            		int index = Integer.parseInt(words[0]);
            		String type = words[1];
            		String name = words[2];
            		int xlow = Integer.parseInt(words[3]);
            		int xhigh = Integer.parseInt(words[4]);
            		int ylow = Integer.parseInt(words[5]);
            		int yhigh = Integer.parseInt(words[6]);
            		int n = Integer.parseInt(words[7]);
            		
            		if(n == 0){//New global block, reset data
            			currentPort = null;
            			portIndex = -1;
            		}
            		
            		int cap = Integer.parseInt(words[8]);
            		double r = Double.parseDouble(words[9]);
            		double c = Double.parseDouble(words[10]);
            		int cost = Integer.parseInt(words[11]);
            		
            		switch (type) {
            			case "SOURCE":
            				//System.out.printf("%8d   %6s   %12s   %5d   %5d   %5d   %5d   %6d   %3d   %4d   %4.2f   %2.4e\n", index, type, name, xlow, xhigh, ylow, yhigh, n, cap, cost, r, c);
            				
            				//Assertions
            				assert name.equals("-");
            				assert r == 0.0;
            				assert c == 0.0;
            				
            				routeNode = new Source(index, xlow, xhigh, ylow, yhigh, n, cap, cost);
            				
            				break;
            			case "SINK":
            				//System.out.printf("%8d   %6s   %12s   %5d   %5d   %5d   %5d   %6d   %3d   %4d   %4.2f   %2.4e\n", index, type, name, xlow, xhigh, ylow, yhigh, n, cap, cost, r, c);
            				
            				//Assertions
            				assert name.equals("-");
            				assert r == 0.0;
            				assert c == 0.0;
            				
            				routeNode = new Sink(index, xlow, xhigh, ylow, yhigh, n, cap, cost);
            				
            				break;
            			case "IPIN":
            				//System.out.printf("%8d   %6s   %12s   %5d   %5d   %5d   %5d   %6d   %3d   %4d   %4.2f   %2.4e\n", index, type, name, xlow, xhigh, ylow, yhigh, n, cap, cost, r, c);

            				//Assertions
            				assert cap == 1;
            				assert r == 0.0;
            				assert c == 0.0;
            				
            				if(currentPort == null){
            					currentPort = name;
            					portIndex = 0;
            				}else if(!currentPort.equals(name)){
            					currentPort = name;
            					portIndex = 0;
            				}
            				routeNode = new Ipin(index, xlow, xhigh, ylow, yhigh, n, name, portIndex, cost);
            				
            				portIndex += 1;
            				
            				break;
            			case "OPIN":
            				//System.out.printf("%8d   %6s   %12s   %5d   %5d   %5d   %5d   %6d   %3d   %4d   %4.2f   %2.4e\n", index, type, name, xlow, xhigh, ylow, yhigh, n, cap, cost, r, c);
            				
            				//Assertions
            				assert cap == 1;
            				assert r == 0.0;
            				assert c == 0.0;
            				
            				if(currentPort == null){
            					currentPort = name;
            					portIndex = 0;
            				}else if(!currentPort.equals(name)){
            					currentPort = name;
            					portIndex = 0;
            				}
            				
            				routeNode = new Opin(index, xlow, xhigh, ylow, yhigh, n, name, portIndex, cost);
            				
            				portIndex += 1;
            				
            				break;
            			case "CHANX":
            				//System.out.printf("%8d   %6s   %12s   %5d   %5d   %5d   %5d   %6d   %3d   %4d   %4.2f   %2.4e\n", index, type, name, xlow, xhigh, ylow, yhigh, n, cap, cost, r, c);
            				
            				//Assertions
            				assert name.equals("-");
            				assert cap == 1;
            				
            				routeNode = new HChan(index, xlow, xhigh, ylow, yhigh, n, r, c, cost);
            				
            				break;
            			case "CHANY":
            				//System.out.printf("%8d   %6s   %12s   %5d   %5d   %5d   %5d   %6d   %3d   %4d   %4.2f   %2.4e\n", index, type, name, xlow, xhigh, ylow, yhigh, n, cap, cost, r, c);
            				
            				//Assertions
            				assert name.equals("-");
            				assert cap == 1;
            				
            				routeNode = new VChan(index, xlow, xhigh, ylow, yhigh, n, r, c, cost);
            				
            				break;
            			default:
            				System.out.println("Unknown type: " + type);
            				break;
            		}
            		this.addRouteNode(routeNode);
            		
            	}else if(lineType == 2) {
            		words = line.split(";");
            		
            		RouteNode parent = this.routeNodes.get(Integer.parseInt(words[0]));
            		
            		int numChildren = Integer.parseInt(words[1]);
            		RouteNode[] children = new RouteNode[numChildren];
            		
            		int childCounter = 0;
            		for(int childIndex = 2; childIndex < words.length; childIndex++) {
            			RouteNode child = this.routeNodes.get(Integer.parseInt(words[childIndex]));
            			children[childCounter] = child;
            			childCounter++;
            		}
            		
            		parent.setChildren(children);
            		
            	}else if(lineType == 3) {
            		words = line.split(";");
            		
            		RouteNode parent = this.routeNodes.get(Integer.parseInt(words[0]));
            		int numChildren = parent.numChildren();
            		
            		short[] switchType = new short[numChildren];
            		int indexCounter = 0;
            		for(int childIndex = 1; childIndex < words.length; childIndex++) {
            			switchType[indexCounter] = Short.parseShort(words[childIndex]);
            			indexCounter++;
            		}
            		
            		parent.setSwitchType(switchType);
            	}
        	}
        }

        reader.close();
	}
	private void assignNamesToSourceAndSink() {
		boolean printInfo = false;//TODO REMOVE
		for(RouteNode routeNode:this.routeNodeMap.get(RouteNodeType.SOURCE)){
			Source source = (Source) routeNode;
			source.setName();
			
			if(printInfo) System.out.println(source.getName());
		}
		
		for(RouteNode routeNode:this.routeNodeMap.get(RouteNodeType.IPIN)){
			Ipin ipin = (Ipin) routeNode;
			ipin.setSinkName();
		}
		if(printInfo) {
			for(RouteNode routeNode:this.routeNodeMap.get(RouteNodeType.SINK)){
				Sink sink = (Sink) routeNode;
				System.out.println(sink.getName());
			}
		}
	}
    private void connectSourceAndSinkToSite() {
    	for(RouteNode routeNode:this.routeNodeMap.get(RouteNodeType.SOURCE)){
			Source source = (Source) routeNode;
			
			Site site = this.getSite(source.xlow, source.ylow);
			if(site.addSource((Source)routeNode) == false) {
				System.err.println("Unable to add " + routeNode + " as source to " + site);
			}
		}
    	for(RouteNode routeNode:this.routeNodeMap.get(RouteNodeType.SINK)){
			Sink sink = (Sink) routeNode;
			
			Site site = this.getSite(sink.xlow, sink.ylow);
			if(site.addSink((Sink)routeNode) == false) {
				System.err.println("Unable to add " + routeNode + " as sink to " + site);
			}
		}
    }
	
	private void addRouteNode(RouteNode routeNode) {
		assert routeNode.index == this.routeNodes.size();
		
		this.routeNodes.add(routeNode);
		this.routeNodeMap.get(routeNode.type).add(routeNode);
	}
	public List<RouteNode> getRouteNodes() {
		return this.routeNodes;
	}
	public int numRouteNodes() {
		return this.routeNodes.size();
	}
	public int numRouteNodes(RouteNodeType type) {
		if(this.routeNodeMap.containsKey(type)) {
			return this.routeNodeMap.get(type).size();
		} else {
			return 0;
		}
	}
	
	public double lowerEstimateConnectionCost(RouteNode source, RouteNode target) {
		int horizontalDistance = Math.max(0, source.xlow - target.xhigh) + Math.max(0, target.xlow - source.xhigh);
		int verticalDistance = Math.max(0, source.ylow - target.yhigh) + Math.max(0, target.ylow - source.yhigh);
		
		//TODO Titan23 architecture has length 16 and length 4 wires
		return horizontalDistance + verticalDistance;
	}
	

	
	@Override
	public String toString() {
		String s = new String();
		
		s+= "The system has " + this.numRouteNodes() + " rr nodes:\n";
		
		for(RouteNodeType type : RouteNodeType.values()) {
			s += "\t" + type + "\t" + this.numRouteNodes(type) + "\n";
		}
		return s;
	}
	
	/********************
	 * Routing statistics
	 ********************/
	public int totalWireLengt() {
		int totalWireLength = 0;
		for(RouteNode routeNode : this.routeNodes) {
			if(routeNode.isWire()) {
				if(routeNode.used()) {
					totalWireLength += routeNode.wireLength();
				}
			}
		}
		return totalWireLength;
	}
	public int congestedTotalWireLengt() {
		int totalWireLength = 0;
		for(RouteNode routeNode : this.routeNodes) {
			if(routeNode.isWire()) {
				if(routeNode.used()) {
					totalWireLength += routeNode.wireLength() * routeNode.routeNodeData.occupation;
				}
			}
		}
		return totalWireLength;
	}
	public int wireSegmentsUsed() {
		int wireSegmentsUsed = 0;
		for(RouteNode routeNode : this.routeNodes) {
			if(routeNode.isWire()) {
				if(routeNode.used()) {
					wireSegmentsUsed++;
				}
			}
		}
		return wireSegmentsUsed;
	}
	
	/**********************
	 * TEST FUNCTIONALITY *
	 **********************/
	public void sanityCheck() {
		for(Site site:this.getSites()) {
			site.sanityCheck();
		}
	}
	public void analyzeRRG() {
		System.out.println("######### RRG #########");
		Map<RouteNodeType, Integer> routeNodeMap = new HashMap<>();
		int numRouteNodes = 0;
		for(RouteNode routeNode:this.routeNodes) {
			if(!routeNodeMap.containsKey(routeNode.type)) routeNodeMap.put(routeNode.type, 0);
			routeNodeMap.put(routeNode.type, routeNodeMap.get(routeNode.type) + 1);
			numRouteNodes++;
		}
		System.out.println("The RRG has " + numRouteNodes + " route nodes");
		for(RouteNodeType type:routeNodeMap.keySet()){
			System.out.println("\t" + type + ": " + routeNodeMap.get(type));
		}
		System.out.println();
		
		//Unconnected route nodes
		routeNodeMap = new HashMap<>();
		int unconnectedRouteNodes = 0;
		for(RouteNode routeNode:this.routeNodes) {
			if(routeNode.numChildren() == 0){
				if(!routeNodeMap.containsKey(routeNode.type)) routeNodeMap.put(routeNode.type, 0);
				routeNodeMap.put(routeNode.type, routeNodeMap.get(routeNode.type) + 1);
				unconnectedRouteNodes++;
			}
		}
		System.out.println("The RRG has " + unconnectedRouteNodes + " route nodes without children");
		for(RouteNodeType type:routeNodeMap.keySet()){
			System.out.println("\t" + type + ": " + routeNodeMap.get(type));
		}
		System.out.println();
		
		System.out.println("#######################");
		System.out.println();
	}
	public void testRRG() {
		System.out.println("Test RRG");
		for(int i = 0; i < this.routeNodes.size(); i++){
			RouteNode routeNode = this.routeNodes.get(i);
			if(routeNode.index != i){
				System.err.println("Problem with index of route node\n\tActual index: " + routeNode.index + "\n\tExpected index: " + i);
			}
		}
		System.out.println("No problems found");
	}
	public void printRoutingGraph() {
		for(RouteNode node : this.getRouteNodes()) {
			System.out.println(node);
			for (RouteNode child : node.children) {
				System.out.println("\t" + child);
			}
			System.out.println();
		}
	}
}
