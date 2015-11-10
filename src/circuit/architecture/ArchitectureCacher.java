package circuit.architecture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import circuit.exceptions.InvalidPlatformException;

public class ArchitectureCacher {

    private static final String CACHE_FOLDER = "data/circuit";

    private String circuitName;
    private File netFile;

    public ArchitectureCacher(String circuitName, File netFile) {
        this.netFile = netFile;
        this.circuitName = circuitName;
    }

    public void store(Architecture architecture) {
        try {
            this.storeThrowing(architecture);
        } catch(IOException error) {}
    }

    private void storeThrowing(Architecture architecture) throws IOException {
        File cacheFile = this.getCachedCircuitFile();
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cacheFile));

        Long cacheTime = this.getNetFileTime();
        out.writeObject(cacheTime);

        out.writeObject(architecture);

        out.close();
    }

    public Architecture loadIfCached() {
        ObjectInputStream in = this.getInputStreamIfCached();

        if(in == null) {
            return null;
        }

        try {
            Architecture architecture = (Architecture) in.readObject();
            in.close();
            return architecture;

        } catch(ClassNotFoundException | IOException e) {
            return null;
        }
    }



    private ObjectInputStream getInputStreamIfCached() {

        // Open the cache file
        File cacheFile = this.getCachedCircuitFile();

        ObjectInputStream in;
        try {
            in = new ObjectInputStream(new FileInputStream(cacheFile));
        } catch(IOException error) {
            return null;
        }

        // Try to read the file time and cache time
        long fileTime = this.getNetFileTime();
        long cacheTime;

        try {
            cacheTime = (Long) in.readObject();
        } catch(ClassNotFoundException | IOException error) {
            return null;
        }

        if(cacheTime == fileTime) {
            return in;
        } else {
            return null;
        }
    }


    private long getNetFileTime() {
        try {
            return this.getNetFileTimeThrowing();

        } catch(IOException | InvalidPlatformException error) {
            return -1;
        }
    }
    private long getNetFileTimeThrowing() throws IOException, InvalidPlatformException {
        Path netFilePath = this.netFile.toPath();
        boolean isUnix = Files.getFileStore(netFilePath).supportsFileAttributeView("unix");

        if(isUnix) {
            FileTime test = (FileTime) Files.getAttribute(netFilePath, "unix:ctime");
            return test.to(TimeUnit.SECONDS);

        } else {
            throw new InvalidPlatformException("Only Unix is supported");
        }
    }


    private File getCachedCircuitFile() {
        return new File(ArchitectureCacher.CACHE_FOLDER, this.circuitName + ".ser");
    }
}
