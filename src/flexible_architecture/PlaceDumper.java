package flexible_architecture;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import flexible_architecture.architecture.BlockType;
import flexible_architecture.block.AbstractBlock;
import flexible_architecture.block.GlobalBlock;
import flexible_architecture.site.AbstractSite;

import util.Logger;

public class PlaceDumper {
	
	private Circuit circuit;
	private File file;
	
	public PlaceDumper(Circuit circuit, File file) {
		this.circuit = circuit;
		this.file = file;
	}
	
	public void dump() {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(this.file)));
		} catch (IOException e) {
			Logger.raise("Could not open placement output file: " + this.file, e);
		}
		
		int length = 0;
		for(BlockType blockType : this.circuit.getGlobalBlockTypes()) {
			for(AbstractBlock block : this.circuit.getBlocks(blockType)) {
				if(block.getName().length() > length) {
					length = block.getName().length();
				}
			}
		}
		
		
		int width = this.circuit.getWidth(), height = this.circuit.getHeight();
		
		this.dumpHeader(writer, width, height, length);
		
		for(int x = 0; x < width; x++) {
			for(int y = 0; y < height; y++) {
				AbstractSite site = this.circuit.getSite(x, y);
				
				int z = 0;
				for(GlobalBlock block : site.getBlocks()) {
					if(block != null) {
						writer.printf("%"+length+"s %7d %7d %7d %d",
								block.getName(), site.getX(), site.getY(), z, block.getIndex());
					}
					z++;
				}
			}
		}
	}
	
	private void dumpHeader(PrintWriter writer, int width, int height, int length) {
		writer.printf("Netlist file: %s.net\n", this.circuit.getName());
		writer.printf("Array size: %d x %d logic blocks\n\n", width, height);
		
		length = Math.max(length, 10);
		writer.printf("%"+length+"s x       y       subblk  block number\n", "block name");
		writer.printf("%"+length+"s --      --      ------  ------------\n", "----------");
	}
}
