package util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Logger {
	public static enum Stream {OUT, ERR};
	public static enum Location {STDOUT, STDERR, FILE};
	
	private static String[] filenames = new String[Stream.values().length];
	private static PrintWriter[] writers = new PrintWriter[Stream.values().length];
	
	static {
		filenames[Stream.OUT.ordinal()] = "out.log";
		filenames[Stream.ERR.ordinal()] = "out.err";
		
		setLocation(Stream.OUT, Location.STDOUT);
		setLocation(Stream.ERR, Location.STDERR);
	}
	
	
	
	public static void setLocation(Stream stream, Location location) {
		openWriter(stream, location);
	}
	public static void setLocation(Stream stream, String filename) {
		filenames[stream.ordinal()] = filename;
		openWriter(stream, Location.FILE);
	}
	
	
	private static void openWriter(Stream stream, Location location) {
		PrintWriter newWriter = null;
		
		switch(location) {
		case STDOUT:
			newWriter = new PrintWriter(System.out, true);
			break;
			
		case STDERR:
			newWriter = new PrintWriter(System.err, true);
			break;
			
		case FILE:
			String filename = filenames[stream.ordinal()];
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
		
		writers[stream.ordinal()] = newWriter;
	}
	
	private static PrintWriter getWriter(Stream stream) {
		return writers[stream.ordinal()];
	}
	
	
	
	
	public static void log(String message) {
		log(Stream.OUT, message);
	}
	public static void log(Stream stream, String message) {
		getWriter(stream).println(message);
	}
	
	public static void log(Throwable exception) {
		log(Stream.ERR, exception);
	}
	public static void log(Stream stream, Throwable exception) {
		exception.printStackTrace(getWriter(stream));
	}
	
	
	
	public static void raise(String message) {
		raise(message, true);
	}
	public static void raise(String message, boolean exit) {
		raise(message, new Exception(), exit);
	}
	public static void raise(String message, Exception exception) {
		raise(message, exception, true);
	}
	public static void raise(String message, Exception exception, boolean exit) {
		log(Stream.ERR, message);
		log(Stream.ERR, exception);
		
		if(exit) {
			System.exit(1);
		}
	}
}
