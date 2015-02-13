package prepackedcircuit.parser.blif;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import circuit.Input;
import circuit.Net;
import circuit.Output;

import prepackedcircuit.Flipflop;
import prepackedcircuit.Lut;
import prepackedcircuit.PrePackedCircuit;

public class BlifReader 
{
	
	private PrePackedCircuit circuit;
	
	public PrePackedCircuit readBlif(String fileName) throws IOException
	{
		circuit = new PrePackedCircuit();
		//Map<String,Pin> sourcePins = new HashMap<String,Pin>(); // net name, source pin of the net
		//Map<String,Vector<Pin>> sinkPins = new HashMap<String,Vector<Pin>>(); // net name, sink pins of the net
		Path path = Paths.get(fileName);
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
				
				if(line != null)
				{
					String[] command = line.split(" +");
					switch(command[0])
					{
						case ".model":
							processModel(command);
							break;
						case ".inputs":
							processInputs(command);
							break;
						case ".outputs":
							processOutputs(command);
							break;
						case ".names":
							processNames(command);
							reader.mark(200);
							while(!reader.readLine().startsWith(".")) //skip truth table
							{
								reader.mark(200);
							}
							reader.reset(); //reset reader to last mark
							break;
						case ".latch":
							processLatch(command);
							break;
						case ".end":
							break;
						default:
							System.out.println("Command " + command[0] + " is not yet supported in blifreader");
							break;
					}
				}
				
			}
		}
		return circuit;
	}
	
	private void processModel(String[] command)
	{
		circuit.setName(command[1]);
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
				circuit.getNets().put(output.name, new Net(output.name));
			}
			circuit.getNets().get(output.name).addSink(output.input);
		}
	}
	
	private void processNames(String[] command)
	{
		Lut lut = new Lut(command[command.length - 1], 1, command.length - 2); //name of LUT = output of LUT
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
	
}
