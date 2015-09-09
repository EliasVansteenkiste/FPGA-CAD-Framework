package util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
	public static enum Stream {OUT, ERR};
	public static enum Location {STDOUT, STDERR, FILE};
	
	private static Location[] locations = new Location[Stream.values().length];
	private static String[] filenames = new String[Stream.values().length];
	private static BufferedWriter[] writers = new BufferedWriter[Stream.values().length];
	
	static {
		locations[Stream.OUT.ordinal()] = Location.STDOUT;
		locations[Stream.ERR.ordinal()] = Location.STDERR;
		
		filenames[Stream.OUT.ordinal()] = "out.log";
		filenames[Stream.ERR.ordinal()] = "out.err";
	}
	
	
	
	public static void setLocation(Stream stream, Location location) {
		locations[stream.ordinal()] = location;
		if(location == Location.FILE) {
			openFile(stream);
		}
	}
	public static void setLocation(Stream stream, String filename) {
		locations[stream.ordinal()] = Location.FILE;
		filenames[stream.ordinal()] = filename;
		openFile(stream);
	}
	
	private static void openFile(Stream stream) {
		String filename = filenames[stream.ordinal()];
		try {
			writers[stream.ordinal()] = new BufferedWriter(new FileWriter(filename));
		} catch (IOException exception) {
			System.err.println("Could not open log file: " + filename);
			System.exit(1);
		}
	}
	
	
	
	
	public static void log(String message) {
		log(Stream.OUT, message);
	}
	public static void log(Stream stream, String message) {
		Location location = locations[stream.ordinal()];
		
		switch(location) {
		case STDOUT:
			System.out.println(message);
			break;
		
		case STDERR:
			System.err.println(message);
			break;
			
		case FILE:
			try {
				writers[stream.ordinal()].write(message + "\n");
			} catch (IOException exception) {
				System.err.println("Could not write to file: " + filenames[stream.ordinal()]);
				System.exit(1);
			}
			break;
		}
	}
	
	
	
	
	public static void raise(String message) {
		raise(message, true);
	}
	public static void raise(String message, boolean exit) {
		raise(message, new Exception());
	}
	public static void raise(String message, Exception exception) {
		raise(message, exception, true);
	}
	public static void raise(String message, Exception exception, boolean exit) {
		log(Stream.ERR, message);
		log(Stream.ERR, exception.toString());
		
		if(exit) {
			System.exit(1);
		}
	}
}
