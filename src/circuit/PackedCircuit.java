package circuit;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;


import placement.parser.PlaatsingUnit;
import placement.parser.Placement;
import architecture.Architecture;
import architecture.FourLutSanitized;
import architecture.HardBlockSite;
import architecture.HeterogeneousArchitecture;
import architecture.RouteNode;
import architecture.Site;
import architecture.SiteType;

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
		return inputs.size() + outputs.size() + clbs.size() + hardBlocks.size();
	}
	
	public Map<String, Clb> getClbs() {
		return clbs;
	}

	public void place(Placement p, FourLutSanitized a) {
		for(Iterator<PlaatsingUnit> i=p.plaatsingsmap.values().iterator();i.hasNext();) {
			PlaatsingUnit pu=i.next();
			Block b=getBlock(pu.naam);
			Site s = a.getSite(pu.x, pu.y, pu.n);
			b.setSite(s);
			b.getSite().block = b;
			b.fixed=true;
		}
	}
	
	public boolean place(Map<String,Integer> inputXPositions, Map<String,Integer> inputYPositions, Map<String,Integer> outputXPositions,
		Map<String,Integer> outputYPositions, Map<String,Integer> clbXPositions, Map<String,Integer> clbYPositions, 
		Map<String,Integer> hbXPositions, Map<String,Integer> hbYPositions, Architecture arch)
	{
		for(Input input: inputs.values())
		{
			int x = inputXPositions.get(input.name);
			int y = inputYPositions.get(input.name);
			Site site = arch.getSite(x, y, 0);
			input.setSite(site);
			site.block = input;
		}
		for(Output output: outputs.values())
		{
			int x = outputXPositions.get(output.name);
			int y = outputYPositions.get(output.name);
			Site site = arch.getSite(x, y, 1);
			output.setSite(site);
			site.block = output;
		}
		for(Clb clb: clbs.values())
		{
			int x = clbXPositions.get(clb.name);
			int y = clbYPositions.get(clb.name);
			Site site = arch.getSite(x, y, 0);
			clb.setSite(site);
			site.block = clb;
		}
		for(Vector<HardBlock> hbVector: hardBlocks)
		{
			for(HardBlock hb: hbVector)
			{
				int x = hbXPositions.get(hb.name);
				int y = hbYPositions.get(hb.name);
				Site site = arch.getSite(x, y, 0);
				hb.setSite(site);
				site.block = hb;
			}
		}
		boolean success = placementConsistencyCheck(arch);
		return success;
	}
	
	public void dumpPlacement(String file) throws FileNotFoundException {
		PrintStream stream = new PrintStream(new FileOutputStream(file));
		
		stream.println("Netlist file: na.net	Architecture file: na.arch");
		stream.println("Array size: 0 x 0 logic blocks");
		stream.println();
		stream.println("#block name	x	y	subblk	block number");
		stream.println("#----------	--	--	------	------------");
				
		for(Block blok:inputs.values()) {
			stream.println(blok.name+"	"+blok.getSite().x+"	"+blok.getSite().y+"	"+blok.getSite().n);
		}
		for(Block blok:clbs.values()) {
			stream.println(blok.name+"	"+blok.getSite().x+"	"+blok.getSite().y+"	"+blok.getSite().n);
		}
		for(Block blok:outputs.values()) {
			stream.println(blok.name+"	"+blok.getSite().x+"	"+blok.getSite().y+"	"+blok.getSite().n);
		}
		stream.close();
	}
	
	/*
	 * Doesn't work at the moment
	 * Breaks the circuit!!!!!!!!!!!!!!!!!
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 */
//	public void placementCLBsConsistencyCheck(FourLutSanitized a){
//		for(Site s:a.siteMap.values()){
//			if(s.block!=null&&s.block.type==BlockType.CLB){
//				if(clbs.remove(s.block.name)==null){
//					System.out.println("Placement consistency check failed! clb:"+s.block.name+", site:"+s);
//					return;
//				}
//			}
//		}
//		System.out.println("Placement consistency check passed!");
//	}
	
	/*
	 * Checks if all blocks have been placed in appropriate positions, 
	 * and if no blocks have gone lost during the placement process
	 */
	public boolean placementConsistencyCheck(Architecture arch)
	{
		//TODO (Arno): make sure this works again
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
		Vector<Site> nonIOSites = arch.getSites(SiteType.CLB, SiteType.HARDBLOCK);
		for(Site site: nonIOSites)
		{
			if(site.block != null && site.block.getSite() == site)
			{
				if(site.block.type == BlockType.CLB && site.type == SiteType.CLB)
				{
					if(blockMap.remove(site.block.name + "_CLB") == null)
					{
						success = false;
					}
				}
				else
				{
					if((site.block.type == BlockType.HARDBLOCK_CLOCKED || site.block.type == BlockType.HARDBLOCK_UNCLOCKED) && 
							site.type == SiteType.HARDBLOCK)
					{
						//Check if hardBlock typename equals hardBlockSite typename
						if(((HardBlock)site.block).getTypeName().equals(((HardBlockSite)site).getTypeName()))
						{
							if(blockMap.remove(site.block.name + "_HB") == null)
							{
								success = false;
							}
						}
					}
				}
			}
		}
		
		for(Site iSite: arch.getSites(SiteType.I))
		{
			if(iSite.block != null && iSite.block.getSite() == iSite)
			{
				if(iSite.block.type == BlockType.INPUT)
				{
					if(blockMap.remove(iSite.block.name + "_INPUT") == null)
					{
						success = false;
					}
				}
			}	
		}
		for(Site oSite: arch.getSites(SiteType.O))
		{
			if(oSite.block != null && oSite.block.getSite() == oSite)
			{
				if(oSite.block.type == BlockType.OUTPUT)
				{
					if(blockMap.remove(oSite.block.name + "_OUTPUT") == null)
					{
						success = false;
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
	
	private Block getBlock(String naam) {
		Block result=null;
		result=clbs.get(naam);
		if (result == null) result=inputs.get(naam);
		if (result == null) result=outputs.get(naam);
		return result;
	}

}