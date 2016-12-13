package pack.util;

public class ErrorLog {
	public static void print(String error){
		Output.newLine();
		Output.println("ERROR: " + error);
		for(StackTraceElement ste : Thread.currentThread().getStackTrace()) {
		    Output.println(ste + "");
		}
		Output.flush();
		System.exit(1);
	}
}
