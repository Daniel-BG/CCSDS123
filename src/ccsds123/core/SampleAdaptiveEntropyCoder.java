package ccsds123.core;

import java.io.IOException;

import com.jypec.util.bits.Bit;
import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;
import com.jypec.util.bits.BitStreamConstants;

import ccsds123.util.Utils;

/**
 * implements 5.4.3.2 of the standard
 * 5.4.3.3 is the HYBRID version with other, more complex options
 * @author Daniel
 *
 */
public class SampleAdaptiveEntropyCoder extends EntropyCoder {
	
	private int uMax;
	private int depth;
	private int gammaZero;
	private int gammaStar;
	
	private int [] accumulator;
	private int counterResetValue;
	
	private SamplingUnit su;
	
	public SampleAdaptiveEntropyCoder(int uMax, int depth, int bands, int gammaZero, int gammaStar, int [] accumulatorInitializationConstant, SamplingUnit su) {
		this.reset(uMax, depth, bands, gammaZero, gammaStar, accumulatorInitializationConstant, su);
	}
	
	public void reset(int uMax, int depth, int bands, int gammaZero, int gammaStar, int [] accumulatorInitializationConstant, SamplingUnit su) {
		if (uMax < 8 || uMax > 32)
			throw new IllegalArgumentException("Umax out of bounds!");
		this.uMax = uMax;
		if (gammaZero < 1 || gammaZero > 8) 
			throw new IllegalArgumentException("GammaZero out of bounds!");
		this.gammaZero = gammaZero;
		if (gammaStar < Math.max(4, this.gammaZero + 1) || gammaStar > 11)
			throw new IllegalArgumentException("GammaStar out of bounds!");
		this.gammaStar = gammaStar;
		
		this.depth = depth;
		this.accumulator = new int[bands];
		this.counterResetValue = 1 << this.gammaZero;
		for (int b = 0; b < accumulator.length; b++) {
			accumulator[b] = this.calcInitialAccValue(Utils.getVectorValue(accumulatorInitializationConstant, b, Constants.DEFAULT_ACC_INIT_CONSTANT));
		}
		this.su = su;
	}
	
	private int calcInitialAccValue(int constant) {
		int modifiedConstant;
		if (constant <= 30 - this.depth) {
			modifiedConstant = constant;
		} else {
			modifiedConstant = 2*constant + this.depth - 30;
		}
		return ((3*(1 << (modifiedConstant + 6)) - 49)*this.counterResetValue) >> 7;
	}
	

	
	
	private int getUintCodeIndex(int b, int t, int cValue) {
		int uIntCodeIndex;
		if (2*cValue > this.accumulator[b] + ((49*cValue) >> 7)) {
			uIntCodeIndex = 0;
		} else {
			uIntCodeIndex = 0;
			while (cValue << (uIntCodeIndex + 1) <= this.accumulator[b] + ((49*cValue) >> 7)) {
				uIntCodeIndex++;
			}
			uIntCodeIndex = Math.min(uIntCodeIndex, this.depth-2);
		}
		return uIntCodeIndex;
	}
	
	private void updateAccumulator(int b, int t, int mappedQuantizerIndex, int cValue) {
		this.accumulator[b] += mappedQuantizerIndex;
		if (cValue == (1 << this.gammaStar) - 1) {
			this.accumulator[b] = (this.accumulator[b] + 1) >> 1;
		}
	}
	
	@Override
	public void code(int mappedQuantizerIndex, int t, int b, BitOutputStream bos) throws IOException {
		if (t == 0) {
			bos.writeBits(this.su.uismpl.sample(mappedQuantizerIndex), this.depth, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
		} else {
			int cValue = this.getCounterValue(t, this.gammaStar, this.gammaZero);
			int uInt = this.su.uismpl.sample(mappedQuantizerIndex);
			int uIntCodeIndex = this.su.uicismpl.sample(this.getUintCodeIndex(b, t, cValue));
			//code
			this.lengthLimitedGolombPowerOfTwoCode(uInt, uIntCodeIndex, bos, this.uMax, this.depth);
			//update accumulator
			this.updateAccumulator(b, t, mappedQuantizerIndex, cValue);
			this.su.accsmpl.sample(this.accumulator[b]);
		}
	}
	
	@Override
	public int decode(int t, int b, BitInputStream bis) throws IOException {
		if (t == 0) {
			int res = this.su.uismpl.unSample(bis.readBits(this.depth, BitStreamConstants.ORDERING_LEFTMOST_FIRST));
			return res;
		} else {
			int cValue = this.getCounterValue(t, this.gammaStar, this.gammaZero);;
			int uIntCodeIndex = this.su.uicismpl.unSample(this.getUintCodeIndex(b, t, cValue));
			int uInt = this.su.uismpl.unSample(this.lengthLimitedGolombPowerOfTwoDecode(uIntCodeIndex, bis, this.uMax, this.depth));
			//update accumulator
			this.updateAccumulator(b, t, uInt, cValue);
			this.su.accsmpl.unSample(this.accumulator[b]);
			return uInt;
		}
	}
	
}
