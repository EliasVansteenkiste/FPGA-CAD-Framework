package circuit;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;


import placement.parser.PlaatsingUnit;
import placement.parser.Placement;
import architecture.FourLutSanitized;
import architecture.RouteNode;
import architecture.Site;

public class PackedCircuit extends Circuit{

	public Map<String,Clb>	clbs;
	public Vector<Clb> vClbs;
	public Vector<Block> vBlocks;
	public Map<String,Net>	globalNets;
	public Map<String,Connection> connections;
	public Set<Connection> cons;

	public Map<Pin,Map<RouteNode,Integer>> BundleRns;
	public Map<Pin,Map<RouteNode,Pin>> firstConnectionEnd;

	public boolean netRouted;
	
	public PackedCircuit() {
		super();
		clbs = new HashMap<String,Clb>();
		globalNets = new HashMap<String,Net>();
		connections = new HashMap<String,Connection>();
	}
	
	public PackedCircuit(Map<String,Output> outputs, Map<String,Input> inputs)
	{
		super(outputs, inputs);
		clbs = new HashMap<String,Clb>();
		globalNets = new HashMap<String,Net>();
		connections = new HashMap<String,Connection>();
	}

	public int numBlocks() {
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
	
	public void randomPlace(Placement io_p, FourLutSanitized a) {
		Random rand= new Random();
		Set<Site> temp = new HashSet<Site>();
		for(int x=0;x<a.width+2;x++){
			for(int y=1;y<a.height+1;y++){
				temp.add(a.getSite(x, y, 0));
			}
		}
		for(int x=1;x<a.width+1;x++){
			temp.add(a.getSite(x, 0, 0));
			temp.add(a.getSite(x, a.height+1, 0));
		}
		for(Iterator<PlaatsingUnit> i=io_p.plaatsingsmap.values().iterator();i.hasNext();) {
			PlaatsingUnit pu=i.next();
			Block b=getBlock(pu.naam);
			Site pl=(Site) temp.toArray()[rand.nextInt(temp.size())];
			temp.remove(pl);
			b.setSite(pl);
			b.getSite().block = b;
			b.fixed=true;
		}
	}
	
	public void RandomPlaceCLBsWithSeed(Placement io_p, FourLutSanitized a, Random rand) {
		Vector<Site> temp = new Vector<Site>();
		for(int x=1;x<a.width+1;x++){
			for(int y=1;y<a.height+1;y++){
				temp.add(a.getSite(x, y, 0));
			}
		}

		for(Iterator<PlaatsingUnit> i=io_p.plaatsingsmap.values().iterator();i.hasNext();) {
			PlaatsingUnit pu=i.next();
			Block b=getBlock(pu.naam);
			int idx = rand.nextInt(temp.size());
			Site pl=(Site) temp.toArray()[idx];
			temp.remove(pl);
			b.setSite(pl);
			b.getSite().block = b;
			b.fixed=true;
		}
	}

	private Block getBlock(String naam) {
		Block result=null;
		result=clbs.get(naam);
		if (result == null) result=inputs.get(naam);
		if (result == null) result=outputs.get(naam);
		return result;
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
	
	public void placementCLBsConsistencyCheck(FourLutSanitized a){
		for(Site s:a.siteMap.values()){
			if(s.block!=null&&s.block.type==BlockType.CLB){
				if(clbs.remove(s.block.name)==null){
					System.out.println("Placement consistency check failed! clb:"+s.block.name+", site:"+s);
					return;
				}
			}
		}
		System.out.println("Placement consistency check passed!");
	}
	
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

}
