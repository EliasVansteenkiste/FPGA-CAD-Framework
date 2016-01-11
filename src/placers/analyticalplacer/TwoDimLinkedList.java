package placers.analyticalplacer;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import placers.analyticalplacer.HeapLegalizer.LegalizerBlock;

/**
 * A linked list that is sorted in two dimensions.
 * A special operation is efficiently splitting the
 * linked list along one dimension, resulting in two
 * correctly sorted TwoDimLinkedLists.
 */
class TwoDimLinkedList implements Iterable<LegalizerBlock> {

    enum Axis {
        X, Y;
        static final int size = Axis.values().length;
    };

    private List<NodeComparator> comparator = new ArrayList<>(Axis.size);
    private List<Node> first = new ArrayList<>(Axis.size);
    private int maxHeight = 0;
    private int size = 0;
    private boolean sorted = true;


    private TwoDimLinkedList() {
        for(int i = 0; i < Axis.size; i++) {
            this.first.add(null);
        }
    }

    TwoDimLinkedList(double[] coordinatesX, double[] coordinatesY) {
        this();

        this.comparator.add(0, new NodeComparator(coordinatesX));
        this.comparator.add(1, new NodeComparator(coordinatesY));
    }
    TwoDimLinkedList(TwoDimLinkedList list) {
        this();

        this.comparator = new ArrayList<NodeComparator>(list.comparator);
    }

    int size() {
        return this.size;
    }

    private Node first(int axisOrdinal) {
        return this.first.get(axisOrdinal);
    }
    private void first(int axisOrdinal, Node value) {
        this.first.set(axisOrdinal, value);
    }


    void add(LegalizerBlock block) {
        Node newNode = new Node(block, this.first);
        for(int i = 0; i < Axis.size; i++) {
            this.first(i, newNode);
        }

        this.size++;
        this.sorted = false;
    }

    void addAll(Collection<LegalizerBlock> blocks) {
        for(LegalizerBlock block : blocks) {
            this.add(block);
        }
    }

    private void sort() {

        // Make a list of all nodes in this list
        List<Node> array = new ArrayList<>(this.size);
        for(Node node = this.first(0); node != null; node = node.next(0)) {
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


    TwoDimLinkedList split(int splitIndex, Axis axis) {
        if(splitIndex > this.size) {
            throw new IndexOutOfBoundsException();
        }

        if(!this.sorted) {
            this.sort();
        }


        TwoDimLinkedList newList = new TwoDimLinkedList(this);
        newList.size = this.size - splitIndex;
        this.size = splitIndex;

        int axisOrdinal = axis.ordinal();
        Node cursor = this.first(axisOrdinal), previousCursor = null;

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
        this.first(axisOrdinal, new Node(null));
        newList.first(axisOrdinal, new Node(null));
        Node thisLast = this.first(axisOrdinal);
        Node newListLast = newList.first(axisOrdinal);

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



    // Iterating happens according to the Y dimension
    @Override
    public Iterator<LegalizerBlock> iterator() {
        return new IteratorY(this.first(Axis.Y.ordinal()));
    }



    private class Node {
        LegalizerBlock block;
        List<Node> nextNode;
        boolean split;

        Node(LegalizerBlock item) {
            this.block = item;

            this.nextNode = new ArrayList<>();
            for(int i = 0; i < Axis.size; i++) {
                this.nextNode.add(null);
            }
        }

        Node(LegalizerBlock item, List<Node> next) {
            this.block = item;
            this.nextNode = new ArrayList<Node>(next);
        }

        Node next(int axisOrdinal) {
            return this.nextNode.get(axisOrdinal);
        }
        void next(int axisOrdinal, Node value) {
            this.nextNode.set(axisOrdinal, value);
        }
    }

    private class NodeComparator implements Comparator<Node> {

        private double[] coordinates;

        NodeComparator(double[] coordinates) {
            this.coordinates = coordinates;
        }

        @Override
        public int compare(Node node1, Node node2) {
            return Double.compare(this.coordinates[node1.block.blockIndex], this.coordinates[node2.block.blockIndex]);
        }

    }


    private class IteratorY implements Iterator<LegalizerBlock> {

        private Node cursor;

        IteratorY(Node first) {
            this.cursor = first;
        }

        @Override
        public boolean hasNext() {
            return this.cursor != null;
        }

        @Override
        public LegalizerBlock next() {
            LegalizerBlock value = this.cursor.block;
            this.cursor = this.cursor.next(Axis.Y.ordinal());
            return value;
        }

        @Override
        public void remove() {
            // Not implemented
        }
    }
}
