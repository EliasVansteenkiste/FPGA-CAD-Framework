package place.circuit.architecture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import place.circuit.exceptions.InvalidFileFormatException;

public class DelayTables implements Serializable {

    private static final long serialVersionUID = 3516264508719006250L;

    private boolean dummyTables = false;
    private transient File file;
    private List<List<Double>>
            ioToIo = new ArrayList<>(),
            ioToClb = new ArrayList<>(),
            clbToIo = new ArrayList<>(),
            clbToClb = new ArrayList<>();

    public DelayTables() {
        this.dummyTables = true;
    }

    public DelayTables(File file) {
        this.file = file;
    }

    public void parse() throws IOException, InvalidFileFormatException {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(this.file));

        this.parseType(reader, "clb_to_clb", this.clbToClb);
        this.parseType(reader, "io_to_clb", this.ioToClb);
        this.parseType(reader, "clb_to_io", this.clbToIo);
        this.parseType(reader, "io_to_io", this.ioToIo);

        reader.close();
    }

    private void parseType(BufferedReader reader, String type, List<List<Double>> matrix) throws IOException, InvalidFileFormatException {

        boolean lineFound = this.findStartingLine(reader, type);

        if (!lineFound) {
            throw new InvalidFileFormatException("Type not found in delays file: " + type);
        }

        this.readMatrix(reader, matrix);
    }

    private boolean findStartingLine(BufferedReader reader, String type) throws IOException {
        String lineToFind = "printing delta_" + type;
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.equals(lineToFind)) {
                reader.readLine();
                reader.readLine();
                reader.readLine();

                return true;
            }
        }

        return false;
    }

    private void readMatrix(BufferedReader reader, List<List<Double>> matrix) throws IOException {
        String line;
        int y = 0;

        while ((line = reader.readLine()) != null) {
            if (line.length() == 0) {
                Collections.reverse(matrix);
                return;
            }

            matrix.add(new ArrayList<Double>());

            String[] lineDelayStrings = line.split("\\s+");
            for(int i = 1; i < lineDelayStrings.length; i++) {
                Double lineDelay = Double.parseDouble(lineDelayStrings[i]);
                matrix.get(y).add(lineDelay);
            }

            y++;
        }
    }


    private List<List<Double>> getTable(BlockCategory fromCategory, BlockCategory toCategory) {
        if(fromCategory == BlockCategory.IO) {
            if(toCategory == BlockCategory.IO) {
                return this.ioToIo;
            } else {
                return this.ioToClb;
            }
        } else {
            if(toCategory == BlockCategory.IO) {
                return this.clbToIo;
            } else {
                return this.clbToClb;
            }
        }
    }

    public double getDelay(BlockCategory fromCategory, BlockCategory toCategory, int deltaX, int deltaY) {
        if(deltaX == 0 && deltaY == 0 || this.dummyTables) {
            return 0;
        }

        return this.getTable(fromCategory, toCategory).get(deltaY).get(deltaX);
    }
}
