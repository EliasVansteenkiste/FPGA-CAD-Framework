package circuit.parser.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import circuit.PackedCircuit;
import circuit.PrePackedCircuit;

public class NetReader
{

	private PrePackedCircuit prePackedCircuit;
	private PackedCircuit packedCircuit;
	
	private boolean insideTopBlock;
	
	public void readNetlist(String fileName, int nbLutInputs) throws IOException
	{
		int lastIndexSlash = fileName.lastIndexOf('/');
		prePackedCircuit = new PrePackedCircuit(nbLutInputs, fileName.substring(lastIndexSlash + 1));
		packedCircuit = new PackedCircuit(prePackedCircuit.getOutputs(), prePackedCircuit.getInputs(), prePackedCircuit.getHardBlocks());
		Path path = Paths.get(fileName);
		try(BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
		{
			insideTopBlock = false;
			
			String line = null;
			outerloop:while((line = reader.readLine()) != null)
			{
				String trimmedLine = line.trim();//Remove leading and trailing whitespace from the line
				String firstPart = trimmedLine.split(" ")[0];
				boolean success;
				switch(firstPart)
				{
					case "<block":
						success = processBlock(trimmedLine);
						if(!success)
						{
							break outerloop;
						}
						break;
					case "<inputs>": //This will only occur for FPGA inputs, not for block inputs
						System.out.println("---INPUTS---");
						success = processFpgaInputs(reader, trimmedLine);
						if(!success)
						{
							break outerloop;
						}
						break;
					case "<outputs>": //This will only occur for FPGA outputs, not for block outputs
						System.out.println("---OUTPUTS---");
						success = processFpgaOutputs(reader, trimmedLine);
						if(!success)
						{
							break outerloop;
						}
						break;
					case "<clocks>": //We discard clocks, just find the end of the section
						success = processClocks(reader, trimmedLine);
						if(!success)
						{
							break outerloop;
						}
						break;
					case "</block>":
						if(!insideTopBlock)
						{
							System.out.println("Something went wrong, insideTopBlock was false");
						}
						break outerloop;
					default:
						System.out.println("An error occurred. First part = " + firstPart);
						break outerloop;
				}
			}
		}
	}
	
	private boolean processBlock(String trimmedLine)
	{
		boolean success;
		if(!insideTopBlock) //If it is the top block: take note of it and discard it
		{
			insideTopBlock = true;
			success = true;
		}
		else //Process the complete block
		{
			System.out.println("Second block: " + trimmedLine);
			success = false;
		}
		return success;
	}
	
	private boolean processFpgaInputs(BufferedReader reader, String trimmedLine) throws IOException
	{
		boolean success = true;
		String[] lineParts = trimmedLine.split(" ");
		boolean finished = false;
		do
		{
			for(int i = 0; i < lineParts.length; i++)
			{
				if(lineParts[i].equals("</inputs>"))
				{
					finished = true;
					break;
				}
				else
				{
					if(!lineParts[i].equals("<inputs>"))
					{
						System.out.println(lineParts[i] + " is an FPGA input");
					}
				}
			}
			lineParts = reader.readLine().trim().split(" ");
		}while(!finished);
		return success;
	}
	
	private boolean processFpgaOutputs(BufferedReader reader, String trimmedLine) throws IOException
	{
		boolean success = true;
		String[] lineParts = trimmedLine.split(" ");
		boolean finished = false;
		do
		{
			for(int i = 0; i < lineParts.length; i++)
			{
				if(lineParts[i].equals("</outputs>"))
				{
					finished = true;
					break;
				}
				else
				{
					if(!lineParts[i].equals("<outputs>"))
					{
						System.out.println(lineParts[i] + " is an FPGA output");
					}
				}
			}
			lineParts = reader.readLine().trim().split(" ");
		}while(!finished);
		return success;
	}
	
	private boolean processClocks(BufferedReader reader, String trimmedLine) throws IOException
	{
		boolean success = true;
		String[] lineParts = trimmedLine.split(" ");
		boolean finished = false;
		do
		{
			for(int i = 0; i < lineParts.length; i++)
			{
				if(lineParts[i].equals("</clocks>"))
				{
					finished = true;
					break;
				}
			}
			lineParts = reader.readLine().trim().split(" ");
		}while(!finished);
		return success;
	}
	
}
