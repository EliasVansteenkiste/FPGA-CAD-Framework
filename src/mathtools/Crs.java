package mathtools;

import java.util.ArrayList;

/*
 * Represents a matrix in the CRS sparse matrix format
 */
public class Crs 
{

	private ArrayList<Double> val;
	private ArrayList<Integer> col_ind;
	private int[] row_ptr;
	
	public Crs(int nbRows)
	{
		val = new ArrayList<Double>();
		col_ind = new ArrayList<Integer>();
		row_ptr = new int[nbRows + 1];
		for(int i = 0; i < nbRows; i++)
		{
			row_ptr[i] = 0;
		}
		row_ptr[nbRows] = 1;
	}
	
	/*
	 * Set element at row i (stating from 0) and column j (starting from 0)
	 */
	public void setElement(int i, int j, double value)
	{
		int nextRowPointer = i+1;
		int nextRowStartIndex = 0;
		while(nextRowPointer < row_ptr.length)
		{
			if(row_ptr[nextRowPointer] != 0)
			{
				nextRowStartIndex = row_ptr[nextRowPointer] - 1;
				break;
			}
			nextRowPointer++;
		}
		if(row_ptr[i] == 0) //All elements in the row are zero
		{
			val.add(nextRowStartIndex, value);
			col_ind.add(nextRowStartIndex, j+1);
			row_ptr[i] = nextRowStartIndex + 1;
			for(int index = i+1; index < row_ptr.length; index++)
			{
				if(row_ptr[index] != 0)
				{
					row_ptr[index] += 1;
				}
			}
		}
		else //There are already nonzero elements in the matrix
		{
			int rowStartIndex = row_ptr[i] - 1;
			int columnIndex;
			for(columnIndex = rowStartIndex; columnIndex < nextRowStartIndex; columnIndex++)
			{
				if(col_ind.get(columnIndex) - 1 >= j)
				{
					break;
				}
			}
			if(col_ind.get(columnIndex) - 1 == j) //The element was already in the matrix ==> only change the value
			{
				val.set(columnIndex, value);
			}
			else
			{
				val.add(columnIndex, value);
				col_ind.add(columnIndex, j+1);
				for(int index = i+1; index < row_ptr.length; index++)
				{
					if(row_ptr[index] != 0)
					{
						row_ptr[index] += 1;
					}
				}
			}
		}
	}
	
	/*
	 * Get element at row i (stating from 0) and column j (starting from 0)
	 */
	public double getElement(int i, int j)
	{
		if(row_ptr[i] == 0) //All elements in the row are zero
		{
			return 0.0;
		}
		int rowStartIndex = row_ptr[i] - 1;
		int nextRowPointer = i+1;
		while(nextRowPointer < row_ptr.length)
		{
			if(row_ptr[nextRowPointer] != 0)
			{
				break;
			}
			nextRowPointer++;
		}
		int rowEndIndex = row_ptr[nextRowPointer] - 1;
		for(int index = rowStartIndex; index < rowEndIndex; index++)
		{
			if(col_ind.get(index) == j + 1)
			{
				return val.get(index);
			}
		}
		return 0.0;
	}

	public double[] getVal() 
	{
		double[] toReturn = new double[val.size()];
		for(int i = 0; i < val.size(); i++)
		{
			toReturn[i] = val.get(i);
		}
		return toReturn;
	}

	public int[] getCol_ind() 
	{
		int[] toReturn = new int[col_ind.size()];
		for(int i = 0; i < col_ind.size(); i++)
		{
			toReturn[i] = col_ind.get(i);
		}
		return toReturn;
	}

	public int[] getRow_ptr() 
	{
		return row_ptr;
	}
	
}
