package route.circuit.resource;

public class SwitchType {
	public final int index;
	public final boolean buffered;
	public final double r;
	public final double cin;
	public final double cout;
	public final double tdel;
	public final double mux_trans_size;
	public final double buf_size;
	
	public SwitchType(String line) {
		String[] words = line.split(" ");
		
		this.index = Integer.parseInt(words[1]);
		this.buffered = Boolean.parseBoolean(words[3]);
		this.r = Double.parseDouble(words[5]);
		this.cin = Double.parseDouble(words[7]);
		this.cout = Double.parseDouble(words[9]);
		this.tdel = Double.parseDouble(words[11]);
		this.mux_trans_size = Double.parseDouble(words[13]);
		this.buf_size = Double.parseDouble(words[15]);
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
