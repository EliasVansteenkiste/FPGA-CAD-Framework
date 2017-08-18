package place.circuit.io;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.block.GlobalBlock;

public class HierarchyParser {

	private String currentLeafNode;
	private Color currentColor;

    private Map<String, String> hierarchyMap;
    private Map<String, Color> colorMap;

    private Circuit circuit;
    private File file;

    public HierarchyParser(Circuit circuit, File file) {
        this.circuit = circuit;
        this.file = file;

        this.hierarchyMap = new HashMap<String, String>();
        this.colorMap = new HashMap<String, Color>();
    }

    public void parse() throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(this.file));

        String line;
        while((line = reader.readLine()) != null) {
            if(line.contains("Leaf Node")){
            	this.currentLeafNode = line.replace("Leaf Node: ", "").split(" ")[0];
            	
            	String[] words = line.split("Color")[1].replace(": (","").replace(")]","").split(",");
            	this.currentColor = new Color(Integer.parseInt(words[0]),Integer.parseInt(words[1]),Integer.parseInt(words[2]));
            }else{
            	String[] words = line.split(" ");
            	for(String word:words){
            		if(word.contains("name")){
            			String name = word.replace("name=\"", "").replace("\"", "");
            			this.hierarchyMap.put(name, this.currentLeafNode);
            			this.colorMap.put(name, this.currentColor);
            		}
            	}
            }
        }

        reader.close();

        for(GlobalBlock block:this.circuit.getGlobalBlocks()){
        	block.setHierarchyNode(this.hierarchyMap.get(block.getName()));
        	block.setColor(this.colorMap.get(block.getName()));
        }
    }
}
