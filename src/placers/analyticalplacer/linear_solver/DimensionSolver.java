package placers.analyticalplacer.linear_solver;

public abstract class DimensionSolver {

    void addConnectionMinMaxUnknown(
            boolean fixed1, int index1, double coordinate1,
            boolean fixed2, int index2, double coordinate2,
            double weightMultiplier, boolean isPseudoConnection) {

        if(coordinate1 < coordinate2) {
            this.addConnection(
                    fixed1, index1, coordinate1,
                    fixed2, index2, coordinate2,
                    weightMultiplier, isPseudoConnection);
        } else {
            this.addConnection(
                    fixed2, index2, coordinate2,
                    fixed1, index1, coordinate1,
                    weightMultiplier, isPseudoConnection);
        }
    }

    abstract void addConnection(
            boolean minFixed, int minIndex, double minCoordinate,
            boolean maxFixed, int maxIndex, double maxCoordinate,
            double weightMultiplier, boolean isPseudoConnection);

    abstract void solve();
}
