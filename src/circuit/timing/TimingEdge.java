package circuit.timing;

public class TimingEdge {

    private double fixedDelay, wireDelay;
    private double slack, criticality;
    private double stagedWireDelay;


    TimingEdge(double fixedDelay) {
        this.fixedDelay = fixedDelay;
    }


    public double getFixedDelay() {
        return this.fixedDelay;
    }
    void setFixedDelay(double fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    public double getTotalDelay() {
        return this.fixedDelay + this.wireDelay;
    }
    public void setWireDelay(double wireDelay) {
        this.wireDelay = wireDelay;
    }

    public double getCost() {
        return this.criticality * this.wireDelay;
    }


    void resetSlack() {
        this.slack = 0;
        this.criticality = 0;
    }
    void setSlack(double slack) {
        this.slack = slack;
    }
    double getSlack() {
        return this.slack;
    }

    void calculateCriticality(double maxDelay, double criticalityExponent) {
        this.criticality = Math.pow(1 - (maxDelay + this.slack) / maxDelay, criticalityExponent);
    }
    public double getCriticality() {
        return this.criticality;
    }


    /*************************************************
     * Functions that facilitate simulated annealing *
     *************************************************/

    void setStagedWireDelay(double stagedWireDelay) {
        this.stagedWireDelay = stagedWireDelay;
    }
    void resetStagedDelay() {
        this.stagedWireDelay = 0;
    }

    void pushThrough() {
        this.wireDelay = this.stagedWireDelay;
    }

    double getDeltaCost() {
        return this.criticality * (this.stagedWireDelay - this.wireDelay);
    }



    @Override
    public String toString() {
        return String.format("%e+%e", this.fixedDelay, this.wireDelay);
    }
}
