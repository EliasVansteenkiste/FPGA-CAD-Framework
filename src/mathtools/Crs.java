package mathtools;

import java.util.ArrayList;

public class Crs
{

	private ArrayList<ArrayList<Double>> values;
	private ArrayList<ArrayList<Integer>> col_ind;
	private int totalNbOfElements;
	
	public Crs(int nbRows)
	{
		totalNbOfElements = 0;
		values = new ArrayList<>(nbRows);
		col_ind = new ArrayList<>(nbRows);
		for(int i = 0; i < nbRows; i++)
		{
			values.add(new ArrayList<Double>());
			col_ind.add(new ArrayList<Integer>());
		}
	}
	
	/*
	 * Set element at row 'row' (stating from 0) and column 'col' (starting from 0)
	 */
	public void setElement(int row, int col, double value)
	{
		ArrayList<Double> valuesRow = values.get(row);
		ArrayList<Integer> colIndRow = col_ind.get(row);
		boolean added = false;
		for(int counter = 0; counter < valuesRow.size(); counter++)
		{
			if(colIndRow.get(counter) == col)
			{
				valuesRow.set(counter, value);
				added = true;
				break;
			}
			if(colIndRow.get(counter) > col)
			{
				valuesRow.add(counter, value);
				colIndRow.add(counter, col);
				totalNbOfElements++;
				added = true;
				break;
			}
		}
		if(!added)
		{
			valuesRow.add(value);
			colIndRow.add(col);
			totalNbOfElements++;
		}
	}
	
	/*
	 * Get element at row 'row' (stating from 0) and column 'col' (starting from 0)
	 */
	public double getElement(int row, int col)
	{
		double toReturn = 0.0;
		ArrayList<Double> valuesRow = values.get(row);
		ArrayList<Integer> colIndRow = col_ind.get(row);
		for(int counter = 0; counter < valuesRow.size(); counter++)
		{
			if(colIndRow.get(counter) == col)
			{
				toReturn = valuesRow.get(counter);
				break;
			}
			if(colIndRow.get(counter) > col)
			{
				break;
			}
		}
		return toReturn;
	}
	
	public double[] getVal()
	{
		double[] toReturn = new double[totalNbOfElements];
		int counter = 0;
		for(int i = 0; i < values.size(); i++)
		{
			ArrayList<Double> valuesRow = values.get(i);
			for(int j = 0; j < valuesRow.size(); j++)
			{
				toReturn[counter++] = valuesRow.get(j);
			}
		}
		return toReturn;
	}
	
	public int[] getCol_ind()
	{
		int[] toReturn = new int[totalNbOfElements];
		int counter = 0;
		for(int i = 0; i < col_ind.size(); i++)
		{
			ArrayList<Integer> colIndRow = col_ind.get(i);
			for(int j = 0; j < colIndRow.size(); j++)
			{
				toReturn[counter++] = colIndRow.get(j);
			}
		}
		return toReturn;
	}
	
	public int[] getRow_ptr()
	{
		int[] toReturn = new int[values.size()+1];
		int counter = 0;
		toReturn[0] = 0;
		for(int i = 0; i < values.size(); i++)
		{
			counter += values.get(i).size();
			toReturn[i+1] = counter;
		}
		return toReturn;
	}
	
	public boolean isSymmetricalAndFinite()
	{
		for(int i = 0; i < values.size(); i++)
		{
			for(int j = i+1; j < values.get(i).size(); j++)
			{
				double element = getElement(i, j);
				if(element != getElement(j, i) || Double.isNaN(element))
				{
					return false;
				}
			}
		}
		return true;
	}
	
}
