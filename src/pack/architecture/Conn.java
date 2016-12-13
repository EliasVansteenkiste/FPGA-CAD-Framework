package pack.architecture;

public class Conn {
	private String name;
	private Pin source;
	private Pin sink;
	private int delay;//ps
	private boolean global;
	
	public Conn(String name, Pin source, Pin sink, boolean global){
		this.name = name;
		this.source = source;
		this.sink = sink;
		this.delay = -1;
		this.global = global;
	}
	
	//GETTERS
	public String get_name(){
		return this.name;
	}
	public Pin get_source(){
		return this.source;
	}
	public Pin get_sink(){
		return this.sink;
	}

	//DELAY
	public int get_delay(){
		return this.delay;
	}
	public void set_delay(int delay){
		this.delay = delay;
	}
	
	//GLOBAL
	public boolean is_global(){
		return this.global;
	}
}