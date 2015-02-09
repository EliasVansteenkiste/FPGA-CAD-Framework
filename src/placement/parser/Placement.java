package placement.parser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import circuit.Circuit;

public class Placement {
	public String netlist_file;
	public String architecture_file;
	
	public Map<String,PlaatsingUnit> plaatsingsmap;

	public int width;
	public int hight;

	public Placement() {
		super();
		plaatsingsmap= new HashMap<String,PlaatsingUnit>(); 
	}
	
	public Placement IOPlacement(Circuit c) {
		Placement result;
		result = new Placement();
		
		for(Iterator<PlaatsingUnit> i=plaatsingsmap.values().iterator();i.hasNext();) {
			PlaatsingUnit pu=i.next();
			if (c.inputs.containsKey(pu.naam)||(c.outputs.containsKey(pu.naam))) {
				result.plaatsingsmap.put(pu.naam,new PlaatsingUnit(pu));
			}
		}
		result.width=this.width;
		result.hight=this.hight;
		
		return result;
	}
	
	public void centerOfGravity() {
		int som_x=0;
		int som_y=0;
		for(PlaatsingUnit pu:plaatsingsmap.values()) {
			som_x+=pu.x;
			som_y+=pu.y;
		}
		double zx=((double)som_x/plaatsingsmap.size()-(double)width/2)/width;
		double zy=((double)som_y/plaatsingsmap.size()-(double)hight/2)/hight;
		
		System.out.println(zx+"	"+zy);
	}
	
}
