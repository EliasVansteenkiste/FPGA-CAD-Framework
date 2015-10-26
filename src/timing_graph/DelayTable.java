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

public class DelayTable {

    private String type;
    private BufferedReader reader;

    private List<List<Double>> delays = new ArrayList<>();

    
    DelayTable(File delayFile, String type) {
        try {
            this.reader = new BufferedReader(new FileReader(delayFile));
        } catch (FileNotFoundException error) {
            Logger.raise("Delays file not found: " + delayFile, error);
        }
        
        this.type = type;
    }

    public void parse() {
        boolean lineFound = false;
        try {
            lineFound = this.findStartingLine();
        } catch(IOException error) {
            Logger.raise("Could not read from delays file", error);
        }
            
        if (!lineFound) {
            Logger.raise("Faild to find type in delays file: " + this.type);
        }
        
        try {
            this.readMatrix();
        } catch (IOException error) {
            Logger.raise("Could not read from delays file", error);
        }
    }

    private boolean findStartingLine() throws IOException {
        String lineToFind = "printing delta_" + this.type;
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

    private void readMatrix() throws IOException {
        String line;
        int y = 0;
        
        while ((line = this.reader.readLine()) != null) {
            if (line.length() == 0) {
                Collections.reverse(this.delays);
                return;
            }
            
            this.delays.add(new ArrayList<Double>());
            
            String[] lineDelayStrings = line.split("\\s+");
            for(int i = 1; i < lineDelayStrings.length; i++) {
                Double lineDelay = Double.parseDouble(lineDelayStrings[i]);
                this.delays.get(y).add(lineDelay);
            }
            
            y++;
        }
    }
}
