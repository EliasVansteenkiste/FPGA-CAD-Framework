package circuit;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import architecture.ClbSite;
import architecture.HardBlockSite;
import architecture.HeterogeneousArchitecture;
import architecture.IoSite;
import architecture.Site;
import architecture.SiteType;
import architecture.old.RouteNode;

public class PackedCircuit extends Circuit{

	public Map<String,Clb>	clbs;
	public Vector<Block> vBlocks;
	public Map<String,Net>	globalNets;
	
	public PackedCircuit() {
		super();
		clbs = new HashMap<String,Clb>();
		globalNets = new HashMap<String,Net>();
	}
	
	public PackedCircuit(Map<String,Output> outputs, Map<String,Input> inputs, Vector<Vector<HardBlock>> hardBlocks)
	{
		super(outputs, inputs, hardBlocks);
		clbs = new HashMap<String,Clb>();
		globalNets = new HashMap<String,Net>();
	}

	public int numBlocks()
	{
		return inputs.size()+outputs.size()+clbs.size();
	}

	public void dumpPlacement(String file) throws FileNotFoundException
	{
		PrintStream stream = new PrintStream(new FileOutputStream(file));
		
		stream.println("Netlist file: na.net	Architecture file: na.arch");
		stream.println("Array size: 0 x 0 logic blocks");
		stream.println();
		stream.println("#block name	x	y	subblk	block number");
		stream.println("#----------	--	--	------	------------");
				
		for(Block blok:inputs.values())
		{
			stream.println(blok.name + "	" + blok.getSite().getX() + "	" + blok.getSite().getY());
		}
		for(Block blok:clbs.values()) {
			stream.println(blok.name + "	" + blok.getSite().getX() + "	" + blok.getSite().getY());
		}
		for(Block blok:outputs.values()) {
			stream.println(blok.name + "	" + blok.getSite().getX() + "	" + blok.getSite().getY());
		}
		stream.close();
	}
	
	/*
	 * Checks if all blocks have been placed in appropriate positions, 
	 * and if no blocks have gone lost during the placement process
	 */
	public boolean placementConsistencyCheck(HeterogeneousArchitecture architecture)
	{
		boolean success = true;
		//Fill blockMap with all blocks in the circuit (IOs, CLBs and all sorts of hardBlocks)
		Map<String,Block> blockMap = new HashMap<>();
		for(Clb clb: clbs.values())
		{
			blockMap.put(clb.name + "_CLB", clb);
		}
		for(Vector<HardBlock> hbVector: hardBlocks)
		{
			for(HardBlock hb: hbVector)
			{
				blockMap.put(hb.name + "_HB", hb);
			}
		}
		for(Input input: inputs.values())
		{
			blockMap.put(input.name + "_INPUT", input);
		}
		for(Output output: outputs.values())
		{
			blockMap.put(output.name + "_OUTPUT", output);
		}
		
		//Loop over all sites in the architecture and see if we find every block of the circuit at a valid site in the architecture
		Collection<Site> sites = architecture.getSites();
		for(Site site: sites)
		{
			if(site.getType() == SiteType.CLB)
			{
				ClbSite clbSite = (ClbSite)site;
				if(clbSite.getClb() != null && clbSite.getClb().getSite() == clbSite)
				{
					if(blockMap.remove(clbSite.getClb().name + "_CLB") == null)
					{
						success = false;
					}
				}
			}
			else
			{
				if(site.getType() == SiteType.HARDBLOCK)
				{
					HardBlockSite hbSite = (HardBlockSite)site;
					if(hbSite.getHardBlock() != null && hbSite.getHardBlock().getSite() == hbSite)
					{
						if(hbSite.getHardBlock().getTypeName().equals(hbSite.getTypeName()))
						{
							if(blockMap.remove(hbSite.getHardBlock().name + "_HB") == null)
							{
								success = false;
							}
						}
					}
				}
			}
		}
		
		for(IoSite ioSite: architecture.getIOSites())
		{
			for(int i = 0; i < ioSite.getCapacity(); i++)
			{
				if(ioSite.getIO(i) != null)
				{
					if(ioSite.getIO(i).type == BlockType.INPUT)
					{
						Input input = (Input)ioSite.getIO(i);
						if(input.getSite() == ioSite)
						{
							if(blockMap.remove(input.name + "_INPUT") == null)
							{
								success = false;
							}
						}
					}
					else
					{
						if(ioSite.getIO(i).type == BlockType.OUTPUT)
						{
							Output output = (Output)ioSite.getIO(i);
							if(output.getSite() == ioSite)
							{
								if(blockMap.remove(output.name + "_OUTPUT") == null)
								{
									success = false;
								}
							}
						}
					}
				}
			}
		}
		
		if(blockMap.size() != 0)
		{
			success = false;
		}
		return success;
	}
	
	public void fillVector()
	{
		vBlocks = new Vector<Block>();
		vBlocks.addAll(clbs.values());
		vBlocks.addAll(inputs.values());
		vBlocks.addAll(outputs.values());
		for(Vector<HardBlock> hbVector: hardBlocks)
		{
			vBlocks.addAll(hbVector);
		}
	}

	public int totalRouteNodes() {
		//Making one big set of all routenodes
		Set<RouteNode> allRNs = new HashSet<RouteNode>();
		for(Net net:this.nets.values()){
			allRNs.addAll(net.routeNodes);
		}
		return allRNs.size();
	}

	public int totalWires() {
		//Making one big set of all routenodes
		Set<RouteNode> allRNs = new HashSet<RouteNode>();
		for(Net net:this.nets.values()){
			allRNs.addAll(net.routeNodes);
		}
		int NOwires = 0;
		for(RouteNode rn : allRNs){
			if(rn.isWire())NOwires++;
		}
		return NOwires;
	}

	public void ripUpRoutng(){
		for(Net net:this.nets.values()){
			net.routeNodes.clear();
		}
	}

}