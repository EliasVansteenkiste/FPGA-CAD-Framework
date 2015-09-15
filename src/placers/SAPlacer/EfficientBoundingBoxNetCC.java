package placers.SAPlacer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import flexible_architecture.Circuit;
import flexible_architecture.architecture.PortType;
import flexible_architecture.block.GlobalBlock;
import flexible_architecture.pin.GlobalPin;

public class EfficientBoundingBoxNetCC implements EfficientCostCalculator
{
	
	private Map<GlobalBlock, List<EfficientBoundingBoxData>> bbDataMap;
	private EfficientBoundingBoxData[] bbDataArray;
	private List<GlobalPin> pins;
	private int numPins;
	private GlobalBlock[] toRevert;
	
	public EfficientBoundingBoxNetCC(Circuit circuit)
	{
		this.toRevert = new GlobalBlock[2]; //Contains the blocks for which the associated boundingBox's might need to be reverted
		
		this.pins = circuit.getGlobalPins(PortType.OUTPUT);
		this.numPins = this.pins.size();
		
		this.bbDataArray = new EfficientBoundingBoxData[this.numPins];
		this.bbDataMap = new HashMap<GlobalBlock, List<EfficientBoundingBoxData>>();
		
		int counter = 0;
		
		
		for(GlobalPin pin : this.pins) {
			EfficientBoundingBoxData bbData = new EfficientBoundingBoxData(pin);
			this.bbDataArray[counter] = bbData;
			
			//Process source block
			if(this.bbDataMap.get(pin.getOwner()) == null) {
				this.bbDataMap.put(pin.getOwner(), new ArrayList<EfficientBoundingBoxData>());
			}
			//Add the current BoundingBoxData object to the arraylist
			//We don't need to check if it is already in because this is the first time we add the current BoundingBoxData object
			this.bbDataMap.get(pin.getOwner()).add(bbData);

			//Process sink blocks
			int numPins = pin.getNumSinks();
			for(int i = 0; i < numPins; i++) {
				GlobalPin sink = pin.getSink(i);
				
				if(this.bbDataMap.get(sink.getOwner()) == null) {
					this.bbDataMap.put(sink.getOwner(), new ArrayList<EfficientBoundingBoxData>());
				}
				List<EfficientBoundingBoxData> sinkBlockList = this.bbDataMap.get(sink.getOwner());
				//Check if the current BoundingBoxData object is already in the arraylist
				//This can happen when a single net has two connections to the same block
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
			
			counter++;
		}
	}
	
	
	public double calculateAverageNetCost() {
		return calculateTotalCost() / this.numPins;
	}
	
	
	public double calculateTotalCost() {
		double totalCost = 0.0;
		for(int i = 0; i < this.bbDataArray.length; i++) {
			totalCost += this.bbDataArray[i].getNetCost();
		}
		return totalCost;
	}
	
	
	public double calculateDeltaCost(Swap swap) {
		double totalDeltaCost = 0.0;
		
		this.toRevert[0] = swap.pl1.getBlock();
		if(this.toRevert[0] != null) {
			List<EfficientBoundingBoxData> bbDataList = this.bbDataMap.get(this.toRevert[0]);
			if(bbDataList != null) {
				for(EfficientBoundingBoxData bbData: bbDataList) {
					bbData.saveState();
					totalDeltaCost += bbData.calculateDeltaCost(this.toRevert[0], swap.pl2);
				}
			}
		}
		
		this.toRevert[1] = swap.pl2.getBlock();
		if(this.toRevert[1] != null) {
			List<EfficientBoundingBoxData> bbDataList = this.bbDataMap.get(this.toRevert[1]);
			if(bbDataList != null) {
				for(EfficientBoundingBoxData bbData: bbDataList) {
					bbData.saveState();
					totalDeltaCost += bbData.calculateDeltaCost(this.toRevert[1], swap.pl1);
				}
			}
		}
		
		return totalDeltaCost;
	}
	
	
	public void recalculateFromScratch() {
		for(int i = 0; i < this.bbDataArray.length; i++) {
			this.bbDataArray[i].calculateBoundingBoxFromScratch();
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
