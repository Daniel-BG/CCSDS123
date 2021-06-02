package ccsds123.core;

import java.util.Arrays;

import ccsds123.core.Constants.LocalSumType;
import ccsds123.util.Utils;

public class CompressorParameters {

	public LocalSumType localSumType;
	public int depth;
	public boolean fullPredictionMode;
	public int predictionBands;
	public int omega;
	public int r;
	
	public int [] absErr;
	public int [] relErr;
	public boolean useAbsoluteErrLimit;
	public boolean useRelativeErrLimit; 
	public int absoluteErrorLimitBitDepth;
	public int relativeErrorLimitBitDepth;
	
	public int [] resolution;
	public int [] damping;
	public int [] offset;
	
	public int tIncExp;
	public int vmin;
	public int vmax;
	
	public int [] intraBandWeightExponentOffsets;
	public int [][] interBandWeightExponentOffsets;
	
	public int uMax;
	public int gammaZero;
	public int gammaStar;
	public int [] accumulatorInitializationConstant;
	
	public int samplesPerBand;
	public int samples;
	public int bands;
	public int lines;
	
	public CompressorParameters() {
		this.setDefaults();
	}

	
	/**
	 * Set parameters for compression
	 */
	public void setDefaults() {
		this.setLocalSumType(Constants.DEFAULT_LOCAL_SUM_TYPE);
		this.setErrors(Constants.DEFAULT_ABSOLUTE_ERROR_LIMIT_BIT_DEPTH, Constants.DEFAULT_RELATIVE_ERROR_LIMIT_BIT_DEPTH, null, null, Constants.DEFAULT_USE_ABS_ERR, Constants.DEFAULT_USE_REL_ERR);
		this.setDepth(Constants.DEFAULT_DEPTH);
		this.setNearLosslessParams(null, null, null);
		this.setWeightUpdateParams(Constants.DEFAULT_T_EXP, Constants.DEFAULT_V_MIN, Constants.DEFAULT_V_MAX);
		this.setOmega(Constants.DEFAULT_OMEGA);
		this.setR(Constants.DEFAULT_R);
		this.setEncoderUpdateParams(Constants.DEFAULT_U_MAX, Constants.DEFAULT_GAMMA_ZERO, Constants.DEFAULT_GAMMA_STAR);
		this.setEncoderInitParams(null);
		this.setPredictionBands(Constants.DEFAULT_P);
		this.setFullPredictionMode(Constants.DEFAULT_FULL_PREDICTION_ENABLED);
		this.setWeightExponentOffsets(null, null);
	}
	
	public void setSize(int samples, int lines, int bands) {
		if (samples < 3 || lines < 3 || bands < 3)
			throw new IllegalArgumentException("Minimum size must be 3x3x3 otherwise it wont work");
		
		this.samples = samples;
		this.lines = lines;
		this.bands = bands;
		this.samplesPerBand = lines*samples;
	}
	
	public void setFullPredictionMode(boolean enable) {
		this.fullPredictionMode = enable;
	}
	
	public void setPredictionBands(int predictionBands) {
		if (predictionBands < Constants.MIN_P || predictionBands > Constants.MAX_P)
			throw new IllegalArgumentException("Number of bands used for prediction out of bounds");
		
		this.predictionBands = predictionBands;
	}
	
	public void setEncoderInitParams(int[] accumulatorInitializationConstant) {
		Utils.checkVector(accumulatorInitializationConstant, Constants.MIN_ACC_INIT_CONSTANT, Constants.get_MAX_ACC_INIT_CONSTANT(this.depth));
		this.accumulatorInitializationConstant = accumulatorInitializationConstant;		
	}
	
	public void setEncoderUpdateParams(int uMax, int gammaZero, int gammaStar) {
		if (uMax < Constants.MIN_U_MAX || uMax > Constants.MAX_U_MAX)
			throw new IllegalArgumentException("Umax out of bounds!");
		this.uMax = uMax;
		if (gammaZero < Constants.MIN_GAMMA_ZERO || gammaZero > Constants.MAX_GAMMA_ZERO) 
			throw new IllegalArgumentException("GammaZero out of bounds!");
		this.gammaZero = gammaZero;
		if (gammaStar < Math.max(Constants.MIN_GAMMA_STAR, this.gammaZero + 1) || gammaStar > Constants.MAX_GAMMA_STAR)
			throw new IllegalArgumentException("GammaStar out of bounds!");
		this.gammaStar = gammaStar;
	}
	
	public void setWeightExponentOffsets(int[] intraBandWeightExponentOffsets, int[][] interBandWeightExponentOffsets) {
		Utils.checkVector(intraBandWeightExponentOffsets, Constants.MIN_WEIGHT_EXPONENT_OFFSET, Constants.MAX_WEIGHT_EXPONENT_OFFSET);
		Utils.checkMatrix(interBandWeightExponentOffsets, Constants.MIN_WEIGHT_EXPONENT_OFFSET, Constants.MAX_WEIGHT_EXPONENT_OFFSET);
		this.intraBandWeightExponentOffsets = intraBandWeightExponentOffsets;
		this.interBandWeightExponentOffsets = interBandWeightExponentOffsets;
	}
	
	public void setWeightUpdateParams(int tIncExp, int vmin, int vmax) {
		if (tIncExp < Constants.MIN_T_EXP || tIncExp > Constants.MAX_T_EXP)
			throw new IllegalArgumentException("Tinc out of bounds");
		this.tIncExp = tIncExp;
		if (vmin < Constants.MIN_V || vmax > Constants.MAX_V || vmin >= vmax)
			throw new IllegalArgumentException("vmin and/or vmax out of bounds");
		this.vmax = vmax;
		this.vmin = vmin;
	}
	
	public void setNearLosslessParams(int[] resolution, int[] damping, int[] offset) {
		Utils.checkVector(resolution, Constants.MIN_RESOLUTION, Constants.MAX_RESOLUTION);
		if (offset != null)
			for (int i = 0; i < offset.length; i++)
				if (offset[i] < Constants.MIN_OFFSET || offset[i] > Constants.get_MAX_OFFSET(this.getResolution(i)))
					throw new IllegalArgumentException("Offset out of bounds");
		
		if (damping != null)
			for (int i = 0; i < damping.length; i++)
				if (damping[i] < Constants.MIN_DAMPING || damping[i] > Constants.get_MAX_DAMPING(this.getResolution(i))) 
					throw new IllegalArgumentException("Offset out of bounds");		
		
		this.resolution = resolution;
		this.damping = damping;
		this.offset = offset;
	}
	
	public void setOmega(int omega) {
		if (omega < Constants.MIN_OMEGA || omega > Constants.MAX_OMEGA)
			throw new IllegalArgumentException("Omega out of bounds");
		this.omega = omega;
	}
	
	public void setDepth(int depth) {
		if (depth < Constants.MIN_DEPTH || depth > Constants.MAX_DEPTH) {
			throw new IllegalArgumentException("Sample depth out of bounds");
		}
		this.depth = depth;
	}
	
	public void setR(int r) {
		if (r < Constants.get_MIN_R(this.depth, this.omega) || r > Constants.MAX_R) {
			throw new IllegalArgumentException("Register size R out of bounds");
		}
		this.r = r;
	}
	
	public void setLocalSumType(LocalSumType localSumType) {
		this.localSumType = localSumType;
	}
	
	public void setErrors(int absoluteErrorLimitBitDepth, int relativeErrorLimitBitDepth, int[] absErr, int[] relErr, boolean useAbsoluteErrLimit, boolean useRelativeErrLimit) {
		this.absoluteErrorLimitBitDepth = absoluteErrorLimitBitDepth;
		this.relativeErrorLimitBitDepth = relativeErrorLimitBitDepth;
		Utils.checkVector(absErr, Constants.MIN_ABS_ERR_VALUE, Constants.get_MAX_ABS_ERR_VALUE(this.absoluteErrorLimitBitDepth));
		Utils.checkVector(relErr, Constants.MIN_REL_ERR_VALUE, Constants.get_MAX_REL_ERR_VALUE(this.relativeErrorLimitBitDepth));
		this.absErr = absErr;
		this.relErr = relErr;
		this.useAbsoluteErrLimit = useAbsoluteErrLimit;
		this.useRelativeErrLimit = useRelativeErrLimit;
	}
	
	public void checkParameterSanity() {
		if (absErr != null && absErr.length != 1 && absErr.length != bands)
			throw new IllegalStateException("AbsErr length error");
		if (relErr != null && relErr.length != 1 && relErr.length != bands)
			throw new IllegalStateException("RelErr length error");
		if (resolution != null && resolution.length != 1 && resolution.length != bands)
			throw new IllegalStateException("resolution length error");
		if (damping != null && damping.length != 1 && damping.length != bands)
			throw new IllegalStateException("damping length error");
		if (offset != null && offset.length != 1 && offset.length != bands)
			throw new IllegalStateException("offset length error");
		if (intraBandWeightExponentOffsets != null && intraBandWeightExponentOffsets.length != 1 && intraBandWeightExponentOffsets.length != bands)
			throw new IllegalStateException("intraBandWeightExponentOffsets length error");
		if (interBandWeightExponentOffsets != null && interBandWeightExponentOffsets.length != 1 && interBandWeightExponentOffsets.length != bands)
			throw new IllegalStateException("interBandWeightExponentOffsets length error");
		if (interBandWeightExponentOffsets != null) 
			for (int i = 0; i < interBandWeightExponentOffsets.length; i++) 
				if (interBandWeightExponentOffsets[i] != null && interBandWeightExponentOffsets[i].length != 1 && interBandWeightExponentOffsets[i].length != this.predictionBands)
					throw new IllegalStateException("interBandWeightExponentOffsets length error");
		if (accumulatorInitializationConstant != null && accumulatorInitializationConstant.length != 1 && accumulatorInitializationConstant.length != bands)
			throw new IllegalStateException("accumulatorInitializationConstant length error");
	}
	
	public int getAbsErrVal(int band) {
		return Utils.getVectorValue(this.absErr, band, Constants.DEFAULT_ABS_ERR_VALUE);
	}
	
	public int getRelErrVal(int band) {
		return Utils.getVectorValue(this.relErr, band, Constants.DEFAULT_REL_ERR_VALUE);
	}
	
	public int getResolution(int band) {
		return Utils.getVectorValue(this.resolution, band, Constants.DEFAULT_RESOLUTION_VALUE);
	}
	
	public int getDamping(int band) {
		return Utils.getVectorValue(this.damping, band, Constants.DEFAULT_DAMPING_VALUE);
	}
	
	public int getOffset(int band) {
		return Utils.getVectorValue(this.offset, band, Constants.DEFAULT_OFFSET_VALUE);
	}
	
	public int getIntraBandWeightExponentOffset(int band) {
		return Utils.getVectorValue(intraBandWeightExponentOffsets, band, Constants.DEFAULT_WEIGHT_EXPONENT_OFFSET);
	}
	
	public int getInterBandWeightExponentOffsets(int band, int p) {
		return Utils.getMatrixValue(interBandWeightExponentOffsets, band, p, Constants.DEFAULT_WEIGHT_EXPONENT_OFFSET);
	}
	
	public int[][] getInitialWeights() { //EQ 31, 32, 33, 34
		int[][] weights;
		if (this.fullPredictionMode) {
			weights = new int[bands][this.predictionBands + 3];
		} else {
			weights = new int[bands][this.predictionBands];
		}
		
		for (int b = 0; b < bands; b++) {
			int windex = 0;
			if (this.fullPredictionMode) {
				weights[b][0] = 0;
				weights[b][1] = 0;
				weights[b][2] = 0;
				windex = 3;
			}
			for (int p = 0; p < this.predictionBands; p++) {
				if (p == 0) {
					weights[b][p+windex] = (7*(1 << this.omega)) >> 3;
				} else {
					weights[b][p+windex] = weights[b][p+windex-1] >> 3; 
				}
			}
		}
		return weights;
	}

	@Override
	public String toString() {
		return "CompressorParameters \n localSumType=" + localSumType + "\n depth=" + depth
				+ "\n fullPredictionMode=" + fullPredictionMode + "\n predictionBands=" + predictionBands
				+ "\n omega=" + omega + "\n r=" + r + "\n absErr=" + Arrays.toString(absErr) + "\n relErr=" + Arrays.toString(relErr)
				+ "\n useAbsoluteErrLimit=" + useAbsoluteErrLimit + "\n useRelativeErrLimit=" + useRelativeErrLimit
				+ "\n absoluteErrorLimitBitDepth=" + absoluteErrorLimitBitDepth + "\n relativeErrorLimitBitDepth="
				+ relativeErrorLimitBitDepth + "\n resolution=" + getResolution(0) + "\n damping=" + getDamping(0) + "\n offset="
				+ getOffset(0) + "\n tIncExp=" + tIncExp + "\n vmin=" + vmin + "\n vmax=" + vmax
				+ "\n intraBandWeightExponentOffsets=" + getIntraBandWeightExponentOffset(0)
				+ "\n interBandWeightExponentOffsets=" + getInterBandWeightExponentOffsets(0,0) + "\n uMax=" + uMax
				+ "\n gammaZero=" + gammaZero + "\n gammaStar=" + gammaStar + "\n accumulatorInitializationConstant="
				+ accumulatorInitializationConstant + "\n samplesPerBand=" + samplesPerBand + "\n samples=" + samples
				+ "\n bands=" + bands + "\n lines=" + lines;
	}

	public void printStatus() {
		System.out.println(this.toString());
	}


	public int getSAInitialAcc(int b) {
		return this.calcInitialAccValue(Utils.getVectorValue(this.accumulatorInitializationConstant, b, Constants.DEFAULT_ACC_INIT_CONSTANT));
	}
	
	private int calcInitialAccValue(int constant) {
		int modifiedConstant;
		if (constant <= 30 - this.depth) {
			modifiedConstant = constant;
		} else {
			modifiedConstant = 2*constant + this.depth - 30;
		}
		return ((3*(1 << (modifiedConstant + 6)) - 49)*(1 << this.gammaZero)) >> 7;
	}

	public long getHInitialAcc(int b) {
		int meanMQIestimate = 5;
		return Utils.getVectorValue(this.accumulatorInitializationConstant, b, 4*(1 << this.gammaZero)*meanMQIestimate);
	}
	
}
