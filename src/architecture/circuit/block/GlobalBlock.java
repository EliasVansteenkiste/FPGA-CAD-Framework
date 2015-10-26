package architecture.circuit.block;

import architecture.BlockType;
import architecture.PortType;
import architecture.circuit.pin.GlobalPin;
import util.Logger;

public class GlobalBlock extends AbstractBlock {
    
    private AbstractSite site;
    
    public GlobalBlock(String name, BlockType type, int index) {
        super(name, type, index);
    }
    
    public AbstractSite getSite() {
        return this.site;
    }
    
    public int getX() {
        return this.site.getX();
    }
    public int getY() {
        return this.site.getY();
    }
    
    
    public void removeSite() {
        if(this.site != null) {
            this.site.removeBlock(this);
            this.site = null;
        } else {
            Logger.raise("Trying to remove the site of an unplaced block");
        }
    }
    public void setSite(AbstractSite site) {
        if(this.site == null) {
            this.site = site;
            this.site.addBlock(this);
        } else {
            Logger.raise("Trying to set the site of a placed block");
        }
    }
    
    @Override
    public AbstractBlock getParent() {
        return null;
    }
    
    @Override
    public GlobalPin createPin(PortType portType, int index) {
        return new GlobalPin(this, portType, index);
    }
}
