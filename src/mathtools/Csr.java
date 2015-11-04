package mathtools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Csr {

    private int numRows;
    private List<Double> selfValues;
    private List<List<CsrTuple>> values;

    private ArrayList<Integer> indexArray, rowPointers;
    private ArrayList<Double> valueArray;

    public Csr(int numRows) {
        this.numRows = numRows;

        this.selfValues = new ArrayList<>(numRows);
        this.values = new ArrayList<>(numRows);
        for(int i = 0; i < numRows; i++) {
            this.selfValues.add(0.0);
            this.values.add(new ArrayList<CsrTuple>());
        }
    }



    public void addElement(int row, int column, double value) {
        if(row == column) {
            this.selfValues.set(row, value + this.selfValues.get(row));

        } else {
            CsrTuple entry = new CsrTuple(column, value);
            this.values.get(row).add(entry);
        }
    }

    public void prepareArrays() {
        this.indexArray = new ArrayList<>(10 * this.numRows);
        this.valueArray = new ArrayList<>(10 * this.numRows);
        this.rowPointers = new ArrayList<>(this.numRows + 1);

        int totalIndex = 0;
        for(int rowIndex = 0; rowIndex < this.numRows; rowIndex++) {
            this.rowPointers.add(totalIndex);

            this.indexArray.add(rowIndex);
            this.valueArray.add(this.selfValues.get(rowIndex));
            totalIndex++;


            List<CsrTuple> row = this.values.get(rowIndex);
            Collections.sort(row);

            CsrTuple initialEntry = row.get(0);
            int previousColumnIndex = initialEntry.getIndex();
            double summedValue = initialEntry.getValue();

            int numValues = row.size();
            int valueIndex = 1;
            while(valueIndex < numValues) {
                CsrTuple entry = row.get(valueIndex);
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


        this.indexArray.trimToSize();
        this.valueArray.trimToSize();
    }

    public List<Integer> getColumnIndexes() {
        return this.indexArray;
    }
    public List<Double> getVal() {
        return this.valueArray;
    }
    public List<Integer> getRowPointers() {
        return this.rowPointers;
    }
}
