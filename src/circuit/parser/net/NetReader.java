package circuit.parser.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import circuit.PackedCircuit;
import circuit.PrePackedCircuit;

/*
 * This class is intended to read vpr .net files
 * It is VERY incomplete and only capable of reading the vtr_benchmark .net files
 */
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
				String firstPart = trimmedLine.split(" +")[0];
				boolean success;
				switch(firstPart)
				{
					case "<block":
						success = processOuterBlock(reader, trimmedLine);
						if(!success)
						{
							System.out.println("Something went wrong while processing a block");
							break outerloop;
						}
						break;
					case "<inputs>": //This will only occur for FPGA inputs, not for block inputs
						System.out.println("\n---INPUTS---");
						success = processFpgaInputs(reader, trimmedLine);
						if(!success)
						{
							System.out.println("Something went wrong while processing top level inputs");
							break outerloop;
						}
						break;
					case "<outputs>": //This will only occur for FPGA outputs, not for block outputs
						System.out.println("\n---OUTPUTS---");
						success = processFpgaOutputs(reader, trimmedLine);
						if(!success)
						{
							System.out.println("Something went wrong while processing top level outputs");
							break outerloop;
						}
						break;
					case "<clocks>": //We discard clocks, just find the end of the section
						success = processClocks(reader, trimmedLine);
						if(!success)
						{
							System.out.println("Something went wrong while processing top level clocks");
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
	
	private boolean processOuterBlock(BufferedReader reader, String trimmedLine) throws IOException
	{
		boolean success;
		if(!insideTopBlock) //If it is the top block: take note of it and discard it
		{
			insideTopBlock = true;
			success = true;
		}
		else //Process the complete block
		{
			System.out.println("\n---STARTED NEW BLOCK---");
			String[] lineParts = trimmedLine.split(" +");
			String name = "";
			if(lineParts[1].substring(0, 4).equals("name"))
			{
				name = lineParts[1].substring(6,lineParts[1].length() - 1);
				System.out.println("\tThe name of the block is: " + name);
			}
			String instanceType = "";
			if(lineParts[2].substring(0,8).equals("instance"))
			{
				String instance = lineParts[2].substring(10, lineParts[2].length() - 1);
				int closingBraceIndex = instance.indexOf('[');
				instanceType = instance.substring(0,closingBraceIndex);
				System.out.println("\tThe instance of the block is: " + instance + ", the instance type of the block is: " + instanceType);
			}
			switch(instanceType)
			{
				case "memory":
					success = processHardBlock(reader, name, instanceType);
					break;
				case "clb":
					success = processClb();
					break;
				default:
					System.out.println("Unknown instance type: " + instanceType);
					success = false;
					break;
			}
		}
		return success;
	}
	
	private boolean processHardBlock(BufferedReader reader, String name, String instanceType) throws IOException
	{
		boolean success = false;
		OuterNLBlock outerBlock = new OuterNLBlock(name, instanceType);
		processBlockInternals(outerBlock, reader, true);
		return success;
	}
	
	private boolean processBlockInternals(NLBlock parentBlock, BufferedReader reader, boolean isOuterBlock) throws IOException
	{
		boolean success = false;
		boolean foundTheEnd = false;
		while(!foundTheEnd)
		{
			String line = reader.readLine().trim();
			String[] lineParts = line.split(" +");
			boolean processedLine = false;
			
			if(lineParts[0].equals("</block>"))
			{
				foundTheEnd = true;
				processedLine = true;
			}
			
			if(lineParts[0].equals("<inputs>"))
			{
				processedLine = true;
				boolean foundInputsEnd = false;
				boolean portOpen = false;
				while(!foundInputsEnd)
				{
					String internalLine = reader.readLine().trim();
					String[] internalLineParts = internalLine.split(" +");
					if(portOpen)
					{
						for(int i = 0; i < internalLineParts.length - 1; i++)
						{
							if(!internalLineParts[i].equals("open"))
							{
								System.out.println("\tI found an input: " + internalLineParts[i]);
							}
						}
						if(!internalLineParts[internalLineParts.length - 1].equals("</port>"))
						{
							System.out.println("\tI found an input: " + internalLineParts[internalLineParts.length - 1]);
						}
						else
						{
							portOpen = false;
						}
					}
					else
					{
						if(internalLineParts[0].equals("</inputs>"))
						{
							foundInputsEnd = true;
						}
						else
						{
							if(internalLineParts[0].equals("<port"))
							{
								int closingBraceIndex = internalLineParts[1].indexOf('>');
								String firstInput = internalLineParts[1].substring(closingBraceIndex + 1);
								if(!firstInput.equals("open"))
								{
									System.out.println("\tI found an input: " + firstInput);
								}
								for(int i = 2; i < internalLineParts.length - 1; i++)
								{
									if(!internalLineParts[i].equals("open"))
									{
										System.out.println("\tI found an input: " + internalLineParts[i]);
									}
								}
								if(!internalLineParts[internalLineParts.length - 1].equals("</port>"))
								{
									portOpen = true;
									System.out.println("\tI found an input: " + internalLineParts[internalLineParts.length - 1]);
								}
							}
							else
							{
								System.out.println("A line was not processed: " + internalLine);
								processedLine = false;
								break;
							}
						}
					}
				}
			}
			
			if(lineParts[0].equals("<outputs>"))
			{
				processedLine = true;
				boolean foundOutputsEnd = false;
				boolean portOpen = false;
				while(!foundOutputsEnd)
				{
					String internalLine = reader.readLine().trim();
					String[] internalLineParts = internalLine.split(" +");
					if(portOpen)
					{
						for(int i = 0; i < internalLineParts.length - 1; i++)
						{
							if(!internalLineParts[i].equals("open"))
							{
								System.out.println("\tI found an output: " + internalLineParts[i]);
							}
						}
						if(!internalLineParts[internalLineParts.length - 1].equals("</port>"))
						{
							System.out.println("\tI found an output: " + internalLineParts[internalLineParts.length - 1]);
						}
						else
						{
							portOpen = false;
						}
					}
					else
					{
						if(internalLineParts[0].equals("</outputs>"))
						{
							foundOutputsEnd = true;
						}
						else
						{
							if(internalLineParts[0].equals("<port"))
							{
								int closingBraceIndex = internalLineParts[1].indexOf('>');
								String firstOutput = internalLineParts[1].substring(closingBraceIndex + 1);
								if(!firstOutput.equals("open"))
								{
									System.out.println("\tI found an output: " + firstOutput);
								}
								for(int i = 2; i < internalLineParts.length - 1; i++)
								{
									if(!internalLineParts[i].equals("open"))
									{
										System.out.println("\tI found an output: " + internalLineParts[i]);
									}
								}
								if(!internalLineParts[internalLineParts.length - 1].equals("</port>"))
								{
									portOpen = true;
									System.out.println("\tI found an output: " + internalLineParts[internalLineParts.length - 1]);
								}
							}
							else
							{
								System.out.println("A line was not processed: " + internalLine);
								processedLine = false;
								break;
							}
						}
					}
				}
			}
			
			if(lineParts[0].equals("<clocks>"))
			{
				processedLine = true;
				boolean foundClocksEnd = false;
				while(!foundClocksEnd)
				{
					String internalLine = reader.readLine().trim();
					String[] internalLineParts = internalLine.split(" +");
					if(internalLineParts[0].equals("</clocks>"))
					{
						foundClocksEnd = true;
					}
					else
					{
						if(internalLineParts[0].equals("<port") && internalLineParts[internalLineParts.length - 1].equals("</port>"))
						{
							String firstClock = internalLineParts[1].substring(internalLineParts[1].indexOf('>') + 1);
							System.out.println("\tI found a clock: " + firstClock);
							for(int i = 2; i < internalLineParts.length - 1; i++)
							{
								System.out.println("\tI found a clock: " + internalLineParts[i]);
							}
						}
						else
						{
							System.out.println("A line was not processed: " + internalLine);
							processedLine = false;
							break;
						}
					}
				}
			}
			
			if(lineParts[0].equals("<block"))
			{
				processedLine = true;
				System.out.println("---FOUND A NEW SUBBLOCK---");
				if(lineParts[1].substring(0, 4).equals("name"))
				{
					String name = lineParts[1].substring(6,lineParts[1].length() - 1);
					System.out.println("\tThe name of the block is: " + name);
				}
				else
				{
					System.out.println("Error in block descrption!");
					processedLine = false;
					break;
				}
				if(lineParts[2].substring(0,8).equals("instance"))
				{
					String instance = lineParts[2].substring(10, lineParts[2].length() - 1);
					int closingBraceIndex = instance.indexOf('[');
					String instanceType = instance.substring(0,closingBraceIndex);
					System.out.println("\tThe instance of the block is: " + instance + ", the instance type of the block is: " + instanceType);
				}
				else
				{
					System.out.println("Error in block descrption!");
					processedLine = false;
					break;
				}
				if(lineParts[lineParts.length - 1].substring(lineParts[lineParts.length - 1].length()-2).equals("/>"))
				{
					System.out.println("\tThis is an empty block!");
				}
				else
				{
					processBlockInternals(parentBlock, reader, false);
				}
			}
			
			if(!processedLine)
			{
				System.out.println("A line was not processed: " + line);
				break;
			}
			
		}
		return success;
	}
	
	private boolean processClb() throws IOException
	{
		boolean success = false;
		
		return success;
	}
	
	private boolean processFpgaInputs(BufferedReader reader, String trimmedLine) throws IOException
	{
		boolean success = true;
		String[] lineParts = trimmedLine.split(" +");
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
			lineParts = reader.readLine().trim().split(" +");
		}while(!finished);
		return success;
	}
	
	private boolean processFpgaOutputs(BufferedReader reader, String trimmedLine) throws IOException
	{
		boolean success = true;
		String[] lineParts = trimmedLine.split(" +");
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
			lineParts = reader.readLine().trim().split(" +");
		}while(!finished);
		return success;
	}
	
	private boolean processClocks(BufferedReader reader, String trimmedLine) throws IOException
	{
		boolean success = true;
		String[] lineParts = trimmedLine.split(" +");
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
			lineParts = reader.readLine().trim().split(" +");
		}while(!finished);
		return success;
	}
	
}
