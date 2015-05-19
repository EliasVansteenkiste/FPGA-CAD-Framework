package architecture;

public class HardBlockSite extends Site
{

	private String typeName;
	
	public HardBlockSite(String name, int x, int y, int n, String typeName)
	{
		super(x, y, n, SiteType.HARDBLOCK, name);
		this.typeName = typeName;
	}
	
	public String getTypeName()
	{
		return typeName;
	}
	
}