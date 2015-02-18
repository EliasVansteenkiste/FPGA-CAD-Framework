package placers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import circuit.Block;
import architecture.Site;

public class BoundingBoxData {

	private double weight;
	
	private int size;
	
	public Set<Block> blocks;
	
	int boundingBox;
	
	int min_x;
	int min_y;
	int max_x;
	int max_y;
	
	Block owner;
	
	public double getWeight(){
		return weight;
	}
	
	public BoundingBoxData(Collection<Block> blocks) {
		this.blocks = new HashSet<Block>();
		this.blocks.addAll(blocks);
		setWeightandSize();
		boundingBox=-1;
		min_x = Integer.MAX_VALUE;
		min_y = -1;
		max_x = Integer.MAX_VALUE;
		max_y = -1;
	}
	
	public int calculateBoundingBox() {
		int bb;
		if(blocks.size()==2){
			Iterator<Block> it = blocks.iterator();
			Block b = it.next();
			Block c = it.next();
			bb=Math.abs(b.getSite().x-c.getSite().x)+Math.abs(b.getSite().y-c.getSite().y)+2;
		}else{
			int xmin = Integer.MAX_VALUE;
			int xmax = -1;
			int ymin = Integer.MAX_VALUE;
			int ymax = -1;
			for(Block bl : blocks) {
				if (bl.getSite().x < xmin)xmin=bl.getSite().x;			
				if (bl.getSite().x > xmax)xmax=bl.getSite().x;			
				if (bl.getSite().y < ymin)ymin=bl.getSite().y;
				if (bl.getSite().y > ymax)ymax=bl.getSite().y;
			}
			
			bb=(xmax-xmin+1)+(ymax-ymin+1);
			min_x = xmin;
			min_y = ymin;
			max_x = xmax;
			max_y = ymax;
		}
		return bb;
	}
	
	public int getBoundingBox() {
		if(boundingBox==-1){
			return calculateBoundingBox();
		}else{
			//if(calculateBoundingBox()!=boundingBox)System.out.println("check:nok");
			return boundingBox;
		}
	}
	
	public int[] boundingBox(Block b1, Site newSite1, Block b2, Site newSite2) {
		int[]output = new int[5];
		if(blocks.size()==2){
			Site oldSite1 = b1.getSite();
			b1.setSite(newSite1);
			Site oldSite2 = b2.getSite();
			b2.setSite(newSite2);
			Iterator<Block> it = blocks.iterator();
			Site s1 = it.next().getSite();
			Site s2 = it.next().getSite();
			output[0]=Math.abs(s1.x-s2.x)+Math.abs(s1.y-s2.y)+2;
			output[1]=-2;
			b1.setSite(oldSite1);
			b2.setSite(oldSite2);
		}else{
			Site oldSite1 = b1.getSite();
			b1.setSite(newSite1);
			Site oldSite2 = b2.getSite();
			b2.setSite(newSite2);
			int xmin = Integer.MAX_VALUE;
			int xmax = -1;
			int ymin = Integer.MAX_VALUE;
			int ymax = -1;
			for(Block bl : blocks) {
				Site s;
				s=bl.getSite();
				if (s.x < xmin)xmin=s.x;
				if (s.x > xmax)xmax=s.x;
				if (s.y < ymin)ymin=s.y;
				if (s.y > ymax)ymax=s.y;
			}
			output[0]=(xmax-xmin+1)+(ymax-ymin+1);
			output[1]= xmin;
			output[2]= xmax;
			output[3]= ymin;
			output[4]= ymax;
			b1.setSite(oldSite1);
			b2.setSite(oldSite2);
		}
		return output;
	}
	
	public int[] boundingBox(Block b, Site newSite) {
		int[] output = new int[5];
		if (blocks.size() == 2) {
			Site oldSite1 = b.getSite();
			b.setSite(newSite);
			Iterator<Block> it = blocks.iterator();
			Site s1 = it.next().getSite();
			Site s2 = it.next().getSite();
			output[0] = Math.abs(s1.x - s2.x) + Math.abs(s1.y - s2.y) + 2;
			output[1] = -2;
			b.setSite(oldSite1);
		} else if (blocks.size() < 5) {
			Site oldSite1 = b.getSite();
			b.setSite(newSite);
			int xmin = Integer.MAX_VALUE;
			int xmax = -1;
			int ymin = Integer.MAX_VALUE;
			int ymax = -1;
			for (Block bl : blocks) {
				Site s;
				s = bl.getSite();
				if (s.x < xmin)
					xmin = s.x;
				if (s.x > xmax)
					xmax = s.x;
				if (s.y < ymin)
					ymin = s.y;
				if (s.y > ymax)
					ymax = s.y;
			}
			output[0] = (xmax - xmin + 1) + (ymax - ymin + 1);
			output[1] = xmin;
			output[2] = xmax;
			output[3] = ymin;
			output[4] = ymax;
			b.setSite(oldSite1);
		} else {
			if (b.getSite().x < max_x && b.getSite().x > min_x && b.getSite().y < max_y && b.getSite().y > min_y) {
				//b inside BB and not on the edges of the BB
				if (newSite.x <= max_x && newSite.x >= min_x && newSite.y <= max_y && newSite.y >= min_y && boundingBox != -1) {
					//newSite inside BB or on the edges of the BB
					output[0] = boundingBox;
					output[1] = -2;
				} else {
					int xmin = min_x;
					int xmax = max_x;
					int ymin = min_y;
					int ymax = max_y;
					if (newSite.x < xmin)
						xmin = newSite.x;
					if (newSite.x > xmax)
						xmax = newSite.x;
					if (newSite.y < ymin)
						ymin = newSite.y;
					if (newSite.y > ymax)
						ymax = newSite.y;
					output[0] = (xmax - xmin + 1) + (ymax - ymin + 1);
					output[1] = xmin;
					output[2] = xmax;
					output[3] = ymin;
					output[4] = ymax;
				}
			} else {
				Site oldSite1 = b.getSite();
				b.setSite(newSite);
				int xmin = Integer.MAX_VALUE;
				int xmax = -1;
				int ymin = Integer.MAX_VALUE;
				int ymax = -1;
				for (Block bl : blocks) {
					Site s;
					s = bl.getSite();
					if (s.x < xmin)
						xmin = s.x;
					if (s.x > xmax)
						xmax = s.x;
					if (s.y < ymin)
						ymin = s.y;
					if (s.y > ymax)
						ymax = s.y;
				}
				output[0] = (xmax - xmin + 1) + (ymax - ymin + 1);
				output[1] = xmin;
				output[2] = xmax;
				output[3] = ymin;
				output[4] = ymax;
				b.setSite(oldSite1);
			}

		}
		return output;
	}
	

	public void setBBandBoundaries(int[] BB_xmin_xmax_ymin_ymax){
		if(BB_xmin_xmax_ymin_ymax[1]!=-2){
			min_x = BB_xmin_xmax_ymin_ymax[1];
			min_y = BB_xmin_xmax_ymin_ymax[2];
			max_x = BB_xmin_xmax_ymin_ymax[3];
			max_y = BB_xmin_xmax_ymin_ymax[4];
		}
	}
	
	public double calculateCost() {
		return weight*calculateBoundingBox();
	}

	public double getCost() {
		return weight*getBoundingBox();
	}
	
	private void setWeightandSize() {
		size = blocks.size();
		switch (size) {
			case 1:  weight=1; break;
			case 2:  weight=1; break;
			case 3:  weight=1; break;
			case 4:  weight=1.0828; break;
			case 5:  weight=1.1536; break;
			case 6:  weight=1.2206; break;
			case 7:  weight=1.2823; break;
			case 8:  weight=1.3385; break;
			case 9:  weight=1.3991; break;
			case 10: weight=1.4493; break;
			case 11:
			case 12:
			case 13:
			case 14:
			case 15: weight=(blocks.size()-10)*(1.6899-1.4493)/5+1.4493;break;				
			case 16:
			case 17:
			case 18:
			case 19:
			case 20: weight=(blocks.size()-15)*(1.8924-1.6899)/5+1.6899;break;
			case 21:
			case 22:
			case 23:
			case 24:
			case 25: weight=(blocks.size()-20)*(2.0743-1.8924)/5+1.8924;break;		
			case 26:
			case 27:
			case 28:
			case 29:
			case 30: weight=(blocks.size()-25)*(2.2334-2.0743)/5+2.0743;break;		
			case 31:
			case 32:
			case 33:
			case 34:
			case 35: weight=(blocks.size()-30)*(2.3895-2.2334)/5+2.2334;break;		
			case 36:
			case 37:
			case 38:
			case 39:
			case 40: weight=(blocks.size()-35)*(2.5356-2.3895)/5+2.3895;break;		
			case 41:
			case 42:
			case 43:
			case 44:
			case 45: weight=(blocks.size()-40)*(2.6625-2.5356)/5+2.5356;break;		
			case 46:
			case 47:
			case 48:
			case 49:
			case 50: weight=(blocks.size()-45)*(2.7933-2.6625)/5+2.6625;break;
			default: weight=(blocks.size()-50)*0.02616+2.7933;break;
		}
	}
	
	public String toString(){
		return owner.toString();
	}
	
}
