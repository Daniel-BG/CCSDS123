package ccsds123.core;

import java.io.IOException;

import com.jypec.util.bits.Bit;
import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;
import com.jypec.util.bits.BitStreamConstants;

public abstract class EntropyCoder {
	
	public abstract void reset(int uMax, int depth, int bands, int samplesPerBand, int gammaZero, int gammaStar, int [] accumulatorInitializationConstant, SamplingUnit su);
	public abstract void code(int mappedQuantizerIndex, int t, int b, BitOutputStream bos) throws IOException;
	public abstract int decode(int t, int b, BitInputStream bis) throws IOException;
	
	protected int getHybridCounterValue(int t, int gammaStar, int gammaZero) {
		return this.getCounterValue(t+1, gammaStar, gammaZero);
	}
	
	protected int getCounterValue(int t, int gammaStar, int gammaZero) {
		int cThresh = (1 << gammaStar) - (1 << gammaZero);
		int cOflow  = (t - ((1 << gammaStar) - (1 << gammaZero) + 1)) % (1 << (gammaStar - 1));
		int cValue = t <= cThresh ? (1 << gammaZero) - 1 + t : ((1 << (gammaStar - 1)) + cOflow);
				
		//sanity check
		if (cValue >= 1 << gammaStar)
			throw new IllegalStateException("This value is too high, something is wrong in the calculation");
		return cValue;
	}
	
	protected void lengthLimitedGolombPowerOfTwoCode(int uInt, int uIntCodeIndex,  BitOutputStream bos, int uMax, int depth) throws IOException {
		int threshold = uInt >> uIntCodeIndex;
		if (threshold < uMax) {
			//threshold zeroes + 1 + uIntCodeIndex lsbs of uInt
			bos.writeBits(0, threshold, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
			bos.writeBit(Bit.BIT_ONE);
			bos.writeBits(uInt, uIntCodeIndex, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
		} else {
			//uMax zeroes + D-bit binary repr of uInt
			bos.writeBits(0, uMax, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
			bos.writeBits(uInt, depth, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
		}
	}
	
	protected int limitedGolombPowerOfTwoDecode(int uIntCodeIndex, BitInputStream bis, int uMax, int depth, BitStreamConstants ordering) throws IOException {		
		int threshold = 0;
		do {
			Bit bit = bis.readBit();
			if (bit == Bit.BIT_ONE)
				break;
			threshold++;
		} while (threshold < uMax);
		
		if (threshold == uMax) {
			int res = bis.readBits(depth, ordering); 
			return res;
		} else {
			return (threshold << uIntCodeIndex) | bis.readBits(uIntCodeIndex, ordering);
		}
	}
	
	protected int lengthLimitedGolombPowerOfTwoDecode(int uIntCodeIndex, BitInputStream bis, int uMax, int depth) throws IOException {
		return limitedGolombPowerOfTwoDecode(uIntCodeIndex, bis, uMax, depth, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
	}
	
	
	protected void reverseLengthLimitedGolombPowerOfTwoCode(int uInt, int uIntCodeIndex,  BitOutputStream bos, int uMax, int depth) throws IOException {
		int threshold = uInt >> uIntCodeIndex;
		if (threshold < uMax) {
			//uIntCodeIndex lsbs of uInt + 1 + threshold zeroes
			bos.writeBits(uInt, uIntCodeIndex, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
			bos.writeBit(Bit.BIT_ONE);
			bos.writeBits(0, threshold, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
		} else {
			//D-bit binary repr of uInt + uMax zeroes
			bos.writeBits(uInt, depth, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
			bos.writeBits(0, uMax, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
		}
	}
	
	
	protected int reverseLengthLimitedGolombPowerOfTwoDecode(int uIntCodeIndex, BitInputStream bis, int uMax, int depth) throws IOException {		
		return limitedGolombPowerOfTwoDecode(uIntCodeIndex, bis, uMax, depth, BitStreamConstants.ORDERING_RIGHTMOST_FIRST);
	}

}