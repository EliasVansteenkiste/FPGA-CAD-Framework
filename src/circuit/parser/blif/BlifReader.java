package circuit.parser.blif;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

import circuit.Flipflop;
import circuit.HardBlock;
import circuit.Input;
import circuit.Lut;
import circuit.Net;
import circuit.Output;
import circuit.PrePackedCircuit;

public class BlifReader 
{
	
	private PrePackedCircuit circuit;
	
	public PrePackedCircuit readBlif(String fileName, int nbLutInputs) throws IOException
	{
		int lastIndexSlash = fileName.lastIndexOf('/');
		circuit = new PrePackedCircuit(nbLutInputs, fileName.substring(lastIndexSlash + 1));
		Path path = Paths.get(fileName);
		Vector<String> blackBoxNames = new Vector<>();
		Vector<Vector<String>> blackBoxInputs = new Vector<>();
		Vector<Vector<String>> blackBoxOutputs = new Vector<>();
		Vector<Boolean> blackBoxClocked = new Vector<>();
		Vector<Integer> blackBoxNbInstantiated = new Vector<>();
		int stopLineNb = findBlackBoxes(path, blackBoxNames, blackBoxInputs, blackBoxOutputs, blackBoxClocked, blackBoxNbInstantiated);
		try(BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
		{
			String line = null;
			int lineNumber = 0;
			while((line = reader.readLine()) != null && lineNumber <= stopLineNb)
			{
				while(line.endsWith("\\"))
				{
					line = line.substring(0, line.length() - 1);
					line = line + reader.readLine();
				}
				
				if(line != null)
				{
					String[] command = line.split(" +");
					if(command[0].length() > 0 && command[0].charAt(0) != '#')
					{
						switch(command[0])
						{
							case ".model":
								processModel(command);
								break;
							case ".inputs":
								processInputs(command);
								break;
							case ".outputs":
								// Disabled by Seppe:
								// I don't think nets should be added for outputs (or at least not
								// in the way they are added now)
								//processOutputs(command);
								break;
							case ".names":
								boolean succes = processNames(command, nbLutInputs);
								reader.mark(200);
								while(!reader.readLine().startsWith(".")) //skip truth table
								{
									reader.mark(200);
								}
								reader.reset(); //reset reader to last mark
								if(!succes)
								{
									return null;
								}
								break;
							case ".latch":
								processLatch(command);
								break;
							case ".subckt":
								processSubcircuit(command, blackBoxNames, blackBoxInputs, blackBoxOutputs, blackBoxClocked, blackBoxNbInstantiated);
								break;
							case ".end":
								break;
							default:
								System.out.println("Command " + command[0] + " is not yet supported in blifreader");
								break;
						}
					}
				}
				lineNumber++;
			}
		}
		return circuit;
	}
	
	private void processModel(String[] command)
	{
		//circuit.setName(command[1]);
	}
	
	private void processInputs(String[] command)
	{
		for(int i = 1; i < command.length; i++)
		{
			Input input = new Input(command[i]);
			circuit.addInput(input);
			if(!circuit.getNets().containsKey(input.name)) //net still needs to be added to the nets hashmap
			{
				circuit.getNets().put(input.name, new Net(input.name));
			}
			circuit.getNets().get(input.name).addSource(input.output);
		}
	}
	
	private void processOutputs(String[] command)
	{
		for(int i = 1; i < command.length; i++)
		{
			Output output = new Output(command[i]);
			circuit.addOutput(output);
			if(!circuit.getNets().containsKey(output.name)) //net still needs to be added to the nets hashmap
			{
				if(output.name.equals("p__cmx0ad_21")) {
					break;
				}
				circuit.getNets().put(output.name, new Net(output.name));
			}
			circuit.getNets().get(output.name).addSink(output.input);
		}
	}
	
	/*
	 * Returns false if failed, true if succeeded
	 */
	private boolean processNames(String[] command, int nbLutInputs)
	{
		if(command.length - 2 > nbLutInputs)
		{
			return false;
		}
		else
		{
			Lut lut;
			if(command.length == 2) //This is a source LUT which doesn't sink any nets but does source a net
			{
				if(command[1].contains("unconn") && command[1].length() == 6)
				{
					return true;
				}
				lut = new Lut(command[command.length - 1], 1, nbLutInputs, true);
			}
			else
			{
				lut = new Lut(command[command.length - 1], 1, nbLutInputs); //name of LUT = output of LUT
			}
			circuit.addLut(lut);
			
			// Take care of nets that are LUT inputs
			for(int i = 1; i <= command.length - 2; i++)
			{
				if(!circuit.getNets().containsKey(command[i])) //net still needs to be added to the nets hashmap
				{
					circuit.getNets().put(command[i], new Net(command[i]));
				}
				circuit.getNets().get(command[i]).addSink(lut.getInputs()[i-1]);
			}
				
			//Take care of net at the LUT output
			if(!circuit.getNets().containsKey(command[command.length - 1])) //net still needs to be added to the nets hashmap
			{
				circuit.getNets().put(command[command.length - 1], new Net(command[command.length - 1]));
			}
			circuit.getNets().get(command[command.length - 1]).addSource(lut.getOutputs()[0]);
			return true;
		}
	}
	
	private void processLatch(String[] command)
	{
		Flipflop.Type type;
		if(command.length >= 5)
		{
			switch(command[3])
			{
				case "fe":
					type = Flipflop.Type.FALLING_EDGE;
					break;
				case "re":
					type = Flipflop.Type.RISING_EDGE;
					break;
				case "ah":
					type = Flipflop.Type.ACTIVE_HIGH;
					break;
				case "al":
					type = Flipflop.Type.ACTIVE_LOW;
					break;
				case "as":
					type = Flipflop.Type.ASYNCHRONOUS;
					break;
				default:
					type = Flipflop.Type.UNSPECIFIED;
					break;
			}
		}
		else
		{
			type = Flipflop.Type.UNSPECIFIED;
		}
		
		Flipflop.InitVal initVal;
		int initValInt = 3;
		if(command.length == 4)
		{
			initValInt = Integer.parseInt(command[3]);
		}
		else
		{
			if(command.length == 6)
			{
				initValInt = Integer.parseInt(command[5]);
			}
		}
		switch(initValInt)
		{
			case 0:
				initVal = Flipflop.InitVal.ZERO;
				break;
			case 1:
				initVal = Flipflop.InitVal.ONE;
				break;
			case 2:
				initVal = Flipflop.InitVal.DONT_CARE;
				break;
			case 3:
				initVal = Flipflop.InitVal.UNKNOWN;
				break;
			default:
				initVal = Flipflop.InitVal.UNKNOWN;
				break;
		}
		
		Flipflop flipflop = new Flipflop(command[2], type, initVal);
		circuit.addFlipflop(flipflop); //name of flipflop = output of flipflop
		
		//take care of net at flipflop input
		if(!circuit.getNets().containsKey(command[1])) //net still needs to be added to the nets hashmap
		{
			circuit.getNets().put(command[1], new Net(command[1]));
		}
		circuit.getNets().get(command[1]).addSink(flipflop.getInput());
		
		//take care of net at flipflop output
		if(!circuit.getNets().containsKey(command[2])) //net still needs to be added to the nets hashmap
		{
			circuit.getNets().put(command[2], new Net(command[2]));
		}
		circuit.getNets().get(command[2]).addSource(flipflop.getOutput());
	}
	
	private void processSubcircuit(String[] command, Vector<String> blackBoxNames, Vector<Vector<String>> blackBoxInputs, 
									Vector<Vector<String>> blackBoxOutputs, Vector<Boolean> blackBoxClocked, 
									Vector<Integer> blackBoxNbInstantiated)
	{
		int index = -1;
		int counter = 0;
		for(String subcktName: blackBoxNames)
		{
			if(subcktName.contains(command[1]))
			{
				index = counter;
				break;
			}
			counter++;
		}
		if(index == -1)
		{
			System.err.println("Subcircuit " + command[1] + " was not found!");
			return;
		}
		HardBlock newHardBlock = new HardBlock(blackBoxNames.get(index) + "_" + blackBoxNbInstantiated.get(index), blackBoxOutputs.get(index), 
												blackBoxInputs.get(index), blackBoxNames.get(index), blackBoxClocked.get(index));
		blackBoxNbInstantiated.set(index, blackBoxNbInstantiated.get(index) + 1);
		circuit.addHardBlock(newHardBlock);
		
		int nbInputs = blackBoxInputs.get(index).size();
		int nbOutputs = blackBoxOutputs.get(index).size();
		
		//Take care of nets at subcircuit inputs
		for(int i = 2; i < 2 + nbInputs; i++)
		{
			int equalSignIndex = command[i].indexOf('=');
			String subcktInputName = command[i].substring(0, equalSignIndex);
			String netName = command[i].substring(equalSignIndex + 1);
			if(!subcktInputName.contains(blackBoxInputs.get(index).get(i - 2)))
			{
				System.err.println("Subcircuit input " + blackBoxInputs.get(index).get(i - 2) + " is not equal to " + subcktInputName);
				return;
			}
			if(!(netName.contains("unconn") && netName.length() == 6))
			{
				if(!circuit.getNets().containsKey(netName)) //net still needs to be added to the nets hashmap
				{
					circuit.getNets().put(netName, new Net(netName));
				}
				circuit.getNets().get(netName).addSink(newHardBlock.getInputs()[i-2]);
			}
		}
		
		//Take care of nets at subcircuit outputs
		int outputStartIndex = 2 + nbInputs;
		for(int i = outputStartIndex; i < outputStartIndex + nbOutputs; i++)
		{
			int equalSignIndex = command[i].indexOf('=');
			String subcktInputName = command[i].substring(0, equalSignIndex);
			String netName = command[i].substring(equalSignIndex + 1);
			if(!subcktInputName.contains(blackBoxOutputs.get(index).get(i - outputStartIndex)))
			{
				System.err.println("Subcircuit input " + blackBoxOutputs.get(index).get(i - outputStartIndex) + " is not equal to " + subcktInputName);
				return;
			}
			if(!(netName.contains("unconn") && netName.length() == 6))
			{
				if(!circuit.getNets().containsKey(netName)) //net still needs to be added to the nets hashmap
				{
					circuit.getNets().put(netName, new Net(netName));
				}
				circuit.getNets().get(netName).addSource(newHardBlock.getOutputs()[i-outputStartIndex]);
				newHardBlock.addOutputNetName(newHardBlock.getOutputs()[i-outputStartIndex], netName);
			}
		}
	}
	
	private int findBlackBoxes(Path path, Vector<String> blackBoxNames, Vector<Vector<String>> blackBoxInputs, Vector<Vector<String>> blackBoxOutputs, 
								Vector<Boolean> blackBoxClocked, Vector<Integer> blackBoxNbInstantiated) throws IOException
	{
		boolean insideModel = false;
		boolean firstEndPassed = false;
		int endLineNb = 0;
		String currentModelName = null;
		Vector<String> inputs = null;
		Vector<String> outputs = null;
		boolean clocked = false;
		try(BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
		{
			String line = null;
			while((line = reader.readLine()) != null)
			{
				while(line.endsWith("\\"))
				{
					line = line.substring(0, line.length() - 1);
					line = line + reader.readLine();
				}
				String[] command = line.split(" +");
				if(command[0].contains(".model"))
				{
					insideModel = true;
					currentModelName = command[1];
					inputs = new Vector<>();
					outputs = new Vector<>();
					clocked = false;
				}
				if(command[0].contains(".inputs"))
				{
					for(int i = 1; i < command.length; i++)
					{
						inputs.add(command[i]);
						if(command[i].contains("clk"))
						{
							clocked = true;
						}
					}
				}
				if(command[0].contains(".outputs"))
				{
					for(int i = 1; i < command.length; i++)
					{
						outputs.add(command[i]);
					}
				}
				if(command[0].contains(".blackbox"))
				{
					if(insideModel)
					{
						blackBoxNames.add(currentModelName);
						blackBoxInputs.add(inputs);
						blackBoxOutputs.add(outputs);
						blackBoxClocked.add(clocked);
						blackBoxNbInstantiated.add(0);
					}
				}
				if(command[0].contains(".end"))
				{
					insideModel = false;
					firstEndPassed = true;
					currentModelName = null;
					inputs = null;
					outputs = null;
					clocked = false;
				}
				if(command[0].contains(".names"))
				{
					reader.mark(200);
					while(!reader.readLine().startsWith(".")) //skip truth table
					{
						reader.mark(200);
					}
					reader.reset(); //reset reader to last mark
				}
				if(!firstEndPassed)
				{
					endLineNb++;
				}
			}
		}
		return endLineNb;
	}
	
}
