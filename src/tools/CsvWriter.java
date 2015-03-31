package tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvWriter
{
	
	private int nbColumns;
	List<List<String>> data;
	
	public CsvWriter(int nbColumns)
	{
		this.nbColumns = nbColumns;
		data = new ArrayList<List<String>>(nbColumns);
		for(int i = 0; i < nbColumns; i++)
		{
			data.add(i, new ArrayList<String>());
		}
	}
	
	public CsvWriter(List<List<String>> data, int nbColumns)
	{
		this.data = data;
		this.nbColumns = nbColumns;
	}
		
	public boolean addRow(String[] rowData)
	{
		boolean toReturn;
		if(rowData.length != nbColumns)
		{
			toReturn = false;
		}
		else
		{
			toReturn = true;
			for(int i = 0; i < nbColumns; i++)
			{
				data.get(i).add(rowData[i].toString());
			}
		}
		return toReturn;
	}
	
	public void writeFile(String fileName)
	{
		try
		{
			File csvFile = new File(fileName);
			if(!csvFile.exists())
			{
				csvFile.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(csvFile.getAbsoluteFile());
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write(this.toString());
			bufferedWriter.close();
		}
		catch(IOException ioe)
		{
			System.err.println("Couldn't write csv file: " + fileName);
		}
	}
	
	@Override
	public String toString()
	{
		String toReturn = "";
		int nbRows = data.get(0).size();
		for(int i = 0; i < nbRows; i++)
		{
			for(int j = 0; j < nbColumns; j++)
			{
				if(j == nbColumns-1)
				{
					toReturn += data.get(j).get(i);
				}
				else
				{
					toReturn += data.get(j).get(i) + ";";
				}
				
			}
			if(i < nbRows - 1)
			{
				toReturn += "\n";
			}
		}
		return toReturn;
	}
	
}
