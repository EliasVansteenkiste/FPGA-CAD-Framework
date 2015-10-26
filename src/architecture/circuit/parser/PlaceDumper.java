package architecture.circuit.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import architecture.circuit.Circuit;
import architecture.circuit.block.AbstractSite;
import architecture.circuit.block.GlobalBlock;


import util.Logger;

public class PlaceDumper {
	
	private Circuit circuit;
	private File netFile, placeFile;
	private String netPath, architecturePath;
	
	public PlaceDumper(Circuit circuit, File netFile, File placeFile) {
		this.circuit = circuit;
		this.netFile = netFile;
		this.placeFile = placeFile;
		
		this.getPaths();
	}
	
	private void getPaths() {
		String userDir = System.getProperty("user.dir");
		File inputFolder = this.netFile.getParentFile();
		
		// Get the net file
		this.netPath = this.netFile.getAbsolutePath().substring(userDir.length() + 1);
		
		// Get the architecture file
		File architectureFile = Util.getArchitectureFile(inputFolder);
		this.architecturePath = architectureFile.getAbsolutePath().substring(userDir.length() + 1);
	}
	
	
	
	public void dump() {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(this.placeFile)));
		} catch (IOException e) {
			Logger.raise("Could not open placement output file: " + this.placeFile, e);
		}
		
		int length = 0;
		for(GlobalBlock block : this.circuit.getGlobalBlocks()) {
			if(block.getName().length() > length) {
				length = block.getName().length();
			}
		}
		
		
		int width = this.circuit.getWidth(), height = this.circuit.getHeight();
		
		this.dumpHeader(writer, width, height, length);
		
		
		Map<AbstractSite, Integer> siteOccupations = new HashMap<AbstractSite, Integer>();
		for(GlobalBlock block : this.circuit.getGlobalBlocks()) {
			AbstractSite site = block.getSite();
			int x = site.getX();
			int y = site.getY();
			int index = block.getIndex();
			
			int z;
			if(siteOccupations.containsKey(site)) {
				z = siteOccupations.get(site);
			} else {
				z = 0;
			}
			siteOccupations.put(site, z + 1);	
			
			
			
			writer.printf("%-"+length+"s %-7d %-7d %-7d #%d\n",
					block.getName(), x, y, z, index);
		}
		
		writer.close();
	}
	
	private void dumpHeader(PrintWriter writer, int width, int height, int length) {
		// Print out the header
		writer.printf("Netlist file: %s   Architecture file: %s\n", this.netPath, this.architecturePath);
		writer.printf("Array size: %d x %d logic blocks\n\n", width - 2, height - 2);
		
		length = Math.max(length, 10);
		writer.printf("%-"+length+"s x       y       subblk  block number\n", "#block name");
		writer.printf("%-"+length+"s --      --      ------  ------------\n", "#----------");
	}
}
