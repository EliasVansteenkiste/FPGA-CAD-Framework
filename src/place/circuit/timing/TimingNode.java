package place.circuit.timing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import place.circuit.architecture.DelayTables;
import place.circuit.block.GlobalBlock;
import place.circuit.block.LeafBlock;
import place.circuit.pin.LeafPin;

public class TimingNode {

    public enum Position {ROOT, INTERMEDIATE, LEAF};

    private LeafBlock block;
    private GlobalBlock globalBlock;
    private LeafPin pin;

    private Position position;

    private TimingEdge[] sourceEdges;
    private TimingEdge[] sinkEdges;
    private List<TimingEdge> tempSourceEdges = new ArrayList<>();
    private  List<TimingEdge> tempSinkEdges = new ArrayList<>();
    private int numSources = 0, numSinks = 0;

    private volatile double arrivalTime, requiredTime;

    //Tarjan's strongly connected components algorithm
    private int index;
    private int lowLink;
    private boolean onStack;

    TimingNode(LeafBlock block, LeafPin pin, Position position, int clockDomain) {
        this.block = block;
        this.pin = pin;

        this.globalBlock = block.getGlobalParent();
        this.globalBlock.addTimingNode(this);

        this.position = position;
    }

    void finish() {
        this.sourceEdges = new TimingEdge[tempSourceEdges.size()];
        this.tempSourceEdges.toArray(this.sourceEdges);
        this.tempSourceEdges.clear();
        this.tempSourceEdges = null;
        
        this.sinkEdges = new TimingEdge[tempSinkEdges.size()];
        this.tempSinkEdges.toArray(this.sinkEdges);
        this.tempSinkEdges.clear();
        this.tempSinkEdges = null;
    }

    public LeafBlock getBlock() {
        return this.block;
    }
    public GlobalBlock getGlobalBlock() {
        return this.globalBlock;
    }
    public LeafPin getPin() {
        return this.pin;
    }
    public Position getPosition() {
        return this.position;
    }

    private void addSource(TimingNode source, TimingEdge edge) {
        this.tempSourceEdges.add(edge);
        this.numSources++;
    }
    TimingEdge addSink(TimingNode sink, double delay, DelayTables delayTables) {
        TimingEdge edge = new TimingEdge(delay, this, sink, delayTables);

        this.tempSinkEdges.add(edge);
        this.numSinks++;

        sink.addSource(this, edge);

        return edge;
    }

    void removeSource(TimingEdge source){
    	if(this.tempSourceEdges.contains(source)){
    		this.tempSourceEdges.remove(source);
    		this.numSources--;
    	}else{
    		System.out.println("This node does not contain source edge");
    	}
    }
    void removeSink(TimingEdge sink){
    	if(this.tempSinkEdges.contains(sink)){
    		this.tempSinkEdges.remove(sink);
    		this.numSinks--;
    	}else{
    		System.out.println("This sink does not contain sink edge");
    	}
    }


    public TimingEdge[] getSinks() {
        return this.sinkEdges;
    }
    public int getNumSinks() {
        return this.numSinks;
    }
    public TimingEdge getSinkEdge(int sinkIndex) {
        return this.sinkEdges[sinkIndex];
    }

    
    //Arrival time
    public void setArrivalTime(double arrivalTime) {
    	this.arrivalTime = arrivalTime;
    }
    public double getArrivalTime() {
    	return this.arrivalTime;
    }
    
	public void recursiveArrivalTimeTraversal(List<TimingNode> traversal, Set<TimingNode> visitedNodes) {
		if(this.position.equals(Position.ROOT)){
			return;
		}else if(visitedNodes.contains(this)){
			return;
		} else {
			for(TimingEdge edge:this.sourceEdges) {
				edge.getSource().recursiveArrivalTimeTraversal(traversal, visitedNodes);
			}
			traversal.add(this);
			visitedNodes.add(this);
		}		
	}
	public void calculateArrivalTime() {
		double maxArrivalTime = 0.0;
		for(TimingEdge edge:this.sourceEdges) {
			double localArrivalTime = edge.getSource().arrivalTime + edge.getTotalDelay();
			if(localArrivalTime > maxArrivalTime) {
				maxArrivalTime = localArrivalTime;
			}
		}
		this.arrivalTime = maxArrivalTime;	
	}

	//Required time
    public void setRequiredTime(double requiredTime) {
    	this.requiredTime = requiredTime;
    }
    public double getRequiredTime() {
    	return this.requiredTime;
    }
    public void calculateRequiredTime() {
		double minRequiredTime = 0.0;
		for(TimingEdge edge:this.sinkEdges) {
			double localRequiredTime = edge.getSink().requiredTime - edge.getTotalDelay();
			if(localRequiredTime < minRequiredTime) {
				minRequiredTime = localRequiredTime;
			}
		}
		this.requiredTime = minRequiredTime;
    }
    void recursiveRequiredTimeTraversal(List<TimingNode> traversal, Set<TimingNode> visitedNodes) {
    	if(this.position.equals(Position.LEAF)){
			return;
		}else if(visitedNodes.contains(this)){
			return;
    	}else {
			for(TimingEdge edge:this.sinkEdges) {
				edge.getSink().recursiveRequiredTimeTraversal(traversal, visitedNodes);
			}
			traversal.add(this);
			visitedNodes.add(this);
    	}
    }


   /****************************************************
    * Tarjan's strongly connected components algorithm *
    ****************************************************/
    void reset(){
    	this.index = -1;
    	this.lowLink = -1;
    	this.onStack = false;
    }
    boolean undefined(){
    	return this.index == -1;
    }
    void setIndex(int index){
    	this.index = index;
    }
    void setLowLink(int lowLink){
    	this.lowLink = lowLink;
    }
    int getIndex(){
    	return this.index;
    }
    int getLowLink(){
    	return this.lowLink;
    }
    void putOnStack(){
    	this.onStack = true;
    }
    void removeFromStack(){
    	this.onStack = false;
    }
    boolean onStack(){
    	return this.onStack;
    }


    /*************************************************
     * Functions that facilitate simulated annealing *
     *************************************************/

    double calculateDeltaCost(GlobalBlock otherBlock) {
    	/*
    	 * When this method is called, we assume that this block and
    	 * the block with which this block will be swapped, already
    	 * have their positions updated (temporarily).
    	 */
    	double cost = 0;

    	for(int sinkIndex = 0; sinkIndex < this.numSinks; sinkIndex++) {
    		TimingEdge edge = this.sinkEdges[sinkIndex];
    		TimingNode sink = edge.getSink();

    		cost += this.calculateDeltaCost(sink, edge);
    	}

    	for(int sourceIndex = 0; sourceIndex < this.numSources; sourceIndex++) {
    		TimingEdge edge = this.sourceEdges[sourceIndex];
    		TimingNode source = edge.getSource();

    		if(source.globalBlock != otherBlock) {
    			cost += this.calculateDeltaCost(source, edge);
    		}
    	}

    	return cost;
    }
    private double calculateDeltaCost(TimingNode otherNode, TimingEdge edge){
    	if(otherNode.globalBlock == this.globalBlock){
    		edge.resetStagedDelay();
    		return 0;
    	}else{
    		edge.setStagedWireDelay(edge.calculateWireDelay());
    		return edge.getDeltaCost();
    	}
    }

    void pushThrough(){
    	for(TimingEdge edge : this.sinkEdges){
    		edge.pushThrough();
    	}
    	for(TimingEdge edge : this.sourceEdges){
    		edge.pushThrough();
    	}
    }


    @Override
    public String toString() {
        return this.pin.toString();
    }
}