package ccsds123.util;

public class Utils {

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
	
	public static long modR(long value, int r) {
		if (r == 64)
			return value;
		
		long offset = 1 << (r - 1);
		long modulus = 1 << r;
		return ((value + offset) % modulus) - offset;
	}
	
	public static long minusOneToThe(long value) {
		if (value % 2 == 0)
			return 1;
		return -1;
	}
	
	/**
	 * Generic template to extract a value from a vector which can be null,
	 * or have 1 element that should be repeated,
	 * or have as many elements as required
	 * @param vector
	 * @param component
	 * @param defaultVal
	 * @return
	 */
	public static int getVectorValue(int [] vector, int component, int defaultVal) {
		if (vector == null) {
			return defaultVal;
		} else if (vector.length == 1) {
			return vector[0];
		} else {
			return vector[component];
		}
	}

	public static int getMatrixValue(int [][] matrix, int firstComponent, int secondComponent, int defaultVal) {
		if (matrix == null) {
			return defaultVal;
		} else if (matrix.length == 1) {
			return Utils.getVectorValue(matrix[0], secondComponent, defaultVal);
		} else {
			return Utils.getVectorValue(matrix[firstComponent], secondComponent, defaultVal);
		}
	}
	
	public static void checkVector(int [] vector, int lowerLimit, int upperLimit) {
		if (vector == null)
			return;
		for (int i = 0; i < vector.length; i++) {
			if (vector[i] < lowerLimit || vector[i] > upperLimit) {
				throw new IllegalArgumentException("Component out of range");
			}
		}
	}
	
	public static void checkMatrix(int [][] matrix, int lowerLimit, int upperLimit) {
		if (matrix == null)
			return;
		for (int i = 0; i < matrix.length; i++)
			Utils.checkVector(matrix[i], lowerLimit, upperLimit);
	}
	
	
	
	
}
