package placers;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Placer {
	
	/**
	 * Place the circuit with all default options.
	 * Equivalent to calling place() with an empty HashMap as parameter.
	 */
	public void place() {
		this.place(new HashMap<String, Object>());
	}
	
	/**
	 * Place the circuit that was given in the constructor.
	 * @param options	A hasmap containing the options for the placer. The accepted options are different for each placer.
	 */
	public abstract void place(HashMap<String, Object> options);
	
}
