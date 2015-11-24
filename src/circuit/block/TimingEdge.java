package circuit.block;

public class TimingEdge {

    private double fixedDelay, totalDelay, criticality;
    private transient double stagedTotalDelay;


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
        return this.totalDelay;
    }
    public void setWireDelay(double wireDelay) {
        this.totalDelay = this.fixedDelay + wireDelay;
    }

    double getStagedTotalDelay() {
        return this.stagedTotalDelay;
    }
    void setStagedWireDelay(double stagedWireDelay) {
        this.stagedTotalDelay = this.fixedDelay + stagedWireDelay;
    }

    public double getCriticality() {
        return this.criticality;
    }
    void setCriticality(double criticality) {
        this.criticality = criticality;
    }


    void pushThrough() {
        this.totalDelay = this.stagedTotalDelay;
    }



    @Override
    public String toString() {
        return String.format("%e", this.totalDelay);
    }
}
