package place.placers.analytical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import place.placers.analytical.HardblockSwarmLegalizer.Block;
import place.placers.analytical.HardblockSwarmLegalizer.Crit;
import place.placers.analytical.HardblockSwarmLegalizer.Net;
import place.placers.analytical.HardblockSwarmLegalizer.Site;

public class RunnableParticle implements Callable<CostAndIndexList>{//Runnable{
	final int pIndex;
	private final int numSites;
	private int velMaxSize;
	
	private Block[] blocks;
	private Site[] sites;
	private List<Crit> pCrits;
	private List<Net> pNets;
//	private Set<Net> pNets;
	
	private List<Swap> swaps;
	private List<Swap> newVel;
	
	private Set<Integer> affectedBlockIndex;
	private Set<Block> affectedBlocks;
	private Set<Net> affectedNets;
	private Set<Crit> affectedCrits;
	
	
	int[] blockIndexList;
	private int[] oldBlockIndexList;
	boolean changed;
	private List<Swap> velocity;
	
	double pCost;
	double pBest;
	int[] pBestIndexList;
	double inertiaWeight, congnitiveRate, socialRate; 
	int[] gBestBlockIdList;
	
	private Thread thread;//TODO

	private volatile boolean running;
	private volatile boolean paused;
	private final Object pauseLock = new Object();
	
	RunnableParticle(int index, Block[] blocks, Site[] sites, int velMaxSize){
		this.pIndex = index;
		
		this.blocks = blocks;
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
		
//		this.doneSignal = doneSignal;
		this.running = true;
		this.paused = false;
		
		this.affectedBlockIndex = new HashSet<>();
		this.affectedBlocks = new HashSet<>();
		this.affectedNets = new HashSet<>();
		this.affectedCrits = new HashSet<>();
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
//		this.pNets = new HashSet<Net>(columnNets);
//		if(!this.compareSame()) System.out.println("happens in p" + this.pIndex);//TODO
	}
	void setPCrits(Set<Crit> columnCrits){
		this.pCrits = new ArrayList<Crit>(columnCrits);
	}
	public void run(){
		while(this.running){
			synchronized(this.pauseLock){
				if(!this.running){// may have changed while running to synchronize on pauseLock
					break;
				}
				if(this.paused){
					try{
						this.pauseLock.wait();// will cause this Thread to block until 
                        						// another thread calls pauseLock.notifyAll()
                        						// Note that calling wait() will 
												// relinquish the synchronized lock that this 
                        						// thread holds on pauseLock so another thread
												// can acquire the lock to call notifyAll()
                        						// (link with explanation below this code)
					}catch(InterruptedException ex){
						break;
					}
					if(!this.running){// running might have changed since we paused
						break;
					}
				}
			}
			this.doWork();
		}
	}
	@Override
	public CostAndIndexList call() throws Exception {
		this.doWork();	
		return new CostAndIndexList(this.pCost, this.pIndex, this.blockIndexList);
	}
	
	void doWork(){
		//update velocity
		this.updateVelocity(this.inertiaWeight, this.congnitiveRate, this.socialRate, this.gBestBlockIdList);
				
		//update blockIndex list		
		this.updateLocations();	
				
		if(this.changed){				
			this.updateBlocksInfo();					
			//update pBest
			this.pCost = this.getCost();//TODO
					
			if(this.pCost < this.pBest){					
				this.pBest = this.pCost;						
				System.arraycopy(this.blockIndexList, 0, this.pBestIndexList, 0, this.numSites);									
			}
		}
//		System.out.println("RP_" + Integer.toString(pIndex) + "finished");
		this.pause();//particle's work finished
	}
	public void stop() {
        running = false;
        // you might also want to interrupt() the Thread that is 
        // running this Runnable, too, or perhaps call:
        resume();
        // to unblock
    }

    public void pause() {
        // you may want to throw an IllegalStateException if !running
        paused = true;
    }
    public boolean paused(){
    	return this.paused;
    }
    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll(); // Unblocks thread
        }
    }
	void setParameters(double w, double c1, double c2, int[] gBest){
		this.inertiaWeight = w;
		this.congnitiveRate = c1;
		this.socialRate = c2;
		System.arraycopy(gBest, 0, this.gBestBlockIdList, 0, gBest.length);//gBest length = this.numSites
	}
	void updateVelocity(double w, double c1, double c2, int[] gBestLocation){		
		List<Swap> weightedVel = this.multipliedByC(this.velocity, w);			
		
		this.getSwapSequence(this.pBestIndexList);				
		List<Swap> cognitiveVel = this.multipliedByC(this.swaps, c1);
		
		this.getSwapSequence(gBestLocation);
		List<Swap> socialVel = this.multipliedByC(this.swaps, c2);// * Math.random());
		
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
		
		if(!Arrays.equals(targetLoc, tmpLoc)){
			for(int m = 0; m < size; m++){
				int value = targetLoc[m];
				if(value != -1){
					for(int n = 0; n < size; n++){
						if(value == tmpLoc[n]){
							if(m != n){
								this.swaps.add(new Swap(m, n));
								doOneSwap(tmpLoc, m , n);	
								break;
							}						
						}
					}
				}		
			}
		}
		
//		System.out.println(this.swaps.size());
	}
	//do swaps to update particle's location: X + Velocity 
	void updateLocations(){
		System.arraycopy(this.blockIndexList, 0, this.oldBlockIndexList, 0, this.numSites);
		int swapsSize = 0;
		if(this.velocity != null) swapsSize = this.velocity.size();
		if(swapsSize > 0){	
			for(int velIndex = 0; velIndex < swapsSize; velIndex++){
				int from = this.velocity.get(velIndex).getFromIndex();
				int to = this.velocity.get(velIndex).getToIndex();
				if(from != to) doOneSwap(this.blockIndexList, from, to);//only update blockIndexList for a particle
			}
		}
		if(Arrays.equals(this.oldBlockIndexList, this.blockIndexList)) this.changed = false;
		else this.changed = true;
	}
	public int[] doOneSwap(int[] indexList, int from, int to){	
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
//	synchronized void updateBlocksInfo(){
	void updateBlocksInfo(){
		for(Block block:blocks){		
			Site site = getSite(this.blockIndexList, block.index);
//			block.setLegalXYs(this.pIndex, site.column, site.row);
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
//	double getCost(int i){
//		double cost = 0.0;
//		double timing = 0.0;
//		double conn = 0.0;
//		for(Net net:this.pNets) conn += net.connectionCost(i)*net.getTotalNum();
//		for(Crit crit:this.pCrits) timing += crit.timingCost(i);
//		cost = timing + conn;
//		cost = timing + conn;		
//		return cost;
//	}
	
	@SuppressWarnings("unused")
	private void getParticleAffectedNetsCrits(){		
		this.affectedBlockIndex.clear();
		this.affectedBlocks.clear();
		this.affectedNets.clear();
		this.affectedCrits.clear();
		for(Swap s:this.newVel){
			this.affectedBlockIndex.add(this.blockIndexList[s.fromIndex]);
			this.affectedBlockIndex.add(this.blockIndexList[s.toIndex]);
		}
		for(int id:this.affectedBlockIndex){
			if(id != -1){
				Block affectedBlock = getBlock(id);
				this.affectedBlocks.add(affectedBlock);
				this.affectedNets.addAll(affectedBlock.mergedNetsMap.keySet());
				this.affectedCrits.addAll(affectedBlock.crits);
			}		
		}
	}
	private Block getBlock(int blockId){
		Block block = null;
		for(Block b:this.blocks){
			if(b.index == blockId) block = b;
		}
		return block;
	}
	
	static class Swap{
		int fromIndex;
		int toIndex;
		public Swap(){
			super();
		}
		public Swap(int fromIndex, int toIndex) {
			super();
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
