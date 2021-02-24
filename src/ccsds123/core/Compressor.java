package ccsds123.core;
import java.io.IOException;

import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;

import ccsds123.core.Constants.LocalSumType;
import ccsds123.util.Sampler;
import ccsds123.util.Utils;

public abstract class Compressor {
	
	protected Sampler<Integer> ssmpl 		= new Sampler<Integer>("c_s");
	protected Sampler<Long> drpsvsmpl 		= new Sampler<Long>("c_drpsv");
	protected Sampler<Long> psvsmpl	 		= new Sampler<Long>("c_psv");
	protected Sampler<Long> prsmpl	 		= new Sampler<Long>("c_pr");
	protected Sampler<Integer> wsmpl		= new Sampler<Integer>("c_w");
	protected Sampler<Long> wusesmpl		= new Sampler<Long>("c_wuse");
	protected Sampler<Long> drpesmpl		= new Sampler<Long>("c_drpe");
	protected Sampler<Long> drsrsmpl		= new Sampler<Long>("c_drsr");
	protected Sampler<Long> cqbcsmpl		= new Sampler<Long>("c_cqbc");
	protected Sampler<Long> mevsmpl			= new Sampler<Long>("c_mev");
	protected Sampler<Long> hrpsvsmpl		= new Sampler<Long>("c_hrpsv");
	protected Sampler<Long> pcdsmpl			= new Sampler<Long>("c_pcd");
	protected Sampler<Long> cldsmpl			= new Sampler<Long>("c_cld");
	protected Sampler<Long> nwdsmpl			= new Sampler<Long>("c_nwd");
	protected Sampler<Long> wdsmpl			= new Sampler<Long>("c_wd");
	protected Sampler<Long> ndsmpl			= new Sampler<Long>("c_nd");
	protected Sampler<Long> lssmpl			= new Sampler<Long>("c_ls");
	protected Sampler<Long> qismpl			= new Sampler<Long>("c_qi");
	protected Sampler<Long> srsmpl			= new Sampler<Long>("c_sr");
	protected Sampler<Long> tsmpl			= new Sampler<Long>("c_ts");
	protected Sampler<Long> mqismpl			= new Sampler<Long>("c_mqi");
	
	protected LocalSumType localSumType;
	protected int depth;
	protected boolean fullPredictionMode;
	protected int predictionBands;
	protected int omega;
	protected int r;
	
	protected int [] absErr;
	protected int [] relErr;
	protected boolean useAbsoluteErrLimit;
	protected boolean useRelativeErrLimit; 
	protected int absoluteErrorLimitBitDepth;
	protected int relativeErrorLimitBitDepth;
	
	protected int [] resolution;
	protected int [] damping;
	protected int [] offset;
	
	protected int tIncExp;
	protected int vmin;
	protected int vmax;
	
	protected int [] intraBandWeightExponentOffsets;
	protected int [][] interBandWeightExponentOffsets;
	
	protected int uMax;
	protected int gammaZero;
	protected int gammaStar;
	protected int [] accumulatorInitializationConstant;
	
	
	/** DEBUG STUFF */
	private static final String sampleBaseDir = "C:/Users/Daniel/Basurero/out/";
	private static final String sampleExt = ".smp";	
	
	public Compressor() {
		this.setDefaults();
		
		Sampler.setSamplePath(sampleBaseDir);
		Sampler.setSampleExt(sampleExt);
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
		this.useAbsoluteErrLimit = useAbsoluteErrLimit | absErr != null;
		this.useRelativeErrLimit = useRelativeErrLimit | relErr != null;
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
	
	protected int getAbsErrVal(int band) {
		return Utils.getVectorValue(this.absErr, band, Constants.DEFAULT_ABS_ERR_VALUE);
	}
	
	protected int getRelErrVal(int band) {
		return Utils.getVectorValue(this.relErr, band, Constants.DEFAULT_REL_ERR_VALUE);
	}
	
	protected int getResolution(int band) {
		return Utils.getVectorValue(this.resolution, band, Constants.DEFAULT_RESOLUTION_VALUE);
	}
	
	protected int getDamping(int band) {
		return Utils.getVectorValue(this.damping, band, Constants.DEFAULT_DAMPING_VALUE);
	}
	
	protected int getOffset(int band) {
		return Utils.getVectorValue(this.offset, band, Constants.DEFAULT_OFFSET_VALUE);
	}
	
	protected int getIntraBandWeightExponentOffset(int band) {
		return Utils.getVectorValue(intraBandWeightExponentOffsets, band, Constants.DEFAULT_WEIGHT_EXPONENT_OFFSET);
	}
	
	protected int getInterBandWeightExponentOffsets(int band, int p) {
		return Utils.getMatrixValue(interBandWeightExponentOffsets, band, p, Constants.DEFAULT_WEIGHT_EXPONENT_OFFSET);
	}
	
	protected int[][] getInitialWeights(int bands) { //EQ 31, 32, 33, 34
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
	
	
	public void compress(int [][][] block, int bands, int lines, int samples, BitOutputStream bos) throws IOException {
		this.checkParameterSanity(bands);
		doCompress(block, bands, lines, samples, bos);
		
		ssmpl.export();
		drpsvsmpl.export();
		psvsmpl.export();
		prsmpl.export();
		wsmpl.export();
		wusesmpl.export();
		drpesmpl.export();
		drsrsmpl.export();
		cqbcsmpl.export();
		mevsmpl.export();
		hrpsvsmpl.export();
		pcdsmpl.export();
		cldsmpl.export();
		nwdsmpl.export();
		wdsmpl.export();
		ndsmpl.export();
		lssmpl.export();
		qismpl.export();
		srsmpl.export();
		tsmpl.export();
		mqismpl.export();
	}
	
	public abstract void doCompress(int [][][] block, int bands, int lines, int samples, BitOutputStream bos) throws IOException;
	
	public abstract int[][][] decompress(int bands, int lines, int samples, BitInputStream bis) throws IOException;
	
	
	
	protected long calcHighResolutionPredSampleValue(long predictedCentralDiff, long localSum) { // EQ 37
		long highResolutionPredSampleValue = 
				Utils.modR(predictedCentralDiff + ((localSum - (ParameterCalc.sMid(this.depth) << 2)) << this.omega), this.r)
				+ (ParameterCalc.sMid(this.depth) << (this.omega + 2))
				+ (1 << (this.omega + 1));
		return Utils.clamp(
				highResolutionPredSampleValue, 
				ParameterCalc.sMin() << (this.omega + 2), 
				(ParameterCalc.sMax(this.depth) << (this.omega + 2)) + (1 << (this.omega + 1)));
	}
	
	protected long calcPredictedSampleValue(long doubleResolutionPredSampleValue) { //EQ 39
		return doubleResolutionPredSampleValue >> 1;
	}

	protected long calcPredictionResidual(long sample, long predictedSampleValue) { //EQ 40
		 return sample - predictedSampleValue;
	}
	
	protected long calcMaxErrVal(int b, long predSampleValue) { //EQ 42, 43, 44, 45
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
	
	protected long calcQuantizerIndex(long predResidual, long maxErrVal, int t) { //EQ 41
		if (t == 0)
			return predResidual;
		return UniformQuantizer.quantize(predResidual, maxErrVal);
	}
	
	protected long calcClipQuantizerBinCenter(long predictedSampleValue, long quantizerIndex, long maxErrVal) { //EQ 48
		return Utils.clamp(predictedSampleValue + quantizerIndex*(2*maxErrVal + 1), ParameterCalc.sMin(), ParameterCalc.sMax(this.depth));
	}
	
	protected long calcDoubleResolutionPredictionError(long clippedQuantizerBinCenter, long doubleResolutionPredictedSampleValue) { //EQ 49
		return (clippedQuantizerBinCenter << 1) - doubleResolutionPredictedSampleValue;
	}
	
	protected long calcDoubleResolutionSampleRepresentative(int b, long clippedQuantizerBinCenter, long quantizerIndex, long maxErrVal, long highResolutionPredSampleValue) { //EQ 47
		long fm = (1 << this.getResolution(b)) - this.getDamping(b);
		long sm = (clippedQuantizerBinCenter << this.omega) - ((Utils.signum(quantizerIndex)*maxErrVal*this.getOffset(b)) << (this.omega - this.getResolution(b)));
		long add = this.getDamping(b)*highResolutionPredSampleValue - (this.getDamping(b) << (this.omega + 1)); 
		long sby = this.omega + this.getResolution(b) + 1; 
		return (((fm * sm) << 2) + add) >> sby;
	}
	
	protected long calcWeightUpdateScalingExponent(int t, int samples) { //EQ 50
		return Utils.clamp(this.vmin + ((t - samples) >> this.tIncExp), this.vmin, this.vmax) + this.depth - this.omega;
	}
	
	
	protected int updateWeight(int weight, long doubleResolutionPredictionError, long diff, long weightUpdateScalingExponent, int weightExponentOffset, int t) { //EQ 51,52,53,54
		if (t == 0)
			throw new IllegalArgumentException("Weight updated undefined for t=0");
		//first of all calculate the exponent above to see if its positive or negative
		int exponent = (int) weightUpdateScalingExponent + weightExponentOffset;
		int result = weight;
		if (exponent > 0) {
			result += ((((Utils.signumPlus((int) doubleResolutionPredictionError)*diff) >> exponent) + 1) >> 1);
		} else {
			result += ((((Utils.signumPlus((int) doubleResolutionPredictionError)*diff) << (-exponent)) + 1) >> 1);
		}
		return Utils.clamp(
				result, 
				ParameterCalc.wMin(this.omega), 
				ParameterCalc.wMax(this.omega));
	}
	
	protected long getLowerTheta(int t, long predictedSampleValue, long maxErrVal) { //EQ 56 a
		if (t == 0) 
			return predictedSampleValue - (int) ParameterCalc.sMin();
		else 
			return (predictedSampleValue - ParameterCalc.sMin() + maxErrVal) / (2*maxErrVal + 1);
	}
	
	protected long getUpperTheta(int t, long predictedSampleValue, long maxErrVal) { //EQ 56 b
		if (t == 0) 
			return (int) ParameterCalc.sMax(this.depth) - predictedSampleValue;
		else 
			return (ParameterCalc.sMax(this.depth) - predictedSampleValue + maxErrVal) / (2*maxErrVal + 1);
	}
	
	protected long calcTheta(int t, long predictedSampleValue, long maxErrVal) { //EQ 56
		return Math.min(getLowerTheta(t, predictedSampleValue, maxErrVal), getUpperTheta(t, predictedSampleValue, maxErrVal));
	}
	
	protected long calcMappedQuantizerIndex(long quantizerIndex, long theta, long doubleResolutionPredictedSampleValue) { //EQ 55
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
				return Math.abs(quantizerIndex) << 1;
			} else {
				return (Math.abs(quantizerIndex) << 1) - 1;
			}
		}
	}
	
	protected long calcSampleRepresentative(int l, int s, long doubleResolutionSampleRepresentative, int sample) { //EQ 46
		if (l > 0 || s > 0) {
			return (doubleResolutionSampleRepresentative + 1) >> 1;
		} else {
			return sample;
		}
	}
	
	protected long calcCentralLocalDiff(int l, int s, long repr, long localSum) { //EQ 24
		/*if (l == 0 && s == 0)
			throw new IllegalArgumentException("Central local diff not defined for t=0");*/
		long res = (repr << 2) - localSum;
		/*long res;
		if (l == 0)
			res = (repBlock[b][l][s-1] << 2) - localSum;
		else if (s == 0)
			res = (repBlock[b][l-1][s] << 2) - localSum;
		else 
			res = (repBlock[b][l-1][s] << 2) - localSum;*/
		return res;
	}
}
