package pack.netlist;

import pack.architecture.Architecture;
import pack.util.ErrorLog;

public class CombLoop {
	private P sinkPin;
	private N net;
	private B block;
	
	public CombLoop(P p){
		this.sinkPin = p;
		this.net = p.get_net();
		if(!p.has_block()){
			ErrorLog.print("This pin should have a block:\n" + this.sinkPin.toInfoString());
		}
		this.block = p.get_block();
		
		if(!this.sinkPin.is_sink_pin()){
			ErrorLog.print("This pin should be a sink pin:\n" + this.sinkPin.toInfoString());
		}else{
			this.net.remove_sink(this.sinkPin);
			this.block.remove_input_pin(this.sinkPin);
		}
	}
	public P getPin(){
		return this.sinkPin;
	}
	public void reconnect(Architecture arch, DelayMap connectionDelay){
		this.block.add_input_pin(this.sinkPin.get_port_name(), this.sinkPin);
		this.net.add_sink(this.sinkPin);
	}
}
