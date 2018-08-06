package route.util;

public class PinCounter {

    private static PinCounter instance = new PinCounter();
    
    public int counter = 0;
     
    // This private constructor is to prevent this object get instantiated more than once.
    private PinCounter(){}
     
    public static PinCounter getInstance() {
        return instance;
    }
    public int addPin() {
    	int temp = this.counter;
    	this.counter += 1;
    	return temp;
    }
}