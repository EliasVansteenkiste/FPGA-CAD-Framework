package place.interfaces;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

public class Logger {
    public static enum Stream {OUT, ERR};
    public static enum Location {STDOUT, STDERR, FILE};

    private String[] filenames = new String[Stream.values().length];
    private PrintWriter[] writers = new PrintWriter[Stream.values().length];


    Logger() {

        // I only want dots as decimal separators
        Locale.setDefault(new Locale("en", "US"));

        this.filenames[Stream.OUT.ordinal()] = "out.log";
        this.filenames[Stream.ERR.ordinal()] = "out.err";

        this.setLocation(Stream.OUT, Location.STDOUT);
        this.setLocation(Stream.ERR, Location.STDERR);
    }


    public void setLocation(Stream stream, Location location) {
        this.openWriter(stream, location);
    }
    public void setLocation(Stream stream, String filename) {
        this.filenames[stream.ordinal()] = filename;
        this.openWriter(stream, Location.FILE);
    }

    public PrintWriter getWriter(Stream stream) {
        return this.writers[stream.ordinal()];
    }
    private void setWriter(Stream stream, PrintWriter writer) {
        this.writers[stream.ordinal()] = writer;
    }


    private void openWriter(Stream stream, Location location) {
        PrintWriter newWriter = null;

        switch(location) {
        case STDOUT:
            newWriter = new PrintWriter(System.out, true);
            break;

        case STDERR:
            newWriter = new PrintWriter(System.err, true);
            break;

        case FILE:
            String filename = this.filenames[stream.ordinal()];
            FileWriter fw = null;

            try {
                fw = new FileWriter(filename);
            } catch(IOException exception) {
                System.err.println("Could not open log file: " + filename);
                exception.printStackTrace();
            }

            newWriter = new PrintWriter(new BufferedWriter(fw));
            break;
        }

        this.setWriter(stream, newWriter);
    }


    public void print(Stream stream, String message) {
        PrintWriter writer = this.getWriter(stream);
        writer.print(message);
        writer.flush();
    }
    private void print(Stream stream, Exception exception) {
        PrintWriter writer = this.getWriter(stream);
        exception.printStackTrace(writer);
    }

    public void println(Stream stream, String message) {
        PrintWriter writer = this.getWriter(stream);
        writer.println(message);
        writer.flush();
    }
    public void println(Stream stream) {
        this.println(stream, "");
    }
    public void printf(Stream stream, String format, Object... args) {
        String formattedMessage = String.format(format, args);
        this.print(stream, formattedMessage);
    }


    public void print(String message) {
        this.print(Stream.OUT, message);
    }
    public void println(String message) {
        this.println(Stream.OUT, message);
    }
    public void println() {
        this.println(Stream.OUT);
    }
    public void printf(String format, Object... args) {
        this.printf(Stream.OUT, format, args);
    }


    public void raise(String message) {
        this.println(Stream.ERR, message);
        System.exit(1);
    }
    public void raise(Exception exception) {
        this.print(Stream.ERR, exception);
        System.exit(1);
    }
    public void raise(String message, Exception exception) {
        this.println(Stream.ERR, message);
        this.raise(exception);
    }
}
