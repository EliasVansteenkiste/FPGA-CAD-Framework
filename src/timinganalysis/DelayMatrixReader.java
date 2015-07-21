package timinganalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DelayMatrixReader
{

	public static boolean readDelayMatrix(double[][] matrix, String fileName)
	{
		try
		{
			File delayFile = new File(fileName);
			if(!delayFile.exists())
			{
				System.err.println("No delay file found!");
				return false;
			}
	    	FileReader fileReader = new FileReader(delayFile.getAbsoluteFile());
	    	BufferedReader bufferedReader = new BufferedReader(fileReader);
	    	String line = bufferedReader.readLine();
	    	int yCounter = 0;
	    	while(line != null)
	    	{
	    		String[] lineParts = line.trim().split(" +");
	    		if(lineParts.length == matrix.length)
	    		{
	    			for(int xCounter = 0; xCounter < lineParts.length; xCounter++)
	    			{
	    				matrix[xCounter][yCounter] = Double.parseDouble(lineParts[xCounter]);
	    			}
	    			line = bufferedReader.readLine();
	    			yCounter++;
	    		}
	    		else
	    		{
	    			System.err.println("The matrix dimensions don't coincide with the matrix found in the file!");
	    			bufferedReader.close();
	    			return false;
	    		}
	    	}
	    	bufferedReader.close();
		}
		catch(IOException ioe)
		{
			System.err.println("Couldn't read delay file");
			return false;
		}
		
		return true;
	}
	
}
