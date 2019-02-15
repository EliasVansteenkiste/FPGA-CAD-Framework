package route.circuit.timing;

import route.circuit.architecture.BlockCategory;
import route.circuit.architecture.DelayTables;

public class TimingEdge {
	private float fixedDelay, wireDelay;
	private float slack, criticality;
	
	private final DelayTables delayTables;
	private final TimingNode source, sink;

    TimingEdge(float fixedDelay, TimingNode source, TimingNode sink, DelayTables delayTables){
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

    public void calculatePlacementEstimatedWireDelay(){
        int deltaX = Math.abs(this.source.getGlobalBlock().getColumn() - this.sink.getGlobalBlock().getColumn());
        int deltaY = Math.abs(this.source.getGlobalBlock().getRow() - this.sink.getGlobalBlock().getRow());

        BlockCategory fromCategory = this.source.getGlobalBlock().getCategory();
        BlockCategory toCategory = this.sink.getGlobalBlock().getCategory();

        this.wireDelay = this.delayTables.getDelay(fromCategory, toCategory, deltaX, deltaY);
    }
    public void setWireDelay(float wireDelay){
        this.wireDelay = wireDelay;
    }

    public float getTotalDelay(){
        return this.fixedDelay + this.wireDelay;
    }

    public float getCost() {
        return this.criticality * this.wireDelay;
    }

    public void resetCriticality(){
        this.criticality = 0;
    }

    public float getCriticality(){
        return this.criticality;
    }
    
    public void calculateCriticality(float maxDelay, float maxCriticality, float criticalityExponent) {
    	if(this.source.hasArrivalTime() && this.sink.hasRequiredTime()) {
        	this.slack = this.sink.getRequiredTime() - this.source.getArrivalTime() - this.getTotalDelay();
        	float tempCriticality  = (1 - this.slack / maxDelay);
        	tempCriticality = (float) (Math.pow(tempCriticality, criticalityExponent) * maxCriticality);
        	
        	if(tempCriticality > this.criticality) this.criticality = tempCriticality;
    	}
    }

    @Override
    public String toString() {
        return String.format("%e+%e", this.fixedDelay, this.wireDelay);
    }
}
