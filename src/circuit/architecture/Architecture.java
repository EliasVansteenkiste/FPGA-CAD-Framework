package circuit.architecture;

import options.Options;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


import util.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Architecture implements Serializable {

    private static final long serialVersionUID = -5436935126902935000L;

    private static final double FILL_GRADE = 1;

    private File file;
    private transient JSONObject blockDefinitions;

    private int ioCapacity;


    public Architecture() {
        this.file = Options.getInstance().getArchitectureFile();
    }

    @SuppressWarnings("unchecked")
    public void parse() {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(this.file));
        } catch (FileNotFoundException exception) {
            Logger.raise("Could not find the architecture file: " + this.file, exception);
        }


        // Read the entire file
        String content = "", line;
        try {
            while((line = reader.readLine()) != null) {
                content += line;
            }
        } catch (IOException exception) {
            Logger.raise("Failed to read from the architecture file: " + this.file, exception);
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

            BlockTypeData.getInstance().addType(typeName, category, height, start, repeat, isClocked, inputs, outputs);

            for(int i = 0; i < modes.size(); i++) {
                BlockTypeData.getInstance().addMode(typeName, modes.get(i), children.get(i));
            }
        }

        BlockTypeData.getInstance().postProcess();
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

            if(key.equals("clock_setup_time")) {
                PortTypeData.getInstance().setClockSetupTime(delay);
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


    public void buildDelayMatrixes() {
        // For this method to work, the macro PRINT_ARRAYS should be defined
        // in vpr: place/timing_place_lookup.c

        Options options = Options.getInstance();

        String circuitName = options.getCircuitName();
        File blifFile = options.getBlifFile();
        File netFile = options.getNetFile();
        File architectureFileVPR = options.getArchitectureFileVPR();

        // Run vpr
        String command = String.format(
                "./vpr %s %s --blif_file %s --net_file %s --place_file vpr_tmp --place --init_t 1 --exit_t 1",
                architectureFileVPR, circuitName, blifFile, netFile);

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
        } catch(IOException error) {
            Logger.raise("Failed to execute vpr: " + command, error);
        }

        // Read output to avoid buffer overflow and deadlock
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        try {
            while ((reader.readLine()) != null) {}
        } catch(IOException error) {
            Logger.raise("Failed to read from vpr output", error);
        }

        // Finish execution
        try {
            process.waitFor();
        } catch(InterruptedException error) {
            Logger.raise("vpr was interrupted", error);
        }

        // Parse the delay tables
        File delaysFile = new File("lookup_dump.echo");
        DelayTables.getInstance().parse(delaysFile);

        // Clean up
        this.deleteFile("vpr_tmp");
        this.deleteFile("vpr_stdout.log");
        this.deleteFile("lookup_dump.echo");
    }

    private void deleteFile(String path) {
        try {
            Files.deleteIfExists(new File(path).toPath());

        } catch(IOException error) {
            Logger.raise("File not found: " + path, error);
        }
    }



    public int getIoCapacity() {
        return this.ioCapacity;
    }

    public double getFillGrade() {
        return Architecture.FILL_GRADE;
    }


    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(BlockTypeData.getInstance());
        out.writeObject(PortTypeData.getInstance());
        out.writeObject(DelayTables.getInstance());
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        BlockTypeData.setInstance((BlockTypeData) in.readObject());
        PortTypeData.setInstance((PortTypeData) in.readObject());
        DelayTables.setInstance((DelayTables) in.readObject());
    }
}
