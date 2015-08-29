package architecture;

import circuit.Block;
import circuit.HardBlock;

public class HardBlockSite extends Site {

	private String typeName;
	
	public HardBlockSite(String typeName, int z, GridTile tyle) {
		super(z, tyle);
		this.typeName = typeName;
	}
	
	public String getTypeName() {
		return typeName;
	}
	
	@Override
	public Block setBlock(Block block) {
		if(block == null) {
			return super.setBlock(null);
		}
		
		if(((HardBlock) block).getTypeName().equals(typeName)) {
			return super.setBlock(block);
		} else {
			try {
				throw new Exception("Invalid block type: " + block.type);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}
	}
}