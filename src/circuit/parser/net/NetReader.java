package circuit.parser.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Vector;

import circuit.Ble;
import circuit.Block;
import circuit.Clb;
import circuit.Flipflop;
import circuit.Flipflop.InitVal;
import circuit.Flipflop.Type;
import circuit.HardBlock;
import circuit.Input;
import circuit.Lut;
import circuit.Net;
import circuit.Output;
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
	
	private ArrayList<ArrayList<Block>> packedIOs;
	
	public void readNetlist(String fileName, int nbLutInputs) throws IOException
	{
		packedIOs = new ArrayList<>();
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
						//System.out.println("\n---INPUTS---");
						success = processFpgaInputs(reader, trimmedLine);
						if(!success)
						{
							System.out.println("Something went wrong while processing top level inputs");
							break outerloop;
						}
						break;
					case "<outputs>": //This will only occur for FPGA outputs, not for block outputs
						//System.out.println("\n---OUTPUTS---");
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
		stripDummyNets();
	}
	
	public PrePackedCircuit getPrePackedCircuit()
	{
		return prePackedCircuit;
	}
	
	public PackedCircuit getPackedCircuit()
	{
		return packedCircuit;
	}
	
//	public ArrayList<ArrayList<Block>> getPackedIOs()
//	{
//		return packedIOs;
//	}
	
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
			//System.out.println("\n---STARTED NEW BLOCK---");
			String[] lineParts = trimmedLine.split(" +");
			String name = "";
			if(lineParts[1].substring(0, 4).equals("name"))
			{
				name = lineParts[1].substring(6,lineParts[1].length() - 1);
				//System.out.println("\tThe name of the block is: " + name);
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
				//System.out.println("\tThe instance of the block is: " + instance + ", the instance type of the block is: " + instanceType);
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
					success = processClb(reader, name, instance, instanceType);
					break;
				case "io":
					success = processIo(reader,name, instance, mode);
					break;
				default:
					System.out.println("Unknown instance type: " + instanceType);
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
		boolean success = processBlockInternals(reader, instance, instances, instanceTypes, inputs, outputs);
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
			
			if(!prePackedCircuit.getNets().containsKey(input)) //net still needs to be added to the nets hashmap
			{
				prePackedCircuit.getNets().put(input, new Net(input));
			}
			prePackedCircuit.getNets().get(input).addSink(hardBlock.getInputs()[i]);
		}
		for(int i = 0; i < topLevelOutputs.size(); i++)
		{
			String output = topLevelOutputs.get(i);
			
			if(!packedCircuit.getNets().containsKey(output)) //net still needs to be added to the nets hashmap
			{
				packedCircuit.getNets().put(output, new Net(output));
			}
			packedCircuit.getNets().get(output).addSource(hardBlock.getOutputs()[i]);
			
			if(!prePackedCircuit.getNets().containsKey(output)) //net still needs to be added to the nets hashmap
			{
				prePackedCircuit.getNets().put(output, new Net(output));
			}
			prePackedCircuit.getNets().get(output).addSource(hardBlock.getOutputs()[i]);
			
			hardBlock.addOutputNetName(hardBlock.getOutputs()[i], output);
		}
//		for(String input:topLevelInputs)
//		{
//			System.out.println("\t" + input + " is a top level input");
//		}
//		for(String output:topLevelOutputs)
//		{
//			System.out.println("\t" + output + " is a top level output");
//		}
		return success;
	}
	
	private boolean processClb(BufferedReader reader, String name, String instance, String instanceType) throws IOException
	{
		ArrayList<String> instances = new ArrayList<>();
		ArrayList<String> instanceTypes = new ArrayList<>();
		ArrayList<String> inputs = new ArrayList<>();
		ArrayList<String> outputs = new ArrayList<>();
		instances.add(instance);
		instanceTypes.add(instanceType);
		boolean success = processBlockInternals(reader, instance, instances, instanceTypes, inputs, outputs);
		ArrayList<String> topLevelInputs = new ArrayList<>();
		ArrayList<String> topLevelOutputs = new ArrayList<>();
		processBlockIOs(instances, instanceTypes, inputs, outputs, topLevelInputs, topLevelOutputs);
		boolean clbHasLut = false;
		String lutName = "";
		boolean clbHasFF = false;
		String ffName = "";
		for(String output:topLevelOutputs)
		{
			if(output.substring(output.length() - 3).equals("LUT"))
			{
				clbHasLut = true;
				lutName = output.substring(0,output.length() - 3);
			}
			else
			{
				if(output.substring(output.length() - 2).equals("FF"))
				{
					clbHasFF = true;
					ffName = output.substring(0, output.length() - 2);
				}
				else
				{
					System.out.println("Found an output that's not a LUT nor a FF: " + output);
					return false;
				}
			}
		}
		if(clbHasLut && clbHasFF)//The clb has both a lut and a ff
		{
			Lut lut = new Lut(lutName, 1, 6);
			Flipflop ff = new Flipflop(ffName, Type.RISING_EDGE, InitVal.UNKNOWN);
			Ble ble = new Ble(ffName, 6, ff, lut, true);
			Clb clb = new Clb(ffName, 1, 6, ble);
			
			prePackedCircuit.addLut(lut);
			prePackedCircuit.addFlipflop(ff);
			//Add lut input nets
			for(int i = 0; i < topLevelInputs.size(); i++)
			{
				String input = topLevelInputs.get(i);
				if(!prePackedCircuit.getNets().containsKey(input)) //net still needs to be added to the nets hashmap
				{
					prePackedCircuit.getNets().put(input, new Net(input));
				}
				prePackedCircuit.getNets().get(input).addSink(lut.getInputs()[i]);
			}
			//Add ff output net
			if(!prePackedCircuit.getNets().containsKey(ffName)) //net still needs to be added to the nets hashmap
			{
				prePackedCircuit.getNets().put(ffName, new Net(ffName));
			}
			prePackedCircuit.getNets().get(ffName).addSource(ff.getOutput());
			//Add net between lut output and ff input
			if(!prePackedCircuit.getNets().containsKey(lutName)) //net still needs to be added to the nets hashmap
			{
				prePackedCircuit.getNets().put(lutName, new Net(lutName));
			}
			prePackedCircuit.getNets().get(lutName).addSource(lut.getOutputs()[0]);
			prePackedCircuit.getNets().get(lutName).addSink(ff.getInput());
			
			packedCircuit.clbs.put(clb.name, clb);
			//Add clb input nets
			for(int i = 0; i < topLevelInputs.size(); i++)
			{
				String input = topLevelInputs.get(i);
				if(!packedCircuit.getNets().containsKey(input)) //net still needs to be added to the nets hashmap
				{
					packedCircuit.getNets().put(input, new Net(input));
				}
				packedCircuit.getNets().get(input).addSink(clb.input[i]);
			}
			//Add clb output net
			if(!packedCircuit.getNets().containsKey(ffName)) //net still needs to be added to the nets hashmap
			{
				packedCircuit.getNets().put(ffName, new Net(ffName));
			}
			packedCircuit.getNets().get(ffName).addSource(clb.output[0]);
		}
		else
		{
			if(clbHasLut)//The clb only has a lut
			{
				Lut lut = new Lut(lutName, 1, 6);
				Ble ble = new Ble(lutName, 6, null, lut, false);
				Clb clb = new Clb(lutName, 1, 6, ble);
				
				prePackedCircuit.addLut(lut);
				//Add lut input nets
				for(int i = 0; i < topLevelInputs.size(); i++)
				{
					String input = topLevelInputs.get(i);
					if(!prePackedCircuit.getNets().containsKey(input)) //net still needs to be added to the nets hashmap
					{
						prePackedCircuit.getNets().put(input, new Net(input));
					}
					prePackedCircuit.getNets().get(input).addSink(lut.getInputs()[i]);
				}
				//Add lut output net
				if(!prePackedCircuit.getNets().containsKey(lutName)) //net still needs to be added to the nets hashmap
				{
					prePackedCircuit.getNets().put(lutName, new Net(lutName));
				}
				prePackedCircuit.getNets().get(lutName).addSource(lut.getOutputs()[0]);
								
				packedCircuit.clbs.put(clb.name, clb);
				//Add clb input nets
				for(int i = 0; i < topLevelInputs.size(); i++)
				{
					String input = topLevelInputs.get(i);
					if(!packedCircuit.getNets().containsKey(input)) //net still needs to be added to the nets hashmap
					{
						packedCircuit.getNets().put(input, new Net(input));
					}
					packedCircuit.getNets().get(input).addSink(clb.input[i]);
				}
				//Add clb output net
				if(!packedCircuit.getNets().containsKey(lutName)) //net still needs to be added to the nets hashmap
				{
					packedCircuit.getNets().put(lutName, new Net(lutName));
				}
				packedCircuit.getNets().get(lutName).addSource(clb.output[0]);
			}
			else//The clb only has a ff
			{
				Flipflop ff = new Flipflop(ffName, Type.RISING_EDGE, InitVal.UNKNOWN);
				Ble ble = new Ble(ffName, 6, ff, null, true);
				Clb clb = new Clb(ffName, 1, 6, ble);
				
				prePackedCircuit.addFlipflop(ff);
				//Add ff input net
				if(topLevelInputs.size() == 1)
				{
					String input = topLevelInputs.get(0);
					if(!prePackedCircuit.getNets().containsKey(input)) //net still needs to be added to the nets hashmap
					{
						prePackedCircuit.getNets().put(input, new Net(input));
					}
					prePackedCircuit.getNets().get(input).addSink(ff.getInput());
				}
				else
				{
					System.out.println("Error: a clb only contains a ff but has more than one input");
					return false;
				}
				//Add ff output net
				if(!prePackedCircuit.getNets().containsKey(ffName)) //net still needs to be added to the nets hashmap
				{
					prePackedCircuit.getNets().put(ffName, new Net(ffName));
				}
				prePackedCircuit.getNets().get(ffName).addSource(ff.getOutput());
				
				packedCircuit.clbs.put(clb.name, clb);
				//Add clb input net
				String input = topLevelInputs.get(0);
				if(!packedCircuit.getNets().containsKey(input)) //net still needs to be added to the nets hashmap
				{
					packedCircuit.getNets().put(input, new Net(input));
				}
				packedCircuit.getNets().get(input).addSink(clb.input[0]);
				//Add clb output net
				if(!packedCircuit.getNets().containsKey(ffName)) //net still needs to be added to the nets hashmap
				{
					packedCircuit.getNets().put(ffName, new Net(ffName));
				}
				packedCircuit.getNets().get(ffName).addSource(clb.output[0]);
			}
		}
		
//		for(String input:topLevelInputs)
//		{
//			System.out.println("\t" + input + " is a top level input");
//		}
//		for(String output:topLevelOutputs)
//		{
//			System.out.println("\t" + output + " is a top level output");
//		}
		
		return success;
	}
	
	private boolean processIo(BufferedReader reader, String name, String instance, String mode) throws IOException
	{
		ArrayList<String> instances = new ArrayList<>();
		ArrayList<String> instanceTypes = new ArrayList<>();
		ArrayList<String> inputs = new ArrayList<>();
		ArrayList<String> outputs = new ArrayList<>();
		boolean success = processBlockInternals(reader, instance, instances, instanceTypes, inputs, outputs);
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
					prePackedCircuit.nets.remove(blockName);
					Output output = packedCircuit.outputs.get(blockName);
					
					if(!packedCircuit.getNets().containsKey(connectedNetName)) //net still needs to be added to the nets hashmap
					{
						packedCircuit.getNets().put(connectedNetName, new Net(connectedNetName));
					}
					packedCircuit.getNets().get(connectedNetName).addSink(output.input);
					
					if(!prePackedCircuit.getNets().containsKey(connectedNetName)) //net still needs to be added to the nets hashmap
					{
						prePackedCircuit.getNets().put(connectedNetName, new Net(connectedNetName));
					}
					prePackedCircuit.getNets().get(connectedNetName).addSink(output.input);
					
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
//					System.out.println("Error: the mode of the io was not recognized!");
//				}
				System.out.println("Error: the mode of the io was not recognized!");
			}
		}
		return success;
	}
	
	private boolean processBlockInternals(BufferedReader reader, String currentInstance, ArrayList<String> instances, ArrayList<String> instanceTypes, 
																ArrayList<String> inputs, ArrayList<String>outputs) throws IOException
	{
		boolean success = true;
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
								//System.out.println("\tI found an input: " + internalLineParts[i]);
								inputs.add(internalLineParts[i]);
							}
						}
						if(!internalLineParts[internalLineParts.length - 1].equals("</port>"))
						{
							//System.out.println("\tI found an input: " + internalLineParts[internalLineParts.length - 1]);
							if(!internalLineParts[internalLineParts.length - 1].equals("open"))
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
						if(internalLineParts[0].equals("</inputs>"))
						{
							foundInputsEnd = true;
						}
						else
						{
							if(internalLineParts[0].equals("<port"))
							{
								int closingBraceIndex = internalLineParts[1].indexOf('>');
								String firstInput = "";
								if(closingBraceIndex != internalLineParts[1].length() - 1)
								{
									firstInput = internalLineParts[1].substring(closingBraceIndex + 1);
								}
								if(!firstInput.equals("open") && !firstInput.equals(""))
								{
									//System.out.println("\tI found an input: " + firstInput);
									inputs.add(firstInput);
								}
								for(int i = 2; i < internalLineParts.length - 1; i++)
								{
									if(!internalLineParts[i].equals("open"))
									{
										//System.out.println("\tI found an input: " + internalLineParts[i]);
										inputs.add(internalLineParts[i]);
									}
								}
								if(!internalLineParts[internalLineParts.length - 1].equals("</port>"))
								{
									portOpen = true;
									//System.out.println("\tI found an input: " + internalLineParts[internalLineParts.length - 1]);
									
									if(internalLineParts.length != 2 && !internalLineParts[internalLineParts.length - 1].equals("open"))
									{
										inputs.add(internalLineParts[internalLineParts.length - 1]);
									}
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
								//System.out.println("\tI found an output: " + internalLineParts[i]);
								if(currentInstance.substring(0,3).equals("lut"))
								{
									outputs.add(internalLineParts[i] + "LUT");
								}
								else
								{
									if(currentInstance.substring(0,2).equals("ff"))
									{
										outputs.add(internalLineParts[i] + "FF");
									}
									else
									{
										outputs.add(internalLineParts[i]);
									}
								}
							}
						}
						if(!internalLineParts[internalLineParts.length - 1].equals("</port>"))
						{
							//System.out.println("\tI found an output: " + internalLineParts[internalLineParts.length - 1]);
							if(!internalLineParts[internalLineParts.length - 1].equals("open"))
							{
								if(currentInstance.substring(0,3).equals("lut"))
								{
									outputs.add(internalLineParts[internalLineParts.length - 1] + "LUT");
								}
								else
								{
									if(currentInstance.substring(0,2).equals("ff"))
									{
										outputs.add(internalLineParts[internalLineParts.length - 1] + "FF");
									}
									else
									{
										outputs.add(internalLineParts[internalLineParts.length - 1]);
									}
								}
							}
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
									//System.out.println("\tI found an output: " + firstOutput);
									if(currentInstance.substring(0,3).equals("lut"))
									{
										outputs.add(firstOutput + "LUT");
									}
									else
									{
										if(currentInstance.substring(0,2).equals("ff"))
										{
											outputs.add(firstOutput + "FF");
										}
										else
										{
											outputs.add(firstOutput);
										}
									}
								}
								for(int i = 2; i < internalLineParts.length - 1; i++)
								{
									if(!internalLineParts[i].equals("open"))
									{
										//System.out.println("\tI found an output: " + internalLineParts[i]);
										if(currentInstance.substring(0,3).equals("lut"))
										{
											outputs.add(internalLineParts[i] + "LUT");
										}
										else
										{
											if(currentInstance.substring(0,2).equals("ff"))
											{
												outputs.add(internalLineParts[i] + "FF");
											}
											else
											{
												outputs.add(internalLineParts[i]);
											}
										}
									}
								}
								if(!internalLineParts[internalLineParts.length - 1].equals("</port>"))
								{
									portOpen = true;
									//System.out.println("\tI found an output: " + internalLineParts[internalLineParts.length - 1]);
									if(!internalLineParts[internalLineParts.length - 1].equals("open"))
									{
										if(currentInstance.substring(0,3).equals("lut"))
										{
											outputs.add(internalLineParts[internalLineParts.length - 1] + "LUT");
										}
										else
										{
											if(currentInstance.substring(0,2).equals("ff"))
											{
												outputs.add(internalLineParts[internalLineParts.length - 1] + "FF");
											}
											else
											{
												outputs.add(internalLineParts[internalLineParts.length - 1]);
											}
										}
									}
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
							//System.out.println("\tI found a clock: " + firstClock);
							for(int i = 2; i < internalLineParts.length - 1; i++)
							{
								//System.out.println("\tI found a clock: " + internalLineParts[i]);
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
				//System.out.println("---FOUND A NEW SUBBLOCK---");
				if(lineParts[1].substring(0, 4).equals("name"))
				{
					String name = lineParts[1].substring(6,lineParts[1].length() - 1);
					//System.out.println("\tThe name of the block is: " + name);
				}
				else
				{
					System.out.println("Error in block descrption!");
					processedLine = false;
					break;
				}
				String instance = "";
				if(lineParts[2].substring(0,8).equals("instance"))
				{
					String instancePlusTail = lineParts[2].substring(10);
					int closingIndex = instancePlusTail.indexOf((char)34);
					instance = instancePlusTail.substring(0, closingIndex);
					instances.add(instance);
					int closingBraceIndex = instance.indexOf('[');
					String instanceType = instance.substring(0,closingBraceIndex);
					boolean alreadyPresent = false;
					for(String presentInstanceType:instanceTypes)
					{
						if(instanceType.equals(presentInstanceType))
						{
							alreadyPresent = true;
						}
					}
					if(!alreadyPresent)
					{
						instanceTypes.add(instanceType);
					}
					//System.out.println("\tThe instance of the block is: " + instance + ", the instance type of the block is: " + instanceType);
				}
				else
				{
					System.out.println("Error in block descrption!");
					processedLine = false;
					break;
				}
				if(lineParts[lineParts.length - 1].substring(lineParts[lineParts.length - 1].length()-2).equals("/>"))
				{
					//System.out.println("\tThis is an empty block!");
				}
				else
				{
					boolean internalSuccess = processBlockInternals(reader, instance, instances, instanceTypes, inputs, outputs);
					if(!internalSuccess && success)
					{
						success = false;
						System.out.println("Encountered a problem!");
					}
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
						//System.out.println(inputName + " is an FPGA input");
						Input input = new Input(inputName);
						packedCircuit.addInput(input);
						
						if(!packedCircuit.getNets().containsKey(input.name)) //net still needs to be added to the nets hashmap
						{
							packedCircuit.getNets().put(input.name, new Net(input.name));
						}
						packedCircuit.getNets().get(input.name).addSource(input.output);
						
						if(!prePackedCircuit.getNets().containsKey(input.name)) //net still needs to be added to the nets hashmap
						{
							prePackedCircuit.getNets().put(input.name, new Net(input.name));
						}
						prePackedCircuit.getNets().get(input.name).addSource(input.output);
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
						//System.out.println(outputName + " is an FPGA output");
						Output output = new Output(outputName);
						packedCircuit.addOutput(output);
						
						if(!packedCircuit.getNets().containsKey(output.name)) //net still needs to be added to the nets hashmap
						{
							packedCircuit.getNets().put(output.name, new Net(output.name));
						}
						packedCircuit.getNets().get(output.name).addSink(output.input);
						
						if(!prePackedCircuit.getNets().containsKey(output.name)) //net still needs to be added to the nets hashmap
						{
							prePackedCircuit.getNets().put(output.name, new Net(output.name));
						}
						prePackedCircuit.getNets().get(output.name).addSink(output.input);
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
				System.out.println("Will strip " + net.name + " because of no source and 1 sink");
			}
			else
			{
				if(net.source != null && net.sinks.size() == 0)
				{
					netsToDelete.add(net.name);
					System.out.println("Will strip " + net.name + " because of source but no sink");
				}
			}
		}
		for(String netToDelete: netsToDelete)
		{
			packedCircuit.nets.remove(netToDelete);
			prePackedCircuit.nets.remove(netToDelete);
		}
	}
	
}
