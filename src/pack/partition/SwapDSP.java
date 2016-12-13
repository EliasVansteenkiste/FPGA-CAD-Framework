package pack.partition;

import java.util.HashSet;

import pack.netlist.B;
import pack.util.ErrorLog;

public class SwapDSP {
	private Part[] result;
	
	public SwapDSP(Part[] result){
		this.result = result;
	}
	public void run(){
		//Move DSP blocks between netlists if both parts have an odd number of DSP blocks
		if((this.result[0].numDSPPrimitives()%2 == 1)  && (this.result[1].numDSPPrimitives()%2 == 1)){
			this.move();
			if((this.result[0].numDSPPrimitives()%2 == 1)  || (this.result[1].numDSPPrimitives()%2 == 1)){
				ErrorLog.print("Something went wrong in block swapping | P0: " + this.result[0].numDSPPrimitives() + " | P1: " + this.result[1].numDSPPrimitives());
			}
		}
	}
	private void move(){
		//Move DSP block that leads to the smallest increase in terminal count
		B moveBlock = null;
		int minTerminalIncrease = Integer.MAX_VALUE;
		for(int i=0;i<2;i++){
			Part from = (i==0) ? this.result[0] : this.result[1];
			Part to = (i==0) ? this.result[1] : this.result[0];
			HashSet<B> primitives = new HashSet<B>(from.getDSPPrimitives());
			for(B b:primitives){
				int terminalIncrease = from.connections(b) - to.connections(b);
				if(terminalIncrease < minTerminalIncrease){
					minTerminalIncrease = terminalIncrease;
					moveBlock = b;
				}
			}
		}
		if(this.result[0].hasBlock(moveBlock)){
			moveBlock.move(this.result[0], this.result[1]);
		}else if(this.result[1].hasBlock(moveBlock)){
			moveBlock.move(this.result[1], this.result[0]);
		}else{
			ErrorLog.print("Move block " + moveBlock + " is no element of part0 and part1");
		}
	}
}