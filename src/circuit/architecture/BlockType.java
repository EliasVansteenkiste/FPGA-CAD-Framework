package circuit.architecture;

import java.util.List;
import java.util.Map;


public class BlockType {
    /**
     * For a big part, this is a wrapper class around the BlockTypeData singleton.
     * This class only stores the type index and mode index. (Memory efficient)
     */

    public static List<BlockType> getGlobalBlockTypes() {
        return BlockTypeData.getInstance().getGlobalBlockTypes();
    }

    public static List<BlockType> getBlockTypes(BlockCategory category) {
        return BlockTypeData.getInstance().getBlockTypes(category);
    }


    private Integer typeIndex, modeIndex;

    public BlockType(String typeName) {
        this.typeIndex = BlockTypeData.getInstance().getTypeIndex(typeName);
        this.modeIndex = null;
    }
    BlockType(int typeIndex) {
        this.typeIndex = typeIndex;
        this.modeIndex = null;
    }
    public BlockType(String typeName, String modeName) {
        this.typeIndex = BlockTypeData.getInstance().getTypeIndex(typeName);
        this.modeIndex = BlockTypeData.getInstance().getModeIndex(this.typeIndex, modeName);
    }



    int getIndex() {
        return this.typeIndex;
    }

    public String getName() {
        return BlockTypeData.getInstance().getName(this.typeIndex);
    }
    public BlockCategory getCategory() {
        return BlockTypeData.getInstance().getCategory(this.typeIndex);
    }
    public boolean isGlobal() {
        return BlockTypeData.getInstance().isGlobal(this.typeIndex);
    }
    public boolean isLeaf() {
        return BlockTypeData.getInstance().isLeaf(this.typeIndex);
    }

    public int getHeight() {
        return BlockTypeData.getInstance().getHeight(this.typeIndex);
    }
    public int getStart() {
        return BlockTypeData.getInstance().getStart(this.typeIndex);
    }
    public int getRepeat() {
        return BlockTypeData.getInstance().getRepeat(this.typeIndex);
    }

    public String getModeName() {
        return BlockTypeData.getInstance().getModeName(this.typeIndex, this.modeIndex);
    }
    public Map<String, Integer> getChildren() {
        return BlockTypeData.getInstance().getChildren(this.typeIndex, this.modeIndex);
    }

    public boolean isClocked() {
        return BlockTypeData.getInstance().isClocked(this.typeIndex);
    }

    public int getNumChildren() {
        return BlockTypeData.getInstance().getNumChildren(this.typeIndex, this.modeIndex);
    }
    public int[] getChildRange(BlockType blockType) {
        return BlockTypeData.getInstance().getChildRange(this.typeIndex, this.modeIndex, blockType.typeIndex);
    }



    public int getNumPins() {
        return PortTypeData.getInstance().getNumPins(this.typeIndex);
    }
    public int[] getInputPortRange() {
        return PortTypeData.getInstance().getInputPortRange(this.typeIndex);
    }
    public int[] getOutputPortRange() {
        return PortTypeData.getInstance().getOutputPortRange(this.typeIndex);
    }

    public List<PortType> getPortTypes() {
        return PortTypeData.getInstance().getPortTypes(this.typeIndex);
    }


    // To compare block types, we only compare the type, and not the mode.

    @Override
    public boolean equals(Object otherObject) {
        if(otherObject instanceof BlockType) {
            return this.equals((BlockType) otherObject);
        } else {
            return false;
        }
    }
    public boolean equals(BlockType otherBlockType) {
        return this.typeIndex == otherBlockType.typeIndex;
    }


    @Override
    public int hashCode() {
        return this.typeIndex;
    }

    @Override
    public String toString() {
        if(this.modeIndex == null) {
            return this.getName();
        } else {
            return this.getName() + "<" + this.getModeName() + ">";
        }
    }
}
