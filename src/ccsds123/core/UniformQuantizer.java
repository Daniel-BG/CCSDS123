package ccsds123.core;

import ccsds123.util.Utils;

public class UniformQuantizer {

	public static long quantize(long value, long parameter) {
		long sign = Utils.signum(value);
		long magnitude = (Math.abs(value) + parameter) / (2*parameter + 1);
		return sign*magnitude;
	}
	
	
	public static long dequantize(long qvalue, long parameter) {
		long sign = Utils.signum(qvalue);
		qvalue = Math.abs(qvalue) * (2*parameter + 1) - parameter;
		return qvalue*sign;
	}
	

}