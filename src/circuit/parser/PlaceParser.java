package circuit.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import circuit.Circuit;
import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;
import circuit.exceptions.FullSiteException;
import circuit.exceptions.PlacedBlockException;

public class PlaceParser {

    private static Pattern sizePattern = Pattern.compile("Array size: (?<width>\\d+) x (?<height>\\d+).*");
    private static Pattern sitePattern = Pattern.compile("(?<block>\\S+)\\s+(?<x>\\d+)\\s+(?<y>\\d+)\\s+(?<z>\\d+).*");

    private Map<String, int[]> coordinates;
    private Circuit circuit;
    private File file;

    public PlaceParser(Circuit circuit, File file) {
        this.circuit = circuit;
        this.file = file;
    }


    public void parse() throws BlockNotFoundException, IOException, PlacedBlockException, FullSiteException {

        BufferedReader reader = new BufferedReader(new FileReader(this.file));


        // Read all the coordinates and store them
        this.coordinates = new HashMap<String, int[]>();

        String line;
        while((line = reader.readLine()) != null) {
            this.processLine(line);
        }


        // Loop over all the blocks in the circuit
        for(GlobalBlock block : this.circuit.getGlobalBlocks()) {

            // Get the coordinate of the block
            String blockName = block.getName();
            if(!this.coordinates.containsKey(blockName)) {
                reader.close();
                throw new BlockNotFoundException(blockName);
            }

            int[] coordinate = this.coordinates.get(blockName);
            int x = coordinate[0], y = coordinate[1];

            // Bind the site and block to each other
            AbstractSite site = this.circuit.getSite(x, y);
            ((GlobalBlock) block).setSite(site);
        }

        reader.close();
    }


    private void processLine(String line) {
        Matcher sizeMatcher = sizePattern.matcher(line);
        boolean sizeMatches = sizeMatcher.matches();

        Matcher siteMatcher = sitePattern.matcher(line);
        boolean siteMatches = siteMatcher.matches();


        if(sizeMatches) {
            int width = Integer.parseInt(sizeMatcher.group("width"));
            int height = Integer.parseInt(sizeMatcher.group("height"));

            this.circuit.setSize(width + 2, height + 2);

        } else if(siteMatches) {
            String blockName = siteMatcher.group("block");
            int x = Integer.parseInt(siteMatcher.group("x"));
            int y = Integer.parseInt(siteMatcher.group("y"));
            int z = Integer.parseInt(siteMatcher.group("z"));

            int[] coordinate = {x, y, z};
            this.coordinates.put(blockName, coordinate);
        }
    }
}
