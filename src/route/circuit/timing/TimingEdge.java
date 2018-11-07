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
    public double getWireDelay() {
    	return this.wireDelay;
    }

    public double getTotalDelay(){
        return this.fixedDelay + this.wireDelay;
    }

    public double getCost() {
        return this.criticality * this.wireDelay;
    }

    void resetSlack(){
        this.slack = 0.0;
        this.criticality = 0;
    }
    void setSlack(double slack){
        this.slack = slack;
    }
    double getSlack(){
        return this.slack;
    }

    void setCriticality(double criticality){
        this.criticality = criticality;
    }
    public double getCriticality(){
        return this.criticality;
    }

    public TimingNode getSource(){
    	return this.source;
    }
    public TimingNode getSink(){
    	return this.sink;
    }
    
    public void calculateCriticality(double globalMaxDelay, double maxCriticality, double criticalityExponent) {
    	this.slack = this.sink.getRequiredTime() - this.source.getArrivalTime() - this.getTotalDelay();
    	double criticality  = (1 - (globalMaxDelay + this.slack) / globalMaxDelay);
    	this.criticality = Math.pow(criticality, criticalityExponent) * maxCriticality;
    }

    @Override
    public String toString() {
        return String.format("%e+%e", this.fixedDelay, this.wireDelay);
    }
}