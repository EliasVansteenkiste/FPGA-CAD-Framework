package util;

public class FloatList {

    private int size = 0;
    private int maxSize;
    private float[] array;

    public FloatList() {
        this(16);
    }

    public FloatList(int initialSize) {
        this.maxSize = initialSize;
        this.array = new float[initialSize];
    }

    public void add(float element) {
        if(this.size == this.maxSize) {
            this.increaseSize();
        }

        this.array[this.size] = element;
        this.size++;
    }

    public void clear() {
        this.size = 0;
    }

    public int size() {
        return this.size;
    }

    public float get(int index) throws IndexOutOfBoundsException {
        if(index >= this.size) {
            throw new IndexOutOfBoundsException();
        }

        return this.array[index];
    }


    private void increaseSize() {
        this.maxSize *= 2;
        float[] newArray = new float[this.maxSize];
        System.arraycopy(this.array, 0, newArray, 0, this.size);
        this.array = newArray;
    }
}
