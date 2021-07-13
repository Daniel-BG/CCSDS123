package ccsds123.core;

public class Constants {
	/////IMAGE PARAMETERS/////
	public static final int MAX_DEPTH = 32;
	public static final int DEFAULT_DEPTH = 16;
	public static final int MIN_DEPTH = 2;
	//////////////////////////
	

	////////LOSSY PARAMETERS////////
	public static final int MIN_ERROR_LIMIT_BIT_DEPTH = 1;
	public static final int DEFAULT_ABSOLUTE_ERROR_LIMIT_BIT_DEPTH = 14;
	public static final int DEFAULT_RELATIVE_ERROR_LIMIT_BIT_DEPTH = 14;
	public static int get_MAX_ERROR_LIMIT_BIT_DEPTH(int depth) { return Math.min(16, depth-1); }
	public static final int MAX_ERROR_LIMIT_BIT_DEPTH = 16;
	
	public static final boolean DEFAULT_USE_ABS_ERR = true;
	public static final boolean DEFAULT_USE_REL_ERR = true;
	
	public static final int MIN_ABS_ERR_VALUE = 0;
	public static final int DEFAULT_ABS_ERR_VALUE = 0;
	public static int get_MAX_ABS_ERR_VALUE(int aelbd) { return (1 << aelbd) - 1; }
	
	public static final int MIN_REL_ERR_VALUE = 0;
	public static final int DEFAULT_REL_ERR_VALUE = 0;
	public static int get_MAX_REL_ERR_VALUE(int relbd) { return (1 << relbd) - 1; }
	/////////////////////////////
	
	
	////////PREDICTOR FINE TUNING////////// (disable for fast lossless pipelining potential)
	public static final int MIN_RESOLUTION = 0;
	public static final int DEFAULT_RESOLUTION_VALUE = 4;
	public static final int MAX_RESOLUTION = 4;
	
	public static final int MIN_DAMPING = 0;
	public static final int DEFAULT_DAMPING_VALUE = 4;
	public static int get_MAX_DAMPING(int resolution) { return (1 << resolution) - 1; }
	
	public static final int MIN_OFFSET = 0;
	public static final int DEFAULT_OFFSET_VALUE = 4;
	public static int get_MAX_OFFSET(int resolution) { return (1 << resolution) - 1; }
	///////////////////////////////
	
	///////COMPRESSION PARAMETERS////////////
	public static enum LocalSumType {
		WIDE_NEIGHBOR_ORIENTED,
		NARROW_NEIGHBOR_ORIENTED,
		WIDE_COLUMN_ORIENTED,
		NARROW_COLUMN_ORIENTED;

		public boolean isWide() {
			return this == WIDE_NEIGHBOR_ORIENTED || this == WIDE_COLUMN_ORIENTED;
		}

		public boolean isNeighbor() {
			return this == NARROW_NEIGHBOR_ORIENTED || this == WIDE_NEIGHBOR_ORIENTED;
		}
	}
	
	public static final boolean DEFAULT_FULL_PREDICTION_ENABLED = true;
	public static final LocalSumType DEFAULT_LOCAL_SUM_TYPE = LocalSumType.WIDE_NEIGHBOR_ORIENTED;
	
	public static final int MIN_OMEGA = 4;
	public static final int DEFAULT_OMEGA = 19;
	public static final int MAX_OMEGA = 19;
	
	public static final int MIN_V = -6;
	public static final int DEFAULT_V_MIN = -1;
	public static final int DEFAULT_V_MAX = 3;
	public static final int MAX_V = 9;
	
	public static final int MIN_T_EXP = 4;
	public static final int DEFAULT_T_EXP = 6;
	public static final int MAX_T_EXP = 11;
	
	public static final int MIN_P = 0;
	public static final int DEFAULT_P = 3;
	public static final int MAX_P = 15;
	
	public static final int MIN_ACC_INIT_CONSTANT = 0;
	public static final int DEFAULT_ACC_INIT_CONSTANT = 5;
	public static int get_MAX_ACC_INIT_CONSTANT(int depth) { return Math.min(depth - 2, 14); }
	
	public static final int MIN_WEIGHT_EXPONENT_OFFSET = -6;
	public static final int DEFAULT_WEIGHT_EXPONENT_OFFSET = 0;
	public static final int MAX_WEIGHT_EXPONENT_OFFSET = 5;
	
	public static int get_MIN_R(int depth, int omega) {	return Math.max(32, depth + omega + 2);	}
	public static final int DEFAULT_R = 64;
	public static final int MAX_R = 64;
	////////////////////////////////////////
	
	//////////ENCODER PARAMETERS///////////
	public static final int MIN_U_MAX = 8;
	public static final int DEFAULT_U_MAX = 18;
	public static final int MAX_U_MAX = 32;
	
	public static final int MIN_GAMMA_ZERO = 1;
	public static final int DEFAULT_GAMMA_ZERO = 1;
	public static final int MAX_GAMMA_ZERO = 8;
	
	public static final int MIN_GAMMA_STAR = 4;
	public static final int DEFAULT_GAMMA_STAR = 6;
	public static final int MAX_GAMMA_STAR = 11;
	///////////////////////////////////////
}
