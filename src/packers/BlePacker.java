package packers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import circuit.BlockType;
import circuit.Net;
import circuit.Pin;

import prepackedcircuit.Ble;
import prepackedcircuit.Flipflop;
import prepackedcircuit.Lut;
import prepackedcircuit.PrePackedCircuit;

/*
 * Packs a PrePackedCircuit containing LUTs and FFs in a PrePackedCircuit containing BLEs
 * Uses pattern matching
 */
public class BlePacker 
{

	private PrePackedCircuit beforeBlePacking;
	private PrePackedCircuit afterBlePacking;
	
	public BlePacker(PrePackedCircuit beforeBlePacking)
	{
		this.beforeBlePacking = beforeBlePacking;
	}
	
	public PrePackedCircuit pack()
	{
		this.afterBlePacking = new PrePackedCircuit(beforeBlePacking.getOutputs(), beforeBlePacking.getInputs());
		
		Map<String,Net> beforeNets = beforeBlePacking.getNets();
		
		//maps lut name to ble name, if the BLE has an active flipflop the BLE always has the name of the flipflop
		Map<String,String> lutToBle = new HashMap<String,String>();

		// 1) Loop over all LUTs
		Collection<Lut> luts = beforeBlePacking.getLuts().values();
		for(Lut lut:luts)
		{
			String netName = lut.name; //We only consider LUTs with one output
			Vector<Pin> netSinkPins = beforeNets.get(netName).sinks;
			
			//If the LUT only drives one FF and no LUTs pack LUT and FF together in a BLE
			if(netSinkPins.size() == 1 && netSinkPins.get(0).owner.type == BlockType.FLIPFLOP)
			{
				Flipflop connectedFF = (Flipflop)netSinkPins.get(0).owner;
				Ble ble = new Ble(connectedFF.name, lut.getInputs().length, connectedFF, lut, true);
				this.afterBlePacking.addBle(ble);
				lutToBle.put(lut.name, ble.name);
			}
			else
			{
				//If the LUT drives no FF inputs: pack LUT alone in a BLE
				//If the LUT drives multiple FFs: pack LUT alone in a BLE
				Ble ble = new Ble(lut.name, lut.getInputs().length, null, lut, false);
				this.afterBlePacking.addBle(ble);
				lutToBle.put(lut.name, ble.name);
			}
			
		}
		
		// 2) Loop over all FFs
		Collection<Flipflop> flipflops = beforeBlePacking.getFlipflops().values();
		for(Flipflop flipflop:flipflops)
		{
			//If it has not been put in a BLE yet: pack FF alone in a BLE
			if(!afterBlePacking.getBles().containsKey(flipflop.name)) //We still need to add the flipflop to a BLE
			{
				Ble ble = new Ble(flipflop.name, 6, flipflop, null, true);
				this.afterBlePacking.addBle(ble);
			}
		}
		
		connectCircuit(lutToBle);
		
		return afterBlePacking;
	}
	
	private void connectCircuit(Map<String,String> lutToBle)
	{
		Map<String,Net> beforeNets = beforeBlePacking.getNets();
		Map<String,Net> afterNets = afterBlePacking.getNets();
		
		for(Net net:beforeNets.values())
		{
			//If source of the net is lut and there is only one sinking block which is a flipflop
			// ==> net is internal to the BLE
			// ==> not necessary to add it to the afterBlePacking nets
			if(!(net.source.owner.type == BlockType.LUT && net.sinks.size() == 1 && net.sinks.get(0).owner.type == BlockType.FLIPFLOP))
			{
				afterNets.put(net.name, new Net(net.name));
				if(net.source.owner.type != BlockType.INPUT)
				{
					afterNets.get(net.name).addSource(afterBlePacking.getBles().get(net.name).getOutput());
				}
				else
				{
					afterNets.get(net.name).addSource(afterBlePacking.getInputs().get(net.name).output);
				}
				
				for(Pin sink:net.sinks)
				{
					String sinkName;
					if(sink.owner.type == BlockType.FLIPFLOP)
					{
						sinkName = sink.owner.name;
						afterNets.get(net.name).addSink(afterBlePacking.getBles().get(sinkName).getInputs()[0]);
					}
					else
					{
						if(sink.owner.type == BlockType.OUTPUT)
						{
							sinkName = sink.owner.name;
							afterNets.get(net.name).addSink(afterBlePacking.getOutputs().get(sinkName).input);
						}
						else //sink must be a lut
						{
							sinkName = lutToBle.get(sink.owner.name);
							int index = -1; //Will throw exception when pin is not found
							for(int i = 0; i < afterBlePacking.getBles().get(sinkName).getLut().getInputs().length; i++)
							{
								Pin input = afterBlePacking.getBles().get(sinkName).getLut().getInputs()[i];
								if(input.name == sink.name)
								{
									index = i;
								}
							}
							afterNets.get(net.name).addSink(afterBlePacking.getBles().get(sinkName).getInputs()[index]);
						}
					}
					
				}
				
			}
		}
	}
	
}
