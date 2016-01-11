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
    private Node cursor;

    private int maxHeight = 0;
    private int numBlocks = 0, size = 0;
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
    int numBlocks() {
        return this.numBlocks;
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

        this.numBlocks++;

        int height = block.macroHeight;
        if(height > this.maxHeight) {
            this.maxHeight = height;
        }
        this.size += height;

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

        // Make a new list, and set the sizes
        TwoDimLinkedList newList = new TwoDimLinkedList(this);

        newList.size = this.size - splitIndex;
        this.size = splitIndex;

        this.numBlocks = 0;
        newList.numBlocks = 0;


        // Split the list according to the given dimension
        int axisOrdinal = axis.ordinal();
        Node cursor = this.initializeSplit(newList, axisOrdinal);

        // Build the two lists on the split dimension
        int splittedSize = 0;
        while(cursor != null) {
            int height = cursor.block.macroHeight;

            if(splittedSize + height <= splitIndex) {
                cursor.split = false;
                this.numBlocks++;

                this.cursor.next(axisOrdinal, cursor);
                this.cursor = cursor;

                splittedSize += height;

            } else {
                cursor.split = true;
                newList.numBlocks++;

                newList.cursor.next(axisOrdinal, cursor);
                newList.cursor = cursor;
            }

            cursor = cursor.next(axisOrdinal);
        }

        this.finishSplit(newList, axisOrdinal);



        // Switch to the other axis: update the linked lists
        // for that axis as well
        axisOrdinal = 1 - axisOrdinal;
        cursor = this.initializeSplit(newList, axisOrdinal);

        while(cursor != null) {
            if(cursor.split) {
                newList.cursor.next(axisOrdinal, cursor);
                newList.cursor = cursor;

            } else {
                this.cursor.next(axisOrdinal, cursor);
                this.cursor = cursor;
            }

            cursor = cursor.next(axisOrdinal);
        }

        this.finishSplit(newList, axisOrdinal);

        return newList;
    }

    private Node initializeSplit(TwoDimLinkedList newList, int axisOrdinal) {
        // Get the current first node
        Node cursor = this.first(axisOrdinal);

        // Make a dummy first node in the two list
        this.cursor = new Node(null);
        newList.cursor = new Node(null);

        this.first(axisOrdinal, this.cursor);
        newList.first(axisOrdinal, newList.cursor);

        return cursor;
    }

    private void finishSplit(TwoDimLinkedList newList, int axisOrdinal) {
        // Mark the last nodes
        this.cursor.next(axisOrdinal, null);
        newList.cursor.next(axisOrdinal, null);

        // Remove the dummy nodes
        this.removeFirst(axisOrdinal);
        newList.removeFirst(axisOrdinal);
    }

    private void removeFirst(int axisOrdinal) {
        this.first(axisOrdinal, this.first(axisOrdinal).next(axisOrdinal));
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

        private Node iterCursor;

        IteratorY(Node first) {
            this.iterCursor = first;
        }

        @Override
        public boolean hasNext() {
            return this.iterCursor != null;
        }

        @Override
        public LegalizerBlock next() {
            LegalizerBlock value = this.iterCursor.block;
            this.iterCursor = this.iterCursor.next(Axis.Y.ordinal());
            return value;
        }

        @Override
        public void remove() {
            // Not implemented
        }
    }
}
