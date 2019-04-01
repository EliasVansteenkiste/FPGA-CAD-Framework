package place.circuit.timing;

import place.circuit.architecture.BlockCategory;
import place.circuit.architecture.DelayTables;

public class TimingEdge {
	private float fixedDelay, wireDelay;
	private float slack, criticality;
	private boolean hasCriticality;
	
    private final DelayTables delayTables;
	private final TimingNode source, sink;

    TimingEdge(float fixedDelay, TimingNode source, TimingNode sink, DelayTables delayTables){
        this.fixedDelay = fixedDelay;
        
        this.source = source;
        this.sink = sink;
        
        this.delayTables = delayTables;
    }

    public float getFixedDelay(){
        return this.fixedDelay;
    }
    void setFixedDelay(float fixedDelay){
        this.fixedDelay = fixedDelay;
    }

    public float calculateWireDelay(){
        int deltaX = Math.abs(this.source.getGlobalBlock().getColumn() - this.sink.getGlobalBlock().getColumn());
        int deltaY = Math.abs(this.source.getGlobalBlock().getRow() - this.sink.getGlobalBlock().getRow());

        BlockCategory fromCategory = this.source.getGlobalBlock().getCategory();
        BlockCategory toCategory = this.sink.getGlobalBlock().getCategory();

        return this.delayTables.getDelay(fromCategory, toCategory, deltaX, deltaY);
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

    void resetSlack(){
        this.hasCriticality = false;
    }
    void setSlack(float slack){
    	this.slack = slack;	
    }
    float getSlack(){
        return this.slack;
    }

    void setCriticality(float criticality){
    	if(!this.hasCriticality) {
    		this.criticality = criticality;
    		this.hasCriticality = true;
    	}else if(criticality > this.criticality) {
    		this.criticality = criticality;
    	}
    }
    public float getCriticality(){
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