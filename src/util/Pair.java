package util;

public class Pair<F extends Comparable<F>, S> implements Comparable<Pair<F, S>> {

    private F first;
    private S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public void setFirst(F first) {
        this.first = first;
    }
    public void setSecond(S second) {
        this.second = second;
    }

    public F getFirst() {
        return this.first;
    }
    public S getSecond() {
        return this.second;
    }


    @Override
    public int compareTo(Pair<F, S> otherPair) {
        return this.first.compareTo(otherPair.first);
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", this.first, this.second);
    }
}
