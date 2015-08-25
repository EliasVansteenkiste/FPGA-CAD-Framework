package placers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import circuit.Block;
import circuit.HardBlock;
import circuit.PackedCircuit;
import circuit.Clb;
import circuit.Input;
import circuit.Output;

import architecture.FourLutSanitized;
import architecture.HardBlockSite;
import architecture.HeterogeneousArchitecture;
import architecture.Site;
import architecture.SiteType;

public class Rplace
{

	public static void place(PackedCircuit c, FourLutSanitized a)
	{
		Random rand= new Random();
		Set<Site> temp = new HashSet<Site>();
		for (int x=1;x<a.getWidth()+1;x++)
		{
			for (int y=1;y<a.getHeight()+1;y++)
			{
				Site s = a.getSite(x,y,0);
				s.block = null;
				temp.add(s);				
			}
		}
		for(Clb b:c.clbs.values())
		{
			if (b instanceof Clb)
			{
				Site site=(Site) temp.toArray()[rand.nextInt(temp.size())];
				temp.remove(site);
				site.block = (Clb) b;
				b.setSite(site);
			}	
		}
	}
	
	public static void placeCLBs(PackedCircuit c, FourLutSanitized a, Random rand)
	{
		Set<Site> temp = new HashSet<Site>();
		for (int x=1;x<a.getWidth()+1;x++)
		{
			for (int y=1;y<a.getHeight()+1;y++)
			{
				Site s = a.getSite(x,y,0);
				s.block = null;
				temp.add(s);				
			}
		}
		for(Clb b:c.clbs.values())
		{
			if (b instanceof Clb)
			{
				Site site=(Site) temp.toArray()[rand.nextInt(temp.size())];
				temp.remove(site);
				site.block = (Clb) b;
				b.setSite(site);
			}	
		}
	}
	
	
	public static void placeCLBsandIOs(PackedCircuit c, FourLutSanitized a, Random rand)
	{
		//Random place CLBs
		Set<Site> temp = new HashSet<Site>();
		for (int x=1;x<a.getWidth()+1;x++)
		{
			for (int y=1;y<a.getHeight()+1;y++)
			{
				Site s = a.getSite(x,y,0);
				s.block = null;
				temp.add(s);				
			}
		}
		for(Clb b:c.clbs.values())
		{
			if (b instanceof Clb)
			{
				Site site=(Site) temp.toArray()[rand.nextInt(temp.size())];
				temp.remove(site);
				site.block = (Clb) b;
				b.setSite(site);
			}	
		}
		//Random Place IOs
		Set<Site> tempInputs = new HashSet<Site>();
		for (int x=1;x<a.getWidth()+1;x++)
		{
			Site s = a.getSite(x,0,0);
			s.block = null;
			tempInputs.add(s);
			Site t = a.getSite(x,a.getHeight()+1,0);
			t.block = null;
			tempInputs.add(t);
		}
		for (int y=1;y<a.getHeight()+1;y++)
		{
			Site s = a.getSite(0,y,0);
			s.block = null;
			tempInputs.add(s);
			Site t = a.getSite(a.getWidth()+1,y,0);
			t.block = null;
			tempInputs.add(t);
		}
		Set<Site> tempOutputs = new HashSet<Site>();
		for (int x=1;x<a.getWidth()+1;x++)
		{
			Site s = a.getSite(x,0,1);
			s.block = null;
			tempOutputs.add(s);
			Site t = a.getSite(x,a.getHeight()+1,1);
			t.block = null;
			tempOutputs.add(t);
		}
		for (int y=1;y<a.getHeight()+1;y++)
		{
			Site s = a.getSite(0,y,1);
			s.block = null;
			tempOutputs.add(s);
			Site t = a.getSite(a.getWidth()+1,y,1);
			t.block = null;
			tempOutputs.add(t);
		}
		for(Input in:c.inputs.values())
		{
			if (in instanceof Input)
			{
				Site site=(Site) tempInputs.toArray()[rand.nextInt(tempInputs.size())];
				tempInputs.remove(site);
				site.block = (Input) in;
				in.setSite(site);
			}	
		}
		for(Output out:c.outputs.values())
		{
			if (out instanceof Output)
			{
				Site site=(Site) tempOutputs.toArray()[rand.nextInt(tempOutputs.size())];
				tempOutputs.remove(site);
				site.block = (Output) out;
				out.setSite(site);
			}	
		}
	}
	
	public static void placeCLBsandFixedIOs(PackedCircuit c, FourLutSanitized a, Random rand)
	{
		//Random place CLBs
		Set<Site> temp = new HashSet<Site>();
		for (int x=1;x<a.getWidth()+1;x++)
		{
			for (int y=1;y<a.getHeight()+1;y++)
			{
				Site s = a.getSite(x,y,0);
				s.block = null;
				temp.add(s);				
			}
		}
		for(Clb b:c.clbs.values())
		{
			if (b instanceof Clb)
			{
				Site site=(Site) temp.toArray()[rand.nextInt(temp.size())];
				temp.remove(site);
				site.block = (Clb) b;
				b.setSite(site);
			}	
		}

		//Deterministic place IOs
		int index = 0;
		for(Input input:c.inputs.values())
		{
			input.fixed = true;
			Site site = a.getISite(index);
			site.block = input;
			input.setSite(site);
			index += 2;
			if(index >= a.getHeight()*2 + a.getWidth()*2)
			{
				index = 1;
			}
		}
		index = 0;
		for(Output output:c.outputs.values())
		{
			output.fixed = true;
			Site site = a.getOSite(index);
			site.block = output;
			output.setSite(site);
			index += 2;
			if(index >= a.getHeight()*2 + a.getWidth()*2)
			{
				index = 1;
			}
		}
	}
	
	public static void placeCLBsandFixedIOs(PackedCircuit c, HeterogeneousArchitecture a, Random rand)
	{
		//Initialize data structures
		ArrayList<Site> clbSites = new ArrayList<>();
		ArrayList<ArrayList<Site>> hardBlockSites = new ArrayList<>();
		ArrayList<String> hardBlockTypes = new ArrayList<>();
		for(int x = 1; x < a.getWidth() + 1; x++)
		{
			for(int y = 1; y < a.getHeight() + 1; y++)
			{
				Site s = a.getSite(x, y, 0);
				s.block = null;
				if(s.type == SiteType.CLB)
				{
					clbSites.add(s);
				}
				else //Must be a hardBlock
				{
					String typeName = ((HardBlockSite)s).getTypeName();
					int curIndex = 0;
					boolean found = false;
					for(String name: hardBlockTypes)
					{
						if(name.contains(typeName))
						{
							hardBlockSites.get(curIndex).add(s);
							found = true;
							break;
						}
						curIndex++;
					}
					if(!found)
					{
						hardBlockTypes.add(typeName);
						ArrayList <Site> newList = new ArrayList<>();
						newList.add(s);
						hardBlockSites.add(newList);
					}
				}
			}
		}
		
		//Random place CLBs
		for(Clb clb: c.clbs.values())
		{
			int randomIndex = rand.nextInt(clbSites.size());
			Site site = (Site)clbSites.get(randomIndex);
			clbSites.remove(randomIndex);
			site.block = clb;
			clb.setSite(site);
		}
				
		//Random place hardBlocks
		for(Vector<HardBlock> hbVector: c.getHardBlocks())
		{
			String curTypeName = hbVector.get(0).getTypeName();
			int curIndex = 0;
			for(String name: hardBlockTypes)
			{
				if(name.contains(curTypeName))
				{
					break;
				}
				curIndex++;
			}
			for(HardBlock hardBlock: hbVector)
			{
				int randomIndex = rand.nextInt(hardBlockSites.get(curIndex).size());
				Site site = (Site)hardBlockSites.get(curIndex).get(randomIndex);
				hardBlockSites.get(curIndex).remove(randomIndex);
				site.block = hardBlock;
				hardBlock.setSite(site);
			}
		}
		
		//Deterministic place IOs
		int index = 0;
		ArrayList<Site> ISites = a.getISites();
		for(Input input:c.inputs.values())
		{
			input.fixed = true;
			//input.fixed = false;
			Site site = ISites.get(index);
			site.block = input;
			input.setSite(site);
			index += 1;
			if(index >= a.getHeight()*2 + a.getWidth()*2)
			{
				index = 1;
			}
		}
		index = 0;
		ArrayList<Site> OSites = a.getOSites();
		for(Output output:c.outputs.values())
		{
			output.fixed = true;
			//output.fixed = false;
			Site site = OSites.get(index);
			site.block = output;
			output.setSite(site);
			index += 1;
			if(index >= a.getHeight()*2 + a.getWidth()*2)
			{
				index = 1;
			}
		}
	}
	
	public static void placeFixedIOs(PackedCircuit c, HeterogeneousArchitecture a)
	{
		//Deterministic place IOs
		int index = 0;
		ArrayList<Site> ISites = a.getISites();
		for(Input input:c.inputs.values())
		{
			input.fixed = true;
			//input.fixed = false;
			Site site = ISites.get(index);
			site.block = input;
			input.setSite(site);
			index += 1;
			if(index >= a.getHeight()*2 + a.getWidth()*2)
			{
				index = 1;
			}
		}
		index = 0;
		ArrayList<Site> OSites = a.getOSites();
		for(Output output:c.outputs.values())
		{
			output.fixed = true;
			//output.fixed = false;
			Site site = OSites.get(index);
			site.block = output;
			output.setSite(site);
			index += 1;
			if(index >= a.getHeight()*2 + a.getWidth()*2)
			{
				index = 1;
			}
		}
	}
	
//	public static void placeCLBsandFixedIOs(PackedCircuit c, HeterogeneousArchitecture a, Random rand, ArrayList<ArrayList<Block>> packedIOs)
//	{
//		//Initialize data structures
//		ArrayList<Site> clbSites = new ArrayList<>();
//		ArrayList<ArrayList<Site>> hardBlockSites = new ArrayList<>();
//		ArrayList<String> hardBlockTypes = new ArrayList<>();
//		for(int x = 1; x < a.getWidth() + 1; x++)
//		{
//			for(int y = 1; y < a.getHeight() + 1; y++)
//			{
//				Site s = a.getSite(x, y, 0);
//				s.block = null;
//				if(s.type == SiteType.CLB)
//				{
//					clbSites.add(s);
//				}
//				else //Must be a hardBlock
//				{
//					String typeName = ((HardBlockSite)s).getTypeName();
//					int curIndex = 0;
//					boolean found = false;
//					for(String name: hardBlockTypes)
//					{
//						if(name.contains(typeName))
//						{
//							hardBlockSites.get(curIndex).add(s);
//							found = true;
//							break;
//						}
//						curIndex++;
//					}
//					if(!found)
//					{
//						hardBlockTypes.add(typeName);
//						ArrayList <Site> newList = new ArrayList<>();
//						newList.add(s);
//						hardBlockSites.add(newList);
//					}
//				}
//			}
//		}
//		
//		//Random place CLBs
//		for(Clb clb: c.clbs.values())
//		{
//			int randomIndex = rand.nextInt(clbSites.size());
//			Site site = (Site)clbSites.get(randomIndex);
//			clbSites.remove(randomIndex);
//			site.block = clb;
//			clb.setSite(site);
//		}
//				
//		//Random place hardBlocks
//		for(Vector<HardBlock> hbVector: c.getHardBlocks())
//		{
//			String curTypeName = hbVector.get(0).getTypeName();
//			int curIndex = 0;
//			for(String name: hardBlockTypes)
//			{
//				if(name.contains(curTypeName))
//				{
//					break;
//				}
//				curIndex++;
//			}
//			for(HardBlock hardBlock: hbVector)
//			{
//				int randomIndex = rand.nextInt(hardBlockSites.get(curIndex).size());
//				Site site = (Site)hardBlockSites.get(curIndex).get(randomIndex);
//				hardBlockSites.get(curIndex).remove(randomIndex);
//				site.block = hardBlock;
//				hardBlock.setSite(site);
//			}
//		}
//		
//		//Deterministic place IOs
//		int index = 0;
//		ArrayList<Site> ISites = a.getISites();
//		ArrayList<Site> OSites = a.getOSites();
//		for(ArrayList<Block> tuple: packedIOs)
//		{
//			Input input = (Input)tuple.get(0);
//			Output output = (Output)tuple.get(1);
//			if(input != null)
//			{
//				input.fixed = false;
//				Site site = ISites.get(index);
//				site.block = input;
//				input.setSite(site);
//			}
//			if(output != null)
//			{
//				output.fixed = false;
//				Site site = OSites.get(index);
//				site.block = output;
//				output.setSite(site);
//			}
//			index += 1;
//			if(index >= a.getHeight()*2 + a.getWidth()*2)
//			{
//				index = 1;
//			}
//		}
//	}
	
}
