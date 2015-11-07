package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import util.Logger;
import circuit.Circuit;

class CircuitCacher {

    private String circuitName;
    private File netFile;

    CircuitCacher(String circuitName, File netFile) {
        this.netFile = netFile;
        this.circuitName = circuitName;
    }

    void store(Circuit circuit) {
        try {
            this.storeCircuitThrowing(circuit);

        } catch(IOException error) {
            Logger.raise("Failed to store cache file", error);
        }
    }

    private void storeCircuitThrowing(Circuit circuit) throws FileNotFoundException, IOException {

        File cacheFile = this.getCachedCircuitFile();
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cacheFile));

        Long cacheTime = this.getNetFileTime();
        out.writeObject(cacheTime);

        out.writeObject(circuit);

        out.close();
    }


    boolean isCached() {
        ObjectInputStream in = this.getInputStreamIfCached();

        if(in == null) {
            return false;

        } else {
            try {
                in.close();
                return true;
            } catch(IOException e) {
                return false;
            }
        }
    }


    Circuit load() {
        try {
            return this.loadThrowing();

        } catch(ClassNotFoundException | IOException e) {
            Logger.log("Something went wrong while reading the cache file");
            return null;
        }
    }
    private Circuit loadThrowing() throws ClassNotFoundException, IOException {
        ObjectInputStream in = this.getInputStreamIfCached();

        // If they are equal: read the cache
        Circuit circuit = (Circuit) in.readObject();

        in.close();

        return circuit;
    }



    private ObjectInputStream getInputStreamIfCached() {
        try {
            return this.getInputStreamIfCachedThrowing();

        } catch(IOException|ClassNotFoundException error) {
            return null;
        }
    }

    private ObjectInputStream getInputStreamIfCachedThrowing() throws FileNotFoundException, IOException, ClassNotFoundException {
        // Open the cache file
        File cacheFile = this.getCachedCircuitFile();
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(cacheFile));

        // Try to read the file time and cache time
        long fileTime = this.getNetFileTime();
        long cacheTime = (Long) in.readObject();

        if(cacheTime == fileTime) {
            return in;
        } else {
            return null;
        }
    }


    private long getNetFileTime() {
        try {
            return this.getNetFileTimeThrowing();
        } catch(IOException error) {
            return -1;
        }
    }
    private long getNetFileTimeThrowing() throws IOException {
        Path netFilePath = this.netFile.toPath();
        boolean isUnix = Files.getFileStore(netFilePath).supportsFileAttributeView("unix");

        if(isUnix) {
            FileTime test = (FileTime) Files.getAttribute(netFilePath, "unix:ctime");
            return test.to(TimeUnit.SECONDS);
        } else {
            Logger.raise("This operation is only supported on Unix platforms");
            return -1;
        }
    }


    private File getCachedCircuitFile() {
        return new File(this.getCacheFolder(), this.circuitName + ".ser");
    }
    private File getCacheFolder() {
        return new File("data/circuit");
    }
}
