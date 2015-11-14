package interfaces;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Logger {
    public static enum Stream {OUT, ERR};
    public static enum Location {STDOUT, STDERR, FILE};

    private String[] filenames = new String[Stream.values().length];
    private PrintWriter[] writers = new PrintWriter[Stream.values().length];


    Logger() {
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


    public void log(Stream stream, String message) {
        PrintWriter writer = this.getWriter(stream);
        writer.print(message);
        writer.flush();
    }
    private void log(Stream stream, Exception exception) {
        PrintWriter writer = this.getWriter(stream);
        exception.printStackTrace(writer);
    }

    public void logln(Stream stream, String message) {
        this.log(stream, message + "\n");
    }
    public void logln(Stream stream) {
        this.log(stream, "\n");
    }
    public void logf(Stream stream, String format, Object... args) {
        String formattedMessage = String.format(format, args);
        this.log(stream, formattedMessage);
    }


    public void log(String message) {
        this.log(Stream.OUT, message);
    }
    public void logln(String message) {
        this.logln(Stream.OUT, message);
    }
    public void logln() {
        this.logln(Stream.OUT);
    }
    public void logf(String format, Object... args) {
        this.logf(Stream.OUT, format, args);
    }


    public void raise(String message) {
        this.logln(Stream.ERR, message);
        System.exit(1);
    }
    public void raise(Exception exception) {
        this.log(Stream.ERR, exception);
        System.exit(1);
    }
    public void raise(String message, Exception exception) {
        this.logln(Stream.ERR, message);
        this.log(Stream.ERR, exception);
        this.raise(exception);
    }
}
