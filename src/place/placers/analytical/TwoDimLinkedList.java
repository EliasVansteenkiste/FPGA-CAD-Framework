package place.placers.analytical;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import place.placers.analytical.HeapLegalizer.LegalizerBlock;

import java.util.ArrayList;

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

    void addAll(Iterable<LegalizerBlock> blocks) {
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



    void split(TwoDimLinkedList list1, TwoDimLinkedList list2, int splitSize, Axis axis) {
        if(splitSize > this.size) {
            throw new IndexOutOfBoundsException();
        }

        if(!this.sorted) {
            this.sort();
        }

        // Split the list according to the given dimension
        int axisOrdinal = axis.ordinal();
        this.cursor = this.first(axisOrdinal);
        list1.initializeSplit(axisOrdinal);
        list2.initializeSplit(axisOrdinal);

        // Build the two lists on the split dimension
        while(this.cursor != null) {
            int height = this.cursor.block.macroHeight;

            if(list1.size + height <= splitSize) {
                if(height > list1.maxHeight) {
                    list1.maxHeight = height;
                }

                this.cursor.split = false;
                list1.numBlocks++;
                list1.size += height;

                this.addSplitNode(list1, axisOrdinal);

            } else {
                if(height > list2.maxHeight) {
                    list2.maxHeight = height;
                }

                this.cursor.split = true;
                list2.numBlocks++;
                list2.size += height;

                this.addSplitNode(list2, axisOrdinal);
            }

            this.cursor = this.cursor.next(axisOrdinal);
        }



        // Switch to the other axis: update the linked lists
        // for that axis as well
        axisOrdinal = 1 - axisOrdinal;
        this.cursor = this.first(axisOrdinal);
        list1.cursor = list1.first(axisOrdinal);
        list2.cursor = list2.first(axisOrdinal);

        while(this.cursor != null) {
            Node splitNode = this.cursor.splitNode;
            if(!this.cursor.split) {
                list1.cursor.next(axisOrdinal, splitNode);
                list1.cursor = splitNode;

            } else {
                list2.cursor.next(axisOrdinal, splitNode);
                list2.cursor = splitNode;
            }

            this.cursor = this.cursor.next(axisOrdinal);
        }

        list1.finishSplit();
        list2.finishSplit();
    }

    private void initializeSplit(int axisOrdinal) {
        this.size = 0;
        this.numBlocks = 0;
        this.maxHeight = 1;

        for(int i = 0; i < Axis.size; i++) {
            this.first(i, new Node(null));
        }

        this.cursor = this.first(axisOrdinal);
    }

    private void finishSplit() {
        // Remove the dummy nodes
        for(int i = 0; i < Axis.size; i++) {
            this.first(i, this.first(i).next(i));
        }
    }

    private void addSplitNode(TwoDimLinkedList splitList, int axisOrdinal) {
        Node splitNode = new Node(this.cursor.block);
        this.cursor.splitNode = splitNode;
        splitList.cursor.next(axisOrdinal, splitNode);
        splitList.cursor = splitNode;
    }


    @Override
    public Iterator<LegalizerBlock> iterator() {
        return new DimIterator(this.first(0), 0);
    }

    public Iterable<LegalizerBlock> blocksX() {
        if(!this.sorted) {
            this.sort();
        }

        return this.blocks(Axis.X);
    }
    public Iterable<LegalizerBlock> blocksY() {
        if(!this.sorted) {
            this.sort();
        }

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
        Node splitNode;
        boolean split;

        Node(LegalizerBlock block) {
            this.block = block;

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
