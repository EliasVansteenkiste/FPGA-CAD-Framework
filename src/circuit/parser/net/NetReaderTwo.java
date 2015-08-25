package circuit.parser.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Vector;

import circuit.Clb;
import circuit.HardBlock;
import circuit.Input;
import circuit.Net;
import circuit.Output;
import circuit.PackedCircuit;

public class NetReaderTwo
{

	private PackedCircuit packedCircuit;
	private boolean insideTopBlock;
	private int clbNum;
	
	public void readNetlist(String fileName, int nbLutInputs, int nbBles) throws IOException
	{
		clbNum = 0;
		int lastIndexSlash = fileName.lastIndexOf('/');
		packedCircuit = new PackedCircuit();
		packedCircuit.setName(fileName.substring(lastIndexSlash + 1));
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
						success = processOuterBlock(reader, trimmedLine, nbBles);
						if(!success)
						{
							System.err.println("Something went wrong while processing a block");
							break outerloop;
						}
						break;
					case "<inputs>": //This will only occur for FPGA inputs, not for block inputs
						success = processFpgaInputs(reader, trimmedLine);
						if(!success)
						{
							System.err.println("Something went wrong while processing top level inputs");
							break outerloop;
						}
						break;
					case "<outputs>": //This will only occur for FPGA outputs, not for block outputs
						success = processFpgaOutputs(reader, trimmedLine);
						if(!success)
						{
							System.err.println("Something went wrong while processing top level outputs");
							break outerloop;
						}
						break;
					case "<clocks>": //We discard clocks, just find the end of the section
						success = processClocks(reader, trimmedLine);
						if(!success)
						{
							System.err.println("Something went wrong while processing top level clocks");
							break outerloop;
						}
						break;
					case "</block>":
						if(!insideTopBlock)
						{
							System.err.println("Something went wrong, insideTopBlock was false");
						}
						break outerloop;
					default:
						System.err.println("An error occurred. First part = " + firstPart);
						break outerloop;
				}
			}
		}
		stripDummyNets();
	}
	
	public PackedCircuit getPackedCircuit()
	{
		return packedCircuit;
	}
	
	private boolean processOuterBlock(BufferedReader reader, String trimmedLine, int nbBles) throws IOException
	{
		boolean success;
		if(!insideTopBlock) //If it is the top block: take note of it and discard it
		{
			insideTopBlock = true;
			success = true;
		}
		else //Process the complete block
		{
			String[] lineParts = trimmedLine.split(" +");
			String name = "";
			if(lineParts[1].substring(0, 4).equals("name"))
			{
				name = lineParts[1].substring(6,lineParts[1].length() - 1);
			}
			String instanceType = "";
			String instance = "";
			if(lineParts[2].substring(0,8).equals("instance"))
			{
				String instancePlusTail = lineParts[2].substring(10);
				int closingIndex = instancePlusTail.indexOf((char)34);
				instance = instancePlusTail.substring(0, closingIndex);
				int closingBraceIndex = instance.indexOf('[');
				instanceType = instance.substring(0,closingBraceIndex);
			}
			String mode = "";
			if(lineParts.length >= 4 && lineParts[3].substring(0,4).equals("mode"))
			{
				String modePlusTail = lineParts[3].substring(6);
				int closingIndex = modePlusTail.indexOf((char)34);
				mode = modePlusTail.substring(0,closingIndex);
			}
			switch(instanceType)
			{
				case "memory":
					success = processHardBlock(reader, name, instance, instanceType);
					break;
				case "mult_36":
					success = processHardBlock(reader, name, instance, instanceType);
					break;
				case "clb":
					success = processClb(reader, name, instance, instanceType, nbBles);
					clbNum++;
					break;
				case "io":
					success = processIo(reader,name, instance, mode);
					break;
				default:
					System.err.println("Unknown instance type: " + instanceType);
					success = false;
					break;
			}
		}
		return success;
	}
	
	private boolean processHardBlock(BufferedReader reader, String name, String instance, String instanceType) throws IOException
	{
		ArrayList<String> instances = new ArrayList<>();
		ArrayList<String> instanceTypes = new ArrayList<>();
		ArrayList<String> inputs = new ArrayList<>();
		ArrayList<String> outputs = new ArrayList<>();
		instances.add(instance);
		instanceTypes.add(instanceType);
		ArrayList<String> recursiveInstancesTracking = new ArrayList<>();
		boolean success = processBlockInternals(reader, instance, instances, instanceTypes, inputs, 
																	outputs, recursiveInstancesTracking);
		ArrayList<String> topLevelInputs = new ArrayList<>();
		ArrayList<String> topLevelOutputs = new ArrayList<>();
		processBlockIOs(instances, instanceTypes, inputs, outputs, topLevelInputs, topLevelOutputs);		
		boolean isClockEdge;
		switch(instanceType)
		{
			case "memory":
				isClockEdge = true;
				break;
			default:
				isClockEdge = false;
				break;
		}
		Vector<String> inputNames = new Vector<>();
		for(int i = 0; i < topLevelInputs.size(); i++)
		{
			inputNames.add(instance + ".in[" + i + "]"); 
		}
		Vector<String> outputNames = new Vector<>();
		for(int i = 0; i < topLevelOutputs.size(); i++)
		{
			outputNames.add(instance + ".out[" + i + "]");
		}
		HardBlock hardBlock = new HardBlock(name, outputNames, inputNames, instanceType, isClockEdge);
		packedCircuit.addHardBlock(hardBlock);
		for(int i = 0; i < topLevelInputs.size(); i++)
		{
			String input = topLevelInputs.get(i);
			
			if(!packedCircuit.getNets().containsKey(input)) //net still needs to be added to the nets hashmap
			{
				packedCircuit.getNets().put(input, new Net(input));
			}
			packedCircuit.getNets().get(input).addSink(hardBlock.getInputs()[i]);	
		}
		for(int i = 0; i < topLevelOutputs.size(); i++)
		{
			String output = topLevelOutputs.get(i);
			
			if(!packedCircuit.getNets().containsKey(output)) //net still needs to be added to the nets hashmap
			{
				packedCircuit.getNets().put(output, new Net(output));
			}
			packedCircuit.getNets().get(output).addSource(hardBlock.getOutputs()[i]);
			
			hardBlock.addOutputNetName(hardBlock.getOutputs()[i], output);
		}
		return success;
	}
	
	private boolean processClb(BufferedReader reader, String name, String instance, String instanceType, int nbBles) throws IOException
	{
		ArrayList<String> instances = new ArrayList<>();
		ArrayList<String> instanceTypes = new ArrayList<>();
		ArrayList<String> inputs = new ArrayList<>();
		ArrayList<String> outputs = new ArrayList<>();
		instances.add(instance);
		instanceTypes.add(instanceType);
		ArrayList<String> recursiveInstancesTracking = new ArrayList<>();
		boolean success = processBlockInternals(reader, instance, instances, instanceTypes, inputs, 
																		outputs, recursiveInstancesTracking);
		ArrayList<String> topLevelInputs = new ArrayList<>();
		ArrayList<String> topLevelOutputs = new ArrayList<>();
		processBlockIOs(instances, instanceTypes, inputs, outputs, topLevelInputs, topLevelOutputs);
		
		Clb clb = new Clb("CLB[" + clbNum + "]", topLevelOutputs.size(), topLevelInputs.size(), nbBles);
		packedCircuit.clbs.put(clb.name, clb);
		for(int i = 0; i < topLevelInputs.size(); i++)
		{
			String input = topLevelInputs.get(i);
			if(!packedCircuit.getNets().containsKey(input)) //net still needs to be added to the nets hashmap
			{
				packedCircuit.getNets().put(input, new Net(input));
			}
			packedCircuit.getNets().get(input).addSink(clb.input[i]);
		}
		for(int i = 0; i < topLevelOutputs.size(); i++)
		{
			String output = topLevelOutputs.get(i);
			if(!packedCircuit.getNets().containsKey(output))
			{
				packedCircuit.getNets().put(output, new Net(output));
			}
			packedCircuit.getNets().get(output).addSource(clb.output[i]);
		}
		
		return success;
	}
	
	private boolean processIo(BufferedReader reader, String name, String instance, String mode) throws IOException
	{
		ArrayList<String> instances = new ArrayList<>();
		ArrayList<String> instanceTypes = new ArrayList<>();
		ArrayList<String> inputs = new ArrayList<>();
		ArrayList<String> outputs = new ArrayList<>();
		ArrayList<String> recursiveInstancesTracking = new ArrayList<>();
		boolean success = processBlockInternals(reader, instance, instances, instanceTypes, inputs, 
																outputs, recursiveInstancesTracking);
		ArrayList<String> topLevelInputs = new ArrayList<>();
		ArrayList<String> topLevelOutputs = new ArrayList<>();
		processBlockIOs(instances, instanceTypes, inputs, outputs, topLevelInputs, topLevelOutputs);
		if(mode.equals("inpad"))
		{
			//We don't have to do anything
		}
		else
		{
			if(mode.equals("outpad"))
			{
				String connectedNetName = topLevelInputs.get(0);
				String blockName = name;
				if(blockName.substring(0,4).equals("out:"))
				{
					blockName = blockName.substring(4);
				}
				if(!blockName.equals(connectedNetName))
				{
					packedCircuit.nets.remove(blockName);
					Output output = packedCircuit.outputs.get(blockName);
					
					if(!packedCircuit.getNets().containsKey(connectedNetName)) //net still needs to be added to the nets hashmap
					{
						packedCircuit.getNets().put(connectedNetName, new Net(connectedNetName));
					}
					packedCircuit.getNets().get(connectedNetName).addSink(output.input);
					
					output.name = connectedNetName;
					packedCircuit.outputs.remove(blockName);
					packedCircuit.outputs.put(output.name, output);
				}
			}
			else
			{
//				if(mode.equals("io"))
//				{
//					Output output = null;
//					if(topLevelInputs.size() > 0)
//					{
//						String connectedNetName = topLevelInputs.get(0);
//						String blockName = name;
//						if(blockName.substring(0,4).equals("out:"))
//						{
//							blockName = blockName.substring(4);
//						}
//						if(!blockName.equals(connectedNetName))
//						{
//							packedCircuit.nets.remove(blockName);
//							prePackedCircuit.nets.remove(blockName);
//							output = packedCircuit.outputs.get(blockName);
//							
//							if(!packedCircuit.getNets().containsKey(connectedNetName)) //net still needs to be added to the nets hashmap
//							{
//								packedCircuit.getNets().put(connectedNetName, new Net(connectedNetName));
//							}
//							packedCircuit.getNets().get(connectedNetName).addSink(output.input);
//							
//							if(!prePackedCircuit.getNets().containsKey(connectedNetName)) //net still needs to be added to the nets hashmap
//							{
//								prePackedCircuit.getNets().put(connectedNetName, new Net(connectedNetName));
//							}
//							prePackedCircuit.getNets().get(connectedNetName).addSink(output.input);
//							
//							output.name = connectedNetName;
//							packedCircuit.outputs.remove(blockName);
//							packedCircuit.outputs.put(output.name, output);
//						}
//						else
//						{
//							output = packedCircuit.outputs.get(blockName);
//						}
//					}
//					
//					Input input = null;
//					if(topLevelOutputs.size() > 0)
//					{
//						String inputName = topLevelOutputs.get(0);
//						input = packedCircuit.inputs.get(inputName);
//					}
//					
//					ArrayList<Block> tuple = new ArrayList<>();
//					tuple.add(input);
//					tuple.add(output);
//					packedIOs.add(tuple);
//				}
//				else
//				{
//					System.err.println("Error: the mode of the io was not recognized!");
//				}
				System.err.println("Error: the mode of the io was not recognized!");
			}
		}
		return success;
	}
	
	private boolean processBlockInternals(BufferedReader reader, String currentInstance, ArrayList<String> instances, 
			ArrayList<String> instanceTypes, ArrayList<String> inputs, ArrayList<String> outputs, 
			ArrayList<String> recursiveInstancesTracking) throws IOException
	{
		boolean success = true;
		
		recursiveInstancesTracking.add(currentInstance);
		
		boolean foundTheEnd = false;
		while (!foundTheEnd)
		{
			String line = reader.readLine().trim();
			String[] lineParts = line.split(" +");
			boolean processedLine = false;

			if (lineParts[0].equals("</block>"))
			{
				foundTheEnd = true;
				processedLine = true;
			}

			if (lineParts[0].equals("<inputs>"))
			{
				processedLine = true;
				boolean foundInputsEnd = false;
				boolean portOpen = false;
				while (!foundInputsEnd)
				{
					String internalLine = reader.readLine().trim();
					String[] internalLineParts = internalLine.split(" +");
					if (portOpen)
					{
						for (int i = 0; i < internalLineParts.length - 1; i++)
						{
							if (!internalLineParts[i].equals("open"))
							{
								inputs.add(internalLineParts[i]);
							}
						}
						if (!internalLineParts[internalLineParts.length - 1].equals("</port>"))
						{
							if (!internalLineParts[internalLineParts.length - 1].equals("open"))
							{
								inputs.add(internalLineParts[internalLineParts.length - 1]);
							}
						}
						else
						{
							portOpen = false;
						}
					}
					else
					{
						if (internalLineParts[0].equals("</inputs>"))
						{
							foundInputsEnd = true;
						}
						else
						{
							if (internalLineParts[0].equals("<port"))
							{
								int closingBraceIndex = internalLineParts[1].indexOf('>');
								String firstInput = "";
								if (closingBraceIndex != internalLineParts[1].length() - 1)
								{
									firstInput = internalLineParts[1].substring(closingBraceIndex + 1);
								}
								if (!firstInput.equals("open") && !firstInput.equals(""))
								{
									inputs.add(firstInput);
								}
								for (int i = 2; i < internalLineParts.length - 1; i++)
								{
									if (!internalLineParts[i].equals("open"))
									{
										inputs.add(internalLineParts[i]);
									}
								}
								if (!internalLineParts[internalLineParts.length - 1].equals("</port>"))
								{
									portOpen = true;
									if (internalLineParts.length != 2 && !internalLineParts[internalLineParts.length - 1].equals("open"))
									{
										inputs.add(internalLineParts[internalLineParts.length - 1]);
									}
								}
							}
							else
							{
								System.err.println("A line was not processed: " + internalLine);
								processedLine = false;
								break;
							}
						}
					}
				}
			}

			if (lineParts[0].equals("<outputs>"))
			{
				processedLine = true;
				boolean foundOutputsEnd = false;
				boolean portOpen = false;
				while (!foundOutputsEnd)
				{
					String internalLine = reader.readLine().trim();
					String[] internalLineParts = internalLine.split(" +");
					if (portOpen)
					{
						for (int i = 0; i < internalLineParts.length - 1; i++)
						{
							if (!internalLineParts[i].equals("open"))
							{
								outputs.add(internalLineParts[i]);
							}
						}
						if (!internalLineParts[internalLineParts.length - 1].equals("</port>"))
						{
							if (!internalLineParts[internalLineParts.length - 1].equals("open"))
							{
								outputs.add(internalLineParts[internalLineParts.length - 1]);
							}
						}
						else
						{
							portOpen = false;
						}
					}
					else
					{
						if (internalLineParts[0].equals("</outputs>"))
						{
							foundOutputsEnd = true;
						}
						else
						{
							if (internalLineParts[0].equals("<port"))
							{
								int closingBraceIndex = internalLineParts[1].indexOf('>');
								String firstOutput = internalLineParts[1].substring(closingBraceIndex + 1);
								if (!firstOutput.equals("open"))
								{
									outputs.add(firstOutput);
								}
								for (int i = 2; i < internalLineParts.length - 1; i++)
								{
									if (!internalLineParts[i].equals("open"))
									{
										outputs.add(internalLineParts[i]);
									}
								}
								if (!internalLineParts[internalLineParts.length - 1].equals("</port>"))
								{
									portOpen = true;
									if (!internalLineParts[internalLineParts.length - 1].equals("open"))
									{
										outputs.add(internalLineParts[internalLineParts.length - 1]);
									}
								}
							}
							else
							{
								System.err.println("A line was not processed: " + internalLine);
								processedLine = false;
								break;
							}
						}
					}
				}
			}

			if (lineParts[0].equals("<clocks>"))
			{
				processedLine = true;
				boolean foundClocksEnd = false;
				while (!foundClocksEnd)
				{
					String internalLine = reader.readLine().trim();
					String[] internalLineParts = internalLine.split(" +");
					if (internalLineParts[0].equals("</clocks>"))
					{
						foundClocksEnd = true;
					}
					else
					{
						if (internalLineParts[0].equals("<port") && internalLineParts[internalLineParts.length - 1].equals("</port>"))
						{
							for (int i = 2; i < internalLineParts.length - 1; i++)
							{
								
							}
						}
						else
						{
							System.err.println("A line was not processed: " + internalLine);
							processedLine = false;
							break;
						}
					}
				}
			}

			if (lineParts[0].equals("<block"))
			{
				processedLine = true;
				if (lineParts[1].substring(0, 4).equals("name"))
				{
					
				}
				else
				{
					System.err.println("Error in block descrption!");
					processedLine = false;
					break;
				}
				String instance = "";
				if (lineParts[2].substring(0, 8).equals("instance"))
				{
					String instancePlusTail = lineParts[2].substring(10);
					int closingIndex = instancePlusTail.indexOf((char) 34);
					instance = instancePlusTail.substring(0, closingIndex);
					instances.add(instance);
					int closingBraceIndex = instance.indexOf('[');
					String instanceType = instance.substring(0, closingBraceIndex);
					boolean alreadyPresent = false;
					for (String presentInstanceType : instanceTypes)
					{
						if (instanceType.equals(presentInstanceType))
						{
							alreadyPresent = true;
						}
					}
					if (!alreadyPresent)
					{
						instanceTypes.add(instanceType);
					}
				}
				else
				{
					System.err.println("Error in block descrption!");
					processedLine = false;
					break;
				}
				if (lineParts[lineParts.length - 1].substring(lineParts[lineParts.length - 1].length() - 2).equals("/>"))
				{

				}
				else
				{
					boolean internalSuccess = processBlockInternals(reader, instance, instances, instanceTypes, inputs, 
																					outputs, recursiveInstancesTracking);
					if (!internalSuccess && success)
					{
						success = false;
						System.err.println("Encountered a problem!");
					}
				}
			}

			if (!processedLine)
			{
				System.err.println("A line was not processed: " + line);
				break;
			}

		}
		
		recursiveInstancesTracking.remove(recursiveInstancesTracking.size() - 1);
		
		return success;
	}
	
	private void processBlockIOs(ArrayList<String> instances, ArrayList<String> instanceTypes, ArrayList<String> inputs, 
			ArrayList<String> outputs, ArrayList<String> topLevelInputsToReturn, ArrayList<String> topLevelOutputsToReturn)
	{
		for(String input:inputs)
		{
			String firstInputPart = input.split("\\.")[0];
			boolean foundIt = false;
			for(String instanceType:instanceTypes)
			{
				if(instanceType.equals(firstInputPart))
				{
					foundIt = true;
					break;
				}
			}
			if(!foundIt)
			{
				for(String instance:instances)
				{
					if(instance.equals(firstInputPart))
					{
						foundIt = true;
						break;
					}
				}
				if(!foundIt)
				{
					topLevelInputsToReturn.add(input);
				}
			}
		}
		
		for(String output:outputs)
		{
			String firstOutputPart = output.split("\\.")[0];
			boolean foundIt = false;
			for(String instance:instances)
			{
				if(instance.equals(firstOutputPart))
				{
					foundIt = true;
					break;
				}
			}
			if(!foundIt)
			{
				for(String instanceType:instanceTypes)
				{
					if(instanceType.equals(firstOutputPart))
					{
						foundIt = true;
						break;
					}
				}
				if(!foundIt)
				{
					topLevelOutputsToReturn.add(output);
				}
			}
		}
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
						String inputName = lineParts[i];
						Input input = new Input(inputName);
						packedCircuit.addInput(input);
						
						if(!packedCircuit.getNets().containsKey(input.name)) //net still needs to be added to the nets hashmap
						{
							packedCircuit.getNets().put(input.name, new Net(input.name));
						}
						packedCircuit.getNets().get(input.name).addSource(input.output);
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
						String outputName = lineParts[i];
						if(outputName.substring(0,4).equals("out:"))
						{
							outputName = outputName.substring(4);
						}
						Output output = new Output(outputName);
						packedCircuit.addOutput(output);
						
						if(!packedCircuit.getNets().containsKey(output.name)) //net still needs to be added to the nets hashmap
						{
							packedCircuit.getNets().put(output.name, new Net(output.name));
						}
						packedCircuit.getNets().get(output.name).addSink(output.input);
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
	
	private void stripDummyNets()
	{
		ArrayList<String> netsToDelete = new ArrayList<>();
		for(Net net: packedCircuit.nets.values())
		{
			if(net.source == null && net.sinks.size() == 1)
			{
				netsToDelete.add(net.name);
				//System.out.println("Will strip " + net.name + " because of no source and 1 sink");
			}
			else
			{
				if(net.source != null && net.sinks.size() == 0)
				{
					netsToDelete.add(net.name);
					//System.out.println("Will strip " + net.name + " because of source but no sink");
				}
			}
			
			if(net.source == null && net.sinks.size() > 1)
			{
				System.out.println("This is a problem: " + net.name + " has no source but more than 1 sink");
			}
			
			
		}
		for(String netToDelete: netsToDelete)
		{
			packedCircuit.nets.remove(netToDelete);
		}
	}
	
}
