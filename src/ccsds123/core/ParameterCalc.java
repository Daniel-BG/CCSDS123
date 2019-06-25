package ccsds123.core;

public class ParameterCalc {
	
	public static long sMid(int depth) {
		return 1l << (depth - 1);
	}
	
	public static long sMin() {
		return 0;
	}
	
	public static long sMax(int depth) {
		return (1l << depth) - 1;
	}
	
	public static int wMin(int omega) {
		return -(1 << (omega + 2));
	}
	
	public static int  wMax(int omega) {
		return (1 << (omega + 2)) - 1;
	}

}
