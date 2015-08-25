package architecture;

import circuit.Block;
import circuit.BlockType;

public class IoSite extends Site {

	private Block[] ios;
	
	public IoSite(int x, int y, int capacity)
	{
		super(x, y, SiteType.IO);
		ios = new Block[capacity];
		for(int i = 0; i < ios.length; i++)
		{
			ios[i] = null;
		}
	}
	
	public void setIO(int n, Block block)
	{
		if(block.type == BlockType.INPUT || block.type == BlockType.OUTPUT)
		{
			ios[n] = block;
		}
	}
	
	public Block getIO(int n)
	{
		return ios[n];
	}
	
	public int getCapacity()
	{
		return ios.length;
	}

}
