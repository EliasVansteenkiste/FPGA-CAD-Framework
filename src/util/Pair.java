package util;

public class Pair<K extends Comparable<K>, V> implements Comparable<Pair<K, V>> {

    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public void setKey(K key) {
        this.key = key;
    }
    public void setValue(V value) {
        this.value = value;
    }

    public K getKey() {
        return this.key;
    }
    public V getValue() {
        return this.value;
    }


    @Override
    public int compareTo(Pair<K, V> otherPair) {
        return this.key.compareTo(otherPair.key);
    }
}
