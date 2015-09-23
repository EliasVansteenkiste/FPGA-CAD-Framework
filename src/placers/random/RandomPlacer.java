package placers.random;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import placers.Placer;

import flexible_architecture.Circuit;
import flexible_architecture.architecture.BlockType;
import flexible_architecture.block.AbstractBlock;
import flexible_architecture.block.GlobalBlock;
import flexible_architecture.site.AbstractSite;

public class RandomPlacer extends Placer {
	
	public RandomPlacer(Circuit circuit, Map<String, String> options) {
		super(circuit, options);
	}
	
	public void place() {
		Random random = new Random(1);
		
		List<BlockType> blockTypes = this.circuit.getGlobalBlockTypes();
		for(BlockType blockType : blockTypes) {
			
			// Get all possible blocks and sites for this type
			List<AbstractBlock> blocks = this.circuit.getBlocks(blockType);
			List<AbstractSite> sites = this.circuit.getSites(blockType);
			
			// Permutate sites
			Collections.shuffle(sites, random);
			
			// Assign each block to a site
			int siteIndex = 0;
			for(AbstractBlock abstractBlock : blocks) {
				AbstractSite site = sites.get(siteIndex);
				GlobalBlock block = (GlobalBlock) abstractBlock;
				
				block.setSite(site);
				
				siteIndex++;
			}
		}
	}	
}
