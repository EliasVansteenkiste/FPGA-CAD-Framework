package placers;

import architecture.Site;

import circuit.Block;
import circuit.Net;

public class EfficientBoundingBoxData
{
	
	private double weight;
	private Block[] blocks;
	
	private int min_x;
	private int nb_min_x;
	private int max_x;
	private int nb_max_x;
	private int min_y;
	private int nb_min_y;
	private int max_y;
	private int nb_max_y;
	private int boundingBox;
	
	private int min_x_old;
	private int nb_min_x_old;
	private int max_x_old;
	private int nb_max_x_old;
	private int min_y_old;
	private int nb_min_y_old;
	private int max_y_old;
	private int nb_max_y_old;
	private int boundingBox_old;
	
	public EfficientBoundingBoxData(Net net)
	{
		blocks = new Block[1 + net.sinks.size()];
		blocks[0] = net.source.owner;
		for(int i = 0; i < blocks.length - 1; i++)
		{
			blocks[i+1] = net.sinks.get(i).owner;
		}
		setWeightandSize();
		boundingBox = -1;
		min_x = Integer.MAX_VALUE;
		min_y = -1;
		max_x = Integer.MAX_VALUE;
		max_y = -1;
		calculateBoundingBoxFromScratch();
	}
	
	public double calculateDeltaCost(Block block, Site newSite)
	{
		double originalBB = boundingBox;
		if((block.x == min_x && nb_min_x == 1 && newSite.x > min_x) || (block.x == max_x && nb_max_x == 1 && newSite.x < max_x) || 
					(block.y == min_y && nb_min_y == 1 && newSite.y > min_y) || (block.y == max_y && nb_max_y == 1 && newSite.y < max_y))
		{
			Site originalSite = block.getSite();
			block.setSite(newSite);
			calculateBoundingBoxFromScratch();
			block.setSite(originalSite);
		}
		else
		{
			min_x_old = min_x;
			nb_min_x_old = nb_min_x;
			max_x_old = max_x;
			nb_max_x_old = nb_max_x;
			min_y_old = min_y;
			nb_min_y_old = nb_min_y;
			max_y_old = max_y;
			nb_max_y_old = nb_max_y;
			boundingBox_old = boundingBox;
			if(newSite.x < min_x)
			{
				min_x = newSite.x;
				nb_min_x = 1;
			}
			else
			{
				if(newSite.x == min_x && block.x != min_x)
				{
					nb_min_x++;
				}
			}
			if(newSite.x > max_x)
			{
				max_x = newSite.x;
				nb_max_x = 1;
			}
			else
			{
				if(newSite.x == max_x && block.x != max_x)
				{
					nb_max_x++;
				}
			}
			if(newSite.y < min_y)
			{
				min_y = newSite.y;
				nb_min_y = 1;
			}
			else
			{
				if(newSite.y == min_y && block.y != min_y)
				{
					nb_min_y++;
				}
			}
			if(newSite.y > max_y)
			{
				max_y = newSite.y;
				nb_max_y = 1;
			}
			else
			{
				if(newSite.y == max_y && block.y != max_y)
				{
					nb_max_y++;
				}
			}
		}
		boundingBox = (max_x-min_x+1)+(max_y-min_y+1);
	
		return weight*(boundingBox-originalBB);
	}
	
	
	public void revert()
	{
		boundingBox = boundingBox_old;
		min_x = min_x_old;
		nb_min_x = nb_min_x_old;
		max_x = max_x_old;
		nb_max_x = nb_max_x_old;
		min_y = min_y_old;
		nb_min_y = nb_min_y_old;
		max_y = max_y_old;
		nb_max_y = nb_max_y_old;
	}
	
	public double getNetCost()
	{
		return boundingBox*weight;
	}
	
	private void calculateBoundingBoxFromScratch() 
	{
		min_x_old = min_x;
		nb_min_x_old = nb_min_x;
		max_x_old = max_x;
		nb_max_x_old = nb_max_x;
		min_y_old = min_y;
		nb_min_y_old = nb_min_y;
		max_y_old = max_y;
		nb_max_y_old = nb_max_y;
		boundingBox_old = boundingBox;
		
		min_x = Integer.MAX_VALUE;
		max_x = -1;
		min_y = Integer.MAX_VALUE;
		max_y = -1;
		for(int i = 0; i < blocks.length; i++)
		{
			if(blocks[i].x < min_x)
			{
				min_x = blocks[i].x;
				nb_min_x = 1;
			}
			else
			{
				if(blocks[i].x == min_x)
				{
					nb_min_x++;
				}
			}
			if(blocks[i].x > max_x)
			{
				max_x = blocks[i].x;
				nb_max_x = 1;
			}
			else
			{
				if(blocks[i].x == max_x)
				{
					nb_max_x++;
				}
			}
			if(blocks[i].y < min_y)
			{
				min_y = blocks[i].y;
				nb_min_y = 1;
			}
			else
			{
				if(blocks[i].y == min_y)
				{
					nb_min_y++;
				}
			}
			if(blocks[i].y > max_y)
			{
				max_y = blocks[i].y;
				nb_max_y = 1;
			}
			else
			{
				if(blocks[i].y == max_y)
				{
					nb_max_y++;
				}
			}
		}
		boundingBox = (max_x-min_x+1)+(max_y-min_y+1);
	}
	
	private void setWeightandSize() {
		int size = blocks.length;
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
			case 15: weight=(size-10)*(1.6899-1.4493)/5+1.4493;break;				
			case 16:
			case 17:
			case 18:
			case 19:
			case 20: weight=(size-15)*(1.8924-1.6899)/5+1.6899;break;
			case 21:
			case 22:
			case 23:
			case 24:
			case 25: weight=(size-20)*(2.0743-1.8924)/5+1.8924;break;		
			case 26:
			case 27:
			case 28:
			case 29:
			case 30: weight=(size-25)*(2.2334-2.0743)/5+2.0743;break;		
			case 31:
			case 32:
			case 33:
			case 34:
			case 35: weight=(size-30)*(2.3895-2.2334)/5+2.2334;break;		
			case 36:
			case 37:
			case 38:
			case 39:
			case 40: weight=(size-35)*(2.5356-2.3895)/5+2.3895;break;		
			case 41:
			case 42:
			case 43:
			case 44:
			case 45: weight=(size-40)*(2.6625-2.5356)/5+2.5356;break;		
			case 46:
			case 47:
			case 48:
			case 49:
			case 50: weight=(size-45)*(2.7933-2.6625)/5+2.6625;break;
			default: weight=(size-50)*0.02616+2.7933;break;
		}
	}
	
}
