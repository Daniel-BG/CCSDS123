package ccsds123.core;

import java.io.IOException;

import com.jypec.util.bits.BitOutputStream;

import ccsds123.core.Constants.LocalSumType;
import ccsds123.util.Utils;

/**
 * Image compression is always done in BIP traversal mode. Increase band, then sample, then line.
 * Input values are assumed possitive
 * @author Daniel
 *
 */
public class Compressor {
	private LocalSumType localSumType;
	private int depth;
	private boolean fullPredictionMode;
	private int predictionBands;
	private int omega;
	
	private int [] absErr;
	private int [] relErr;
	private boolean useAbsoluteErrLimit;
	private boolean useRelativeErrLimit; 
	
	private int [] resolution;
	private int [] damping;
	private int [] offset;
	
	private int tIncExp;
	private int vmin;
	private int vmax;
	
	private int [] intraBandWeightExponentOffsets;
	private int [][] interBandWeightExponentOffsets;
	
	private int uMax;
	private int gammaZero;
	private int gammaStar;
	private int [] accumulatorInitializationConstant;
	
	public Compressor() {
		this.setDefaults();
	}

	/**
	 * Set parameters for compression
	 */
	public void setDefaults() {
		this.setLocalSumType(Constants.DEFAULT_LOCAL_SUM_TYPE);
		this.setErrors(null, null);
		this.setDepth(Constants.DEFAULT_DEPTH);
		this.setNearLosslessParams(null, null, null);
		this.setWeightUpdateParams(Constants.DEFAULT_T_EXP, Constants.DEFAULT_V_MIN, Constants.DEFAULT_V_MAX);
		this.setOmega(Constants.DEFAULT_OMEGA);
		this.setEncoderUpdateParams(Constants.DEFAULT_U_MAX, Constants.DEFAULT_GAMMA_ZERO, Constants.DEFAULT_GAMMA_STAR);
		this.setEncoderInitParams(null);
		this.setPredictionBands(Constants.DEFAULT_P);
		this.setFullPredictionMode(Constants.DEFAULT_FULL_PREDICTION_ENABLED);
		this.setWeightExponentOffsets(null, null);
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
	
	public void setLocalSumType(LocalSumType localSumType) {
		this.localSumType = localSumType;
	}
	
	public void setErrors(int[] absErr, int[] relErr) {
		if (absErr == null) {
			this.useAbsoluteErrLimit = false;
		} else {
			this.absErr = absErr;
			this.useAbsoluteErrLimit = true;
		}
		if (relErr == null) {
			this.useRelativeErrLimit = false;
		} else {
			this.relErr = relErr;
			this.useRelativeErrLimit = true;
		}
	}
	
	
	
	public void checkParameterSanity(int bands) {
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
	

	
	private int getAbsErrVal(int band) {
		return Utils.getVectorValue(this.absErr, band, Constants.DEFAULT_ABS_ERR_VALUE);
	}
	
	private int getRelErrVal(int band) {
		return Utils.getVectorValue(this.relErr, band, Constants.DEFAULT_REL_ERR_VALUE);
	}
	
	private int getResolution(int band) {
		return Utils.getVectorValue(this.resolution, band, Constants.DEFAULT_RESOLUTION_VALUE);
	}
	
	private int getDamping(int band) {
		return Utils.getVectorValue(this.damping, band, Constants.DEFAULT_DAMPING_VALUE);
	}
	
	private int getOffset(int band) {
		return Utils.getVectorValue(this.offset, band, Constants.DEFAULT_OFFSET_VALUE);
	}
	
	private int getIntraBandWeightExponentOffset(int band) {
		return Utils.getVectorValue(intraBandWeightExponentOffsets, band, Constants.DEFAULT_WEIGHT_EXPONENT_OFFSET);
	}
	
	private int getInterBandWeightExponentOffsets(int band, int p) {
		return Utils.getMatrixValue(interBandWeightExponentOffsets, band, p, Constants.DEFAULT_WEIGHT_EXPONENT_OFFSET);
	}
	
	
	

	/**
	 * 
	 * @param block block[band][line][sample]
	 * @param bands
	 * @param lines
	 * @param samples
	 * @param bos
	 * @throws IOException 
	 */
	public void compress(int [][][] block, int bands, int lines, int samples, BitOutputStream bos) throws IOException {
		this.checkParameterSanity(bands);
		
		
		int [][][] repBlock = new int[bands][lines][samples];
		int [][][] diffBlock = new int[bands][lines][samples];

		
		////WEIGHT INITIALIZATION 4.6.3.2
		int [][] weights = this.getInitialWeights(bands);
		
		SampleAdaptiveEntropyCoder entropyCoder = new SampleAdaptiveEntropyCoder(this.uMax, this.depth, bands, this.gammaZero, this.gammaStar, this.accumulatorInitializationConstant);
		
		//LOOPS are interchangeable as long as samples come after lines
		//BIP: lines>samples>bands
		//BIL: lines>bands>sample
		//BSQ: bands>lines>samples
		for (int l = 0; l < lines; l++) {
			for (int s = 0; s < samples; s++) {
				for (int b = 0; b < bands; b++) {
					int t = l*samples + s;
					//compressing sample block[b][l][s]
					
					////LOCAL SUM BEGIN 4.4
					long localSum = this.calcLocalSum(b, l, s, repBlock, samples);
					////LOCAL SUM END
	
					////LOCAL DIFF BEGIN 4.5
					long northDiff = this.calcNorthDiff(b, l, s, repBlock, localSum);
					long westDiff = this.calcWestDiff(b, l, s, repBlock, localSum);
					long northWestDiff = this.calcNorthWestDiff(b, l, s, repBlock, localSum);
					long centralLocalDiff = this.calcCentralLocalDiff(b, l, s, repBlock, localSum);
					diffBlock[b][l][s] = (int) centralLocalDiff;
					////LOCAL DIFF END
					
					
					//PREDICTED CENTRAL LOCAL DIFFERENCE 4.7.1
					long predictedCentralDiff = this.calcPredictedCentralDiff(b, l, s, weights, northDiff, westDiff, northWestDiff, diffBlock);
					//PREDICTED CENTRAL LOCAL DIFFERENCE END
				
					
					//HR PREDICTED SAMPLE VALUE 4.7.2
					long highResolutionPredSampleValue = this.calcHighResolutionPredSampleValue(predictedCentralDiff, localSum);
					//DR PREDICTED SAMPLE VALUE 4.7.3
					long doubleResolutionPredSampleValue = this.calcDoubleResolutionSampleValue(b, l, s, highResolutionPredSampleValue, diffBlock);
					//PREDICTED SAMMPLE VALUE 4.7.4
					long predictedSampleValue = this.calcPredictedSampleValue(doubleResolutionPredSampleValue);
					//PRED SAMPLE VALUE END
						
					//PRED RES 4.8.1 + 4.8.2.1
					long predictionResidual = this.calcPredictionResidual(block[b][l][s], predictedSampleValue);
					long maxErrVal = this.calcMaxErrVal(b, predictedSampleValue);
					long quantizerIndex = this.calcQuantizerIndex(predictionResidual, maxErrVal);
					
					//DR SAMPLE REPRESENTATIVE AND SAMPLE REPRESENTATIVE 4.9
					long clippedQuantizerBinCenter = this.calcClipQuantizerBinCenter(predictedSampleValue, quantizerIndex, maxErrVal);
					long doubleResolutionSampleRepresentative = this.calcDoubleResolutionSampleRepresentative(b, clippedQuantizerBinCenter, quantizerIndex, maxErrVal, highResolutionPredSampleValue);
					long sampleRepresentative = this.calcSampleRepresentative(l, s, doubleResolutionSampleRepresentative, block[b][l][s]);

					repBlock[b][l][s] = (int) sampleRepresentative;
					
					//DR PRED ERR 4.10.1
					long doubleResolutionPredictionError = this.calcDoubleResolutionPredictionError(clippedQuantizerBinCenter, doubleResolutionPredSampleValue);
					//WEIGHT UPDATE SCALING EXPONENT 4.10.2
					long weightUpdateScalingExponent = this.calcWeightUpdateScalingExponent(t, samples);
					//WEIGHT UPDATE 4.10.3
					int windex = 0;
					if (this.fullPredictionMode) {
						int weightExponentOffset = this.getIntraBandWeightExponentOffset(b);
						//north, west, northwest
						weights[b][0] = this.updateWeight(weights[b][0], doubleResolutionPredictionError, northDiff, weightUpdateScalingExponent, weightExponentOffset);
						weights[b][1] = this.updateWeight(weights[b][1], doubleResolutionPredictionError, westDiff, weightUpdateScalingExponent, weightExponentOffset);
						weights[b][2] = this.updateWeight(weights[b][2], doubleResolutionPredictionError, northWestDiff, weightUpdateScalingExponent, weightExponentOffset);
						windex = 3;
					}
					for (int p = 0; p < this.predictionBands; p++) {
						if (b - p > 0) 
							weights[b][windex+p] = this.updateWeight(weights[b][windex+p], doubleResolutionPredictionError, diffBlock[b-p-1][l][s], weightUpdateScalingExponent, getInterBandWeightExponentOffsets(b, p));
					}
					
					//MAPPED QUANTIZER INDEX 4.11
					long theta = this.calcTheta(t, predictedSampleValue, maxErrVal);
					long mappedQuantizerIndex = calcMappedQuantizerIndex(quantizerIndex, theta, doubleResolutionPredSampleValue);
					
					//Send to coder to generate the binary output stream
					entropyCoder.code((int) mappedQuantizerIndex, t, b, bos);
				}
			}
		}
	}
	
	
	private int[][] getInitialWeights(int bands) {
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
	
	private long calcLocalSum(int b, int l, int s, int[][][] repBlock, int samples) { //EQ 20, 21, 22, 23
		long localSum = 0;
		switch (this.localSumType) {
			case WIDE_NEIGHBOR_ORIENTED: { //EQ 20
				if (l > 0 && s > 0 && s < samples - 1) {
					localSum = repBlock[b][l][s-1] + repBlock[b][l-1][s-1] + repBlock[b][l-1][s] + repBlock[b][l-1][s+1];
				} else if (l == 0 && s > 0) {
					localSum = repBlock[b][l][s-1] << 2;
				} else if (l > 0 && s == 0) {
					localSum = (repBlock[b][l-1][s] + repBlock[b][l-1][s+1]) << 1;
				} else if (l > 0 && s == samples - 1) {
					localSum = repBlock[b][l][s-1] + repBlock[b][l-1][s-1] + (repBlock[b][l-1][s] << 1);
				} else {
					if (l != 0 || s != 0) {
						throw new IllegalStateException("Should not get here");
					}
				}
				break;
			}
			case NARROW_NEIGHBOR_ORIENTED: { //EQ 21
				if (l > 0 && s > 0 && s < samples - 1) {
					localSum = repBlock[b][l-1][s-1] + (repBlock[b][l-1][s] << 1) + repBlock[b][l-1][s+1];
				} else if (l == 0 && s > 0 && b > 0) {
					localSum = repBlock[b-1][l][s-1];
				} else if (l > 0 && s == 0) {
					localSum = (repBlock[b][l-1][s] + repBlock[b][l-1][s+1]) << 1;
				} else if (l > 0 && s == samples - 1) {
					localSum = (repBlock[b][l-1][s-1] + repBlock[b][l-1][s]) << 1;
				} else if (l == 0 && s > 0 && b == 0) {
					localSum = ParameterCalc.sMid(this.depth) << 2;
				} else {
					if (l != 0 || s != 0) {
						throw new IllegalStateException("Should not get here");
					}
				}
			}
			case WIDE_COLUMN_ORIENTED: { //EQ 22
				if (l > 0) {
					localSum = repBlock[b][l-1][s] << 2;
				} else if (l == 0 && s > 0) {
					localSum = repBlock[b][l][s-1] << 2;
				} else {
					if (l != 0 || s != 0) {
						throw new IllegalStateException("Should not get here");
					}
				}
				break;
			}
			case NARROW_COLUMN_ORIENTED: { //EQ 23
				if (l > 0) {
					localSum = repBlock[b][l-1][s] << 2;
				} else if (l == 0 && s > 0 && b > 0) {
					localSum = repBlock[b-1][l][s-1] << 2;
				} else if (l == 0 && s > 0 && b == 0) {
					localSum = ParameterCalc.sMid(this.depth) << 2;
				} else {
					if (l != 0 || s != 0) {
						throw new IllegalStateException("Should not get here");
					}
				}
				break;
			}
		}
		return localSum;
	}
	
	private long calcCentralLocalDiff(int b, int l, int s, int[][][] repBlock, long localSum) {
		return (repBlock[b][l][s] << 2) - localSum;
	}
	
	private long calcNorthDiff (int b, int l, int s, int[][][] repBlock, long localSum) { //EQ 25
		if (this.fullPredictionMode && (s != 0 || l != 0))
			if (l > 0) 
				return (repBlock[b][l-1][s] << 2) - localSum;
		return 0;
	}
	
	private long calcWestDiff (int b, int l, int s, int[][][] repBlock, long localSum) { //EQ 26
		if (this.fullPredictionMode && (s != 0 || l != 0)) {
			if (s > 0 && l > 0) {
				return (repBlock[b][l][s-1] << 2) - localSum;
			} else if (s == 0 && l > 0) {
				return (repBlock[b][l-1][s] << 2) - localSum;
			}
		}
		return 0;
	}
	
	private long calcNorthWestDiff (int b, int l, int s, int[][][] repBlock, long localSum) { //EQ 27
		if (this.fullPredictionMode && (s != 0 || l != 0)) {
			if (s > 0 && l > 0) {
				return (repBlock[b][l-1][s-1] << 2) - localSum;
			} else if (s == 0 && l > 0) {
				return (repBlock[b][l-1][s] << 2) - localSum;
			}
		}
		return 0;
	}
	
	private long calcPredictedCentralDiff (int b, int l, int s, int[][] weights, long northDiff, long westDiff, long northWestDiff, int[][][] diffBlock) { //EQ 36
		long predictedCentralDiff = 0;
		if (b != 0 || this.fullPredictionMode) {
			int windex = 0;
			if (this.fullPredictionMode) {
				predictedCentralDiff += weights[b][0] * northDiff;
				predictedCentralDiff += weights[b][1] * westDiff;
				predictedCentralDiff += weights[b][2] * northWestDiff;
				windex = 3;
			}
			for (int p = 0; p < this.predictionBands; p++) {
				if (b - p > 0) 
					predictedCentralDiff += weights[b][p+windex] * diffBlock[b-p-1][l][s];
			}
		}
		
		return predictedCentralDiff;
	}
	
	private long calcHighResolutionPredSampleValue(long predictedCentralDiff, long localSum) { // EQ 37
		long highResolutionPredSampleValue = predictedCentralDiff + ((localSum - (ParameterCalc.sMid(this.depth) << 2)) << this.omega)
				+ (ParameterCalc.sMid(this.depth) << (this.omega + 2))
				+ (1 << (this.omega + 1));
		return Utils.clamp(
				highResolutionPredSampleValue, 
				ParameterCalc.sMin() << (this.omega + 2), 
				(ParameterCalc.sMax(this.depth) << (this.omega + 2)) + (1 << (this.omega + 1)));
	}
	
	private long calcDoubleResolutionSampleValue(int b, int l, int s, long highResolutionPredSampleValue, int[][][] block) { //EQ 38
		long doubleResolutionPredSampleValue = 0;
		if (s > 0 || l > 0) {
			doubleResolutionPredSampleValue = highResolutionPredSampleValue >> (this.omega + 1);
		} else if (this.predictionBands == 0 || b == 0) {
			doubleResolutionPredSampleValue = ParameterCalc.sMid(this.depth) << 1;
		} else {
			doubleResolutionPredSampleValue = block[b-1][l][s] << 1;;
		}
		return doubleResolutionPredSampleValue;
	}
	
	private long calcPredictedSampleValue(long doubleResolutionPredSampleValue) { //EQ 39
		return doubleResolutionPredSampleValue >> 1;
	}
	
	private long calcPredictionResidual(long sample, long predictedSampleValue) { //EQ 40
		 return sample - predictedSampleValue;
	}
	
	private long calcQuantizerIndex(long predResidual, long maxErrVal) { //EQ 41
		return UniformQuantizer.quantize(predResidual, maxErrVal);
	}
	
	private long calcMaxErrVal(int b, long predSampleValue) { //EQ 42, 43, 44, 45
		long maxErrVal = 0;
		if (this.useAbsoluteErrLimit && this.useRelativeErrLimit) {
			maxErrVal = Math.min(this.getAbsErrVal(b), (this.getRelErrVal(b)*Math.abs(predSampleValue)) >> this.depth); //EQ 45
		} else if (this.useRelativeErrLimit) {
			maxErrVal = (this.getRelErrVal(b)*Math.abs(predSampleValue)) >> this.depth; //EQ 44
		} else if (this.useAbsoluteErrLimit) {
			maxErrVal = this.getAbsErrVal(b); //EQ 43
		} else { //no errors
			maxErrVal = 0; //EQ 42
		}
		return maxErrVal;
	}
	
	private long calcSampleRepresentative(int l, int s, long doubleResolutionSampleRepresentative, int sample) { //EQ 46
		if (l > 0 || s > 0) {
			return (doubleResolutionSampleRepresentative + 1) >> 1;
		} else {
			return sample;
		}
	}
	
	private long calcDoubleResolutionSampleRepresentative(int b, long clippedQuantizerBinCenter, long quantizerIndex, long maxErrVal, long highResolutionPredSampleValue) { //EQ 47
		long fm = (1 << this.getResolution(b)) - this.getDamping(b);
		long sm = (clippedQuantizerBinCenter << this.omega) - (Utils.signum(quantizerIndex)*maxErrVal*this.getOffset(b) << (this.omega - this.getResolution(b)));
		long add = this.getDamping(b)*highResolutionPredSampleValue - (this.getDamping(b) << (this.omega + 1)); 
		long sby = (1 << (this.omega + this.getResolution(b) + 1)); 
		return (((fm * sm) << 2) + add) >> sby;
	}
	
	private long calcClipQuantizerBinCenter(long predictedSampleValue, long quantizerIndex, long maxErrVal) { //EQ 48
		return Utils.clamp(predictedSampleValue + quantizerIndex*(2*maxErrVal + 1), ParameterCalc.sMin(), ParameterCalc.sMax(this.depth));
	}
	
	private long calcDoubleResolutionPredictionError(long clippedQuantizerBinCenter, long doubleResolutionPredictedSampleValue) { //EQ 49
		return (clippedQuantizerBinCenter << 1) - doubleResolutionPredictedSampleValue;
	}
	
	private long calcWeightUpdateScalingExponent(int t, int samples) { //EQ 50
		return Utils.clamp(this.vmin + ((t - samples) >> this.tIncExp), this.vmin, this.vmax) + this.depth - this.omega;
	}
	
	private int updateWeight(int weight, long doubleResolutionPredictionError, long diff, long weightUpdateScalingExponent, int weightExponentOffset) { //EQ 51,52,53,54
		//first of all calculate the exponent above to see if its positive or negative
		int exponent = (int) weightUpdateScalingExponent + weightExponentOffset;
		int result = weight;
		if (exponent > 0) {
			result += ((((Utils.signumPlus((int) doubleResolutionPredictionError)*diff) >> exponent) + 1) >> 1);
		} else {
			result += ((((Utils.signumPlus((int) doubleResolutionPredictionError)*diff) << exponent) + 1) >> 1);
		}
		return Utils.clamp(
				result, 
				ParameterCalc.wMin(this.omega), 
				ParameterCalc.wMax(this.omega));
	}
	
	private long calcMappedQuantizerIndex(long quantizerIndex, long theta, long doubleResolutionPredictedSampleValue) { //EQ 55
		if (Math.abs(quantizerIndex) > theta) {
			return Math.abs(quantizerIndex) + theta;
		} else {
			long val;
			if (doubleResolutionPredictedSampleValue % 2 == 0) {
				val = quantizerIndex;
			} else {
				val = -quantizerIndex;
			}
			if (val >= 0 && theta >= val) {
				return quantizerIndex << 1;
			} else {
				return (quantizerIndex << 1) - 1;
			}
		}
	}
	
	private long calcTheta(int t, long predictedSampleValue, long maxErrVal) { //EQ 56
		if (t == 0) {
			return Math.min(predictedSampleValue - (int) ParameterCalc.sMin(), (int) ParameterCalc.sMax(this.depth) - predictedSampleValue);
		} else {
			long a = (predictedSampleValue - ParameterCalc.sMin() + maxErrVal) / (2*maxErrVal + 1);
			long b = (ParameterCalc.sMax(this.depth) - predictedSampleValue + maxErrVal) / (2*maxErrVal + 1);
			return Math.min(a, b);
		}
	}
	

	
}
