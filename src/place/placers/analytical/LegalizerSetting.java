package place.placers.analytical;

public class LegalizerSetting {
    private final double startValue;
    private final double finalValue;
    
    private double value;
    private final boolean isInt;
    
    private final double multiplier;
    private final boolean doMultiply;
    
    public LegalizerSetting(double startValue, double finalValue, int numIterations){
    	this.startValue = startValue;
    	this.finalValue = finalValue;
    	
    	this.value = this.startValue;
    	this.isInt = false;
    	
    	this.multiplier = Math.pow(this.finalValue / this.startValue, 1.0 / (numIterations - 1.0));
    	this.doMultiply = true;
    }
    public LegalizerSetting(double value){
    	this.startValue = -1;
    	this.finalValue = -1;
    	this.multiplier = -1;
    	this.doMultiply = false;
    	
    	this.value = value;
    	this.isInt = false;
    }
    public LegalizerSetting(int value){
    	this.startValue = -1;
    	this.finalValue = -1;
    	this.multiplier = -1;
    	this.doMultiply = false;
    	
    	this.value = value;
    	this.isInt = true;
    }

    public void multiply(){
    	if(this.doMultiply){
        	this.value *= this.multiplier;
    	}
    }
    public double getValue(){
    	return this.value;
    }
    public boolean isInt(){
    	return this.isInt;
    }
}
