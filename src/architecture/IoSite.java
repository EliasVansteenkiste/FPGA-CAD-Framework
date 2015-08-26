package architecture;

import circuit.Block;
import circuit.BlockType;

public class IoSite extends Site {

	private Block io;
	
	public IoSite(int z, GridTile tyle)
	{
		super(z,tyle);
		io = null;
	}
	
	@Override
	public void setBlock(Block block)
	{
		if(block.type == BlockType.INPUT || block.type == BlockType.OUTPUT)
		{
			io = block;
		}
	}
	
	@Override
	public Block getBlock()
	{
		return io;
	}

}
