package architecture.circuit.parser;

import java.io.File;

import util.Logger;

public class Util {
    
    public static File getArchitectureFile(File folder) {
        
        File[] files = folder.listFiles();
        File architectureFile = null;
        
        for(File file : files) {
            String path = file.getAbsolutePath();
            if(path.substring(path.length() - 4).equals(".xml")) {
                if(architectureFile != null) {
                    Logger.raise("Multiple architecture files found in the input folder");
                }
                architectureFile = file;
            }
        }
        
        if(architectureFile == null) {
            Logger.raise("No architecture file found in the inputfolder");
        }
        
        return architectureFile;
    }
}
