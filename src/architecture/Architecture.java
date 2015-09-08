package architecture;

import java.util.ArrayList;
import java.util.Random;

import circuit.Block;
import circuit.PackedCircuit;

public abstract class Architecture {
	
	protected Random rand = new Random();
	
	protected GridTile[][] tileArray;
	private ArrayList<Site> siteVector = new ArrayList<>();
	protected ArrayList<IoSite> IOSites = new ArrayList<>();
	
	protected int width, height;
	protected int IOSiteCapacity;
	
	protected Architecture() {
		
	}
	
	public Architecture(int width, int height, int IOSiteCapacity) {
		this.width = width;
		this.height = height;
		this.IOSiteCapacity = IOSiteCapacity;
	}
	
	public static int getNbLutInputs(String type) {
		switch(type) {
			case "4lut":
				return 6;
			case "heterogeneous":
				return 6;
		}
	
		return 0;
	}
	
	public static Architecture newArchitecture(String type, PackedCircuit circuit, int IOSiteCapacity) {
		switch(type) {
			case "4lut":
				return new FourLutSanitized(circuit, IOSiteCapacity);
			case "heterogeneous":
				return new HeterogeneousArchitecture(circuit, IOSiteCapacity);
		}
		
		return null;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	
	public ArrayList<Site> getSites() {
		return siteVector;
	}
	
	public Site getSite(int x, int y, int z) {
		return tileArray[x][y].getSite(z);
	}
	
	public GridTile getTile(int x, int y) {
		return tileArray[x][y];
	}
	
	protected void addTile(GridTile tile) {
		tileArray[tile.getX()][tile.getY()] = tile;
		for(int i = 0; i < tile.getCapacity(); i++)
		{
			siteVector.add(tile.getSite(i));
		}
	}
	
	
	public ArrayList<IoSite> getIOSites() {
		return IOSites;
	}
	
	public IoSite getIOSite(int index) {
		return IOSites.get(index);
	}
	
	protected int getNumIOSites() {
		return IOSites.size();
	}
	
	protected void putIoSite(int x, int y)
	{
		GridTile IOTile = GridTile.constructIOGridTile(x, y, IOSiteCapacity);
		addTile(IOTile);
		for(int i = 0; i < IOSiteCapacity; i++)
		{
			IOSites.add((IoSite)IOTile.getSite(i));
		}
	}
	

	
	
	public Block placeBlock(int x, int y, int z, Block block) {
		Site site = this.getSite(x, y, z);
		return this.placeBlock(site, block);
	}
	public Block placeBlock(Site site, Block block) {
		block.setSite(site);
		return site.setBlock(block);
	}

	
	
	
	public abstract Site randomClbSite(int Rlim, Site pl1);
	public abstract Site randomHardBlockSite(int Rlim, HardBlockSite pl1);
	public abstract Site randomIOSite(int Rlim, Site pl1);
	
	public void setRand(Random rand)
	{
		this.rand = rand;
	}
}
