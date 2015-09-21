package architecture;

import circuit.Block;
import circuit.BlockType;

public class IoSite extends Site {

	private Block io;
	
	public IoSite(int z, GridTile tyle)
	{
		super(z, tyle);
		io = null;
	}
	
	@Override
	public Block setBlock(Block block)
	{
		if(block == null || block.type == BlockType.INPUT || block.type == BlockType.OUTPUT) {
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
	
	@Override
	public Block getBlock()
	{
		return io;
	}

}
