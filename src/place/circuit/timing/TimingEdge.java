package place.circuit.timing;

import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.DelayTables;

public class TimingEdge {
	private double fixedDelay, wireDelay;
	private double slack, criticality;
	
    private final DelayTables delayTables;
	private final TimingNode source, sink;

    TimingEdge(double fixedDelay, TimingNode source, TimingNode sink, DelayTables delayTables){
        this.fixedDelay = fixedDelay;
        
        this.source = source;
        this.sink = sink;
        
        this.delayTables = delayTables;
    }

    public double getFixedDelay(){
        return this.fixedDelay;
    }
    void setFixedDelay(double fixedDelay){
        this.fixedDelay = fixedDelay;
    }

    public double getWireDelay(){
    	return this.wireDelay;
    }
    public void calculateWireDelay(){
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

    @Override
    public String toString() {
        return String.format("%e+%e", this.fixedDelay, this.wireDelay);
    }
}