package placers;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Placer {
	
	public void place() {
		this.place(new HashMap<String, Object>());
	}
	
	public abstract void place(HashMap<String, Object> options);
	
}
