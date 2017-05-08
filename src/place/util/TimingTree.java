package place.util;

import java.util.ArrayList;
import java.util.HashMap;

public class TimingTree {
	private final boolean enabled;
	private HashMap<String, Long> timers;
	private ArrayList<Time> timingResult;

	public TimingTree(){
		this.enabled = true;
		
		this.timers = new HashMap<String, Long>();
		this.timingResult = new ArrayList<Time>();
	}
	public TimingTree(boolean enabled){
		this.enabled = enabled;
		
		if(this.enabled){
			this.timers = new HashMap<String, Long>();
			this.timingResult = new ArrayList<Time>();
		}
	}
	
	public void start(String name){
		if(this.enabled){
			this.timers.put(name, System.nanoTime());
		}
	}
	public void time(String name){
		if(this.enabled){
			long start = this.timers.remove(name);
			long end = System.nanoTime();
			
			int index = this.timers.size();
			
			Time time = new Time(name, index, start, end);

			this.timingResult.add(time);
			
			if(index == 0){
				this.printTiming();
			}
		}
	}
	
	void printTiming(){
		Time head = this.timingResult.get(this.timingResult.size() - 1);
		Time parent = head;
		Time previous = head;
		for(int i = this.timingResult.size() - 2; i >= 0; i--){
			Time current = this.timingResult.get(i);
			
			if(current.depth() == parent.depth()){
				parent = parent.getParent();
				parent.addChild(current);
				current.setParent(parent);
			}else if(current.depth() == parent.depth() + 1){
				parent.addChild(current);
				current.setParent(parent);
			}else if(current.depth() == parent.depth() + 2){
				parent = previous;
				parent.addChild(current);
				current.setParent(parent);
			}
			
			previous = current;
		}
		System.out.println(head);
		this.timers = new HashMap<String, Long>();
		this.timingResult = new ArrayList<Time>();
	}
}
class Time {
	String name;

	int depth;

	long start;
	long end;

	Time parent;
	ArrayList<Time> children;

	Time(String name, int index, long start, long end){
		this.name = name;

		this.depth = index;

		this.start = start;
		this.end = end;

		this.parent = null;
		this.children = new ArrayList<Time>();
	}

	int depth(){
		return this.depth;
	}

	void setParent(Time parent){
		this.parent = parent;
	}
	Time getParent(){
		return this.parent;
	}
	void addChild(Time child){
		this.children.add(child);
	}
	Time[] getChildren(){
		int counter = this.children.size() - 1;
		Time[] result = new Time[this.children.size()];
		
		for(Time child:this.children){
			result[counter] = child;
			counter--;
		}
		return result;
	}
	
	@Override
	public String toString(){
		String result = new String();

		for(int i = 0; i < this.depth; i++){
			this.name = "\t" + this.name;
		}
		double time = (this.end -  this.start) * Math.pow(10, -3);

		if(time < 1000){
			result += String.format(this.name + " took %.0f ns\n", time);
		}else{
			time /= 1000;
			result += String.format(this.name + " took %.0f ms\n", time);
		}
    	
		for(Time child:this.getChildren()){
			result += child.toString();
		}
		
		return result;
	}
}