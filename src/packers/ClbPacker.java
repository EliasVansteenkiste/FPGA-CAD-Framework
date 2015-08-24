package packers;

import java.util.Map;

import circuit.BlePackedCircuit;
import circuit.BlockType;
import circuit.Clb;
import circuit.Net;
import circuit.PackedCircuit;
import circuit.Ble;
import circuit.Pin;

/*
 * This is dummy for now: just puts every BLE in a CLB
 */
public class ClbPacker 
{
	
	private BlePackedCircuit beforeClbPacking;
	private PackedCircuit afterClbPacking;
	
	public ClbPacker(BlePackedCircuit beforeClbPacking)
	{
		this.beforeClbPacking = beforeClbPacking;
	}
	
	public PackedCircuit pack()
	{
		this.afterClbPacking = new PackedCircuit(beforeClbPacking.getOutputs(), beforeClbPacking.getInputs(), beforeClbPacking.getHardBlocks());
		int nbClbInputs = beforeClbPacking.getNbBleInputs();
		
		for(Ble ble:beforeClbPacking.getBles().values())
		{
			Clb clb = new Clb(ble.name, 1, nbClbInputs, 1);
			clb.addBle(ble);
			afterClbPacking.clbs.put(clb.name, clb);
		}
		
		connectCircuit();
		
		return afterClbPacking;
	}
	
	private void connectCircuit()
	{
		Map<String,Net> beforeNets = beforeClbPacking.getNets();
		Map<String,Net> afterNets = afterClbPacking.getNets();
		
		for(Net net:beforeNets.values())
		{
			afterNets.put(net.name, new Net(net.name));
			if(net.source.owner.type == BlockType.BLE)
			{
				afterNets.get(net.name).addSource(afterClbPacking.clbs.get(net.name).output[0]);
			}
			else
			{
				if(net.source.owner.type == BlockType.INPUT)
				{
					afterNets.get(net.name).addSource(afterClbPacking.getInputs().get(net.name).output);
				}
				else //Net source must be a hardblock pin
				{
					afterNets.get(net.name).addSource(net.source); //Its the same hardblock...
				}
			}
			
			for(Pin sink:net.sinks)
			{
				if(sink.owner.type == BlockType.BLE)
				{
					int index = -1; //Will throw exception when pin is not found
					for(int i = 0; i < afterClbPacking.clbs.get(sink.owner.name).getBle(0).getNbInputs(); i++)
					{
						Pin input = afterClbPacking.clbs.get(sink.owner.name).getBle(0).getInputs()[i];
						if(input.name == sink.name)
						{
							index = i;
							break;
						}
					}
					afterNets.get(net.name).addSink(afterClbPacking.clbs.get(sink.owner.name).input[index]);
				}
				else //Sink must be a circuit output pin
				{
					if(sink.owner.type == BlockType.OUTPUT)
					{
						afterNets.get(net.name).addSink(afterClbPacking.getOutputs().get(sink.owner.name).input);
					}
					else
					{
						afterNets.get(net.name).addSink(sink); //Its the same hard block...
					}
				}
			}
		}
	}
	
}
