package circuit.block;

class TimingEdge {

    private double fixedDelay, totalDelay, criticality;
    private transient double stagedTotalDelay;


    public TimingEdge(double fixedDelay) {
        this.fixedDelay = fixedDelay;
    }


    public double getFixedDelay() {
        return this.fixedDelay;
    }
    public void setFixedDelay(double fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    public double getTotalDelay() {
        return this.totalDelay;
    }
    public void setWireDelay(double wireDelay) {
        this.totalDelay = this.fixedDelay + wireDelay;
    }

    public double getStagedTotalDelay() {
        return this.stagedTotalDelay;
    }
    public void setStagedWireDelay(double stagedWireDelay) {
        this.stagedTotalDelay = this.fixedDelay + stagedWireDelay;
    }

    public double getCriticality() {
        return this.criticality;
    }
    public void setCriticality(double criticality) {
        this.criticality = criticality;
    }


    public void pushThrough() {
        this.totalDelay = this.stagedTotalDelay;
    }



    @Override
    public String toString() {
        return String.format("%e", this.totalDelay);
    }
}
