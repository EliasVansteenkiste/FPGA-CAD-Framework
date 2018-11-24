package route.circuit.timing;

import java.util.ArrayList;
import java.util.List;

import route.circuit.architecture.DelayTables;
import route.circuit.block.GlobalBlock;
import route.circuit.pin.AbstractPin;

public class TimingNode {

    public enum Position {ROOT, INTERMEDIATE, LEAF, C_SOURCE, C_SINK};

    private final GlobalBlock globalBlock;
    private final AbstractPin pin;
    private final int clockDomain;

    private final Position position;

    private final ArrayList<TimingEdge> sourceEdges = new ArrayList<>();
    private final ArrayList<TimingEdge> sinkEdges = new ArrayList<>();
    private int numSources = 0, numSinks = 0;

    private double arrivalTime, requiredTime;
    private boolean hasArrivalTime, hasRequiredTime;

    //Tarjan's strongly connected components algorithm
    private int index;
    private int lowLink;
    private boolean onStack;

    TimingNode(GlobalBlock globalBlock, AbstractPin pin, Position position, int clockDomain) {
        this.pin = pin;

        this.globalBlock = globalBlock;
        this.globalBlock.addTimingNode(this);

        this.position = position;
        
        this.clockDomain = clockDomain;
    }

    void compact() {
    	this.sourceEdges.trimToSize();
        this.sinkEdges.trimToSize();
    }

    public GlobalBlock getGlobalBlock() {
        return this.globalBlock;
    }
    public AbstractPin getPin() {
        return this.pin;
    }
    public Position getPosition() {
        return this.position;
    }
    
    public int getClockDomain() {
    	return this.clockDomain;
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
    
    @Override
    public String toString() {
        return this.pin.toString();
    }
}
