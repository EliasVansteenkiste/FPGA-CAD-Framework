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
class TwoDimLinkedList {

    enum Axis {
        X, Y;
        static final int size = Axis.values().length;
    };

    private List<NodeComparator> comparator = new ArrayList<>(Axis.size);
    private List<Node> first = new ArrayList<>(Axis.size);
    private Node cursor;

    private int maxHeight;
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

    LegalizerBlock getFirst(Axis axis) {
        return this.first(axis.ordinal()).block;
    }

    int maxHeight() {
        return this.maxHeight;
    }

    int maxIndex(Axis axis) {
        int index = 0;
        for(LegalizerBlock block : this.blocks(axis)) {
            int macroHeight = block.macroHeight;

            if(macroHeight == this.maxHeight) {
                return index;
            }

            index += macroHeight;
        }

        return -1;
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

        // Sort along the two axes and update linked lists
        for(int axisOrdinal = 0; axisOrdinal < Axis.size; axisOrdinal++) {
            Collections.sort(array, this.comparator.get(axisOrdinal));

            Node previousNode = array.get(0), node;
            this.first(axisOrdinal, previousNode);

            for(int i = 1; i < this.numBlocks; i++) {
                node = array.get(i);
                previousNode.next(axisOrdinal, node);
                previousNode = node;
            }
            previousNode.next(axisOrdinal, null);
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

        this.size = 0;
        newList.size = 0;

        this.numBlocks = 0;
        newList.numBlocks = 0;

        this.maxHeight = 0;
        newList.maxHeight = 0;


        // Split the list according to the given dimension
        int axisOrdinal = axis.ordinal();
        Node cursor = this.initializeSplit(newList, axisOrdinal);

        // Build the two lists on the split dimension
        while(cursor != null) {
            int height = cursor.block.macroHeight;

            if(this.size + height <= splitIndex) {
                if(height > this.maxHeight) {
                    this.maxHeight = height;
                }

                cursor.split = false;
                this.numBlocks++;
                this.size += height;

                this.cursor.next(axisOrdinal, cursor);
                this.cursor = cursor;

            } else {
                if(height > newList.maxHeight) {
                    newList.maxHeight = height;
                }

                cursor.split = true;
                newList.numBlocks++;
                newList.size += height;

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



    public Iterable<LegalizerBlock> blocksX() {
        return this.blocks(Axis.X);
    }
    public Iterable<LegalizerBlock> blocksY() {
        return this.blocks(Axis.Y);
    }

    private Iterable<LegalizerBlock> blocks(Axis axis) {
        final int axisOrdinal = axis.ordinal();
        final Node first = this.first(axisOrdinal);

        return new Iterable<LegalizerBlock>() {
            @Override
            public Iterator<LegalizerBlock> iterator() {
                return new DimIterator(first, axisOrdinal);
            }
        };
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


    private class DimIterator implements Iterator<LegalizerBlock> {

        private Node iterCursor;
        private int axisOrdinal;

        DimIterator(Node first, int axisOrdinal) {
            this.iterCursor = first;
            this.axisOrdinal = axisOrdinal;
        }

        @Override
        public boolean hasNext() {
            return this.iterCursor != null;
        }

        @Override
        public LegalizerBlock next() {
            LegalizerBlock value = this.iterCursor.block;
            this.iterCursor = this.iterCursor.next(this.axisOrdinal);
            return value;
        }

        @Override
        public void remove() {
            // Not supported
        }
    }
}
