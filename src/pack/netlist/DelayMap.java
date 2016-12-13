package pack.netlist;

import java.util.HashMap;
import java.util.Map;

public class DelayMap {
	private Map<String,Integer> connectionDelay;
	
	public DelayMap(){
		this.connectionDelay = new HashMap<String,Integer>();
	}
	public void addDelay(P sourcePin, P sinkPin, int delay){
		this.connectionDelay.put(sourcePin.get_id() + sinkPin.get_id(), delay);
	}
	public int getDelay(P sourcePin, P sinkPin){
		return this.connectionDelay.get(sourcePin.get_id() + sinkPin.get_id());
	}
}
