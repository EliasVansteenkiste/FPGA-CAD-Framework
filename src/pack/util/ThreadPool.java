package pack.util;

import java.util.ArrayList;

public class ThreadPool {
	private int size;
	private ArrayList<Integer> emptyThreads;
	private int maxUsage;
	
	public ThreadPool(int size){
		this.size = size;
		this.emptyThreads = new ArrayList<Integer>();
		for(int i=0; i<this.size; i++){
			this.emptyThreads.add(i);
		}
		this.maxUsage = 0;
	}
	public boolean isEmpty(){
		return this.emptyThreads.isEmpty();
	}
	public int usedThreads(){
		return (this.size - this.emptyThreads.size());
	}
	public int getThread(){
		int thread = this.emptyThreads.remove(0);
		int threadsUsed = this.size - this.emptyThreads.size();
		if(threadsUsed > this.maxUsage){
			this.maxUsage = threadsUsed;
		}
		return thread;
	}
	public void addThread(Integer thread){
		this.emptyThreads.add(thread);
	}
	public int maxUsage(){
		return this.maxUsage;
	}
	public int size(){
		return this.size;
	}
}
