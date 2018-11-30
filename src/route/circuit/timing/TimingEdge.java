package route.circuit.timing;

import route.circuit.architecture.BlockCategory;
import route.circuit.architecture.DelayTables;

public class TimingEdge {
	private double fixedDelay, wireDelay;
	private double slack, criticality;
	
	private final DelayTables delayTables;
	private final TimingNode source, sink;

    TimingEdge(double fixedDelay, TimingNode source, TimingNode sink, DelayTables delayTables){
        this.fixedDelay = fixedDelay;
        this.wireDelay = 0;
        
        this.source = source;
        this.sink = sink;
        
        this.delayTables = delayTables;
    }
    
    public TimingNode getSource(){
    	return this.source;
    }
    public TimingNode getSink(){
    	return this.sink;
    }

    public double getFixedDelay(){
        return this.fixedDelay;
    }
    public void setFixedDelay(double fixedDelay){
        this.fixedDelay = fixedDelay;
    }

    public void calculatePlacementEstimatedWireDelay(){
        int deltaX = Math.abs(this.source.getGlobalBlock().getColumn() - this.sink.getGlobalBlock().getColumn());
        int deltaY = Math.abs(this.source.getGlobalBlock().getRow() - this.sink.getGlobalBlock().getRow());

        BlockCategory fromCategory = this.source.getGlobalBlock().getCategory();
        BlockCategory toCategory = this.sink.getGlobalBlock().getCategory();

        this.wireDelay = this.delayTables.getDelay(fromCategory, toCategory, deltaX, deltaY);
    }
    public void setWireDelay(double wireDelay){
        this.wireDelay = wireDelay;
    }

    public double getTotalDelay(){
        return this.fixedDelay + this.wireDelay;
    }

    public double getCost() {
        return this.criticality * this.wireDelay;
    }

    void resetSlack(){
        this.slack = 0;
        this.criticality = 0;
    }

    public double getCriticality(){
        return this.criticality;
    }
    
    public void calculateCriticality(double maxDelay, double maxCriticality, double criticalityExponent) {
    	if(this.source.hasArrivalTime() && this.sink.hasRequiredTime()) {
        	this.slack = this.sink.getRequiredTime() - this.source.getArrivalTime() - this.getTotalDelay();
        	double tempCriticality  = (1 - (maxDelay + this.slack) / maxDelay);
        	tempCriticality = Math.pow(tempCriticality, criticalityExponent) * maxCriticality;
        	
        	if(tempCriticality > this.criticality) this.criticality = tempCriticality;
    	}
    }

    @Override
    public String toString() {
        return String.format("%e+%e", this.fixedDelay, this.wireDelay);
    }
}
