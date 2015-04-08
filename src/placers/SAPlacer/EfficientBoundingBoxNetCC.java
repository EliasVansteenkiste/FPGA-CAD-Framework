package placers.SAPlacer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import circuit.Block;
import circuit.Net;
import circuit.PackedCircuit;
import circuit.Pin;

public class EfficientBoundingBoxNetCC implements EfficientCostCalculator
{
	
	private Map<Block,ArrayList<EfficientBoundingBoxData>> bbDataMap;
	private EfficientBoundingBoxData[] bbDataArray;
	private int nbNets;
	private Block[] toRevert;
	
	public EfficientBoundingBoxNetCC(PackedCircuit circuit)
	{
		toRevert = new Block[2]; //Contains the blocks for which the associated boundingBox's might need to be reverted
		bbDataMap = new HashMap<>();
		nbNets = circuit.getNets().size();
		bbDataArray = new EfficientBoundingBoxData[nbNets];
		int counter = 0;
		
		for(Net net: circuit.getNets().values())
		{
			EfficientBoundingBoxData bbData = new EfficientBoundingBoxData(net);
			bbDataArray[counter] = bbData;
			
			//Process source block
			if(bbDataMap.get(net.source.owner) == null)
			{
				bbDataMap.put(net.source.owner, new ArrayList<EfficientBoundingBoxData>());
			}
			//Add the current BoundingBoxData object to the arraylist
			//We don't need to check if it is already in because this is the first time we add the current BoundingBoxData object
			bbDataMap.get(net.source.owner).add(bbData);

			//Process sink blocks
			for(Pin sink: net.sinks)
			{
				if(bbDataMap.get(sink.owner) == null)
				{
					bbDataMap.put(sink.owner, new ArrayList<EfficientBoundingBoxData>());
				}
				ArrayList<EfficientBoundingBoxData> sinkBlockList = bbDataMap.get(sink.owner);
				//Check if the current BoundingBoxData object is already in the arraylist
				//This can happen when a single net has two connections to the same block
				boolean isAlreadyIn = false;
				for(EfficientBoundingBoxData data: sinkBlockList) 
				{
					if(data == bbData)
					{
						isAlreadyIn = true;
						break;
					}
				}
				if(!isAlreadyIn)
				{
					sinkBlockList.add(bbData);
				}
			}
			counter++;
		}
	}
	
	public double calculateAverageNetCost()
	{
		return calculateTotalCost() / nbNets;
	}
	
	public double calculateTotalCost()
	{
		double totalCost = 0.0;
		for(int i = 0; i < bbDataArray.length; i++)
		{
			totalCost += bbDataArray[i].getNetCost();
		}
		return totalCost;
	}
	
	public double calculateDeltaCost(Swap swap)
	{
		double totalDeltaCost = 0.0;
		
		toRevert[0] = swap.pl1.block;
		if(swap.pl1.block != null)
		{
			ArrayList<EfficientBoundingBoxData> bbDataList = bbDataMap.get(swap.pl1.block);
			for(EfficientBoundingBoxData bbData: bbDataList)
			{
				bbData.saveState();
				totalDeltaCost += bbData.calculateDeltaCost(swap.pl1.block, swap.pl2);
			}
		}
		
		toRevert[1] = swap.pl2.block;
		if(swap.pl2.block != null)
		{
			ArrayList<EfficientBoundingBoxData> bbDataList = bbDataMap.get(swap.pl2.block);
			for(EfficientBoundingBoxData bbData: bbDataList)
			{
				bbData.saveState();
				totalDeltaCost += bbData.calculateDeltaCost(swap.pl2.block, swap.pl1);
			}
		}
		
		return totalDeltaCost;
	}
	
	public void recalculateFromScratch()
	{
		for(int i = 0; i < bbDataArray.length; i++)
		{
			bbDataArray[i].calculateBoundingBoxFromScratch();
		}
	}
	
	public void revert()
	{
		if(toRevert[0] != null)
		{
			ArrayList<EfficientBoundingBoxData> bbDataList = bbDataMap.get(toRevert[0]);
			for(EfficientBoundingBoxData bbData: bbDataList)
			{
				bbData.revert();
			}
		}
		if(toRevert[1] != null)
		{
			ArrayList<EfficientBoundingBoxData> bbDataList = bbDataMap.get(toRevert[1]);
			for(EfficientBoundingBoxData bbData: bbDataList)
			{
				bbData.revert();
			}
		}
	}
	
	public void pushThrough()
	{
		if(toRevert[0] != null)
		{
			ArrayList<EfficientBoundingBoxData> bbDataList = bbDataMap.get(toRevert[0]);
			for(EfficientBoundingBoxData bbData: bbDataList)
			{
				bbData.pushThrough();
			}
		}
		if(toRevert[1] != null)
		{
			ArrayList<EfficientBoundingBoxData> bbDataList = bbDataMap.get(toRevert[1]);
			for(EfficientBoundingBoxData bbData: bbDataList)
			{
				bbData.pushThrough();
			}
		}
	}
	
}
