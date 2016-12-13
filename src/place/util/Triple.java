package place.util;

import java.io.Serializable;

public class Triple<F extends Comparable<F>, S, T> implements Comparable<Triple<F, S, T>>, Serializable {

    private static final long serialVersionUID = 4434847305741889772L;

    private F first;
    private S second;
    private T third;

    public Triple(F first, S second, T third) {
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
    public int compareTo(Triple<F, S, T> otherTriplet) {
        return this.first.compareTo(otherTriplet.first);
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.first == null) ? 0 : this.first.hashCode());
        result = prime * result + ((this.second == null) ? 0 : this.second.hashCode());
        result = prime * result + ((this.third == null) ? 0 : this.third.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        Triple other = (Triple) obj;
        if(this.first == null) {
            if(other.first != null)
                return false;
        } else if(!this.first.equals(other.first))
            return false;
        if(this.second == null) {
            if(other.second != null)
                return false;
        } else if(!this.second.equals(other.second))
            return false;
        if(this.third == null) {
            if(other.third != null)
                return false;
        } else if(!this.third.equals(other.third))
            return false;
        return true;
    }


    @Override
    public String toString() {
        return String.format("(%s, %s, %s)", this.first, this.second, this.third);
    }
}
