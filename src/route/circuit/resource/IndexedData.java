package route.circuit.resource;

public class IndexedData {
	public final int index;
	public final float base_cost;//TODO BaseCost (Based on length of wire?)
	public final int length;
	public final float inv_length;
	
	public final float t_linear;
	public final float t_quadratic;
	public final float c_load;
	
	public final int orthoCostIndex;
	private IndexedData ortho_data;
	
	public IndexedData(String line) {
		while(line.contains("  ")) line = line.replace("  ", " ");
		
		String[] words = line.split(" ");
		
		this.index = Integer.parseInt(words[1]);
		this.base_cost = Float.parseFloat(words[5]);
		
		this.inv_length = Float.parseFloat(words[7]);
		this.length = (int)Math.round(1.0 / this.inv_length);
		
		this.t_linear = Float.parseFloat(words[9]);
		this.t_quadratic = Float.parseFloat(words[11]);
		this.c_load = Float.parseFloat(words[13]);
		
		this.orthoCostIndex = Integer.parseInt(words[3]);
		this.ortho_data = null;
	}
	
	public void setOrthoData(IndexedData orthoData) {
		this.ortho_data = orthoData;
	}
	public IndexedData getOrthoData() {
		return this.ortho_data;
	}
	
	@Override
	public String toString() {
		String result = "";
				
		result += "indexed data " + this.index + ":" + "\n";
		result += "    base_cost: " + this.base_cost + "\n";
		result += "       length: " + this.length + "\n";
		result += "   inv_length: " + this.inv_length + "\n";
		result += "     t_linear: " + this.t_linear + "\n";
		result += "  t_quadratic: " + this.t_quadratic + "\n";
		result += "       c_load: " + this.c_load + "\n";
		result += "  ortho index: " + this.orthoCostIndex + "\n";
		
		return result;
	}
}
