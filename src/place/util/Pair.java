package place.util;

import java.io.Serializable;

public class Pair<F extends Comparable<F>, S> implements Comparable<Pair<F, S>>, Serializable {

    private static final long serialVersionUID = 8521307827779345129L;

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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.first == null) ? 0 : this.first.hashCode());
        result = prime * result + ((this.second == null) ? 0 : this.second.hashCode());
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
        Pair other = (Pair) obj;
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
        return true;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", this.first, this.second);
    }
}
