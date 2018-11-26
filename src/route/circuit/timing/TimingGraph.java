package route.circuit.timing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import route.util.Pair;
import route.circuit.Circuit;
import route.circuit.architecture.BlockCategory;
import route.circuit.architecture.BlockType;
import route.circuit.architecture.PortType;
import route.circuit.block.AbstractBlock;
import route.circuit.block.GlobalBlock;
import route.circuit.block.LeafBlock;
import route.circuit.pin.AbstractPin;
import route.circuit.pin.GlobalPin;
import route.circuit.pin.LeafPin;
import route.circuit.resource.RouteNode;
import route.circuit.timing.TimingNode.Position;
import route.route.Connection;

public class TimingGraph {

    private static String VIRTUAL_IO_CLOCK = "virtual-io-clock";

    private Circuit circuit;

    // A map of clock domains, along with their unique id
    private Map<String, Integer> clockNamesToDomains = new HashMap<>();
    private Map<Integer, Integer> clockDomainFanout = new HashMap<>();
    private int numClockDomains = 0;
    private int virtualIoClockDomain;

    private List<TimingNode> timingNodes = new ArrayList<>();
    private Map<Integer, List<TimingNode>> rootNodes, leafNodes;

    private List<TimingEdge> timingEdges  = new ArrayList<>();
    private List<List<TimingEdge>> timingNets = new ArrayList<>();

    private List<ClockDomain> clockDomains;
    private double globalMaxDelay;
    
    //Tarjan's strongly connected components algorithm
    private int index;
    private Stack<TimingNode> stack;
    private List<SCC> scc;

    public TimingGraph(Circuit circuit) {
        this.circuit = circuit;

        // Create the virtual io clock domain
        this.virtualIoClockDomain = 0;
        this.clockNamesToDomains.put(VIRTUAL_IO_CLOCK, this.virtualIoClockDomain);
        this.clockDomainFanout.put(this.numClockDomains, 0);
        this.numClockDomains++;
    }

    /******************************************
     * These functions build the timing graph *
     ******************************************/

    public void build() {
    	System.out.println("Build timing graph\n");
        this.buildGraph();
        
        /******************************************
         *     Test functions to check if all     * 
         *     timing nodes are connected         *
         ******************************************/
        for(TimingNode node : this.timingNodes) {
        	if(node.getPosition().equals(Position.ROOT) || node.getPosition().equals(Position.INTERMEDIATE)) {
        		if(node.getNumSinks() == 0) {
        			System.out.println(node + " " + node.getPosition() + " has no sinks");
        		}
        	}
        }
        for(TimingNode node : this.timingNodes) {
        	if(node.getPosition().equals(Position.INTERMEDIATE) || node.getPosition().equals(Position.LEAF)) {
        		if(node.getNumSources() == 0) {
        			System.out.println(node + " " + node.getPosition() + " has no sources");
        		}
        	}
        }
        
        for(TimingNode node : this.timingNodes) {
        	if(node.getPosition().equals(Position.LEAF)) {
        		if(node.getNumSinks() > 0) {
        			System.out.println(node + " " + node.getPosition() + " has sinks");
        		}
        	}
        }
        
        for(TimingNode node : this.timingNodes) {
        	if(node.getPosition().equals(Position.C_SOURCE)) {
        		for(AbstractPin abstractPin : node.getPin().getSinks()) {
        			if(!abstractPin.hasTimingNode()) {
        				System.out.println(node.getPin() + " => " + abstractPin);
        			}
        		}
        	}
        }
        for(TimingNode node : this.timingNodes) {
        	if(node.getPosition().equals(Position.C_SINK)) {
        		if(!node.getPin().getSource().hasTimingNode()) {
        			System.out.println(node.getPin().getSource() + " => " + node.getPin());
        		}
        	}
        }
        /*******************************************/
        
        this.cutCombLoop();
        
        this.setRootAndLeafNodes();
        
        this.setClockDomains();
        
        System.out.println("Timing Graph:");
        
        System.out.println("   Num clock domains " + this.numClockDomains);
        System.out.println("   Num timing nodes " + this.timingNodes.size());
        System.out.println("      Root " + this.rootNodes.size());
        for(int i = 0 ; i < this.numClockDomains; i++) {
        	System.out.println("         " + i + " " + this.rootNodes.get(i).size());
        }
        System.out.println("      Leaf " + this.leafNodes.size());
        for(int i = 0 ; i < this.numClockDomains; i++) {
        	System.out.println("         " + i + " " + this.leafNodes.get(i).size());
        }
        System.out.println("   Num timing edges " + this.timingEdges.size());
        System.out.println();
        
    }
    
    public void initializeTiming() {
	
    	this.calculatePlacementEstimatedWireDelay();
    	this.calculateArrivalTimes();
    	
    	System.out.println("Timing information (based on placement estimated wire delay)\n");
    	this.printDelays();
        
        //System.out.println(this.criticalPathToString());
        System.out.println();
    }

    private void buildGraph() {
        List<Double> clockDelays = new ArrayList<Double>();

        // Create all timing nodes
        for(BlockType leafBlockType : BlockType.getLeafBlockTypes()) {
            boolean isClocked = leafBlockType.isClocked();

            for(AbstractBlock abstractBlock : this.circuit.getBlocks(leafBlockType)) {
                LeafBlock block = (LeafBlock) abstractBlock;
                boolean isConstantGenerator = isClocked ? false : this.isConstantGenerator(block);

                // Get the clock domain and clock setup time
                int clockDomain = -1;
                double clockDelay = 0;
                if(isClocked) {
                    Pair<Integer, Double> clockDomainAndDelay = this.getClockDomainAndDelay(block);
                    clockDomain = clockDomainAndDelay.getFirst();

                    // We don't include the clock setup time in the critical path delay,
                    // because VPR also doesn't does this for the critical path. When you
                    // let VPR print out the critical path the clock setup time IS included,
                    // but it isn't included in the stdout estimation.
                    // clockDelay = clockDomainAndDelay.getSecond();
                } else if(isConstantGenerator) {
                    clockDomain = this.virtualIoClockDomain;
                }

                // If the block is clocked: create TimingNodes for the input pins
                if(isClocked) {
                	for(AbstractPin abstractPin : block.getInputPins()) {
                		if(abstractPin.getSource() != null) {
                        	//Find source of net and check if it is the source of a global net.
                        	AbstractPin source = abstractPin;
                        	while(source.getSource() != null) {
                        		source = source.getSource();
                        	}
                        	if(!this.isSourceOfGlobalNet(source)) {
                        		LeafPin inputPin = (LeafPin) abstractPin;
                        		TimingNode node = new TimingNode(block.getGlobalParent(), inputPin, Position.LEAF, clockDomain);
                        		inputPin.setTimingNode(node);
                        		
                        		clockDelays.add(0.0);
                        		this.timingNodes.add(node);
                        		
                        		this.clockDomainFanout.put(clockDomain, this.clockDomainFanout.get(clockDomain) + 1);
                        	}
                        }
                    }
                }
                
                
                // Add the output pins of this timing graph root or intermediate node
                Position position = (isClocked || isConstantGenerator) ? Position.ROOT : Position.INTERMEDIATE;
                for(AbstractPin abstractPin : block.getOutputPins()) {
                    LeafPin outputPin = (LeafPin) abstractPin;
                    
                    if(outputPin.getNumSinks() > 0 && !this.isSourceOfClockNet(outputPin) && !this.isSourceOfGlobalNet(outputPin)) {
                    	TimingNode node = new TimingNode(block.getGlobalParent(), outputPin, position, clockDomain);
                    	outputPin.setTimingNode(node);

                    	this.timingNodes.add(node);

                    	if(position == Position.ROOT) {
                    		clockDelays.add(clockDelay + outputPin.getPortType().getSetupTime());
                    	} else {
                    		clockDelays.add(0.0);
                    	}
                    }
                }
            }
        }
        
        //Add timing nodes for the connections
        for(GlobalBlock globalBlock : this.circuit.getGlobalBlocks()) {
        	for(AbstractPin abstractPin : globalBlock.getOutputPins()) {
        		GlobalPin outputPin = (GlobalPin) abstractPin;
        		
        		AbstractPin current = outputPin;
        		while(current.getSource() != null) {
        			current = current.getSource();
        		}
        		
        		AbstractPin pathSource = current;
        		if(pathSource.hasTimingNode()) {
        			if(outputPin.getNumSinks() > 0) {
            			TimingNode node = new TimingNode(globalBlock, outputPin, Position.C_SOURCE, -1);
            			outputPin.setTimingNode(node);
            			
            			this.timingNodes.add(node);  
            			
            			clockDelays.add(0.0);
            		}
        		}
        	}
        }
        for(GlobalBlock globalBlock : this.circuit.getGlobalBlocks()) {
        	for(AbstractPin abstractPin : globalBlock.getInputPins()) {
        		GlobalPin inputPin = (GlobalPin) abstractPin;
        		
        		if(inputPin.getSource() != null) {
        			if(inputPin.getSource().hasTimingNode()) {
            			TimingNode node = new TimingNode(globalBlock, inputPin, Position.C_SINK, -1);
            			inputPin.setTimingNode(node);
            			
            			this.timingNodes.add(node);  
            			
            			clockDelays.add(0.0);
            		}
        		}
        	}
        }
        
        //Connect the timing nodes
        int numNodes = this.timingNodes.size();
        for(int i = 0; i < numNodes; i++) {
            TimingNode node = this.timingNodes.get(i);
            
            if(node.getPosition() != Position.LEAF && !this.isSourceOfClockNet(node)) {
                this.traverseFromSource(node, clockDelays.get(i));
            }
        }
        
        for(TimingNode node : this.timingNodes) {
            node.compact();
        }
        
        System.out.println("Clock domain fanout");
        for(int clockDomain = 0; clockDomain < this.numClockDomains; clockDomain++) {
        	System.out.println("   " + clockDomain + ": " + this.clockDomainFanout.get(clockDomain));
        }
        System.out.println();
    }
    
    private boolean isSourceOfClockNet(TimingNode node) {
    	return this.isSourceOfClockNet(node.getPin());
    }
    private boolean isSourceOfClockNet(AbstractPin pin) {
    	if(pin.getNumSinks() > 0) {
    		for(AbstractPin sink : pin.getSinks()) {
    			if(!isSourceOfClockNet(sink)) return false;
    		}
    	} else {
    		return pin.isClock();
    	}
    	
    	return true;
    }
    
    private boolean isSourceOfGlobalNet(AbstractPin pin) {
    	return this.circuit.getGlobalNetNames().contains(pin.getOwner().getName());
    }
    
    private boolean isConstantGenerator(LeafBlock block) {
        for(AbstractPin inputPin : block.getInputPins()) {
            if(inputPin.getSource() != null) {
                return false;
            }
        }

        return true;
    }

    private Pair<Integer, Double> getClockDomainAndDelay(LeafBlock block) {
        /**
         * This method should only be called for clocked blocks.
         */

        String clockName;
        double clockDelay = 0;

        // If the block is an input
        if(block.getGlobalParent().getCategory() == BlockCategory.IO) {
            clockName = VIRTUAL_IO_CLOCK;

        // If the block is a regular block
        } else{
            // A clocked leaf block has exactly 1 clock pin
            List<AbstractPin> clockPins = block.getClockPins();
            assert(clockPins.size() == 1);
            AbstractPin clockPin = clockPins.get(0);

            while(true) {
                AbstractPin sourcePin = clockPin.getSource();
                if(sourcePin == null) {
                    break;
                }
                
                clockDelay += sourcePin.getPortType().getDelay(clockPin.getPortType(), sourcePin.getOwner().getIndex(), clockPin.getOwner().getIndex(), sourcePin.getIndex(), clockPin.getIndex());
                clockPin = sourcePin;
            }

            clockName = clockPin.getOwner().getName();
            assert(!clockName.equals(VIRTUAL_IO_CLOCK));

            if(!this.clockNamesToDomains.containsKey(clockName)) {
                this.clockNamesToDomains.put(clockName, this.numClockDomains);
                this.clockDomainFanout.put(this.numClockDomains, 0);
                this.numClockDomains++;
            }
        }
        return new Pair<Integer, Double>(this.clockNamesToDomains.get(clockName), clockDelay);
    }

    private void traverseFromSource(TimingNode pathSourceNode, double clockDelay) {

        GlobalBlock pathSourceBlock = pathSourceNode.getGlobalBlock();
        AbstractPin pathSourcePin = pathSourceNode.getPin();

        Map<GlobalBlock, List<TimingEdge>> sourceTimingNets = new HashMap<>();

        Stack<TraversePair> todo = new Stack<>();
        todo.push(new TraversePair(pathSourcePin, clockDelay));

        while(!todo.empty()) {
            TraversePair traverseEntry = todo.pop();
            AbstractPin sourcePin = traverseEntry.pin;
            double delay = traverseEntry.delay;

            AbstractBlock sourceBlock = sourcePin.getOwner();
            PortType sourcePortType = sourcePin.getPortType();

            if((sourcePin.hasTimingNode() && sourcePin != pathSourcePin) || this.isEndpin(sourceBlock, sourcePin, pathSourcePin)) {
                delay += sourcePortType.getSetupTime();
                TimingNode pathSinkNode = sourcePin.getTimingNode();

                // If pathSinkNode is null, this sinkPin doesn't have any sinks
                // so isn't used in the timing graph
                if(pathSinkNode != null) {
                    TimingEdge edge = pathSourceNode.addSink(pathSinkNode, delay, this.circuit.getArchitecture().getDelayTables());
                    this.timingEdges.add(edge);

                    GlobalBlock pathSinkBlock = pathSinkNode.getGlobalBlock();
                    if(pathSinkBlock != pathSourceBlock) {
                        if(!sourceTimingNets.containsKey(pathSinkBlock)) {
                            sourceTimingNets.put(pathSinkBlock, new ArrayList<TimingEdge>());
                        }
                        sourceTimingNets.get(pathSinkBlock).add(edge);
                    }
                }
            } else {
                List<AbstractPin> sinkPins;
                if(sourceBlock.isLeaf() && sourcePin != pathSourcePin) {
                    sinkPins = sourceBlock.getOutputPins();
                } else {
                    sinkPins = sourcePin.getSinks();
                }
                for(AbstractPin sinkPin : sinkPins) {
                    if(sinkPin != null) {
                        double sourceSinkDelay = sourcePortType.getDelay(sinkPin.getPortType(), sourcePin.getOwner().getIndex(), sinkPin.getOwner().getIndex(), sourcePin.getIndex(), sinkPin.getIndex());
                        
                        if(sourceSinkDelay >= 0.0){ // Only consider existing connections
                            double totalDelay = delay + sourceSinkDelay;

                            // Loops around an unclocked block are rare, but not inexistent.
                            // The proper way to handle these is probably to add the loop
                            // delay to all other output pins of this block.
                            if(sinkPin != pathSourcePin) {
                                todo.push(new TraversePair(sinkPin, totalDelay));
                            }
                        }
                    }
                }
            }
        }

        for(List<TimingEdge> timingNet : sourceTimingNets.values()) {
            this.timingNets.add(timingNet);
        }
    }

    private boolean isEndpin(AbstractBlock block, AbstractPin pin, AbstractPin pathSourcePin) {
        return block.isLeaf() && (pin != pathSourcePin) && (block.isClocked() && pin.isInput() || pin.isOutput());
    }

    private class TraversePair {
        AbstractPin pin;
        double delay;

        TraversePair(AbstractPin pin, double delay) {
            this.pin = pin;
            this.delay = delay;
        }
    }

    private void setRootAndLeafNodes(){
    	this.rootNodes = new HashMap<>();
    	this.leafNodes = new HashMap<>();
    
    	for(int clockDomain = 0; clockDomain < this.numClockDomains; clockDomain++) {
    		List<TimingNode> clockDomainRootNodes = new ArrayList<>();
    		List<TimingNode> clockDomainLeafNodes = new ArrayList<>();
    		
	        for (TimingNode timingNode:this.timingNodes) {
	        	if (timingNode.getClockDomain() == clockDomain) {
		        	if (timingNode.getPosition().equals(Position.ROOT)) {
		        		clockDomainRootNodes.add(timingNode);
		        	} else if (timingNode.getPosition().equals(Position.LEAF)) {
		        		clockDomainLeafNodes.add(timingNode);
		        	}
	        	}
	        }
	        
	        this.rootNodes.put(clockDomain, clockDomainRootNodes);
	        this.leafNodes.put(clockDomain, clockDomainLeafNodes);
    	}
    }

    /****************************************************
     * Functionality to find combinational loops with   *
     * Tarjan's strongly connected components algorithm *
     ****************************************************/
    private void cutCombLoop(){

    	long start = System.nanoTime();

    	System.out.println("Cut combinational loops iteratively");

    	int iteration = 0;
    	boolean finalIteration = false;
    	
    	while(!finalIteration){

    		int cutLoops = 0;
    		finalIteration = true;

    		//Initialize iteration
        	this.index = 0;
        	this.stack = new Stack<>();
        	this.scc = new ArrayList<>();
        	for(TimingNode v:this.timingNodes){
        		v.reset();
        	}

        	//Find SCC
        	for(TimingNode v:this.timingNodes){
        		if(v.undefined()){
        			strongConnect(v);
        		}
        	}

        	//Analyze SCC
        	for(SCC scc:this.scc){
        		if(scc.size > 1){
        			this.timingEdges.remove(scc.cutLoop());
        			finalIteration = false;
        			cutLoops++;
        		}
        	}
        	
        	System.out.println("\titeration " + iteration + " | " + cutLoops + " loops cut");
    	}

    	long end = System.nanoTime();
    	double time = (end - start) * 1e-9;

    	System.out.printf("\n\tcut loops took %.2f s\n\n", time);
    }
    private void strongConnect(TimingNode v){
    	v.setIndex(this.index);
    	v.setLowLink(this.index);
    	this.index++;

    	this.stack.add(v);
    	v.putOnStack();

    	for(TimingEdge e:v.getSinkEdges()){
    		TimingNode w = e.getSink();
    		if(w.undefined()){
    			strongConnect(w);
    			
    			int lowLink = Math.min(v.getLowLink(), w.getLowLink());
    			v.setLowLink(lowLink);
    		}else if(w.onStack()){
    			int lowLink = Math.min(v.getLowLink(), w.getIndex());
    			v.setLowLink(lowLink);
    		}
    	}
    	
    	if(v.getLowLink() == v.getIndex()){
    		TimingNode w;
    		SCC scc = new SCC();
    		do{
    			w = this.stack.pop();
    			w.removeFromStack();
    			scc.addElement(w);
    		}while(w != v);
    		this.scc.add(scc);
    	}
    }
    
    public class SCC {
    	//Class for strongly connected component
    	List<TimingNode> elements;
    	int size;

    	SCC(){
    		this.elements = new ArrayList<>();
    		this.size = 0;
    	}

    	void addElement(TimingNode v){
    		this.elements.add(v);
    		this.size++;
    	}

    	TimingEdge cutLoop(){
    		TimingNode v = this.elements.get(this.elements.size()-1);

    		for(TimingEdge e:v.getSinkEdges()){
    			TimingNode w = e.getSink();
    			if(w == this.elements.get(this.elements.size()-2)){
    	    		v.removeSink(e);
    	    		w.removeSource(e);

    	    		return e;
    			}
    		}
    		System.out.println("Edge not cut correctly");
    		return null;
    	}
    }

    /****************************************************************
     * These functions calculate the criticality of all connections *
     ****************************************************************/
    public double getMaxDelay() {
        return this.globalMaxDelay * 1e9;
    }

    public void calculatePlacementEstimatedWireDelay() {
    	for(TimingEdge edge : this.timingEdges) {
    		edge.calculatePlacementEstimatedWireDelay();
    	}
    }
    public void calculateActualWireDelay() {
    	//Set wire delay of the connections
    	for(Connection connection : this.circuit.getConnections()) {
    		double wireDelay = 0.0;
    		for(RouteNode routeNode : connection.routeNodes) {
    			wireDelay += routeNode.getDelay();
    		}
    		connection.timingEdge.setWireDelay(wireDelay);
    	}
    }
    
    
    
    public void calculateArrivalTimes() {
    	//Initialization
        this.globalMaxDelay = 0;
        
    	for(ClockDomain clockDomain : this.clockDomains) {
    		if(clockDomain.hasPaths) {
    			double maxDelay = 0;
                
            	for(TimingNode node : this.timingNodes) {
                    node.resetArrivalTime();
                    node.resetRequiredTime();
                }
                
            	List<TimingNode> clockDomainRootNodes = clockDomain.clockDomainRootNodes;
            	List<TimingNode> clockDomainLeafNodes = clockDomain.clockDomainLeafNodes;
            	
            	//Arrival time
                for(TimingNode rootNode: clockDomainRootNodes){
                	rootNode.setArrivalTime(0.0);
                }
                for(TimingNode leafNode: clockDomainLeafNodes){
                	leafNode.recursiveArrivalTime(clockDomain.sourceClockDomain);
                	if(leafNode.getArrivalTime() > maxDelay){
                		maxDelay = leafNode.getArrivalTime();
                	}
                }
                
                clockDomain.setMaxDelay(maxDelay);
                if(maxDelay > this.globalMaxDelay) {
                	this.globalMaxDelay = maxDelay;
                }
    		}
        }
    }

    public void calculateArricalRequiredAndCriticality(float maxCriticality, float criticalityExponent) {
    	//Initialization
        this.globalMaxDelay = 0;
        
        for(TimingEdge edge : this.timingEdges) {
        	edge.resetSlack();
        }
        
    	for(ClockDomain clockDomain : this.clockDomains) {
    		if(clockDomain.includeDomain()) {
    			List<TimingNode> clockDomainRootNodes = this.rootNodes.get(clockDomain.sourceClockDomain);
        		List<TimingNode> clockDomainLeafNodes = this.leafNodes.get(clockDomain.sinkClockDomain);
        		
        		double maxDelay = 0;
                
            	for(TimingNode node : this.timingNodes) {
                    node.resetArrivalTime();
                    node.resetRequiredTime();
                }
            	
            	//Arrival time
                for(TimingNode rootNode: clockDomainRootNodes){
                	rootNode.setArrivalTime(0.0);
                }
                for(TimingNode leafNode: clockDomainLeafNodes){
                	leafNode.recursiveArrivalTime(clockDomain.sourceClockDomain);
                	if(leafNode.getArrivalTime() > maxDelay){
                		maxDelay = leafNode.getArrivalTime();
                	}
                }
                
                clockDomain.setMaxDelay(maxDelay);
                if(maxDelay > this.globalMaxDelay) {
                	this.globalMaxDelay = maxDelay;
                }

            	//Required time
            	for(TimingNode leafNode: clockDomainLeafNodes) {
            		leafNode.setRequiredTime(0.0);
            	}
                for(TimingNode rootNode: clockDomainRootNodes) {
                	rootNode.recursiveRequiredTime(clockDomain.sinkClockDomain);
                }
                
                //Criticality
                for(Connection connection : this.circuit.getConnections()) {
                	connection.timingEdge.calculateCriticality(maxDelay, maxCriticality, criticalityExponent);
                	connection.setCriticality((float) connection.timingEdge.getCriticality());
                }
    		}
        }
    }
    
//    public String criticalPathToString() {
//    	List<TimingNode> criticalPath = new ArrayList<>();
//		TimingNode node = this.getEndNodeOfCriticalPath();
//		criticalPath.add(node);
//		while(!node.getSourceEdges().isEmpty()){
//    		node = this.getSourceNodeOnCriticalPath(node);
//    		criticalPath.add(node);
//    	}
//    	
//    	int maxLen = 25;
//    	for(TimingNode criticalNode:criticalPath){
//    		if(criticalNode.toString().length() > maxLen){
//    			maxLen = criticalNode.toString().length();
//    		}
//    	}
//    	
//    	System.out.println();
//    	String delay = String.format("Critical path: %.3f ns", this.globalMaxDelay * Math.pow(10, 9));
//    	String result = String.format("%-" + maxLen + "s  %-3s %-3s  %9s %6s %8s\n", delay, "x", "y", "Tarr (ns)", "Clock", "Position");
//    	result += String.format("%-" + maxLen + "s..%3s.%3s..%9s..%5s..%8s\n","","","","","","").replace(" ", "-").replace(".", " ");
//    	for(TimingNode criticalNode:criticalPath){
//    		result += this.printNode(criticalNode, maxLen);
//    	}
//    	
//    	double netDelay = 0.0;
//    	int numNets = 0;
//    	for(TimingNode sinkNode : criticalPath) {
//    		if(sinkNode.getPosition() == Position.C_SINK) {
//    			netDelay += sinkNode.getArrivalTime() - sinkNode.getSourceEdges().get(0).getSource().getArrivalTime();
//    			numNets++;
//    		}
//    	}
//    	
//    	result += "\nNum Nets  " +numNets + "\n";
//    	result += "Net Delay " + String.format("%.2e", netDelay) + "\n";
//    	return result;
//    }
//    private TimingNode getEndNodeOfCriticalPath(){
//    	TimingNode endNode = null;
//    	for(TimingNode leafNode: this.leafNodes){
//    		if(compareDouble(leafNode.getArrivalTime(), this.globalMaxDelay)){
//    			if(endNode == null){
//    				endNode = leafNode;
//    			}else{
//    				System.out.println("Warning: more than one end node has an arrival time equal to the critical path delay");
//    			}
//    		}
//    	}
//    	return endNode;
//    }
//    private TimingNode getSourceNodeOnCriticalPath(TimingNode sinkNode){
//    	TimingNode sourceNode = null;
//		for(TimingEdge edge: sinkNode.getSourceEdges()){
//			if(this.compareDouble(edge.getSource().getArrivalTime(), sinkNode.getArrivalTime() - edge.getTotalDelay())){
//				if(sourceNode == null){
//					sourceNode = edge.getSource();
//				}else{
//					sourceNode = edge.getSource();
//					System.out.println("Warning: more than one source node on the critical path");
//				}
//			}
//		}
//		return sourceNode;
//    }
//    private String printNode(TimingNode node, int maxLen){
//    	String nodeInfo = node.toString();
//    	int x = node.getGlobalBlock().getColumn();
//    	int y = node.getGlobalBlock().getRow();
//    	double delay = node.getArrivalTime() * Math.pow(10, 9);
//    	int clock = node.getClockDomain();
//    	String pos = "" + node.getPosition();
//    	
//    	return String.format("%-" + maxLen + "s  %3d %3d  %9.3f  %5d  %8s\n", nodeInfo, x, y, delay, clock, pos);
//    }
//    private boolean compareDouble(double var1, double var2){
//    	return Math.abs(var1 - var2) < Math.pow(10, -12);
//    }
    
    public double calculateTotalCost() {
    	double totalCost = 0;

    	for(List<TimingEdge> net : this.timingNets) {
    		double netCost = 0;
    		for(TimingEdge edge : net) {
    			double cost = edge.getCost();
    			if(cost > netCost) {
    				netCost = cost;
    			}
    		}
 
    		totalCost += netCost;
    	}

    	return totalCost;
    }
    
    private void setClockDomains() {
    	for(TimingNode node : this.timingNodes) {
    		node.setNumClockDomains(this.numClockDomains);
    	}
    	
		for(TimingNode node : this.timingNodes) {
    		if(node.getPosition() == Position.LEAF) {
    	    	node.setSourceClockDomains();
    		} else if(node.getPosition() == Position.ROOT) {
        		node.setSinkClockDomains();
    		}
    	}
		
    	this.clockDomains = new ArrayList<>();
    	for(int sourceClockDomain = 0; sourceClockDomain < this.numClockDomains; sourceClockDomain++) {
    		for(int sinkClockDomain = 0; sinkClockDomain < this.numClockDomains; sinkClockDomain++) {
        		ClockDomain clockDomain = new ClockDomain(sourceClockDomain, sinkClockDomain, this.rootNodes.get(sourceClockDomain), this.leafNodes.get(sinkClockDomain));
        		this.clockDomains.add(clockDomain);
    		}
    	}
    }
    
    public void printDelays() {
		System.out.printf("Max delay %.3f ns\n", 1e9 * this.globalMaxDelay);
		System.out.printf("Timing cost %.3e\n", this.calculateTotalCost());
		System.out.println();
		
		for(ClockDomain clockDomain : this.clockDomains) {
			System.out.println(clockDomain);
		}
		System.out.println();
		
		/* Calculate geometric mean f_max (fanout-weighted and unweighted) from the diagonal (intra-domain) entries of critical_path_delay, 
		excluding domains without intra-domain paths (for which the timing constraint is DO_NOT_ANALYSE) and virtual clocks. */

		double geomeanPeriod = 1;
		double fanoutWeightedGeomeanPeriod = 0;
		int fanout = 0, totalFanout = 0;
		int numValidClockDomains = 0;
		for(ClockDomain clockDomain : this.clockDomains) {
			if(clockDomain.includeDomain()) {
				geomeanPeriod *= clockDomain.getMaxDelay();
				fanout = this.clockDomainFanout.get(clockDomain.sinkClockDomain);
				totalFanout += fanout;
				fanoutWeightedGeomeanPeriod += Math.log(clockDomain.getMaxDelay()) * fanout;    
				
				numValidClockDomains++;
			}
		}
		
		geomeanPeriod = Math.pow(geomeanPeriod, 1.0/numValidClockDomains);
		fanoutWeightedGeomeanPeriod = Math.exp(fanoutWeightedGeomeanPeriod/totalFanout);

		System.out.printf("Geometric mean intra-domain period: %.3f ns\n", 1e9 * geomeanPeriod);
		System.out.printf("Fanout-weighted geomean intra-domain period: %.3f ns\n", 1e9 * fanoutWeightedGeomeanPeriod);
		System.out.println();
    }
}
