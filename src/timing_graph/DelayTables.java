package timing_graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import util.Logger;

public class DelayTables {

    private BufferedReader reader;

    private List<List<Double>>
            ioToIo = new ArrayList<>(),
            ioToClb = new ArrayList<>(),
            clbToIo = new ArrayList<>(),
            clbToClb = new ArrayList<>();

    
    DelayTables(File delayFile) {
        try {
            this.reader = new BufferedReader(new FileReader(delayFile));
        } catch (FileNotFoundException error) {
            Logger.raise("Delays file not found: " + delayFile, error);
        }
    }

    public void parse() {
        this.parseType("clb_to_clb", this.clbToClb);
        this.parseType("io_to_clb", this.ioToClb);
        this.parseType("clb_to_io", this.clbToIo);
        this.parseType("io_to_io", this.ioToIo);
    }
    
    private void parseType(String type, List<List<Double>> matrix) {
        
        boolean lineFound = false;
        try {
            lineFound = this.findStartingLine(type);
        } catch(IOException error) {
            Logger.raise("Could not read from delays file", error);
        }
            
        if (!lineFound) {
            Logger.raise("Faild to find type in delays file: " + type);
        }
        
        try {
            this.readMatrix(matrix);
        } catch (IOException error) {
            Logger.raise("Could not read from delays file", error);
        }
    }

    private boolean findStartingLine(String type) throws IOException {
        String lineToFind = "printing delta_" + type;
        String line;

        while ((line = this.reader.readLine()) != null) {
            if (line.equals(lineToFind)) {
                this.reader.readLine();
                this.reader.readLine();
                this.reader.readLine();
                
                return true;
            }
        }

        return false;
    }

    private void readMatrix(List<List<Double>> matrix) throws IOException {
        String line;
        int y = 0;
        
        while ((line = this.reader.readLine()) != null) {
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
    
    
    double getIoToIo(int x, int y) {
        return this.ioToIo.get(x).get(y);
    }
    double getIoToClb(int x, int y) {
        return this.ioToClb.get(x).get(y);
    }
    double getClbToIo(int x, int y) {
        return this.clbToIo.get(x).get(y);
    }
    double getClbToClb(int x, int y) {
        return this.clbToClb.get(x).get(y);
    }
}
