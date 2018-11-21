package place.placers.analytical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import place.placers.analytical.HardblockSwarmLegalizer.Block;
import place.placers.analytical.HardblockSwarmLegalizer.Crit;
import place.placers.analytical.HardblockSwarmLegalizer.Net;
import place.placers.analytical.HardblockSwarmLegalizer.Site;

public class Particle{
	final int pIndex;
	private final int numSites;
	private int velMaxSize;
	
	private Block[] orderedblocks;
	private Site[] sites;
	private List<Crit> pCrits;
	private List<Net> pNets;
	
	private List<Swap> swaps;
	private List<Swap> newVel;
	
	int[] blockIndexList;
	private int[] oldBlockIndexList;
	boolean changed;
	private List<Swap> velocity;
	
	double pCost;
	double pBest;
	int[] pBestIndexList;
	double inertiaWeight, congnitiveRate, socialRate; 
	int[] gBestBlockIdList;

	Particle(int index, Block[] blocks, Site[] sites, int velMaxSize){
		this.pIndex = index;
		
		this.orderedblocks = blocks;
		this.sites = sites;
		this.numSites = this.sites.length;
		this.velMaxSize = velMaxSize;
		
		this.velocity = new ArrayList<Swap>();
		this.blockIndexList = new int[numSites];
		this.oldBlockIndexList = new int[numSites];
		this.changed = false;
		this.pCost = 0.0;
		this.pBest = 0.0;
		this.pBestIndexList = new int[this.numSites];
		this.gBestBlockIdList = new int[this.numSites];
		
		this.swaps = new ArrayList<Swap>();
		this.newVel = new ArrayList<Swap>();
		
	}
	void setVelocity(List<Swap> vel){
		this.velocity = new ArrayList<Swap>(vel);
	}
	void abandonOldVelocity(){
		this.velocity.clear();
	}
	void addPermutationSwap(Swap s){
		this.velocity.add(s);
	}
	List<Swap> getVelopcity(){
		return this.velocity;
	}
	void setPNets(Set<Net> columnNets){
		this.pNets = new ArrayList<Net>(columnNets);
		
	}
	void setPCrits(Set<Crit> columnCrits){
		this.pCrits = new ArrayList<Crit>(columnCrits);
	}	
	void doWork(){
		//update velocity
		this.updateVelnew(this.inertiaWeight, this.congnitiveRate, this.socialRate, this.gBestBlockIdList);
				
		//update blockIndex list		
		this.updateLocations();	
	
		if(this.changed){				
			
			this.updateBlocksInfo();					
//			//update pBest
			this.pCost = this.getCost();//TODO

			if(this.pCost < this.pBest){					
				this.pBest = this.pCost;						
				System.arraycopy(this.blockIndexList, 0, this.pBestIndexList, 0, this.numSites);									
			}
		}
		
	}
	
	void setParameters(double w, double c1, double c2, int[] gBest){
		this.inertiaWeight = w;
		this.congnitiveRate = c1;
		this.socialRate = c2;
		System.arraycopy(gBest, 0, this.gBestBlockIdList, 0, gBest.length);
	}
	void updateVelnew(double w, double c1, double c2, int[] gBestLocation){
		List<Swap> swapSequence = new ArrayList<>();
		if(c1 != 0){
			this.getSwapSequence(this.pBestIndexList);
			swapSequence = this.multipliedByC(this.swaps, c1);
		}else if(c2 != 0){
			this.getSwapSequence(gBestLocation);
			swapSequence = this.multipliedByC(this.swaps, c2);
		}else if(w != 0){
			swapSequence = this.multipliedByC(this.velocity, w);
		}
		this.newVel.clear();

		if(swapSequence.size() > this.velMaxSize){
			for(int l = 0; l < this.velMaxSize; l++){
				this.newVel.add(swapSequence.get(l));
			}
		}else if(swapSequence.size() > 0){
			this.newVel.addAll(swapSequence);
		}
		this.setVelocity(this.newVel);
	}
	void updateVelocity(double w, double c1, double c2, int[] gBestLocation){		
		List<Swap> weightedVel = this.multipliedByC(this.velocity, w);			
		
		this.getSwapSequence(this.pBestIndexList);				
		List<Swap> cognitiveVel = this.multipliedByC(this.swaps, c1);
		
		this.getSwapSequence(gBestLocation);
		List<Swap> socialVel = this.multipliedByC(this.swaps, c2);
		
		this.newVel.clear();
		
		int length0 = 0;
		int length1 = 0;
		int length2 = 0;
		if(weightedVel != null) length0 = weightedVel.size();
		if(cognitiveVel != null) length1 = cognitiveVel.size();
		if(socialVel != null) length2 = socialVel.size();
		
		if(length0 + length1 + length2 < this.velMaxSize){
			if(weightedVel != null) this.newVel.addAll(weightedVel);
			if(cognitiveVel != null) this.newVel.addAll(cognitiveVel);
			if(socialVel != null) this.newVel.addAll(socialVel);
		}else{
			int length0Max = (int)Math.round(w / (w + c1 + c2)*this.velMaxSize);
			int length1Max = (int)Math.round(c1 / (w+ c1 + c2) * this.velMaxSize);
			int length2Max = this.velMaxSize - length1Max - length0Max;
			if(weightedVel != null && length0Max != 0){
				if(weightedVel.size() <= length0Max){
					this.newVel.addAll(weightedVel);
				}else{
					for(int l = 0; l < length0Max; l++){
						this.newVel.add(weightedVel.get(l));
					}
				}
			}
			if(cognitiveVel != null && length1Max != 0){
				if(cognitiveVel.size() <= length1Max){
					this.newVel.addAll(cognitiveVel);
				}else{
					for(int l = 0; l < length1Max; l++){
						this.newVel.add(cognitiveVel.get(l));
					}
				}
			}
			if(socialVel != null && length2Max != 0){
				if(socialVel.size() <= length2Max){
					this.newVel.addAll(socialVel);
				}else{
					for(int l = 0; l < length2Max; l++){
						this.newVel.add(socialVel.get(l));
					}
				}
			}
		}
		this.setVelocity(this.newVel);
	}
	//Velocity multiplied by a constant
	private List<Swap> multipliedByC(List<Swap> vel, double c){
		List<Swap> weightedVel = new ArrayList<>();
		int newSize = (int)Math.floor(vel.size() * c);
		if(vel.size() != 0){
			if(c == 0){
				weightedVel = null;
			}
			if(c == 1){
				weightedVel.addAll(vel);
			}
			if(c < 1){	
				for(int newVelIndex = 0; newVelIndex < newSize; newVelIndex++){
					weightedVel.add(vel.get(newVelIndex));
				}
			}else if(c > 1){
				int nLoop = (int)Math.floor(newSize / vel.size());
				for(int n = 0; n < nLoop; n++){
					weightedVel.addAll(vel);
				}
				int leftLength = newSize - nLoop * vel.size();
				for(int newVelIndex = 0; newVelIndex < leftLength; newVelIndex++){
					weightedVel.add(vel.get(newVelIndex));
				}
			}
		}
		return weightedVel;
	}
	//pBest(gBest) - X 
	private void getSwapSequence(int[] targetLoc){	
		this.swaps.clear();
		int size = targetLoc.length;
		int[] tmpLoc = new int[size];
		System.arraycopy(this.blockIndexList, 0, tmpLoc, 0, size);
		
		while(!Arrays.equals(targetLoc, tmpLoc)){
			for(int m = 0; m < size; m++){
				int value = targetLoc[m];
				if(value != -1){
					for(int n = 0; n < size; n++){
						if(value == tmpLoc[n]){
							if(m != n){
								this.swaps.add(new Swap(m, n));
								doIndexSwap(tmpLoc, m , n);	
								break;
							}						
						}
					}
				}		
			}
		}
		
	}
	//do swaps to update particle's location: X + Velocity 
	void updateLocations(){
		System.arraycopy(this.blockIndexList, 0, this.oldBlockIndexList, 0, this.numSites);
		int swapsSize = 0;
		if(this.velocity != null) swapsSize = this.velocity.size();
		if(swapsSize > 0){	
			for(Swap s:this.velocity){
				int from = s.getFromIndex();
				int to = s.getToIndex();
				if(from != to) doIndexSwap(this.blockIndexList, from, to);//only update blockIndexList for a particle
			}
		}
		if(Arrays.equals(this.oldBlockIndexList, this.blockIndexList)) this.changed = false;
		else this.changed = true;
	}
	
	
	Block getBlock(int index){
		Block block = null;
		if(index == -1){
			block = null;
		}
		else{
			for(Block b:this.orderedblocks){
				if(b.index == index) block = b;
			}
		}
		
		return block;
	}
	public int[] doIndexSwap(int[] indexList, int from, int to){	
		int tmp;
		int indexFrom = indexList[from];
		int indexTo = indexList[to];
		if(indexFrom != indexTo){
			tmp = indexFrom;
			indexList[from] = indexTo;
			indexList[to] = tmp;
		}
		return indexList;
	}
	void updateBlocksInfo(){
		for(Block block:orderedblocks){		
			Site site = getSite(this.blockIndexList, block.index);
			block.setLegalXY(site.column, site.row);
		}
	}
	private Site getSite(int[] list, int value){
		int position = 0;
		for(int pos = 0; pos < list.length; pos++){
			if(list[pos] == value) position = pos;
		}
		return this.sites[position];
	}
	
	double getCost(){
		double cost = 0.0;
		double timing = 0.0;
		double conn = 0.0;

		for(Net net:this.pNets) conn += net.connectionCost()*net.getTotalNum();
		for(Crit crit:this.pCrits) timing += crit.timingCost();
		cost = timing + conn;
		
		return cost;
	}
	
	static class Swap{
		int fromIndex;
		int toIndex;

		public Swap(int fromIndex, int toIndex) {
			this.fromIndex = fromIndex;
			this.toIndex = toIndex;
		}
		public int getFromIndex() {
			return this.fromIndex;
		}
		public void setFromIndex(int fromIndex) {
			this.fromIndex = fromIndex;
		}
		public int getToIndex() {
			return this.toIndex;
		}
		public void setToIndex(int toIndex) {
			this.toIndex = toIndex;
		}			
	}
	
}
