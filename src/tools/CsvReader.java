package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvReader
{
	
	private List<List<String>> data;
	private int nbColumns;
	
	/*
	 * Reads csv file to internal data structures
	 * Returns true if succeeded, false if not succeeded
	 */
	public boolean readFile(String fileName)
	{
		String[] rows;
		try
		{
			File csvFile = new File(fileName);
			if(!csvFile.exists())
			{
				return false;
			}
			FileReader fileReader = new FileReader(csvFile.getAbsoluteFile());
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			ArrayList<String> rowsList = new ArrayList<>();
			String curLine = bufferedReader.readLine();
			int nbRows = 0;
			while(curLine != null)
			{
				rowsList.add(curLine);
				nbRows++;
				curLine = bufferedReader.readLine();
			}
			bufferedReader.close();
			rows = new String[nbRows];
			rowsList.toArray(rows);
		}
		catch(IOException ioe)
		{
			System.err.println("Couldn't write csv file: " + fileName);
			return false;
		}
		nbColumns = 1;
		int curSemiColonIndex = rows[0].indexOf(';', 0);
		while(curSemiColonIndex != -1)
		{
			nbColumns++;
			curSemiColonIndex = rows[0].indexOf(';', curSemiColonIndex + 1);
		}
		data = new ArrayList<List<String>>(nbColumns);
		for(int i = 0; i < nbColumns; i++)
		{
			data.add(i, new ArrayList<String>());
		}
		for(int i = 0; i < rows.length; i++)
		{
			int startIndex = 0;
			int endIndex = rows[i].indexOf(';', 0);
			for(int j = 0; j < nbColumns; j++)
			{
				data.get(j).add(rows[i].substring(startIndex, endIndex));
				startIndex = endIndex + 1;
				endIndex = rows[i].indexOf(';', endIndex + 1);
				if(endIndex < 0)
				{
					endIndex = rows[i].length();
				}
			}
		}
		return true;
	}
	
	public List<List<String>> getData()
	{
		return data;
	}
	
	public int getNbColumns()
	{
		return nbColumns;
	}
	
	public int getNbRows()
	{
		return data.get(0).size();
	}
	
	public String[] getColumn(int column, int startRow, int endRow)
	{
		if(column >= nbColumns)
		{
			return null;
		}
		int nbRows = data.get(0).size();
		if(startRow < 0 || endRow >= nbRows)
		{
			return null;
		}
		String[] toReturn = new String[endRow - startRow + 1];
		for(int i = startRow; i <= endRow; i++)
		{
			toReturn[i-startRow] = data.get(column).get(i);
		}
		return toReturn;
	}
	
}
