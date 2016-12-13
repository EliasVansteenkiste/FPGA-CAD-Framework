package place.util;

public class IntList {

    private int size = 0;
    private int maxSize;
    private int[] array;

    public IntList() {
        this(16);
    }

    public IntList(int initialSize) {
        this.maxSize = initialSize;
        this.array = new int[initialSize];
    }

    public void add(int element) {
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

    public int get(int index) throws IndexOutOfBoundsException {
        if(index >= this.size) {
            throw new IndexOutOfBoundsException();
        }

        return this.array[index];
    }


    private void increaseSize() {
        this.maxSize *= 2;
        int[] newArray = new int[this.maxSize];
        System.arraycopy(this.array, 0, newArray, 0, this.size);
        this.array = newArray;
    }
}
