package place.circuit.io;

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
    private Map<String, String> hierarchyMap;

    private Circuit circuit;
    private File file;

    public HierarchyParser(Circuit circuit, File file) {
        this.circuit = circuit;
        this.file = file;
        
        this.hierarchyMap = new HashMap<String, String>();
    }

    public void parse() throws IOException {
    	
        BufferedReader reader = new BufferedReader(new FileReader(this.file));

        String line;
        while((line = reader.readLine()) != null) {
            if(line.contains("Leaf Node")){
            	this.currentLeafNode = line.replace("Leaf Node: ", "").split(" ")[0];
            }else{
            	String[] words = line.split(" ");
            	for(String word:words){
            		if(word.contains("name")){
            			word = word.replace("name=\"", "");
            			word = word.replace("\"", "");
            			this.hierarchyMap.put(word, this.currentLeafNode);
            		}
            	}
            }
        }
        for(GlobalBlock block:this.circuit.getGlobalBlocks()){
        	block.setHierarchyNode(this.hierarchyMap.get(block.getName()));
        }
        reader.close();
    }
}
