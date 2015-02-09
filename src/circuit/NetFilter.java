package circuit;
import java.io.File;
import java.io.FilenameFilter;

class NetFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        return (name.endsWith(".net"));
    }
}