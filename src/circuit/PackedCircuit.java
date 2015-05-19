package circuit;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;


import placement.parser.PlaatsingUnit;
import placement.parser.Placement;
import architecture.FourLutSanitized;
import architecture.RouteNode;
import architecture.Site;

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
	
	public void fillVector()
	{
		vBlocks = new Vector<Block>();
		vBlocks.addAll(clbs.values());
		vBlocks.addAll(inputs.values());
		vBlocks.addAll(outputs.values());
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