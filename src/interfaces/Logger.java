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



    public void logToStream(Stream stream, String message) {
        PrintWriter writer = this.getWriter(stream);
        writer.print(message);
        writer.flush();
    }
    public void logToStream(Stream stream, Exception exception) {
        exception.printStackTrace(this.getWriter(stream));
    }



    public void log(String message) {
        this.logToStream(Stream.OUT, message);
    }
    public void logln(String message) {
        this.log(message + "\n");
    }
    public void logln() {
        this.logln("");
    }
    public void logf(String message, Object... args) {
        String formattedMessage = String.format(message, args);
        this.log(formattedMessage);
    }


    public void raise(String message) {
        this.logToStream(Stream.ERR, message);
        System.exit(1);
    }
    public void raise(Exception exception) {
        this.logToStream(Stream.ERR, exception);
        System.exit(1);
    }
    public void raise(String message, Exception exception) {
        this.logToStream(Stream.ERR, message);
        this.logToStream(Stream.ERR, exception);
        this.raise(exception);
    }
}
