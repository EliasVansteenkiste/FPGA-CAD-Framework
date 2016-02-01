package circuit.architecture;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class BlockType implements Serializable, Comparable<BlockType> {

    /**
     * For a big part, this is a wrapper class around the BlockTypeData singleton.
     * This class only stores the type index and mode index. (Memory efficient)
     */

    private static final long serialVersionUID = 7705884820007183572L;

    public static List<BlockType> getBlockTypes() {
        List<BlockType> types = new ArrayList<BlockType>();
        for(BlockCategory category : BlockCategory.values()) {
            types.addAll(BlockType.getBlockTypes(category));
        }
        return types;
    }
    public static List<BlockType> getBlockTypesWithModes() {
        return BlockTypeData.getInstance().getBlockTypesWithModes();
    }
    public static List<BlockType> getGlobalBlockTypes() {
        List<BlockType> types = new ArrayList<BlockType>();
        types.addAll(BlockType.getBlockTypes(BlockCategory.IO));
        types.addAll(BlockType.getBlockTypes(BlockCategory.CLB));
        types.addAll(BlockType.getBlockTypes(BlockCategory.HARDBLOCK));

        return types;
    }
    public static List<BlockType> getLeafBlockTypes() {
        return BlockType.getBlockTypes(BlockCategory.LEAF);
    }

    public static List<BlockType> getBlockTypes(BlockCategory category) {
        return BlockTypeData.getInstance().getBlockTypes(category);
    }


    private Integer typeIndex, modeIndex;

    public BlockType(BlockType parentBlockType, String typeName) {
        this.typeIndex = BlockTypeData.getInstance().getTypeIndex(parentBlockType, typeName);
        this.modeIndex = null;
    }
    public BlockType(BlockType parentBlockType, String typeName, String modeName) {
        this.typeIndex = BlockTypeData.getInstance().getTypeIndex(parentBlockType, typeName);
        this.modeIndex = BlockTypeData.getInstance().getModeIndex(this.typeIndex, modeName);
    }

    BlockType(int typeIndex) {
        this.typeIndex = typeIndex;
        this.modeIndex = null;
    }
    BlockType(int typeIndex, String modeName) {
        this.typeIndex = typeIndex;
        this.modeIndex = BlockTypeData.getInstance().getModeIndex(this.typeIndex, modeName);
    }



    int getTypeIndex() {
        return this.typeIndex;
    }
    int getModeIndex() {
        return this.modeIndex;
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
    public int getPriority() {
        return BlockTypeData.getInstance().getPriority(this.typeIndex);
    }

    public String getModeName() {
        return BlockTypeData.getInstance().getModeName(this.typeIndex, this.modeIndex);
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
    public int[] getClockPortRange() {
        return PortTypeData.getInstance().getClockPortRange(this.typeIndex);
    }

    public List<PortType> getPortTypes() {
        return PortTypeData.getInstance().getPortTypes(this.typeIndex);
    }


    public PortType getCarryFromPort() {
        return PortTypeData.getInstance().getCarryFromPort(this.typeIndex);
    }
    public PortType getCarryToPort() {
        return PortTypeData.getInstance().getCarryToPort(this.typeIndex);
    }
    public int getCarryOffsetY() {
        return PortTypeData.getInstance().getCarryOffsetY(this.typeIndex);
    }


    // To compare block types, we only compare the type, and not the mode.
    @Override
    public int compareTo(BlockType otherBlockType) {
        return Integer.compare(this.typeIndex, otherBlockType.typeIndex);
    }

    @Override
    public boolean equals(Object otherObject) {
        if(otherObject instanceof BlockType) {
            return this.equals((BlockType) otherObject);

        } else {
            return false;
        }
    }
    public boolean equals(BlockType otherBlockType) {
        return this.typeIndex.equals(otherBlockType.typeIndex);
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
