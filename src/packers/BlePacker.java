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
		Map<String,Net> afterNets = afterBlePacking.getNets();
		
		//maps lut name to ble name, if the BLE has an active flipflop the BLE always has the name of the flipflop
		Map<String,String> lutToBle = new HashMap<String,String>();
		
/*		Collection<Input> inputs = beforeBlePacking.getInputs().values();
		for(Input input:inputs)
		{
			afterNets.put(input.name, new Net(input.name));
			afterNets.get(input.name).addSource(input.output);
		}
		
		Collection<Output> outputs = beforeBlePacking.getOutputs().values();
		for(Output output:outputs)
		{
			afterNets.put(output.name, new Net(output.name));
			afterNets.get(output.name).addSink(output.input);
		}
*/
		
		int ifs = 0;
		int elses = 0;
		
		// 1) Loop over all LUTs
		Collection<Lut> luts = beforeBlePacking.getLuts().values();
		for(Lut lut:luts)
		{
			//Get the number of FFs that are driven from the LUT
			String netName = lut.name; //We only consider LUTs with one output
			Vector<Pin> netSinkPins = beforeNets.get(netName).sinks;
/*			int ffSinks = 0;
			Flipflop connectedFF = null;
			for(Pin sinkPin:netSinkPins)
			{
				if(sinkPin.owner.type == BlockType.FLIPFLOP)
				{
					ffSinks++;
					connectedFF = (Flipflop)sinkPin.owner;
				}
			}
*/
			

			
			//If the LUT only drives one FF and no LUTs pack LUT and FF together in a BLE
			if(netSinkPins.size() == 1 && netSinkPins.get(0).owner.type == BlockType.FLIPFLOP)
			{
				Flipflop connectedFF = (Flipflop)netSinkPins.get(0).owner;
				Ble ble = new Ble(connectedFF.name, lut.getInputs().length, connectedFF, lut, true);
				this.afterBlePacking.addBle(ble);
				lutToBle.put(lut.name, ble.name);
				ifs++;
				
//				//Take care of net at the BLE output
//				if(!afterNets.containsKey(connectedFF.name))
//				{
//					afterNets.put(connectedFF.name, new Net(connectedFF.name));
//				}
//				afterNets.get(connectedFF.name).addSource(connectedFF.getOutput());
//				
//				// Take care of nets at BLE input
//				Pin[] lutInputs = lut.getInputs();
//				for (int i = 0; i < lutInputs.length; i++) 
//				{
//					if (lutInputs[i].con != null) // check if net is connected
//					{
//						Net inputNet = beforeNets.get(lutInputs[i].con.source.owner.name);
//						if (!afterNets.containsKey(inputNet.name)) 
//						{
//							afterNets.put(inputNet.name, new Net(inputNet.name));
//						}
//						afterNets.get(inputNet.name).addSink(lutInputs[i]);
//					}
//				}
			}
			else
			{
				//If the LUT drives no FF inputs: pack LUT alone in a BLE
				//If the LUT drives multiple FFs: pack LUT alone in a BLE
				Ble ble = new Ble(lut.name, lut.getInputs().length, null, lut, false);
				this.afterBlePacking.addBle(ble);
				lutToBle.put(lut.name, ble.name);
				elses++;
				
/*				//Take care of net at the BLE output
				if(!afterNets.containsKey(lut.name))
				{
					afterNets.put(lut.name, new Net(lut.name));
				}
				afterNets.get(lut.name).addSource(lut.getOutputs()[0]);
				
				// Take care of nets at BLE input
				Pin[] lutInputs = lut.getInputs();
				for(int i = 0; i < lutInputs.length; i++)
				{
					if(lutInputs[i].con != null) // check if net is connected
					{
						Net inputNet = beforeNets.get(lutInputs[i].con.source.owner.name);
						if(!afterNets.containsKey(inputNet.name))
						{
							afterNets.put(inputNet.name, new Net(inputNet.name));
						}
						afterNets.get(inputNet.name).addSink(lutInputs[i]);
					}
				}*/
			}
			
		}
		
		System.out.println("\n\n\n ifs: " + ifs + ", elses: " + elses + "\n\n\n");
		System.out.println("nbBLEs: " + afterBlePacking.getBles().size());
		System.out.println("\n\n\n");
		
		// 2) Loop over all FFs
		Collection<Flipflop> flipflops = beforeBlePacking.getFlipflops().values();
		for(Flipflop flipflop:flipflops)
		{
			//If it has not been put in a BLE yet: pack FF alone in a BLE
			if(!afterNets.containsKey(flipflop.name)) //We still need to add the flipflop to a BLE
			{
				Ble ble = new Ble(flipflop.name, ((Lut)(luts.toArray()[0])).getInputs().length, flipflop, null, true);
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
				
//				String netName; //netName is the name of the sourcing block
//				if(net.source.owner.type == BlockType.FLIPFLOP || net.source.owner.type == BlockType.INPUT)
//				{
//					netName = net.source.owner.name;
//				}
//				else // must be a lut that sources
//				{
//					netName = lutToBle.get(net.source.owner.name); // name of BLE where the lut is in (can be together with a FF)
//				}
//				afterNets.put(netName, new Net(netName));
//				afterNets.get(netName).addSource(afterBlePacking.getBles().get(netName).getOutput());
				
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
							int index = 0;
							for(int i = 0; i < afterBlePacking.getBles().get(sinkName).getInputs().length; i++)
							{
								Pin input = afterBlePacking.getBles().get(sinkName).getInputs()[i];
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
