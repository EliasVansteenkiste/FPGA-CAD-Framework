package place.circuit.timing;

import java.util.ArrayList;
import java.util.List;

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

    private final ArrayList<TimingEdge> sourceEdges = new ArrayList<>();
    private final ArrayList<TimingEdge> sinkEdges = new ArrayList<>();
    private int numSources = 0, numSinks = 0;

    private double arrivalTime, requiredTime;
    private boolean hasArrivalTime, hasRequiredTime;

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

    void compact() {
    	this.sourceEdges.trimToSize();
        this.sinkEdges.trimToSize();
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
        this.sourceEdges.add(edge);
        this.numSources++;
    }
    TimingEdge addSink(TimingNode sink, double delay, DelayTables delayTables) {
        TimingEdge edge = new TimingEdge(delay, this, sink, delayTables);

        this.sinkEdges.add(edge);
        this.numSinks++;

        sink.addSource(this, edge);

        return edge;
    }

    void removeSource(TimingEdge source){
    	if(this.sourceEdges.contains(source)){
    		this.sourceEdges.remove(source);
    		this.numSources--;
    	}else{
    		System.out.println("This node does not contain source edge");
    	}
    }
    void removeSink(TimingEdge sink){
    	if(this.sinkEdges.contains(sink)){
    		this.sinkEdges.remove(sink);
    		this.numSinks--;
    	}else{
    		System.out.println("This sink does not contain sink edge");
    	}
    }

    public List<TimingEdge> getSources() {
        return this.sourceEdges;
    }
    public List<TimingEdge> getSinks() {
        return this.sinkEdges;
    }

    public int getNumSources() {
        return this.numSources;
    }
    public int getNumSinks() {
        return this.numSinks;
    }

    public TimingEdge getSourceEdge(int sourceIndex) {
        return this.sourceEdges.get(sourceIndex);
    }
    public TimingEdge getSinkEdge(int sinkIndex) {
        return this.sinkEdges.get(sinkIndex);
    }

    //Arrival time
    void resetArrivalTime() {
        this.hasArrivalTime = false;
    }
    void setArrivalTime(double arrivalTime) {
    	this.arrivalTime = arrivalTime;
    	this.hasArrivalTime = true;
    }
    boolean hasArrivalTime() {
    	return this.hasArrivalTime;
    }
    double getArrivalTime() {
    	return this.arrivalTime;
    }
	double recursiveArrivalTime() {
		if(this.hasArrivalTime()) {
			return this.getArrivalTime();
		} else {
			double maxArrivalTime = 0.0;
			for(TimingEdge edge:this.sourceEdges) {
				double localArrivalTime = edge.getSource().recursiveArrivalTime() + edge.getTotalDelay();
				if(localArrivalTime > maxArrivalTime) {
					maxArrivalTime = localArrivalTime;
				}
			}
			this.setArrivalTime(maxArrivalTime);
			return this.getArrivalTime();
		}		
	}

	//Required time
    void resetRequiredTime() {
    	this.hasRequiredTime = false;
    }
    void setRequiredTime(double requiredTime) {
    	this.requiredTime = requiredTime;
    	this.hasRequiredTime = true;
    }
    boolean hasRequiredTime() {
    	return this.hasRequiredTime;
    }
    double getRequiredTime() {
    	return this.requiredTime;
    }
    double recursiveRequiredTime() {
    	if(this.hasRequiredTime()) {
    		return this.getRequiredTime();
    	}else {
			double minRequiredTime = 0.0;
			for(TimingEdge edge:this.sinkEdges) {
				double localRequiredTime = edge.getSink().recursiveRequiredTime() - edge.getTotalDelay();
				if(localRequiredTime < minRequiredTime) {
					minRequiredTime = localRequiredTime;
				}
			}
			this.setRequiredTime(minRequiredTime);
			return this.getRequiredTime();
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
    		TimingEdge edge = this.sinkEdges.get(sinkIndex);
    		TimingNode sink = edge.getSink();

    		cost += this.calculateDeltaCost(sink, edge);
    	}

    	for(int sourceIndex = 0; sourceIndex < this.numSources; sourceIndex++) {
    		TimingEdge edge = this.sourceEdges.get(sourceIndex);
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