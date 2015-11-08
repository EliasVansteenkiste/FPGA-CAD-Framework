package circuit.block;


public class TimingGraphEntry {
    private GlobalBlock source;
    private GlobalBlock sink;
    private double criticality;
    private int numSinks;

    TimingGraphEntry(GlobalBlock source, GlobalBlock sink, double criticality, int numSinks) {
        this.source = source;
        this.sink = sink;
        this.criticality = criticality;
        this.numSinks = numSinks;
    }

    public GlobalBlock getSource() {
        return this.source;
    }
    public GlobalBlock getSink() {
        return this.sink;
    }
    public double getCriticality() {
        return this.criticality;
    }
    public int getNumSinks() {
        return this.numSinks;
    }
}
