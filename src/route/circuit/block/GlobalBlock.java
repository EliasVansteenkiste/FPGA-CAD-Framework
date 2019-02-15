package route.circuit.block;

import java.util.ArrayList;
import java.util.List;

import route.circuit.architecture.BlockType;
import route.circuit.architecture.PortType;
import route.circuit.exceptions.PlacedBlockException;
import route.circuit.pin.GlobalPin;
import route.circuit.resource.Instance;
import route.circuit.timing.TimingNode;

public class GlobalBlock extends AbstractBlock {

    private Instance siteInstance;
    private ArrayList<TimingNode> timingNodes = new ArrayList<TimingNode>();

    public GlobalBlock(String name, BlockType type, int index) {
        super(name, type, index);
    }

    @Override
    public void compact() {
        super.compact();
        this.timingNodes.trimToSize();
    }

    public Instance getSiteInstance() {
        return this.siteInstance;
    }

    public int getColumn() {
        return this.siteInstance.getParentSite().getColumn();
    }
    public int getRow() {
        return this.siteInstance.getParentSite().getRow();
    }

    public boolean hasCarry() {
        return this.blockType.getCarryFromPort() != null;
    }
    public GlobalPin getCarryIn() {
        return (GlobalPin) this.getPin(this.blockType.getCarryToPort(), 0);
    }
    public GlobalPin getCarryOut() {
        return (GlobalPin) this.getPin(this.blockType.getCarryFromPort(), 0);
    }

    public void setSiteInstance(Instance siteInstance) throws PlacedBlockException {
        if(this.siteInstance != null) {
            throw new PlacedBlockException();
        }

        this.siteInstance = siteInstance;
        this.siteInstance.setBlock(this);
    }


    public void addTimingNode(TimingNode node) {
        this.timingNodes.add(node);
    }
    public List<TimingNode> getTimingNodes() {
        return this.timingNodes;
    }

    @Override
    public AbstractBlock getParent() {
        return null;
    }

    @Override
    protected GlobalPin createPin(PortType portType, int index) {
        return new GlobalPin(this, portType, index);
    }
}
