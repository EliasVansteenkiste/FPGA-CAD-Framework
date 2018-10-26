package route.circuit.resource;

public class RouteSwitch {
	public final int index;
	public final boolean buffered;
	
	public final float r;
	public final float cin;
	public final float cout;
	public final float tdel;
	
	public final float mux_trans_size;
	public final float buf_size;
	
	public RouteSwitch(String line) {
		while(line.contains("  ")) line = line.replace("  ", " ");
		
		String[] words = line.split(" ");
		
		this.index = Integer.parseInt(words[1]);
		this.buffered = Boolean.parseBoolean(words[3]);
		
		this.r = Float.parseFloat(words[5]);
		this.cin = Float.parseFloat(words[7]);
		this.cout = Float.parseFloat(words[9]);
		
		this.tdel = Float.parseFloat(words[11]);
		this.mux_trans_size = Float.parseFloat(words[13]);
		this.buf_size = Float.parseFloat(words[15]);
	}
	
	public double getDelay() {
		return this.tdel;
	}
	
	@Override
	public String toString() {
		String result = "";
				
		result += "switch " + this.index + ":" + "\n";
		result += "        buffered: " + this.buffered + "\n";
		result += "               r: " + this.r + "\n";
		result += "             cin: " + this.cin + "\n";
		result += "            cout: " + this.cout + "\n";
		result += "            tdel: " + this.tdel + "\n";
		result += "  mux_trans_size: " + this.mux_trans_size + "\n";
		result += "        buf_size: " + this.buf_size + "\n";
		
		return result;
	}
}
