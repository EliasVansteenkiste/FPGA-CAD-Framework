package circuit;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/*
 * Represents a circuit that has not yet been completely packed and thus cannot be placed yet
 * This circuit can contain inputs, outputs, BLEs and nets
 * This class can be used to represent a circuit in the intermediate phase of packing
 */
public class BlePackedCircuit extends Circuit
{

	private Map<String,Ble> bles;
	private int nbBleInputs;
	
	public BlePackedCircuit(int nbBleInputs)
	{
		super();
		this.nbBleInputs = nbBleInputs;
		this.bles = new HashMap<String,Ble>();
	}
	
	public BlePackedCircuit(Map<String,Output> outputs, Map<String,Input> inputs, Vector<Vector<HardBlock>> hardBlocks, int nbBleInputs)
	{
		super(outputs, inputs, hardBlocks);
		this.nbBleInputs = nbBleInputs;
		this.bles = new HashMap<String,Ble>();
	}
	
	public void addBle(Ble ble)
	{
		if(ble.getInputs().length == this.nbBleInputs)
		{
			this.bles.put(ble.name, ble);
		}
		else
		{
			System.err.println("Wrong BLE size!");
		}
	}
	
	public Map<String, Ble> getBles()
	{
		return bles;
	}
	
	public int getNbBleInputs()
	{
		return this.nbBleInputs;
	}
	
}
