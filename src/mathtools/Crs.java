package mathtools;

import java.util.ArrayList;

public class Crs {

    private ArrayList<ArrayList<Double>> values;
    private ArrayList<ArrayList<Integer>> colInd;
    private int numElements;
    
    public Crs(int nbRows) {
        this.numElements = 0;
        this.values = new ArrayList<>(nbRows);
        this.colInd = new ArrayList<>(nbRows);
        
        for(int i = 0; i < nbRows; i++) {
            this.values.add(new ArrayList<Double>());
            this.colInd.add(new ArrayList<Integer>());
        }
    }
    
    /*
     * Set element at row 'row' (stating from 0) and column 'col' (starting from 0)
     */
    public void setElement(int row, int col, double value) {
        ArrayList<Double> valuesRow = this.values.get(row);
        ArrayList<Integer> colIndRow = this.colInd.get(row);
        boolean added = false;
        
        for(int counter = 0; counter < valuesRow.size(); counter++) {
            
            if(colIndRow.get(counter) == col) {
                valuesRow.set(counter, value);
                added = true;
                break;
            }
            
            if(colIndRow.get(counter) > col) {
                valuesRow.add(counter, value);
                colIndRow.add(counter, col);
                this.numElements++;
                added = true;
                break;
            }
        }
        
        if(!added) {
            valuesRow.add(value);
            colIndRow.add(col);
            this.numElements++;
        }
    }
    
    /*
     * Get element at row 'row' (starting from 0) and column 'col' (starting from 0)
     */
    public double getElement(int row, int col) {
        double toReturn = 0.0;
        ArrayList<Double> valuesRow = this.values.get(row);
        ArrayList<Integer> colIndRow = this.colInd.get(row);
        
        for(int counter = 0; counter < valuesRow.size(); counter++) {
            if(colIndRow.get(counter) == col) {
                toReturn = valuesRow.get(counter);
                break;
            }
            
            if(colIndRow.get(counter) > col) {
                break;
            }
        }
        
        return toReturn;
    }
    
    public double[] getVal() {
        double[] toReturn = new double[this.numElements];
        int counter = 0;
        
        for(int i = 0; i < this.values.size(); i++) {
            ArrayList<Double> valuesRow = this.values.get(i);
            for(int j = 0; j < valuesRow.size(); j++) {
                toReturn[counter++] = valuesRow.get(j);
            }
        }
        
        return toReturn;
    }
    
    public int[] getColInd() {
        int[] toReturn = new int[this.numElements];
        int counter = 0;
        
        for(int i = 0; i < this.colInd.size(); i++) {
            ArrayList<Integer> colIndRow = this.colInd.get(i);
            for(int j = 0; j < colIndRow.size(); j++) {
                toReturn[counter++] = colIndRow.get(j);
            }
        }
        
        return toReturn;
    }
    
    public int[] getRow_ptr() {
        int[] toReturn = new int[this.values.size()+1];
        int counter = 0;
        toReturn[0] = 0;
        for(int i = 0; i < this.values.size(); i++) {
            counter += this.values.get(i).size();
            toReturn[i+1] = counter;
        }
        return toReturn;
    }
    
    public boolean isSymmetricalAndFinite() {
        for(int i = 0; i < this.values.size(); i++) {
            for(int j = i+1; j < this.values.get(i).size(); j++) {
                double element = getElement(i, j);
                if(element != getElement(j, i) || Double.isNaN(element)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public int getMemoryUsageDouble() {
        int doubleMemory = 0;
        for(ArrayList<Double> dbVector : this.values) {
            doubleMemory += dbVector.size() * 8;
        }
        
        int intMemory = 0;
        for(ArrayList<Integer> intVector : this.colInd) {
            intMemory += intVector.size() * 4;
        }
        
        return doubleMemory + intMemory;
    }
    
    public int getMemoryUsageFloat() {
        int floatMemory = 0;
        for(ArrayList<Double> dbVector : this.values) {
            floatMemory += dbVector.size() * 4;
        }
        
        int intMemory = 0;
        for(ArrayList<Integer> intVector : this.colInd) {
            intMemory += intVector.size() * 4;
        }
        
        return floatMemory + intMemory;
    }
}
