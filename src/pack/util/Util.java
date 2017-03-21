package pack.util;

import java.io.File;

public class Util {
	public static boolean fileExists(String file){
		File f = new File(file);
		if(f.exists() && !f.isDirectory()) { 
		    return true;
		}else{
			return false;
		}
	}
	
	//MATH FUNCTIONS
	public static double round(double number, int digits){
		if(digits > 6){
			return number;
		}else{
			double roundNumber = number;
			roundNumber *= Math.pow(10, digits);
			roundNumber = Math.round(roundNumber);
			roundNumber /= Math.pow(10, digits);
			return roundNumber;
		}
	}
	
	//PRINT FUNCTIONS
	public static String tabs(int tabs){
		switch(tabs){
			case 0:		return "";
			case 1:		return "	";
			case 2:		return "		";
			case 3:		return "			";
			case 4:		return "				";
			case 5:		return "					";
			case 6:		return "						";
			case 7:		return "							";
			case 8:		return "								";
			case 9:		return "									";
			case 10:	return "										";
			case 11:	return "											";
			case 12:	return "												";
			case 13:	return "													";
			case 14:	return "														";
			case 15:	return "															";
			case 16:	return "																";
			case 17:	return "																	";
			case 18:	return "																		";
			case 19:	return "																			";
			case 20:	return "																				";
			default: 	ErrorLog.print("To many tabs: " + tabs);
						return "";
		}
	}
	
	//TOSTRING
	public static String str(int i){
		return "" + i + "";
	}
	public static String str(double i){
		return "" + i + "";
	}
	
	public static String fill(String s, int length){
		while(s.length() < length){
			s += " ";
		}
		return s;
	}
	public static String fill(int i, int length){
		String s = Util.str(i);
		while(s.length() < length){
			s += " ";
		}
		return s;
	}
	public static String fill(double d, int length){
		String s = Util.str(d);
		while(s.length() < length){
			s += " ";
		}
		return s;
	}

	public static String parseDigit(double digit){
		if(digit > 1000000){
			return Util.round((1.0*digit)/1000000,1) + "M";
		}else if(digit > 1000){
			return Util.round((1.0*digit)/1000,1) + "K";
		}else{
			return Util.str(digit);
		}
	}
	
	//READ BLIF SPECIFIC STATISTICS
	public static boolean isMoleculeType(String type){
		if(type.equals("HALF_DSP")){
			return true;
		}else if(type.equals("DSP")){
			return true;
		}else if(type.equals("RAM")){
			return true;
		}else if(type.equals("CARRY_CHAIN")){
			return true;
		}else if(type.equals("LUT_FF")){
			return true;
		}else if(type.equals("SHARE_CHAIN")){
			return true;
		}else if(type.equals("MWR")){
			return true;
		}else{
			return false;
		}
	}
}