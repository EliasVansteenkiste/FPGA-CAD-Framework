package route.circuit.architecture;

import java.util.HashMap;
import java.util.Map;

public class DelayMap {
	private Map<Long, Double> delayMap;
	
	public DelayMap() {
		this.delayMap = new HashMap<>();
	}
	
	public void add(DelayElement d) {
		this.delayMap.put(this.delayId(d.sourceBlockIndex, d.sourcePortIndex, d.sinkBlockIndex, d.sinkPortIndex), d.delay);
	}
	
	public Double get(int sourceBlockIndex, int sourcePortIndex, int sinkBlockIndex, int sinkPortIndex) {
		for (int i : new int[]{sourceBlockIndex, -1}) {
			for (int j : new int[]{sourcePortIndex, -1}) {
				
				long sourceId = cantor_pairing_function(i+1, j+1);
				
				for (int k : new int[]{sinkBlockIndex, -1}) {
					for (int l : new int[]{sinkPortIndex, -1}) {
						
						long sinkId = cantor_pairing_function(k+1, l+1);
						
						Double delay = this.delayMap.get(this.delayId(sourceId, sinkId));
						if(delay != null) {
							return delay;
						}
					}
				}
			}
		}
		return null;
	}
	
	private long delayId(int sourceBlockIndex, int sourcePortIndex, int sinkBlockIndex, int sinkPortIndex) {
		long sourceId = cantor_pairing_function(sourceBlockIndex+1, sourcePortIndex+1);
		long sinkId = cantor_pairing_function(sinkBlockIndex+1, sinkPortIndex+1);
		return delayId(sourceId, sinkId);
	}
	private long delayId(long sourceId, long sinkId) {
		return cantor_pairing_function(sourceId, sinkId);
	}
	private long cantor_pairing_function(long k1, long k2) {
		return ((k1 + k2) * (k1 + k2 + 1)) / 2 + k2;
	}
}
