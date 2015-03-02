package placers;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import circuit.PackedCircuit;
import circuit.Clb;
import circuit.Input;
import circuit.Output;

import architecture.FourLutSanitized;
import architecture.Site;



public class Rplace {

	public static void place(PackedCircuit c, FourLutSanitized a) {
		Random rand= new Random();
		Set<Site> temp = new HashSet<Site>();
		for (int x=1;x<a.width+1;x++) {
			for (int y=1;y<a.height+1;y++) {
				Site s = a.siteArray[x][y][0];
				s.block = null;
				temp.add(s);				
			}
		}
		for(Clb b:c.clbs.values()) {
			if (b instanceof Clb) {
				//System.out.println("yeah");
				Site site=(Site) temp.toArray()[rand.nextInt(temp.size())];
				temp.remove(site);
				site.block = (Clb) b;
				b.setSite(site);
			}	
		}
	}
	
	public static void placeCLBs(PackedCircuit c, FourLutSanitized a, Random rand) {
		Set<Site> temp = new HashSet<Site>();
		for (int x=1;x<a.width+1;x++) {
			for (int y=1;y<a.height+1;y++) {
				Site s = a.siteArray[x][y][0];
				s.block = null;
				temp.add(s);				
			}
		}
		for(Clb b:c.clbs.values()) {
			if (b instanceof Clb) {
				//System.out.println("yeah");
				Site site=(Site) temp.toArray()[rand.nextInt(temp.size())];
				temp.remove(site);
				site.block = (Clb) b;
				b.setSite(site);
			}	
		}
	}
	
	
	public static void placeCLBsandIOs(PackedCircuit c, FourLutSanitized a, Random rand) {
		//Random place CLBs
		Set<Site> temp = new HashSet<Site>();
		for (int x=1;x<a.width+1;x++) {
			for (int y=1;y<a.height+1;y++) {
				Site s = a.siteArray[x][y][0];
				s.block = null;
				temp.add(s);				
			}
		}
		for(Clb b:c.clbs.values()) {
			if (b instanceof Clb) {
				//System.out.println("yeah");
				Site site=(Site) temp.toArray()[rand.nextInt(temp.size())];
				temp.remove(site);
				site.block = (Clb) b;
				b.setSite(site);
			}	
		}
		//Random Place IOs
		Set<Site> tempInputs = new HashSet<Site>();
		for (int x=1;x<a.width+1;x++) {
			Site s = a.siteArray[x][0][0];
			s.block = null;
			tempInputs.add(s);
			Site t = a.siteArray[x][a.height+1][0];
			t.block = null;
			tempInputs.add(t);
		}
		for (int y=1;y<a.height+1;y++) {
			Site s = a.siteArray[0][y][0];
			s.block = null;
			tempInputs.add(s);
			Site t = a.siteArray[a.width+1][y][0];
			t.block = null;
			tempInputs.add(t);
		}
		Set<Site> tempOutputs = new HashSet<Site>();
		for (int x=1;x<a.width+1;x++) {
			Site s = a.siteArray[x][0][1];
			s.block = null;
			tempOutputs.add(s);
			Site t = a.siteArray[x][a.height+1][1];
			t.block = null;
			tempOutputs.add(t);
		}
		for (int y=1;y<a.height+1;y++) {
			Site s = a.siteArray[0][y][1];
			s.block = null;
			tempOutputs.add(s);
			Site t = a.siteArray[a.width+1][y][1];
			t.block = null;
			tempOutputs.add(t);
		}
		for(Input in:c.inputs.values()) {
			if (in instanceof Input) {
				//System.out.println("yeah");
				Site site=(Site) tempInputs.toArray()[rand.nextInt(tempInputs.size())];
				tempInputs.remove(site);
				site.block = (Input) in;
				in.setSite(site);
			}	
		}
		for(Output out:c.outputs.values()) {
			if (out instanceof Output) {
				//System.out.println("yeah");
				Site site=(Site) tempOutputs.toArray()[rand.nextInt(tempOutputs.size())];
				tempOutputs.remove(site);
				site.block = (Output) out;
				out.setSite(site);
			}	
		}
	}
	
	public static void placeCLBsandFixedIOs(PackedCircuit c, FourLutSanitized a, Random rand) {
		//Random place CLBs
		Set<Site> temp = new HashSet<Site>();
		for (int x=1;x<a.width+1;x++) {
			for (int y=1;y<a.height+1;y++) {
				Site s = a.siteArray[x][y][0];
				s.block = null;
				temp.add(s);				
			}
		}
		for(Clb b:c.clbs.values()) {
			if (b instanceof Clb) {
				//System.out.println("yeah");
				Site site=(Site) temp.toArray()[rand.nextInt(temp.size())];
				temp.remove(site);
				site.block = (Clb) b;
				b.setSite(site);
			}	
		}
		int index = 0;
		for(Input input:c.inputs.values())
		{
			input.fixed = true;
			Site site = a.Isites.get(index);
			site.block = input;
			input.setSite(site);
			index += 2;
			if(index >= a.Isites.size())
			{
				index = 1;
			}
		}
		index = 0;
		for(Output output:c.outputs.values())
		{
			output.fixed = true;
			Site site = a.Osites.get(index);
			site.block = output;
			output.setSite(site);
			index += 2;
			if(index >= a.Osites.size())
			{
				index = 1;
			}
		}
	}
}
