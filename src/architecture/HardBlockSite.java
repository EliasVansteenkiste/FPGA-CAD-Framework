package architecture;

import circuit.HardBlock;

public class HardBlockSite extends Site
{

	private String typeName;
	private HardBlock hardBlock;
	
	public HardBlockSite(int x, int y, String typeName)
	{
		super(x, y, SiteType.HARDBLOCK);
		this.typeName = typeName;
		hardBlock = null;
	}
	
	public String getTypeName()
	{
		return typeName;
	}
	
	public void setHardBlock(HardBlock hardBlock)
	{
		if(hardBlock.getTypeName().equals(typeName))
		{
			this.hardBlock = hardBlock;
		}
	}
	
	public HardBlock getHardBlock()
	{
		return hardBlock;
	}
	
}