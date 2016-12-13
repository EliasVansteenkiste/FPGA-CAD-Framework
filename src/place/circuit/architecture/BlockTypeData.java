package place.circuit.architecture;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import place.util.Triple;

class BlockTypeData implements Serializable {
    /**
     * This is a singleton class. It should be serialized explicitly!
     */

    private static final long serialVersionUID = 4923006752028503183L;

    // Stuff that provides singleton functionality
    private static BlockTypeData instance = new BlockTypeData();
    static BlockTypeData getInstance() {
        return BlockTypeData.instance;
    }
    static void setInstance(BlockTypeData instance) {
        BlockTypeData.instance = instance;
    }



    private Map<Triple<Integer, Integer, String>, Integer> types = new HashMap<>();
    private List<String> typeNames = new ArrayList<>();

    private List<BlockCategory> categories = new ArrayList<>();
    private List<List<BlockType>> blockTypesPerCategory = new ArrayList<>();
    private List<BlockType> blockTypesWithModes = new ArrayList<>();

    private List<Integer> heights = new ArrayList<>();
    private List<Integer> columnStarts = new ArrayList<>();
    private List<Integer> columnRepeats = new ArrayList<>();
    private List<Integer> priorities = new ArrayList<>();

    private List<Boolean> clocked = new ArrayList<>();


    private List<Map<String, Integer>> modes = new ArrayList<>();
    private List<List<String>> modeNames = new ArrayList<>();
    private List<List<Map<BlockType, Integer>>> children = new ArrayList<>();

    private List<List<List<Integer>>> childStarts = new ArrayList<>();
    private List<List<List<Integer>>> childEnds = new ArrayList<>();
    private List<List<Integer>> numChildren = new ArrayList<>();



    BlockTypeData() {
        for(int i = 0; i < BlockCategory.values().length; i++) {
            this.blockTypesPerCategory.add(new ArrayList<BlockType>());
        }
    }


    BlockType addType(
            BlockType parentBlockType,
            String typeName,
            BlockCategory category,
            int height,
            int start,
            int repeat,
            int priority,
            boolean clocked,
            Map<String, Integer> inputs,
            Map<String, Integer> outputs,
            Map<String, Integer> clocks) {

        // Return false if the block type already exists
        assert(this.types.get(typeName) == null);

        int typeIndex = this.typeNames.size();
        this.typeNames.add(typeName);

        int parentTypeIndex = parentBlockType == null ? -1 : parentBlockType.getTypeIndex();
        int parentModeIndex = parentBlockType == null ? -1 : parentBlockType.getModeIndex();
        Triple<Integer, Integer, String> key = new Triple<>(
                parentTypeIndex,
                parentModeIndex,
                typeName);
        this.types.put(key, typeIndex);

        BlockType newBlockType = new BlockType(parentBlockType, typeName);

        this.categories.add(category);
        this.blockTypesPerCategory.get(category.ordinal()).add(newBlockType);

        this.heights.add(height);
        this.columnStarts.add(start);
        this.columnRepeats.add(repeat);
        this.priorities.add(priority);

        this.clocked.add(clocked);

        this.modeNames.add(new ArrayList<String>());
        this.modes.add(new HashMap<String, Integer>());
        this.children.add(new ArrayList<Map<BlockType, Integer>>());

        PortTypeData.getInstance().addPorts(typeIndex, inputs, outputs, clocks);

        return newBlockType;
    }


    BlockType addMode(BlockType blockType, String modeName) {
        int typeIndex = blockType.getTypeIndex();

        // Make sure this mode doesn't exist yet
        assert(this.modes.get(typeIndex).get(modeName) == null);

        int modeIndex = this.modeNames.get(typeIndex).size();
        this.modeNames.get(typeIndex).add(modeName);
        this.modes.get(typeIndex).put(modeName, modeIndex);

        this.children.get(typeIndex).add(new HashMap<BlockType, Integer>());

        BlockType newBlockType = new BlockType(typeIndex, modeName);
        this.blockTypesWithModes.add(newBlockType);

        return newBlockType;
    }

    void addChild(BlockType blockType, BlockType childBlockType, int numChildren) {
        int typeIndex = blockType.getTypeIndex();
        int modeIndex = blockType.getModeIndex();
        this.children.get(typeIndex).get(modeIndex).put(childBlockType, numChildren);
    }



    /**
     * This method should be called after all block types and modes have been added.
     * It caches some data.
     */
    void postProcess() {
        this.cacheChildren();
        PortTypeData.getInstance().postProcess();
    }

    private void cacheChildren() {
        int numTypes = this.types.size();
        for(int typeIndex = 0; typeIndex < numTypes; typeIndex++) {
            List<Map<BlockType, Integer>> typeChildren = this.children.get(typeIndex);
            List<List<Integer>> typeChildStarts = new ArrayList<List<Integer>>();
            List<List<Integer>> typeChildEnds = new ArrayList<List<Integer>>();
            List<Integer> typeNumChildren = new ArrayList<Integer>();

            int numModes = this.modeNames.get(typeIndex).size();
            for(int modeIndex = 0; modeIndex < numModes; modeIndex++) {
                typeChildStarts.add(new ArrayList<Integer>(Collections.nCopies(numTypes, (Integer) null)));
                typeChildEnds.add(new ArrayList<Integer>(Collections.nCopies(numTypes, (Integer) null)));
                int numChildren = 0;

                for(Map.Entry<BlockType, Integer> childEntry : typeChildren.get(modeIndex).entrySet()) {
                    String childName = childEntry.getKey().getName();
                    int childTypeIndex = this.types.get(new Triple<>(typeIndex, modeIndex, childName));
                    int childCount = childEntry.getValue();

                    typeChildStarts.get(modeIndex).set(childTypeIndex, numChildren);
                    numChildren += childCount;
                    typeChildEnds.get(modeIndex).set(childTypeIndex, numChildren);
                }

                typeNumChildren.add(numChildren);
            }

            this.childStarts.add(typeChildStarts);
            this.childEnds.add(typeChildEnds);
            this.numChildren.add(typeNumChildren);
        }
    }



    int getTypeIndex(BlockType parentBlockType, String typeName) {

        int parentTypeIndex = parentBlockType == null ? -1 : parentBlockType.getTypeIndex();
        int parentModeIndex = parentBlockType == null ? -1 : parentBlockType.getModeIndex();

        Triple<Integer, Integer, String> key = new Triple<>(
                parentTypeIndex,
                parentModeIndex,
                typeName);

        return this.types.get(key);
    }

    int getModeIndex(int typeIndex, String argumentModeName) {
        String modeName = (argumentModeName == null) ? "" : argumentModeName;
        return this.modes.get(typeIndex).get(modeName);
    }



    List<BlockType> getBlockTypes(BlockCategory category) {
        return this.blockTypesPerCategory.get(category.ordinal());
    }

    List<BlockType> getBlockTypesWithModes() {
        return this.blockTypesWithModes;
    }



    String getName(int typeIndex) {
        return this.typeNames.get(typeIndex);
    }
    BlockCategory getCategory(int typeIndex) {
        return this.categories.get(typeIndex);
    }
    boolean isGlobal(int typeIndex) {
        BlockCategory category = this.getCategory(typeIndex);
        return category != BlockCategory.INTERMEDIATE && category != BlockCategory.LEAF;
    }
    boolean isLeaf(int typeIndex) {
        BlockCategory category = this.getCategory(typeIndex);
        return category == BlockCategory.LEAF;
    }

    int getHeight(int typeIndex) {
        return this.heights.get(typeIndex);
    }
    int getStart(int typeIndex) {
        return this.columnStarts.get(typeIndex);
    }
    int getRepeat(int typeIndex) {
        return this.columnRepeats.get(typeIndex);
    }
    int getPriority(int typeIndex) {
        return this.priorities.get(typeIndex);
    }

    String getModeName(int typeIndex, int modeIndex) {
        return this.modeNames.get(typeIndex).get(modeIndex);
    }

    boolean isClocked(int typeIndex) {
        return this.clocked.get(typeIndex);
    }

    int getNumChildren(int typeIndex, int modeIndex) {
        return this.numChildren.get(typeIndex).get(modeIndex);
    }
    int[] getChildRange(int typeIndex, int modeIndex, int childTypeIndex) {
        int childStart = this.childStarts.get(typeIndex).get(modeIndex).get(childTypeIndex);
        int childEnd = this.childEnds.get(typeIndex).get(modeIndex).get(childTypeIndex);

        int[] childRange = {childStart, childEnd};
        return childRange;
    }
}
