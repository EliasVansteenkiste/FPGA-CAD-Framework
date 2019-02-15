package place.circuit.io;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import place.circuit.Circuit;
import place.circuit.block.GlobalBlock;
import place.hierarchy.HierarchyNode;
import place.hierarchy.LeafNode;

public class HierarchyParser {
    private Circuit circuit;
    private File file;
    
	private List<LeafNode> leafNodes;
	private LeafNode currentNode;
	
	private Map<String, GlobalBlock> nameToGlobalblockMap;

    public HierarchyParser(Circuit circuit, File file) {
        this.circuit = circuit;
        this.file = file;

        this.leafNodes = new ArrayList<LeafNode>();
    }

    public void parse() throws IOException {

    	this.makeNameToGlobalBlockMap();
    	
        BufferedReader reader = new BufferedReader(new FileReader(this.file));

        String line;
        while((line = reader.readLine()) != null) {
            if(line.contains("Leaf Node")){
            	this.currentNode = new LeafNode(this.leafNodes.size(), this.getIdentifier(line), this.getColor(line));
            	this.leafNodes.add(this.currentNode);
            }else{
            	String name = this.getBlockname(line);

            	GlobalBlock block = this.nameToGlobalblockMap.remove(name);
            	this.currentNode.add(block);
            	block.setLeafNode(this.currentNode);
            }
        }

        reader.close();

        //Test functionality
        if(!this.nameToGlobalblockMap.isEmpty()){
        	System.out.println("Not all global blocks have a hierarchy node assigned! (" + this.nameToGlobalblockMap.size() + " unassigned blocks");
        	for(GlobalBlock block:this.nameToGlobalblockMap.values()){
        		System.out.println("\t" + block.getName());
        	}
        }
        
        HierarchyNode root = new HierarchyNode("");
        this.buildHierarchy(root);
    }
    private void makeNameToGlobalBlockMap(){
    	this.nameToGlobalblockMap = new HashMap<String, GlobalBlock>();
    	
    	for(GlobalBlock block:this.circuit.getGlobalBlocks()){
    		this.nameToGlobalblockMap.put(block.getName(),block);
    	}
    }
    private String getIdentifier(String line){
    	return line.replace("Leaf Node: ", "").split(" ")[0];
    }
    private Color getColor(String line){
    	String[] words = line.split("Color")[1].replace(": (","").replace(")]","").split(",");
    	return new Color(Integer.parseInt(words[0]),Integer.parseInt(words[1]),Integer.parseInt(words[2]));
    }
    private String getBlockname(String line){
    	String[] words = line.split(" ");
    	for(String word:words){
    		if(word.contains("name")){
    			return word.replace("name=\"", "").replace("\"", "");
    		}
    	}
    	return null;
    }
    
    private void buildHierarchy(HierarchyNode parent){
    	LeafNode ln0 = this.findLeafNode(parent.getIdentifier() + "0");
    	LeafNode ln1 = this.findLeafNode(parent.getIdentifier() + "1");
    	
    	if(ln0 == null){
    		HierarchyNode node0 = new HierarchyNode(parent.getIdentifier() + "0");
    		node0.setParent(parent);
    		parent.addChild(node0);
    		this.buildHierarchy(node0);
    	}else{
    		ln0.setParent(parent);
    		parent.addChild(ln0);
    	}
    	
    	
    	if(ln1 == null){
    		HierarchyNode node1 = new HierarchyNode(parent.getIdentifier() + "1");
    		node1.setParent(parent);
    		parent.addChild(node1);
    		this.buildHierarchy(node1);
    	}else{
    		ln1.setParent(parent);
    		parent.addChild(ln1);
    	}
    }
    private LeafNode findLeafNode(String identifier){
    	for(LeafNode leafNode:this.leafNodes){
    		if(leafNode.getIdentifier().equals(identifier)){
    			return leafNode;
    		}
    	}
    	return null;
    }
}
