package place.placers.simulatedannealing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.block.GlobalBlock;
import place.circuit.pin.AbstractPin;
import place.circuit.pin.GlobalPin;



public class EfficientBoundingBoxNetCC {

    private Map<GlobalBlock, List<EfficientBoundingBoxData>> bbDataMap;
    private ArrayList<EfficientBoundingBoxData> bbDataArray;
    private int numPins;

    // Contains the blocks for which the associated boundingBox's might need to be reverted
    private List<GlobalBlock> toRevert = new ArrayList<>();

    public EfficientBoundingBoxNetCC(Circuit circuit) {

        this.bbDataArray = new ArrayList<EfficientBoundingBoxData>();
        this.bbDataMap = new HashMap<GlobalBlock, List<EfficientBoundingBoxData>>();

        // Process all nets by iterating over all net source pins
        this.numPins = 0;
        for(GlobalBlock block : circuit.getGlobalBlocks()) {
            for(AbstractPin pin : block.getOutputPins()) {
                this.processPin((GlobalPin) pin);
            }
        }

        this.bbDataArray.trimToSize();
    }

    private void processPin(GlobalPin pin) {

        // Dont't count pins without sinks, or pins that feed clocks
        int numSinks = pin.getNumSinks();
        if(numSinks == 0 || pin.getSink(0).getPortType().isClock()) {
            return;
        }

        this.numPins++;

        EfficientBoundingBoxData bbData = new EfficientBoundingBoxData(pin);
        this.bbDataArray.add(bbData);

        // Process source block
        if(this.bbDataMap.get(pin.getOwner()) == null) {
            this.bbDataMap.put(pin.getOwner(), new ArrayList<EfficientBoundingBoxData>());
        }
        // Add the current BoundingBoxData object to the arraylist
        // We don't need to check if it is already in because this is the first time we add the current BoundingBoxData object
        this.bbDataMap.get(pin.getOwner()).add(bbData);

        // Process sink blocks
        for(int i = 0; i < numSinks; i++) {
            GlobalPin sink = pin.getSink(i);

            if(this.bbDataMap.get(sink.getOwner()) == null) {
                this.bbDataMap.put(sink.getOwner(), new ArrayList<EfficientBoundingBoxData>());
            }
            List<EfficientBoundingBoxData> sinkBlockList = this.bbDataMap.get(sink.getOwner());
            // Check if the current BoundingBoxData object is already in the arraylist
            // This can happen when a single net has two connections to the same block
            boolean isAlreadyIn = false;
            for(EfficientBoundingBoxData data: sinkBlockList) {
                if(data == bbData) {
                    isAlreadyIn = true;
                    break;
                }
            }

            if(!isAlreadyIn) {
                sinkBlockList.add(bbData);
            }
        }
    }


    public double calculateAverageNetCost() {
        return calculateTotalCost() / this.numPins;
    }


    public double calculateTotalCost() {
        double totalCost = 0.0;
        for(int i = 0; i < this.numPins; i++) {
            totalCost += this.bbDataArray.get(i).getNetCost();
        }
        return totalCost;
    }

    public double calculateBlockCost(GlobalBlock block){
    	double cost = 0.0;
    	if(this.bbDataMap.containsKey(block)){
        	for(EfficientBoundingBoxData data:this.bbDataMap.get(block)){
        		cost += data.getFanoutWeightedNetCost();
        	}
    	}
    	return cost;
    }

    public void recalculateFromScratch() {
        for(int i = 0; i < this.numPins; i++) {
            this.bbDataArray.get(i).calculateBoundingBoxFromScratch();
        }
    }


    public void revert() {
        for(GlobalBlock block : this.toRevert) {
            List<EfficientBoundingBoxData> bbDataList = this.bbDataMap.get(block);
            if(bbDataList != null) {
                for(EfficientBoundingBoxData bbData: bbDataList) {
                    bbData.revert();
                }
            }
        }
    }


    public void pushThrough() {
        for(GlobalBlock block : this.toRevert) {
            List<EfficientBoundingBoxData> bbDataList = this.bbDataMap.get(block);
            if(bbDataList != null) {
                for(EfficientBoundingBoxData bbData: bbDataList) {
                    bbData.pushThrough();
                }
            }
        }
    }
}
