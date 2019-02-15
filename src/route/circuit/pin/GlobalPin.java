package route.circuit.pin;

import route.circuit.architecture.PortType;
import route.circuit.block.GlobalBlock;
import route.util.PinCounter;

public class GlobalPin extends AbstractPin {
	private int id;
	private String netName;

    public GlobalPin(GlobalBlock owner, PortType portType, int index) {
        super(owner, portType, index);
        
        this.id = PinCounter.getInstance().addPin();
    }
    
    public void setNetName(String netName){
    	this.netName = netName;
    }
    public String getNetName(){
    	return this.netName;
    }

    @Override
    public GlobalBlock getOwner() {
        return (GlobalBlock) this.owner;
    }
    
    @Override
    public int hashCode() {
    	return this.id;
    }
}
