package mathtools;

import java.lang.Comparable;

class FastCrsTuple implements Comparable<FastCrsTuple> {
    private int index;
    private double value;

    FastCrsTuple(int index, double value) {
        this.index = index;
        this.value = value;
    }

    int getIndex() {
        return this.index;
    }

    double getValue() {
        return this.value;
    }


    @Override
    public int compareTo(FastCrsTuple otherTuple) {
        return Integer.compare(this.index, otherTuple.index);
    }

    @Override
    public String toString() {
        return String.format("(%d, %g)", this.index, this.value);
    }
}
