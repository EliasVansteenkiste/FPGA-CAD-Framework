package architecture;

import circuit.Block;
import circuit.HardBlock;

public class HardBlockSite extends Site
{

	private String typeName;
	private HardBlock hardBlock;
	
	public HardBlockSite(String typeName, int z, GridTile tyle)
	{
		super(z, tyle);
		this.typeName = typeName;
		hardBlock = null;
	}
	
	public String getTypeName()
	{
		return typeName;
	}
	
	@Override
	public void setBlock(Block hardBlock)
	{
		if(((HardBlock)hardBlock).getTypeName().equals(typeName))
		{
			this.hardBlock = (HardBlock)hardBlock;
		}
	}
	
	@Override
	public Block getBlock()
	{
		return hardBlock;
	}
	
}