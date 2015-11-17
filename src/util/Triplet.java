package util;

public class Triplet<F extends Comparable<F>, S, T> implements Comparable<Triplet<F, S, T>> {

    private F first;
    private S second;
    private T third;

    public Triplet(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public void setFirst(F first) {
        this.first = first;
    }
    public void setSecond(S second) {
        this.second = second;
    }
    public void setThird(T third) {
        this.third = third;
    }

    public F getFirst() {
        return this.first;
    }
    public S getSecond() {
        return this.second;
    }
    public T getThird() {
        return this.third;
    }

    @Override
    public int compareTo(Triplet<F, S, T> otherTriplet) {
        return this.first.compareTo(otherTriplet.first);
    }
}
