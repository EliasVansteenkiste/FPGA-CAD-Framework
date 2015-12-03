package placers.analyticalplacer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

/**
 * A linked list that is sorted in two dimensions.
 * A special operation is efficiently splitting the
 * linked list along one dimension, resulting in two
 * correctly sorted TwoDimLinkedLists.
 */
class TwoDimLinkedList<E extends Comparable<E>> implements Iterable<E> {

    private NodeComparator<E> comparatorX, comparatorY;
    private Node<E> firstX, firstY;
    private int size = 0;
    private boolean sorted = true;


    TwoDimLinkedList(Comparator<E> comparatorX, Comparator<E> comparatorY) {
        this.comparatorX = new NodeComparator<E>(comparatorX);
        this.comparatorY = new NodeComparator<E>(comparatorY);
    }
    private TwoDimLinkedList(TwoDimLinkedList<E> list) {
        this.comparatorX = list.comparatorX;
        this.comparatorY = list.comparatorY;
    }

    int size() {
        return this.size;
    }

    Comparator<E> getComparatorX() {
        return this.comparatorX.itemComparator;
    }
    Comparator<E> getComparatorY() {
        return this.comparatorY.itemComparator;
    }

    void add(E value) {
        Node<E> newNode = new Node<>(value, this.firstX, this.firstY);

        this.firstX = newNode;
        this.firstY = newNode;
        this.size++;

        this.sorted = false;
    }

    void addAll(Collection<E> values) {
        for(E value : values) {
            this.add(value);
        }
    }

    E get(int getIndex) {
        if(getIndex >= this.size) {
            throw new IndexOutOfBoundsException();
        }

        if(!this.sorted) {
            this.sort();
        }

        int index = 0;
        for(E value : this) {
            if(index == getIndex) {
                return value;
            }
            index++;
        }

        // Can never happen
        return null;
    }

    private void sort() {

        // Make a list of all nodes in this list
        @SuppressWarnings("unchecked")
        Node<E>[] array = new Node[this.size];
        {
            int i = 0;
            for(Node<E> node = this.firstX; node != null; node = node.nextX) {
                array[i++] = node;
            }
        }

        // Sort along X-axis and update linked list
        Arrays.sort(array, this.comparatorX);

        this.firstX = array[0];
        for(int i = 1; i < this.size; i++) {
            array[i - 1].nextX = array[i];
        }
        array[this.size - 1].nextX = null;



        // Sort along Y-axis and update the linked list
        Arrays.sort(array, this.comparatorY);

        this.firstY = array[0];
        for(int i = 1; i < this.size; i++) {
            array[i - 1].nextY = array[i];
        }
        array[this.size - 1].nextY = null;


        this.sorted = true;
    }


    /*Iterable<E> iteratorX() {
        if(!this.sorted) {
            this.sort();
        }

        return new IterableX(this.firstX);
    }
    Iterable<E> iteratorY() {
        if(!this.sorted) {
            this.sort();
        }

        return new IterableY(this.firstY);
    }*/




    TwoDimLinkedList<E> splitX(int splitIndex) {
        if(splitIndex > this.size) {
            throw new IndexOutOfBoundsException();
        }

        if(!this.sorted) {
            this.sort();
        }

        TwoDimLinkedList<E> newList = new TwoDimLinkedList<E>(this);
        newList.size = this.size - splitIndex;
        this.size = splitIndex;


        Node<E> cursor = this.firstX, previousCursor = null;

        // Mark the first splitIndex nodes as "not splitted"
        for(int index = 0; index < splitIndex; index++) {
            cursor.split = false;
            previousCursor = cursor;
            cursor = cursor.nextX;
        }

        // Choose the node at position splitIndex as the first
        // X node of the new list
        if(previousCursor != null) {
            previousCursor.nextX = null;
        }
        newList.firstX = cursor;

        // Mark the rest of the nodes as "splitted"
        while(cursor != null) {
            cursor.split = true;
            cursor = cursor.nextX;
        }


        // Create two dummy nodes as the first Y node of the
        // created lists
        cursor = this.firstY;
        this.firstY = new Node<E>(null, null, null);
        newList.firstY = new Node<E>(null, null, null);
        Node<E> thisLastY = this.firstY;
        Node<E> newListLastY = newList.firstY;
        while(cursor != null) {
            if(cursor.split) {
                newListLastY.nextY = cursor;
                newListLastY = cursor;

            } else {
                thisLastY.nextY = cursor;
                thisLastY = cursor;
            }

            cursor = cursor.nextY;
        }

        thisLastY.nextY = null;
        newListLastY.nextY = null;
        this.firstY = this.firstY.nextY;
        newList.firstY = newList.firstY.nextY;

        return newList;
    }


    // Sorry, duplicate code again
    TwoDimLinkedList<E> splitY(int splitIndex) {
        if(splitIndex > this.size) {
            throw new IndexOutOfBoundsException();
        }

        if(!this.sorted) {
            this.sort();
        }

        TwoDimLinkedList<E> newList = new TwoDimLinkedList<E>(this);
        newList.size = this.size - splitIndex;
        this.size = splitIndex;


        Node<E> cursor = this.firstY, previousCursor = null;

        // Mark the first splitIndex nodes as "not splitted"
        for(int index = 0; index < splitIndex; index++) {
            cursor.split = false;
            previousCursor = cursor;
            cursor = cursor.nextY;
        }

        // Choose the node at position splitIndex as the first
        // X node of the new list
        if(previousCursor != null) {
            previousCursor.nextY = null;
        }
        newList.firstY = cursor;

        // Mark the rest of the nodes as "splitted"
        while(cursor != null) {
            cursor.split = true;
            cursor = cursor.nextY;
        }


        // Create two dummy nodes as the first X node of the
        // created lists
        cursor = this.firstX;
        this.firstX = new Node<E>(null, null, null);
        newList.firstX = new Node<E>(null, null, null);
        Node<E> thisLastX = this.firstX;
        Node<E> newListLastX = newList.firstX;
        while(cursor != null) {
            if(cursor.split) {
                newListLastX.nextX = cursor;
                newListLastX = cursor;

            } else {
                thisLastX.nextX = cursor;
                thisLastX = cursor;
            }

            cursor = cursor.nextX;
        }

        thisLastX.nextX = null;
        newListLastX.nextX = null;
        this.firstX = this.firstX.nextX;
        newList.firstX = newList.firstX.nextX;

        return newList;
    }


    // It never really matters in which order we iterate
    // over the list. Just take the x direction.
    @Override
    public Iterator<E> iterator() {
        return new IteratorX(this.firstX);
    }



    private class Node<P> {
        P item;
        Node<P> nextX, nextY;
        boolean split;

        Node(P item, Node<P> nextX, Node<P> nextY) {
            this.item = item;
            this.nextX = nextX;
            this.nextY = nextY;
        }
    }

    private class NodeComparator<P> implements Comparator<Node<P>> {

        Comparator<P> itemComparator;

        NodeComparator(Comparator<P> itemComparator) {
            this.itemComparator = itemComparator;
        }

        @Override
        public int compare(Node<P> node1, Node<P> node2) {
            return this.itemComparator.compare(node1.item, node2.item);
        }

    }

    private class IterableX implements Iterable<E> {

        private Node<E> first;

        IterableX(Node<E> first) {
            this.first = first;
        }

        @Override
        public Iterator<E> iterator() {
            return new IteratorX(this.first);
        }

    }
    private class IteratorX implements Iterator<E> {

        private Node<E> cursor;

        IteratorX(Node<E> first) {
            this.cursor = first;
        }

        @Override
        public boolean hasNext() {
            return this.cursor != null;
        }

        @Override
        public E next() {
            E value = this.cursor.item;
            this.cursor = this.cursor.nextX;
            return value;
        }

        @Override
        public void remove() {
            // Not implemented
        }
    }

    private class IterableY implements Iterable<E> {

        private Node<E> first;

        IterableY(Node<E> first) {
            this.first = first;
        }

        @Override
        public Iterator<E> iterator() {
            return new IteratorY(this.first);
        }

    }
    private class IteratorY implements Iterator<E> {

        private Node<E> cursor;

        IteratorY(Node<E> first) {
            this.cursor = first;
        }

        @Override
        public boolean hasNext() {
            return this.cursor != null;
        }

        @Override
        public E next() {
            E value = this.cursor.item;
            this.cursor = this.cursor.nextY;
            return value;
        }

        @Override
        public void remove() {
            // Not implemented
        }
    }
}
