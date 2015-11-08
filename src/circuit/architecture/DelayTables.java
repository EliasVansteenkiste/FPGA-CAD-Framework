package circuit.architecture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import util.Logger;

public class DelayTables implements Serializable {

    private static final long serialVersionUID = 3516264508719006250L;

    private static DelayTables instance = new DelayTables();
    public static DelayTables getInstance() {
        return DelayTables.instance;
    }
    static void setInstance(DelayTables instance) {
        DelayTables.instance = instance;
    }

    private List<List<Double>>
            ioToIo = new ArrayList<>(),
            ioToClb = new ArrayList<>(),
            clbToIo = new ArrayList<>(),
            clbToClb = new ArrayList<>();


    public void parse(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException error) {
            Logger.raise("Delays file not found: " + file, error);
        }

        try {
            this.parseType(reader, "clb_to_clb", this.clbToClb);
            this.parseType(reader, "io_to_clb", this.ioToClb);
            this.parseType(reader, "clb_to_io", this.clbToIo);
            this.parseType(reader, "io_to_io", this.ioToIo);

        } catch(IOException error) {
            Logger.raise("Failed to read from delays file: " + file, error);
        }
    }

    private void parseType(BufferedReader reader, String type, List<List<Double>> matrix) throws IOException {

        boolean lineFound = this.findStartingLine(reader, type);

        if (!lineFound) {
            Logger.raise("Faild to find type in delays file: " + type);
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


    public double getDelay(BlockCategory fromCategory, BlockCategory toCategory, int deltaX, int deltaY) {
        if(deltaX == 0 && deltaY == 0) {
            return 0;
        }

        List<List<Double>> matrix;

        if(fromCategory == BlockCategory.IO) {
            if(toCategory == BlockCategory.IO) {
                matrix = this.ioToIo;
            } else {
                matrix = this.ioToClb;
            }
        } else {
            if(toCategory == BlockCategory.IO) {
                matrix = this.clbToIo;
            } else {
                matrix = this.clbToClb;
            }
        }


        double delay = matrix.get(deltaY).get(deltaX);
        if(delay <= 0) {
            Logger.raise(String.format("Negative wire delay: (%d, %d)", deltaX, deltaY));
        }

        return delay;
    }

    public double getIoToIo(int x, int y) {
        return this.ioToIo.get(x).get(y);
    }
    public double getIoToClb(int x, int y) {
        return this.ioToClb.get(x).get(y);
    }
    public double getClbToIo(int x, int y) {
        return this.clbToIo.get(x).get(y);
    }
    public double getClbToClb(int x, int y) {
        return this.clbToClb.get(x).get(y);
    }
}
