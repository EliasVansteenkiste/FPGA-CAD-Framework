package place.placers.analytical;

public class LegalizerSetting {
    private double startValue;
    private double finalValue;
    private double multiplier;
    
    private double value;
    
    public LegalizerSetting(double startValue, double finalValue, int numIterations){
    	this.startValue = startValue;
    	this.finalValue = finalValue;
    	
    	this.multiplier = Math.pow(this.finalValue / this.startValue, 1.0 / (numIterations - 1.0));
    	
    	this.value = this.startValue;
    }

    public void multiply(){
    	this.value *= this.multiplier;
    }
    public double getValue(){
    	return this.value;
    }
}
