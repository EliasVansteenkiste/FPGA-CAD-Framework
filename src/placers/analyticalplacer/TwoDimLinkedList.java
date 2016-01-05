package placers.analyticalplacer;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * A linked list that is sorted in two dimensions.
 * A special operation is efficiently splitting the
 * linked list along one dimension, resulting in two
 * correctly sorted TwoDimLinkedLists.
 */
class TwoDimLinkedList<E extends Comparable<E>> implements Iterable<E> {

    enum Axis {
        X, Y;
        static final int size = Axis.values().length;
    };

    private List<NodeComparator<E>> comparator = new ArrayList<>(Axis.size);
    private List<Node<E>> first = new ArrayList<>(Axis.size);
    private int size = 0;
    private boolean sorted = true;


    private TwoDimLinkedList() {
        for(int i = 0; i < Axis.size; i++) {
            this.first.add(null);
        }
    }

    TwoDimLinkedList(Comparator<E> comparatorX, Comparator<E> comparatorY) {
        this();

        this.comparator.add(0, new NodeComparator<E>(comparatorX));
        this.comparator.add(1, new NodeComparator<E>(comparatorY));
    }
    TwoDimLinkedList(TwoDimLinkedList<E> list) {
        this();

        this.comparator = new ArrayList<NodeComparator<E>>(list.comparator);
    }

    int size() {
        return this.size;
    }

    private Node<E> first(int axisOrdinal) {
        return this.first.get(axisOrdinal);
    }
    private void first(int axisOrdinal, Node<E> value) {
        this.first.set(axisOrdinal, value);
    }


    void add(E value) {
        Node<E> newNode = new Node<>(value, this.first);
        for(int i = 0; i < Axis.size; i++) {
            this.first(i, newNode);
        }

        this.size++;
        this.sorted = false;
    }

    void addAll(Collection<E> values) {
        for(E value : values) {
            this.add(value);
        }
    }

    private void sort() {

        // Make a list of all nodes in this list
        List<Node<E>> array = new ArrayList<>(this.size);
        for(Node<E> node = this.first(0); node != null; node = node.next(0)) {
            array.add(node);
        }

        // Sort along the two axises and update linked lists
        for(int axisOrdinal = 0; axisOrdinal < Axis.size; axisOrdinal++) {
            Collections.sort(array, this.comparator.get(axisOrdinal));

            this.first(axisOrdinal, array.get(0));
            for(int i = 1; i < this.size; i++) {
                array.get(i - 1).next(axisOrdinal, array.get(i));
            }
            array.get(this.size - 1).next(axisOrdinal, null);
        }

        this.sorted = true;
    }


    int indexOf(E element, Axis axis) {
        int axisOrdinal = axis.ordinal();
        Comparator<E> itemComparator = this.comparator.get(axisOrdinal).itemComparator;;

        Node<E> cursor = this.first(axisOrdinal);
        for(int i = 0; i < this.size; i++) {
            if(itemComparator.compare(cursor.item, element) >= 0) {
                return i;
            }
            cursor = cursor.next(axisOrdinal);
        }

        return this.size;
    }


    TwoDimLinkedList<E> split(int splitIndex, Axis axis) {
        if(splitIndex > this.size) {
            throw new IndexOutOfBoundsException();
        }

        if(!this.sorted) {
            this.sort();
        }


        TwoDimLinkedList<E> newList = new TwoDimLinkedList<E>(this);
        newList.size = this.size - splitIndex;
        this.size = splitIndex;

        int axisOrdinal = axis.ordinal();
        Node<E> cursor = this.first(axisOrdinal), previousCursor = null;

        // Mark the first splitIndex nodes as "not splitted"
        for(int index = 0; index < splitIndex; index++) {
            cursor.split = false;
            previousCursor = cursor;
            cursor = cursor.next(axisOrdinal);
        }

        // Choose the node at position splitIndex as the first
        // X node of the new list
        if(previousCursor != null) {
            previousCursor.next(axisOrdinal, null);
        }
        newList.first(axisOrdinal, cursor);

        // Mark the rest of the nodes as "splitted"
        while(cursor != null) {
            cursor.split = true;
            cursor = cursor.next(axisOrdinal);
        }


        // Switch to the other axis: update the linked list
        // for that axis as well
        axisOrdinal = 1 - axisOrdinal;

        // Create two dummy nodes
        cursor = this.first(axisOrdinal);
        this.first(axisOrdinal, new Node<E>(null));
        newList.first(axisOrdinal, new Node<E>(null));
        Node<E> thisLast = this.first(axisOrdinal);
        Node<E> newListLast = newList.first(axisOrdinal);

        while(cursor != null) {
            if(cursor.split) {
                newListLast.next(axisOrdinal, cursor);
                newListLast = cursor;

            } else {
                thisLast.next(axisOrdinal, cursor);
                thisLast = cursor;
            }

            cursor = cursor.next(axisOrdinal);
        }

        // Remove the dummy nodes
        thisLast.next(axisOrdinal, null);
        newListLast.next(axisOrdinal, null);
        this.first(axisOrdinal, this.first(axisOrdinal).next(axisOrdinal));
        newList.first(axisOrdinal, newList.first(axisOrdinal).next(axisOrdinal));

        return newList;
    }



    // It never really matters in which order we iterate
    // over the list. Just take the x direction.
    @Override
    public Iterator<E> iterator() {
        return new IteratorX(this.first(Axis.X.ordinal()));
    }



    private class Node<P> {
        P item;
        List<Node<P>> next;
        boolean split;

        Node(P item) {
            this.item = item;

            this.next = new ArrayList<>();
            for(int i = 0; i < Axis.size; i++) {
                this.next.add(null);
            }
        }

        Node(P item, List<Node<P>> next) {
            this.item = item;
            this.next = new ArrayList<Node<P>>(next);
        }

        Node<P> next(int axisOrdinal) {
            return this.next.get(axisOrdinal);
        }
        void next(int axisOrdinal, Node<P> value) {
            this.next.set(axisOrdinal, value);
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
            this.cursor = this.cursor.next(Axis.X.ordinal());
            return value;
        }

        @Override
        public void remove() {
            // Not implemented
        }
    }
}
