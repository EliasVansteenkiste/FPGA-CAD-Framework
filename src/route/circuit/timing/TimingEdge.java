package route.circuit.timing;

import route.circuit.architecture.BlockCategory;
import route.circuit.architecture.DelayTables;

public class TimingEdge {
	private double fixedDelay, wireDelay;
	private double slack, criticality;
	
    private final DelayTables delayTables;
	private final TimingNode source, sink;

	private double stagedWireDelay;

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

    public double calculateWireDelay(){
        int deltaX = Math.abs(this.source.getGlobalBlock().getColumn() - this.sink.getGlobalBlock().getColumn());
        int deltaY = Math.abs(this.source.getGlobalBlock().getRow() - this.sink.getGlobalBlock().getRow());

        BlockCategory fromCategory = this.source.getGlobalBlock().getCategory();
        BlockCategory toCategory = this.sink.getGlobalBlock().getCategory();

        return this.delayTables.getDelay(fromCategory, toCategory, deltaX, deltaY);
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


    /*************************************************
     * Functions that facilitate simulated annealing *
     *************************************************/
    void setStagedWireDelay(double stagedWireDelay){
    	this.stagedWireDelay = stagedWireDelay;
    }
    void resetStagedDelay(){
    	this.stagedWireDelay = 0.0;
    }
    
    void pushThrough(){
    	this.wireDelay = this.stagedWireDelay;
    }
    
    double getDeltaCost(){
    	return this.criticality * (this.stagedWireDelay - this.wireDelay);
    }


    @Override
    public String toString() {
        return String.format("%e+%e", this.fixedDelay, this.wireDelay);
    }
}