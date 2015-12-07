package placers.analyticalplacer;

abstract class CostCalculator {
    private boolean ints;
    private int[] intX, intY;
    private double[] doubleX, doubleY;

    protected abstract double calculate(boolean recalculateCriticalities);

    double calculate(int[] x, int[] y, boolean recalculateCriticalities) {
        this.intX = x;
        this.intY = y;
        this.ints = true;

        return this.calculate(recalculateCriticalities);
    }
    double calculate(double[] x, double[] y) {
        this.doubleX = x;
        this.doubleY = y;
        this.ints = false;

        return this.calculate(true);
    }


    protected double getX(int index) {
        if(this.ints) {
            return this.intX[index];
        } else {
            return this.doubleX[index];
        }
    }
    protected double getY(int index) {
        if(this.ints) {
            return this.intY[index];
        } else {
            return this.doubleY[index];
        }
    }

    protected boolean isInts() {
        return this.ints;
    }
}
