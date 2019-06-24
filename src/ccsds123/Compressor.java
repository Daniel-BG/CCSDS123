package ccsds123;

import com.jypec.util.bits.BitOutputStream;

/**
 * Image compression is always done in BIP traversal mode. Increase band, then sample, then line.
 * Input values are assumed possitive
 * @author Daniel
 *
 */
public class Compressor {
	
	public static enum LocalSumType {
		WIDE_NEIGHBOR_ORIENTED,
		NARROW_NEIGHBOR_ORIENTED,
		WIDE_COLUMN_ORIENTED,
		NARROW_COLUMN_ORIENTED
	}
	
	
	private LocalSumType localSumType = LocalSumType.WIDE_NEIGHBOR_ORIENTED;
	private int depth = 16;
	private boolean fullPredictionMode = true;
	private int predictionBands = 3;
	private int omega = 8;
	
	private int [] absErr;
	private int [] relErr;
	private boolean useAbsoluteErrLimit;
	private boolean useRelativeErrLimit; 
	
	private int [] resolution;
	private int [] damping;
	private int [] offset;
	
	private int tIncExp = 4;
	private int vmin = -6;
	private int vmax = 9;
	
	private int [][] weightExponentOffsets;
	
	/**
	 * Set parameters for compression
	 */
	public void set(LocalSumType lst, int depth, boolean fullPredictionMode, int predictionBands, int omega, 
			int[] absErr, int[] relErr, 
			int[] resolution, int[] damping, int[] offset,
			int tIncExp, int vmin, int vmax,
			int[][] weightExponentOffsets) {
		
		this.tIncExp = tIncExp;
		this.vmax = vmax;
		this.vmin = vmin;
		
		
		this.localSumType = lst;
		if (depth < 2 || depth > 32) {
			throw new IllegalArgumentException("Depth out of bounds");
		}
		this.depth = depth;
		this.fullPredictionMode = fullPredictionMode;
		this.predictionBands = predictionBands;
		this.omega = omega;
		
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
		this.resolution = resolution;
		this.damping = damping;
		this.offset = offset;
		
		this.weightExponentOffsets = weightExponentOffsets;
	}
	
	/**
	 * Generic template to extract a value from a vector which can be null,
	 * or have 1 element that should be repeated,
	 * or have as many elements as required
	 * @param vector
	 * @param band
	 * @param defaultVal
	 * @return
	 */
	private int getVectorValue(int [] vector, int band, int defaultVal) {
		if (vector == null) {
			return defaultVal;
		} else if (vector.length == 1) {
			return vector[0];
		} else {
			return vector[band];
		}
	}
	
	private int getAbsErrVal(int band) {
		return this.getVectorValue(this.absErr, band, 0);
	}
	
	private int getRelErrVal(int band) {
		return this.getVectorValue(this.relErr, band, 0);
	}
	
	private int getResolution(int band) {
		return this.getVectorValue(this.resolution, band, 0);
	}
	
	private int getDamping(int band) {
		return this.getVectorValue(this.damping, band, 0);
	}
	
	private int getOffset(int band) {
		return this.getVectorValue(this.offset, band, 0);
	}
	
	
	

	/**
	 * 
	 * @param block block[band][line][sample]
	 * @param bands
	 * @param lines
	 * @param samples
	 * @param bos
	 */
	public void compress(int [][][] block, int bands, int lines, int samples, BitOutputStream bos) {
		
		int [][][] repBlock = new int[bands][lines][samples];
		int [][][] diffBlock = new int[bands][lines][samples];

		
		////WEIGHT INITIALIZATION 4.6.3.2
		int [][] weights;
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
		//WEIGHT INITIALIZATION END
		
		SampleAdaptiveEntropyCoder entropyCoder = new SampleAdaptiveEntropyCoder();
		
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
						//north
						weights[b][0] = this.updateWeight(weights[b][0], doubleResolutionPredictionError, northDiff, weightUpdateScalingExponent, weightExponentOffsets[b][0]);
						//west
						weights[b][1] = this.updateWeight(weights[b][1], doubleResolutionPredictionError, westDiff, weightUpdateScalingExponent, weightExponentOffsets[b][1]);
						//northwest
						weights[b][2] = this.updateWeight(weights[b][2], doubleResolutionPredictionError, northWestDiff, weightUpdateScalingExponent, weightExponentOffsets[b][2]);
						windex = 3;
					}
					for (int p = 0; p < this.predictionBands; p++) {
						if (b - p > 0) 
							weights[b][windex+p] = this.updateWeight(weights[b][windex+p], doubleResolutionPredictionError, diffBlock[b-p-1][l][s], weightUpdateScalingExponent, weightExponentOffsets[b][windex+p]);
					}
					
					//MAPPED QUANTIZER INDEX 4.11
					long theta = this.calcTheta(t, predictedSampleValue, maxErrVal);
					long mappedQuantizerIndex = calcMappedQuantizerIndex(quantizerIndex, theta, doubleResolutionPredSampleValue);
					
					entropyCoder.code(mappedQuantizerIndex);
				}
			}
		}
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
		return Util.clamp(
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
		long sm = (clippedQuantizerBinCenter << this.omega) - (Util.signum(quantizerIndex)*maxErrVal*this.getOffset(b) << (this.omega - this.getResolution(b)));
		long add = this.getDamping(b)*highResolutionPredSampleValue - (this.getDamping(b) << (this.omega + 1)); 
		long sby = (1 << (this.omega + this.getResolution(b) + 1)); 
		return (((fm * sm) << 2) + add) >> sby;
	}
	
	private long calcClipQuantizerBinCenter(long predictedSampleValue, long quantizerIndex, long maxErrVal) { //EQ 48
		return Util.clamp(predictedSampleValue + quantizerIndex*(2*maxErrVal + 1), ParameterCalc.sMin(), ParameterCalc.sMax(this.depth));
	}
	
	private long calcDoubleResolutionPredictionError(long clippedQuantizerBinCenter, long doubleResolutionPredictedSampleValue) { //EQ 49
		return (clippedQuantizerBinCenter << 1) - doubleResolutionPredictedSampleValue;
	}
	
	private long calcWeightUpdateScalingExponent(int t, int samples) { //EQ 50
		return Util.clamp(this.vmin + ((t - samples) >> this.tIncExp), this.vmin, this.vmax) + this.depth - this.omega;
	}
	
	private int updateWeight(int weight, long doubleResolutionPredictionError, long diff, long weightUpdateScalingExponent, int weightExponentOffset) { //EQ 51,52,53,54
		//first of all calculate the exponent above to see if its positive or negative
		int exponent = (int) weightUpdateScalingExponent + weightExponentOffset;
		int result = weight;
		if (exponent > 0) {
			result += ((((Util.signumPlus((int) doubleResolutionPredictionError)*diff) >> exponent) + 1) >> 1);
		} else {
			result += ((((Util.signumPlus((int) doubleResolutionPredictionError)*diff) << exponent) + 1) >> 1);
		}
		return Util.clamp(
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
