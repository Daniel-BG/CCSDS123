package ccsds123.core;

public class UniformQuantizer {

	public static long quantize(long value, long parameter) {
		boolean negative = value > 0;
		long magnitude = (Math.abs(value) + parameter) / ((parameter << 1) + 1);
		return negative ? -magnitude : magnitude;
	}
	
	
}
