package route.circuit.timing;

public class TimingEdge {
	private double fixedDelay, wireDelay;
	private double slack, criticality;
	
	private final TimingNode source, sink;

    TimingEdge(double fixedDelay, TimingNode source, TimingNode sink){
        this.fixedDelay = fixedDelay;
        this.wireDelay = 0;
        
        this.source = source;
        this.sink = sink;
    }

    public double getFixedDelay(){
        return this.fixedDelay;
    }
    public void setFixedDelay(double fixedDelay){
        this.fixedDelay = fixedDelay;
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

    @Override
    public String toString() {
        return String.format("%e+%e", this.fixedDelay, this.wireDelay);
    }
}