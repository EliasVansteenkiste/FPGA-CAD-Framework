package placers.SAPlacer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import circuit.Circuit;
import circuit.block.GlobalBlock;
import circuit.pin.GlobalPin;



public class EfficientBoundingBoxNetCC implements EfficientCostCalculator
{

    private Map<GlobalBlock, List<EfficientBoundingBoxData>> bbDataMap;
    private ArrayList<EfficientBoundingBoxData> bbDataArray;
    private int numPins;
    private GlobalBlock[] toRevert;

    public EfficientBoundingBoxNetCC(Circuit circuit) {
        this.toRevert = new GlobalBlock[2]; //Contains the blocks for which the associated boundingBox's might need to be reverted

        List<GlobalPin> pins = circuit.getGlobalOutputPins();
        int maxNumPins = pins.size();

        this.bbDataArray = new ArrayList<EfficientBoundingBoxData>(maxNumPins);
        this.bbDataMap = new HashMap<GlobalBlock, List<EfficientBoundingBoxData>>();

        this.numPins = 0;
        for(GlobalPin pin : pins) {

            int numSinks = pin.getNumSinks();
            if(numSinks > 0) {
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
        }

        this.bbDataArray.trimToSize();
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


    public double calculateDeltaCost(Swap swap) {
        double totalDeltaCost = 0.0;

        this.toRevert[0] = swap.getBlock1();
        if(this.toRevert[0] != null) {
            List<EfficientBoundingBoxData> bbDataList = this.bbDataMap.get(this.toRevert[0]);
            if(bbDataList != null) {
                for(EfficientBoundingBoxData bbData: bbDataList) {
                    bbData.saveState();
                    totalDeltaCost += bbData.calculateDeltaCost(this.toRevert[0], swap.getSite2());
                }
            }
        }

        this.toRevert[1] = swap.getBlock2();
        if(this.toRevert[1] != null) {
            List<EfficientBoundingBoxData> bbDataList = this.bbDataMap.get(this.toRevert[1]);
            if(bbDataList != null) {
                for(EfficientBoundingBoxData bbData: bbDataList) {
                    bbData.saveState();
                    totalDeltaCost += bbData.calculateDeltaCost(this.toRevert[1], swap.getBlock1().getSite());
                }
            }
        }

        return totalDeltaCost;
    }


    public void recalculateFromScratch() {
        for(int i = 0; i < this.numPins; i++) {
            this.bbDataArray.get(i).calculateBoundingBoxFromScratch();
        }
    }


    public void revert() {
        if(this.toRevert[0] != null) {
            List<EfficientBoundingBoxData> bbDataList = this.bbDataMap.get(this.toRevert[0]);
            if(bbDataList != null) {
                for(EfficientBoundingBoxData bbData: bbDataList) {
                    bbData.revert();
                }
            }
        }

        if(this.toRevert[1] != null) {
            List<EfficientBoundingBoxData> bbDataList = this.bbDataMap.get(this.toRevert[1]);
            if(bbDataList != null) {
                for(EfficientBoundingBoxData bbData: bbDataList) {
                    bbData.revert();
                }
            }
        }
    }


    public void pushThrough() {
        if(this.toRevert[0] != null) {
            List<EfficientBoundingBoxData> bbDataList = this.bbDataMap.get(this.toRevert[0]);
            if(bbDataList != null) {
                for(EfficientBoundingBoxData bbData: bbDataList) {
                    bbData.pushThrough();
                }
            }
        }

        if(this.toRevert[1] != null) {
            List<EfficientBoundingBoxData> bbDataList = this.bbDataMap.get(this.toRevert[1]);
            if(bbDataList != null) {
                for(EfficientBoundingBoxData bbData: bbDataList) {
                    bbData.pushThrough();
                }
            }
        }
    }
}
