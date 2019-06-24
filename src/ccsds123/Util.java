package ccsds123;

public class Util {

	public static long clamp(long value, long min, long max) {
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}
	
	public static int clamp(int value, int min, int max) {
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}
	
	
	public static long signum(long val) {
		if (val > 0)
			return 1;
		else if (val == 0)
			return 0;
		return -1;
	}
	
	public static int signumPlus(int val) {
		if (val >= 0)
			return 1;
		return -1;
	}
	
}
