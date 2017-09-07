package pack.netlist;

import pack.util.ErrorLog;

//Stratixiv architecture
//LAB <loc priority="1" type="fill"/>								=> 1
//M9K <loc priority="5" repeat="26" start="5" type="col"/>			=> 2
//M114K <loc priority="10" repeat="43" start="33" type="col"/>		=> 3
//DSP <loc type="col" start="6" repeat="40" priority="15"/>			=> 4
//PLL <loc priority="90" start="1" type="col"/>						=> 5

public class FPGA {
	private int[] arch;
	private int maxSize;
	
	private int sizeX;
	private int sizeY;
	
	private int availableLAB;
	private int availableM9K;
	private int availableM144K;
	private int availableDSP;
	private int availablePLL;
	
	public FPGA(){//TODO get the properties from architecture file
		this.maxSize = 500;
		this.arch = new int[this.maxSize];
		for(int i=0;i<this.maxSize;i++){
			this.arch[i] = 1;//LAB
		}
		for(int i=5;i<this.maxSize;i+=26){
			this.arch[i] = 2;//"M9K"
		}
		for(int i=33;i<this.maxSize;i+=43){
			this.arch[i] = 3;//"M144K"
		}
		for(int i=6;i<this.maxSize;i+=40){
			this.arch[i] = 4;//"DSP";
		}
		this.arch[1] = 5;//"PLL";
		
		this.sizeX = 0;
		this.sizeY = 0;
		
		this.update_available_blocks();
	}
	public void set_size(int sizeX,int sizeY){
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		if(this.sizeX != (int)Math.round(this.sizeY * 1.35)){
			ErrorLog.print("Size X should be equal to " + (int)Math.round(this.sizeY * 1.35) + " but is equal to " + this.sizeX);
		 }
		if(this.sizeX > this.maxSize){
			ErrorLog.print("Maximum sizeX of " + this.maxSize + " exceeded => " + this.sizeX);
		}
		this.update_available_blocks();
	}
	public void increase_size(){
		this.sizeY += 1;
		this.sizeX = (int)Math.round(sizeY * 1.35);
		if(this.sizeX > this.maxSize){
			ErrorLog.print("Maximum sizeX of " + this.maxSize + " exceeded => " + this.sizeX);
		}
		this.update_available_blocks();
	}
	private void update_available_blocks(){
		this.availableLAB = 0;
		this.availableM9K = 0;
		this.availableM144K = 0;
		this.availableDSP = 0;
		this.availablePLL = 0;
		
		for(int a=1;a<=this.sizeX;a++){
			switch (this.arch[a]) {
				case 1: this.availableLAB += this.sizeY; break;
				case 2: this.availableM9K += this.sizeY; break;
				case 3: this.availableM144K += Math.floor(this.sizeY*0.125); break;
				case 4: this.availableDSP += Math.floor(this.sizeY*0.25); break;
				case 5: this.availablePLL += this.sizeY; break;
				default: ErrorLog.print("Unknown block type => " + this.arch[a]);
			}
		}
	}
	public int sizeX(){
		return this.sizeX;
	}
	public int sizeY(){
		return this.sizeY;
	}
	public int LAB(){
		return this.availableLAB;
	}
	public int M9K(){
		return this.availableM9K;
	}
	public int M144K(){
		return this.availableM144K;
	}
	public int DSP(){
		return this.availableDSP;
	}
	public int PLL(){
		return this.availablePLL;
	}
}
