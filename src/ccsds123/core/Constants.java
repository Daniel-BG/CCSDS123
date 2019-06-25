package ccsds123.core;

public class Constants {
	
	public static enum LocalSumType {
		WIDE_NEIGHBOR_ORIENTED,
		NARROW_NEIGHBOR_ORIENTED,
		WIDE_COLUMN_ORIENTED,
		NARROW_COLUMN_ORIENTED
	}

	public static final int DEFAULT_ABS_ERR_VALUE = 0;
	public static final int DEFAULT_REL_ERR_VALUE = 0;
	public static final int DEFAULT_RESOLUTION_VALUE = 0;
	public static final int DEFAULT_DAMPING_VALUE = 0;
	public static final int DEFAULT_OFFSET_VALUE = 0;
	public static final int DEFAULT_WEIGHT_EXPONENT_OFFSET = 0;
	public static final int DEFAULT_DEPTH = 16;
	public static final int DEFAULT_T_EXP = 4;
	public static final int DEFAULT_V_MIN = -6;
	public static final int DEFAULT_V_MAX = 9;
	public static final int DEFAULT_OMEGA = 19;
	public static final int DEFAULT_U_MAX = 10;
	public static final int DEFAULT_GAMMA_ZERO = 6;
	public static final int DEFAULT_GAMMA_STAR = 10;
	public static final int DEFAULT_P = 3;
	public static final boolean DEFAULT_FULL_PREDICTION_ENABLED = true;
	public static final LocalSumType DEFAULT_LOCAL_SUM_TYPE = LocalSumType.NARROW_NEIGHBOR_ORIENTED;
	
	
	public static final int MAX_DEPTH = 32;
	public static final int MIN_DEPTH = 2;
	
	public static final int MIN_OMEGA = 4;
	public static final int MAX_OMEGA = 19;
	
	public static final int MIN_V = -6;
	public static final int MAX_V = 9;
	
	public static final int MIN_T_EXP = 4;
	public static final int MAX_T_EXP = 11;
	
	public static final int MIN_U_MAX = 8;
	public static final int MAX_U_MAX = 32;
	
	public static final int MIN_GAMMA_ZERO = 1;
	public static final int MAX_GAMMA_ZERO = 8;
	
	public static final int MIN_GAMMA_STAR = 4;
	public static final int MAX_GAMMA_STAR = 11;
	
	public static final int MIN_P = 0;
	public static final int MAX_P = 15;
	
	public static final int MIN_ACC_INIT_CONSTANT = 0;
	public static int get_MAX_ACC_INIT_CONSTANT(int depth) {
		return Math.min(depth - 2, 14);
	}
	
	public static final int MIN_WEIGHT_EXPONENT_OFFSET = -6;
	public static final int MAX_WEIGHT_EXPONENT_OFFSET = 5;
	
	public static final int MIN_RESOLUTION = 0;
	public static final int MAX_RESOLUTION = 4;
	
	public static final int MIN_DAMPING = 0;
	public static int get_MAX_DAMPING(int resolution) {
		return (1 << resolution) - 1;
	}
	
	public static final int MIN_OFFSET = 0;
	public static int get_MAX_OFFSET(int resolution) {
		return (1 << resolution) - 1;
	}
	
	
	
	
	
	
	
}
