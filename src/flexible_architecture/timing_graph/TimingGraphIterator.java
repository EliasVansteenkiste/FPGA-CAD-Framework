package flexible_architecture.timing_graph;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import flexible_architecture.block.GlobalBlock;

public class TimingGraphIterator implements Iterator<TimingGraphEntry> {
	
	private GlobalBlock sourceBlock;
	private Iterator<Map.Entry<GlobalBlock, List<TimingNode>>> sourceBlockIterator;
	
	private TimingNode sourceNode;
	private Iterator<TimingNode> sourceNodeIterator;
	
	private Iterator<Map.Entry<TimingNode, TimingEdge>> sinkNodeIterator;
	
	
	TimingGraphIterator(Map<GlobalBlock, List<TimingNode>> nodesInGlobalBlocks) {
		this.sourceBlockIterator = nodesInGlobalBlocks.entrySet().iterator();
		
		while(this.sourceNodeIterator == null || !this.sourceNodeIterator.hasNext()) {
			Map.Entry<GlobalBlock, List<TimingNode>> sourceBlockEntry = this.sourceBlockIterator.next();
			this.sourceBlock = sourceBlockEntry.getKey();
			this.sourceNodeIterator = sourceBlockEntry.getValue().iterator();
		}
		
		this.sourceNode = this.sourceNodeIterator.next();
		this.sinkNodeIterator = this.sourceNode.sinks.entrySet().iterator();
	}
	
	public boolean hasNext() {
		return this.sourceBlockIterator.hasNext() || this.sourceNodeIterator.hasNext() || this.sinkNodeIterator.hasNext();
	}
	
	public TimingGraphEntry next() {
		while(!this.sinkNodeIterator.hasNext()) {
			
			while(!this.sourceNodeIterator.hasNext()) {
				Map.Entry<GlobalBlock, List<TimingNode>> sourceBlockEntry = this.sourceBlockIterator.next();
				this.sourceBlock = sourceBlockEntry.getKey();
				this.sourceNodeIterator = sourceBlockEntry.getValue().iterator();
			}
			
			this.sourceNode = this.sourceNodeIterator.next();
			this.sinkNodeIterator = this.sourceNode.sinks.entrySet().iterator();
		}
		
		
		Map.Entry<TimingNode, TimingEdge> sinkNodeEntry = this.sinkNodeIterator.next();
		return new TimingGraphEntry(
				this.sourceBlock,
				sinkNodeEntry.getKey().getOwner(),
				sinkNodeEntry.getValue().getCriticality(),
				this.sourceNode.sinks.size());
	}
	
	public void remove() {
		// Not supported
	}
}
