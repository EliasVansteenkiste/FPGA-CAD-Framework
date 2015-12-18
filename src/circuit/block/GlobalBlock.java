package circuit.block;

import java.util.ArrayList;
import java.util.List;

import circuit.architecture.BlockType;
import circuit.architecture.PortType;
import circuit.exceptions.FullSiteException;
import circuit.exceptions.InvalidBlockException;
import circuit.exceptions.PlacedBlockException;
import circuit.exceptions.UnplacedBlockException;
import circuit.pin.GlobalPin;

public class GlobalBlock extends AbstractBlock {

    private AbstractSite site;
    private ArrayList<LeafBlock> leafs = new ArrayList<LeafBlock>();

    private Macro macro;
    private int macroOffsetY = 0;

    public GlobalBlock(String name, BlockType type, int index) {
        super(name, type, index);
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


    public void addLeaf(LeafBlock block) {
        this.leafs.add(block);
    }
    public List<LeafBlock> getLeafBlocks() {
        return this.leafs;
    }

    @Override
    public AbstractBlock getParent() {
        return null;
    }

    @Override
    protected GlobalPin createPin(PortType portType, int index) {
        return new GlobalPin(this, portType, index);
    }

    @Override
    public void compact() {
        super.compact();
        this.leafs.trimToSize();
    }
}
