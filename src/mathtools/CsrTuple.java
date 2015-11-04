package mathtools;

import java.lang.Comparable;

class CsrTuple implements Comparable<CsrTuple> {
    private int index;
    private double value;

    CsrTuple(int index, double value) {
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
    public int compareTo(CsrTuple otherTuple) {
        return Integer.compare(this.index, otherTuple.index);
    }

    @Override
    public String toString() {
        return String.format("(%d, %g)", this.index, this.value);
    }
}
