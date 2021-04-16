package ccsds123.core;

import java.io.IOException;

import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;
import com.jypec.util.bits.BitStreamConstants;

/**
 * implements 5.4.3.2 of the standard
 * @author Daniel
 *
 */
public class SampleAdaptiveEntropyCoder extends EntropyCoder {
	
	
	public SampleAdaptiveEntropyCoder(SamplingUnit su, CompressorParameters cp) {
		super(su, cp);
	}

	private int [] accumulator;
	
	
	@Override
	public void reset() {
		this.accumulator = new int[this.cp.bands];
		for (int b = 0; b < accumulator.length; b++) {
			accumulator[b] = this.cp.getSAInitialAcc(b);
		}
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
			uIntCodeIndex = Math.min(uIntCodeIndex, this.cp.depth-2);
		}
		return uIntCodeIndex;
	}
	
	private void updateAccumulator(int b, int t, int mappedQuantizerIndex, int cValue) {
		this.accumulator[b] += mappedQuantizerIndex;
		if (cValue == (1 << this.cp.gammaStar) - 1) {
			this.accumulator[b] = (this.accumulator[b] + 1) >> 1;
		}
	}
	
	@Override
	public void code(int mappedQuantizerIndex, int t, int b, BitOutputStream bos) throws IOException {
		if (t == 0) {
			bos.writeBits(this.su.uismpl.sample(mappedQuantizerIndex), this.cp.depth, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
		} else {
			int cValue = this.getCounterValue(t);
			int uInt = this.su.uismpl.sample(mappedQuantizerIndex);
			int uIntCodeIndex = this.su.uicismpl.sample(this.getUintCodeIndex(b, t, cValue));
			//code
			this.lengthLimitedGolombPowerOfTwoCode(uInt, uIntCodeIndex, bos);
			//update accumulator
			this.updateAccumulator(b, t, mappedQuantizerIndex, cValue);
			this.su.accsmpl.sample((long) this.accumulator[b]);
		}
	}
	
	@Override
	public int decode(int t, int b, BitInputStream bis) throws IOException {
		if (t == 0) {
			int res = this.su.uismpl.unSample(bis.readBits(this.cp.depth, BitStreamConstants.ORDERING_LEFTMOST_FIRST));
			return res;
		} else {
			int cValue = this.getCounterValue(t);
			int uIntCodeIndex = this.su.uicismpl.unSample(this.getUintCodeIndex(b, t, cValue));
			int uInt = this.su.uismpl.unSample(this.lengthLimitedGolombPowerOfTwoDecode(uIntCodeIndex, bis));
			//update accumulator
			this.updateAccumulator(b, t, uInt, cValue);
			this.su.accsmpl.unSample((long) this.accumulator[b]);
			return uInt;
		}
	}
	
}
