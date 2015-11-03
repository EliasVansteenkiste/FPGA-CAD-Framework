package architecture.circuit.block;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


import util.Logger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlexibleArchitecture {

    private static final double FILL_GRADE = 1;

    private String filename;
    private JSONObject blockDefinitions;

    private int ioCapacity;

    public FlexibleArchitecture(String filename) {
        this.filename = filename;
    }

    @SuppressWarnings("unchecked")
    public void parse() {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(this.filename));
        } catch (FileNotFoundException exception) {
            Logger.raise("Could not find the architecture file: " + this.filename, exception);
        }


        // Read the entire file
        String content = "", line;
        try {
            while((line = reader.readLine()) != null) {
                content += line;
            }
        } catch (IOException exception) {
            Logger.raise("Failed to read from the architecture file: " + this.filename, exception);
        }

        // Parse the JSONObject
        JSONObject jsonContent = (JSONObject) JSONValue.parse(content);
        this.blockDefinitions = (JSONObject) jsonContent.get("blocks");


        // Set the IO capacity
        this.ioCapacity = (int) (long) jsonContent.get("io_capacity");


        // Add all the block types
        this.addBlockTypes();

        // Get all the delays
        this.processDelays((Map<String, Double>) jsonContent.get("delays"));
    }


    private void addBlockTypes() {

        @SuppressWarnings("unchecked")
        Set<String> blockTypes = this.blockDefinitions.keySet();

        for(String typeName : blockTypes) {
            JSONObject definition = this.getDefinition(typeName);

            // Get some general info
            boolean isGlobal = definition.containsKey("globalCategory");
            boolean isLeaf = (boolean) definition.get("leaf");

            String category;
            if(isGlobal) {
                category = (String) definition.get("globalCategory");
            } else if(isLeaf) {
                category = "leaf";
            } else {
                category = "local";
            }

            boolean isClocked = false;
            if(isLeaf) {
                isClocked = (boolean) definition.get("clocked");
            }

            int height = 1, start = 1, repeat = 1;
            if(category.equals("hardblock")) {
                height = (int) (long) definition.get("height");
                start = (int) (long) definition.get("start");
                repeat = (int) (long) definition.get("repeat");
            }



            // Get the port counts
            @SuppressWarnings("unchecked")
            Map<String, JSONObject> ports = (Map<String, JSONObject>) definition.get("ports");

            Map<String, Integer> inputs = castIntegers(ports.get("input"));
            Map<String, Integer> outputs = castIntegers(ports.get("output"));



            // Get the modes and children
            List<String> modes = new ArrayList<String>();
            List<Map<String, Integer>> children = new ArrayList<Map<String, Integer>>();

            // If the block is a leaf: there are no modes, the only mode is unnamed
            if(isLeaf) {
                modes.add("");
                children.add(this.getChildren(definition));


            // There is only one mode, but we have to name it like the block for some reason
            } else if(!definition.containsKey("modes")) {
                modes.add(typeName);
                children.add(this.getChildren(definition));


            // There are multiple modes
            } else {
                JSONObject modeDefinitions = (JSONObject) definition.get("modes");

                @SuppressWarnings("unchecked")
                Set<String> modeNames = modeDefinitions.keySet();

                for(String mode : modeNames) {
                    modes.add(mode);
                    children.add(this.getChildren((JSONObject) modeDefinitions.get(mode)));
                }
            }

            BlockType.addType(typeName, category, height, start, repeat, isClocked, inputs, outputs);

            for(int i = 0; i < modes.size(); i++) {
                BlockType.addMode(typeName, modes.get(i), children.get(i));
            }
        }

        BlockType.postProcess();
    }

    private JSONObject getDefinition(String blockType) {
        return (JSONObject) this.blockDefinitions.get(blockType);
    }

    private Map<String, Integer> getChildren(JSONObject subDefinition) {
        return castIntegers((JSONObject) subDefinition.get("children"));
    }

    private Map<String, Integer> castIntegers(JSONObject subDefinition) {
        @SuppressWarnings("unchecked")
        Set<String> keys = (Set<String>) subDefinition.keySet();

        Map<String, Integer> newSubDefinition = new HashMap<String, Integer>();

        for(String key : keys) {
            int value = (int) (long) subDefinition.get(key);
            newSubDefinition.put(key, value);
        }

        return newSubDefinition;
    }

    private void processDelays(Map<String, Double> delays) {
        Pattern keyPattern = Pattern.compile("(?<sourceBlock>[^.-]+)(\\.(?<sourcePort>[^-]+))?-(?<sinkBlock>[^.-]+)(\\.(?<sinkPort>.+))?");

        for(Map.Entry<String, Double> delayEntry : delays.entrySet()) {
            String key = delayEntry.getKey();
            Double delay = delayEntry.getValue();

            if(key.equals("input_setup_time")) {
                PortType.setInputSetupTime(delay);
                continue;
            }


            Matcher matcher = keyPattern.matcher(key);
            matcher.matches();




            String sourceBlockName = matcher.group("sourceBlock");
            String sourcePortName = matcher.group("sourcePort");
            String sinkBlockName = matcher.group("sinkBlock");
            String sinkPortName = matcher.group("sinkPort");

            if(sourcePortName == null) {
                PortType portType = new PortType(sinkBlockName, sinkPortName);
                portType.setSetupTime(delay);

            } else if(sinkPortName == null) {
                PortType portType = new PortType(sourceBlockName, sourcePortName);
                portType.setSetupTime(delay);

            } else {
                PortType sourcePortType = new PortType(sourceBlockName, sourcePortName);
                PortType sinkPortType = new PortType(sinkBlockName, sinkPortName);
                sourcePortType.setDelay(sinkPortType, delay);
            }
        }
    }



    public int getIoCapacity() {
        return this.ioCapacity;
    }

    public double getFillGrade() {
        return FlexibleArchitecture.FILL_GRADE;
    }
}
