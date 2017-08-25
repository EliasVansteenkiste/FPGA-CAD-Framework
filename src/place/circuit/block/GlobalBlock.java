package place.circuit.block;

import java.util.ArrayList;
import java.util.List;

import place.circuit.architecture.BlockType;
import place.circuit.architecture.PortType;
import place.circuit.exceptions.FullSiteException;
import place.circuit.exceptions.InvalidBlockException;
import place.circuit.exceptions.PlacedBlockException;
import place.circuit.exceptions.UnplacedBlockException;
import place.circuit.pin.GlobalPin;
import place.circuit.timing.TimingNode;
import place.hierarchy.LeafNode;

public class GlobalBlock extends AbstractBlock {

    private AbstractSite site;
    private ArrayList<TimingNode> timingNodes = new ArrayList<TimingNode>();

    private Macro macro;
    private int macroOffsetY = 0;

    private LeafNode leafNode;

    public GlobalBlock(String name, BlockType type, int index) {
        super(name, type, index);

        this.leafNode = null;
    }

    //Leaf node
    public void setLeafNode(LeafNode hierarchyNode){
    	this.leafNode = hierarchyNode;
    }
    public LeafNode getLeafNode(){
    	return this.leafNode;
    }
    public boolean hasLeafNode(){
    	return !(this.leafNode == null);
    }

    @Override
    public void compact() {
        super.compact();
        this.timingNodes.trimToSize();
    }

    public AbstractSite getSite() {
        return this.site;
    }

    public int getColumn() {
        return this.site.getColumn();
    }
    public int getRow() {
        return this.site.getRow();
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

    public void setMacro(Macro macro, int offsetY) {
        this.macro = macro;
        this.macroOffsetY = offsetY;
    }
    public Macro getMacro() {
        return this.macro;
    }
    public int getMacroOffsetY() {
        return this.macroOffsetY;
    }
    public boolean isInMacro() {
        return this.macro != null;
    }

    public void removeSite() throws UnplacedBlockException, InvalidBlockException {
        if(this.site == null) {
            throw new UnplacedBlockException();
        }

        this.site.removeBlock(this);
        this.site = null;
    }
    public void setSite(AbstractSite site) throws PlacedBlockException, FullSiteException {
        if(this.site != null) {
            throw new PlacedBlockException();
        }

        this.site = site;
        this.site.addBlock(this);
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
