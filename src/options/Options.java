package options;

import interfaces.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;


public class Options {

    public enum StartingStage {NET, PLACE};

    private Logger logger;

    private String circuitName;
    private StartingStage startingStage;

    private File architectureFile;
    private File architectureFileVPR;

    private File blifFile;
    private File netFile;
    private File inputPlaceFile;
    private File outputPlaceFile;

    private boolean visual;

    private int numPlacers;
    private List<String> placers;
    private List<Map<String, String>> placerOptions;


    Options(Logger logger) {
        this.logger = logger;
        this.startingStage = StartingStage.NET;
    }

    private void checkFileExistence(String prefix, File file) {
        if(!file.exists()) {
            this.logger.raise(prefix + " file not found: " + file);

        } else if(file.isDirectory()) {
            this.logger.raise(prefix + " file is a directory:" + file);
        }
    }


    void setCircuitName(String name) {
        this.circuitName = name;
    }
    public String getCircuitName() {
        return this.circuitName;
    }


    void setBlifFile(String path) {
        this.setBlifFile(new File(path));
    }
    void setBlifFile(File file) {
        this.blifFile = file;
        this.checkFileExistence("Blif", this.blifFile);
    }
    public File getBlifFile() {
        return this.blifFile;
    }

    void setNetFile(String path) {
        this.setNetFile(new File(path));
    }
    void setNetFile(File file) {
        this.netFile = file;
        this.checkFileExistence("Net", this.netFile);
    }
    public File getNetFile() {
        return this.netFile;
    }

    void setInputPlaceFile(String path) {
        this.setInputPlaceFile(new File(path));
    }
    void setInputPlaceFile(File file) {
        this.startingStage = StartingStage.PLACE;
        this.inputPlaceFile = file;
        this.checkFileExistence("Input place", this.inputPlaceFile);
    }
    public File getInputPlaceFile() {
        return this.inputPlaceFile;
    }

    void setOutputPlaceFile(String path) {
        this.setOutputPlaceFile(new File(path));
    }
    void setOutputPlaceFile(File file) {
        this.outputPlaceFile = file;
    }
    public File getOutputPlaceFile() {
        return this.outputPlaceFile;
    }

    public StartingStage getStartingStage() {
        return this.startingStage;
    }


    void setArchitectureFile(String path) {
        this.setArchitectureFile(new File(path));
    }
    void setArchitectureFile(File file) {
        this.architectureFile = file;
        this.checkFileExistence("Architecture", this.architectureFile);
    }
    public File getArchitectureFile() {
        return this.architectureFile;
    }


    void guessArchitectureFileVPR() {
        File[] files = this.netFile.getParentFile().listFiles();
        File architectureFile = null;

        for(File file : files) {
            String path = file.getAbsolutePath();
            if(path.substring(path.length() - 4).equals(".xml")) {
                if(architectureFile != null) {
                    this.logger.raise("Multiple architecture files found in the input folder");
                }
                architectureFile = file;
            }
        }

        if(architectureFile == null) {
            this.logger.raise("No architecture file found in the inputfolder");
        }

        this.setArchitectureFileVPR(architectureFile);
    }

    void setArchitectureFileVPR(String path) {
        this.setArchitectureFileVPR(new File(path));
    }
    void setArchitectureFileVPR(File file) {
        this.architectureFileVPR = file;
        this.checkFileExistence("Architecture VPR", this.architectureFileVPR);
    }
    public File getArchitectureFileVPR() {
        return this.architectureFileVPR;
    }

    void setVisual(boolean visual) {
        this.visual = visual;
    }
    public boolean getVisual() {
        return this.visual;
    }


    void setPlacers(List<String> placers, List<Map<String, String>> placerOptions) {
        int numPlacers = placers.size();
        int numPlacerOptions = placerOptions.size();
        if(numPlacers != numPlacerOptions) {
            this.logger.raise(String.format(
                    "The number op placer options (%d) is not equal to the number of placers (%d",
                    numPlacerOptions, numPlacers));
        }

        this.numPlacers = numPlacers;
        this.placers = placers;
        this.placerOptions = placerOptions;
    }

    public int getNumPlacers() {
        return this.numPlacers;
    }
    public String getPlacer(int index) {
        return this.placers.get(index);
    }
    public Map<String, String> getPlacerOptions(int index) {
        return this.placerOptions.get(index);
    }
}
