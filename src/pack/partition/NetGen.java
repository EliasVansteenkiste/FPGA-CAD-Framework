package pack.partition;

import pack.netlist.Netlist;

public class NetGen implements Runnable{
	private Part part;
	private Netlist parent;
	private Netlist result;
	private int thread;
	
	public NetGen(Part part, Netlist parent, int thread){
		this.part = part;
		this.parent = parent;
		this.thread = thread;
	}
	public Netlist result(){
		return this.result;
	}
	public Netlist parent(){
		return this.parent;
	}
	public int thread(){
		return this.thread;
	}
	@Override
	public void run(){
		this.result = new Netlist(this.part, this.parent);
	}
}
