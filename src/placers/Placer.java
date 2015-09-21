package placers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import flexible_architecture.Circuit;

public abstract class Placer {
	
	protected static Map<String, String> defaultOptions = new HashMap<String, String>();
	protected static List<String> requiredOptions = new ArrayList<String>();
	
	protected Circuit circuit;
	protected Map<String, String> options;
	
	
	protected Placer(Circuit circuit, Map<String, String> options) {
		this.circuit = circuit;
		this.options = options;
		
		this.parseOptions();
	}
	
	protected void parseOptions() {
		for(String option : defaultOptions.keySet()) {
			if(!this.options.containsKey(option)) {
				this.options.put(option,  defaultOptions.get(option));
			}
		}
	}
	
	
	public abstract void place();
	
	
	protected boolean hasOption(String optionName) {
		return this.options.containsKey(optionName);
	}
	protected String getOption(String optionName) {
		return this.options.get(optionName);
	}
}
