package circuit;

import java.util.HashMap;
import java.util.Map;

/*
 * Represents a circuit that has not yet been completely packed and thus cannot be placed yet
 * This circuit can contain inputs, outputs, BLEs and nets
 * This class can be used to represent a circuit in the intermediate phase of packing
 */
public class BlePackedCircuit extends Circuit
{

	private Map<String,Ble> bles;
	
	public BlePackedCircuit()
	{
		super();
		this.bles = new HashMap<String,Ble>();
	}
	
	public BlePackedCircuit(Map<String,Output> outputs, Map<String,Input> inputs)
	{
		super(outputs, inputs);
		this.bles = new HashMap<String,Ble>();
	}
	
	public void addBle(Ble ble)
	{
		this.bles.put(ble.name, ble);
	}
	
	public Map<String, Ble> getBles()
	{
		return bles;
	}
	
}
