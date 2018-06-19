package place.circuit.pin;

import place.circuit.architecture.PortType;
import place.circuit.block.GlobalBlock;

public class GlobalPin extends AbstractPin {
	private String netName;

    public GlobalPin(GlobalBlock owner, PortType portType, int index) {
        super(owner, portType, index);
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
    public GlobalPin getSink(int index) {
        return (GlobalPin) super.getSink(index);
    }
}
