package mathtools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FastCrs {

    private int numRows;
    private List<List<FastCrsTuple>> values;

    private List<Integer> indexArray, rowPointers;
    private List<Double> valueArray;

    public FastCrs(int numRows) {
        this.numRows = numRows;
        this.values = new ArrayList<>(numRows);
        for(int i = 0; i < numRows; i++) {
            this.values.add(new ArrayList<FastCrsTuple>());
        }
    }



    public void addElement(int row, int column, double value) {
        FastCrsTuple entry = new FastCrsTuple(column, value);
        this.values.get(row).add(entry);
    }

    public void prepareArrays() {
        this.indexArray = new ArrayList<>();
        this.valueArray = new ArrayList<>();
        this.rowPointers = new ArrayList<>(this.numRows + 1);

        int totalIndex = 0;
        for(int rowIndex = 0; rowIndex < this.numRows; rowIndex++) {
            this.rowPointers.add(totalIndex);
            List<FastCrsTuple> row = this.values.get(rowIndex);
            Collections.sort(row);


            FastCrsTuple initialEntry = row.get(0);
            int previousColumnIndex = initialEntry.getIndex();
            double summedValue = initialEntry.getValue();

            int numValues = row.size();
            int valueIndex = 1;
            while(valueIndex < numValues) {
                FastCrsTuple entry = row.get(valueIndex);
                int columnIndex = entry.getIndex();
                double value = entry.getValue();

                if(columnIndex == previousColumnIndex) {
                    summedValue += value;

                } else {
                    this.indexArray.add(previousColumnIndex);
                    this.valueArray.add(summedValue);
                    totalIndex++;
                    summedValue = value;
                }

                previousColumnIndex = columnIndex;
                valueIndex++;
            }

            this.indexArray.add(previousColumnIndex);
            this.valueArray.add(summedValue);
            totalIndex++;
        }

        this.rowPointers.add(totalIndex);
    }

    public List<Integer> getColInd() {
        return this.indexArray;
    }
    public List<Double> getVal() {
        return this.valueArray;
    }
    public List<Integer> getRowPointers() {
        return this.rowPointers;
    }
}
