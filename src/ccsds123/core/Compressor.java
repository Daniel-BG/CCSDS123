package ccsds123.core;
import java.io.IOException;

import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;

import ccsds123.util.Sampler;
import ccsds123.util.Utils;

public abstract class Compressor {
	
	protected SamplingUnit su;
	protected CompressorParameters parameters;
	protected EntropyCoder entropyCoder;
	
	
	/** DEBUG STUFF */
	private static final String sampleBaseDir = "C:/Users/Daniel/Basurero/out/";
	private static final String sampleExt = ".smp";	
	
	public Compressor(EntropyCoder ec, CompressorParameters parameters, SamplingUnit su) {
		this.parameters = parameters;
		this.entropyCoder = ec;
		this.su = su;
		
		Sampler.setSamplePath(sampleBaseDir);
		Sampler.setSampleExt(sampleExt);
	}
	
	public SamplingUnit getSamplingUnit() {
		return this.su;
	}


	
	
	public void compress(int [][][] block, BitOutputStream bos) throws IOException {
		this.parameters.checkParameterSanity();
		this.parameters.printStatus();
		//system out the compression parameters
		
				
		doCompress(block, bos);
		
		this.su.export(true, this.parameters.bands, this.parameters.lines, this.parameters.samples);
	}
	
	public abstract void doCompress(int [][][] block, BitOutputStream bos) throws IOException;
	
	public abstract int[][][] decompress(BitInputStream bis) throws IOException;
	
	
	
	protected long calcHighResolutionPredSampleValue(long predictedCentralDiff, long localSum) { // EQ 37
		long highResolutionPredSampleValue = 
				Utils.modR(predictedCentralDiff + ((localSum - (ParameterCalc.sMid(this.parameters.depth) << 2)) << this.parameters.omega), this.parameters.r)
				+ (ParameterCalc.sMid(this.parameters.depth) << (this.parameters.omega + 2))
				+ (1 << (this.parameters.omega + 1));
		return Utils.clamp(
				highResolutionPredSampleValue, 
				ParameterCalc.sMin() << (this.parameters.omega + 2), 
				(ParameterCalc.sMax(this.parameters.depth) << (this.parameters.omega + 2)) + (1 << (this.parameters.omega + 1)));
	}
	
	protected long calcPredictedSampleValue(long doubleResolutionPredSampleValue) { //EQ 39
		return doubleResolutionPredSampleValue >> 1;
	}

	protected long calcPredictionResidual(long sample, long predictedSampleValue) { //EQ 40
		 return sample - predictedSampleValue;
	}
	
	protected long calcMaxErrVal(int b, long predSampleValue, int t) { //EQ 42, 43, 44, 45
		long maxErrVal = 0;
		if (t == 0)
			return 0;
		if (this.parameters.useAbsoluteErrLimit && this.parameters.useRelativeErrLimit) {
			maxErrVal = Math.min(this.parameters.getAbsErrVal(b), (this.parameters.getRelErrVal(b)*Math.abs(predSampleValue)) >> this.parameters.depth); //EQ 45
		} else if (this.parameters.useRelativeErrLimit) {
			maxErrVal = (this.parameters.getRelErrVal(b)*Math.abs(predSampleValue)) >> this.parameters.depth; //EQ 44
		} else if (this.parameters.useAbsoluteErrLimit) {
			maxErrVal = this.parameters.getAbsErrVal(b); //EQ 43
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
		return Utils.clamp(predictedSampleValue + quantizerIndex*(2*maxErrVal + 1), ParameterCalc.sMin(), ParameterCalc.sMax(this.parameters.depth));
	}
	
	protected long calcDoubleResolutionPredictionError(long clippedQuantizerBinCenter, long doubleResolutionPredictedSampleValue) { //EQ 49
		return (clippedQuantizerBinCenter << 1) - doubleResolutionPredictedSampleValue;
	}
	
	protected long calcDoubleResolutionSampleRepresentative(int b, long clippedQuantizerBinCenter, long quantizerIndex, long maxErrVal, long highResolutionPredSampleValue) { //EQ 47
		long fm = (1 << this.parameters.getResolution(b)) - this.parameters.getDamping(b);
		long sm = (clippedQuantizerBinCenter << this.parameters.omega) - ((Utils.signum(quantizerIndex)*maxErrVal*this.parameters.getOffset(b)) << (this.parameters.omega - this.parameters.getResolution(b)));
		long add = this.parameters.getDamping(b)*highResolutionPredSampleValue - (this.parameters.getDamping(b) << (this.parameters.omega + 1)); 
		long sby = this.parameters.omega + this.parameters.getResolution(b) + 1; 
		return (((fm * sm) << 2) + add) >> sby;
	}
	
	protected long calcWeightUpdateScalingExponent(int t, int samples) { //EQ 50
		return Utils.clamp(this.parameters.vmin + ((t - samples) >> this.parameters.tIncExp), this.parameters.vmin, this.parameters.vmax) + this.parameters.depth - this.parameters.omega;
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
				ParameterCalc.wMin(this.parameters.omega), 
				ParameterCalc.wMax(this.parameters.omega));
	}
	
	protected long getLowerTheta(int t, long predictedSampleValue, long maxErrVal) { //EQ 56 a
		if (t == 0) 
			return predictedSampleValue - (int) ParameterCalc.sMin();
		else 
			return (predictedSampleValue - ParameterCalc.sMin() + maxErrVal) / (2*maxErrVal + 1);
	}
	
	protected long getUpperTheta(int t, long predictedSampleValue, long maxErrVal) { //EQ 56 b
		if (t == 0) 
			return (int) ParameterCalc.sMax(this.parameters.depth) - predictedSampleValue;
		else 
			return (ParameterCalc.sMax(this.parameters.depth) - predictedSampleValue + maxErrVal) / (2*maxErrVal + 1);
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
