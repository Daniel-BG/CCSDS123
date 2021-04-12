package ccsds123.core;

import java.io.IOException;
import java.util.Stack;

import com.jypec.util.bits.Bit;
import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;
import com.jypec.util.bits.BitStreamConstants;
import com.jypec.util.bits.BitTwiddling;

import ccsds123.core.hybridtables.CodeCreator;
import ccsds123.core.hybridtables.Codeword;
import ccsds123.core.hybridtables.TreeTable;
import ccsds123.util.Utils;

/**
 * 
 * implements 5.4.3.3 of the standard, the HYBRID entropy coder
 * @author Daniel
 *
 */
public class HybridEntropyCoder extends EntropyCoder {
	
	private int uMax;
	private int gammaStar, gammaZero;
	private long[] accumulator;
	private int depth;
	private TreeTable<Codeword>[] baseTables;
	private TreeTable<Codeword>[] activeTables;
	private int bands;
	private int samplesPerBand;
	
	@SuppressWarnings("unchecked")
	@Override
	public void reset(int uMax, int depth, int bands, int samplesPerBand, int gammaZero, int gammaStar, int[] accumulatorInitializationConstant, SamplingUnit su) {
		this.uMax = uMax; //8 <= umax <= 32
		this.gammaStar = gammaStar;
		this.gammaZero = gammaZero;
		this.depth = depth;
		this.bands = bands;
		this.samplesPerBand = samplesPerBand;
		this.accumulator = new long[bands];
		for (int b = 0; b < accumulator.length; b++) {
			int meanMQIestimate = 5;
			accumulator[b] = Utils.getVectorValue(accumulatorInitializationConstant, b, 4*(1 << gammaZero)*meanMQIestimate);
		}
		this.baseTables = CodeCreator.getCodeTables();
		this.activeTables = (TreeTable<Codeword>[]) new TreeTable<?>[16];
		for (int i = 0; i < this.baseTables.length; i++) {
			this.activeTables[i] = this.baseTables[i];
		}
	}
	
	
	private int getK(long counter, long accumulator) {
		int k = 1;
		while (counter*(1l << (k+1+2)) <= accumulator + ((49*counter) >> 5))
			k++;
		if (k < 2)
			throw new IllegalStateException("Should not be 1, check eq 66");
		
		return Math.min(k, Math.max(this.depth-2, 2));
	}
	
	private int getCodeIndex(long acc, long counter) {
		int codeIndex = 0;
		for (int i = 0; i < 16; i++) {
			if (acc<<14 < (long) counter * (long) CodeCreator.THRESHOLD[i])
				codeIndex = i;
			else
				break;
		}
		return codeIndex;
	}
	
	private void debug(long value, int bits, String text) {
		//System.out.println(text + " -> " + bits + "'h" + Long.toHexString(value));
	}
	
	Stack<Long> debugCnts = new Stack<Long>();
	Stack<Long> debugAccs = new Stack<Long>();
	
	@Override
	public void code(int mappedQuantizerIndex, int t, int b, BitOutputStream bos) throws IOException {
		//generate counter for current iteration
		long counterTm1 = this.getHybridCounterValue(t-1, this.gammaStar, this.gammaZero);
		long counterT = this.getHybridCounterValue(t, this.gammaStar, this.gammaZero);
		long accT;
		debug(mappedQuantizerIndex, this.depth, "Coding mqi");

		if (t == 0) {
			//code raw mqi value
			debug(mappedQuantizerIndex, this.depth, "Write raw MQI");
			bos.writeBits(mappedQuantizerIndex, this.depth, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
			accT = this.accumulator[b];
		} else {
			//output last acc bit if we are losing it
			if (counterTm1 == ((1l<<this.gammaStar) - 1)) {
				Bit bit = this.accumulator[b] % 2 == 0 ? Bit.BIT_ZERO : Bit.BIT_ONE;
				bos.writeBit(bit);
				debug(bit.toInteger(), this.depth, "Write excess bit from acc: " + Long.toHexString(this.accumulator[b]));
			}
			//update accumulator for current iteration
			this.updateAcc(b, mappedQuantizerIndex, (int) counterTm1);
			accT = this.accumulator[b];

			
			//perform high or low entropy coding
			if (accT*(1l<<14) >= (long) CodeCreator.THRESHOLD[0] * counterT) {
				//high entropy
				int k = this.getK(counterT, accT);
				debug(mappedQuantizerIndex, k, "Write High entropy MQI + (" + accT + "," + counterT + ")");
				this.reverseLengthLimitedGolombPowerOfTwoCode(mappedQuantizerIndex, k, bos, this.uMax, this.depth);
			} else {
				//low entropy
				int codeIndex = this.getCodeIndex(accT, counterT);
				int inputSymbol = mappedQuantizerIndex <= CodeCreator.INPUTSYMBOLLIMIT[codeIndex] ? mappedQuantizerIndex : CodeCreator.CODE_X_VAL;
				if (inputSymbol == CodeCreator.CODE_X_VAL) {
					int codeQuant = mappedQuantizerIndex - CodeCreator.INPUTSYMBOLLIMIT[codeIndex] - 1;
					debug(codeQuant, 0, "Write low entropy excess");
					this.reverseLengthLimitedGolombPowerOfTwoCode(codeQuant, 0, bos, this.uMax, this.depth);
				}
				TreeTable<Codeword> currentTable = this.activeTables[codeIndex];
				TreeTable<Codeword> entry = currentTable.getChild(inputSymbol); 
				if (entry.isTree()) { //go to next node
					this.activeTables[codeIndex] = entry; 
				} else {	//output final code and reset table
					Codeword cw = entry.getValue();
					debug(cw.getValue(), cw.getBits(), "Write table codeword");
					bos.writeBits(cw.getValue(), cw.getBits(), BitStreamConstants.ORDERING_LEFTMOST_FIRST);
					this.activeTables[codeIndex] = this.baseTables[codeIndex];
				}
			}
		}
		
		debugCnts.add(counterT);
		debugAccs.add(accT);
		
		if (t == this.samplesPerBand - 1 && b == this.bands - 1) {//last sample, flush things
			//flush all active tables with their flush codes
			for (int i = 0; i < 16; i++) {
				Codeword flushWord = this.activeTables[i].getValue();
				debug(flushWord.getValue(), flushWord.getBits(), "Flush table");
				bos.writeBits(flushWord.getValue(), flushWord.getBits(), BitStreamConstants.ORDERING_LEFTMOST_FIRST);
			}
			//flush accumulators
			for (int i = 0; i < this.bands; i++) {
				debug(this.accumulator[i], 2 + this.depth + this.gammaStar, "Flush acc");
				bos.writeBits(this.accumulator[i], 2 + this.depth + this.gammaStar, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
			}
			//flush a single '1' bit so that the end can be identified when reading backwards
			debug(1, 1, "Mark EOS");
			bos.writeBit(Bit.BIT_ONE);
		}
	}
	
	private void updateAcc(int b, int mqi, int counter) {
		if (counter < ((1<<this.gammaStar) - 1)) {
			this.accumulator[b] = this.accumulator[b] + 4*mqi;
		} else {
			this.accumulator[b] = (this.accumulator[b] + 4*mqi + 1) >> 1;
		}
	}
	
	private int[] decodedMQI;
	@Override
	public int decode(int t, int b, BitInputStream bis) throws IOException {
		if (t == 0 && b == 0)
			this.fullyDecode(bis);
		return decodedMQI[b*this.samplesPerBand + t];
	}
	
	private void fullyDecode(BitInputStream inputBis) throws IOException {
		BitInputStream bis = inputBis.createReverseStream();
		//initialize decoding things
		TreeTable<TreeTable<Codeword>>[] reverseTables = CodeCreator.getReverseTables();
		TreeTable<TreeTable<Codeword>>[] reverseFlushTables = CodeCreator.getReverseFlushTables();
		decodedMQI = new int[this.bands*this.samplesPerBand];
		
		//first read until the first one
		while (bis.readBit() == Bit.BIT_ZERO)
			debug(0, 1, "Read input padding");
		debug(1, 1, "Read end of input padding");
		//read the accumulators
		this.accumulator = new long[this.bands];
		for (int i = this.bands - 1; i >= 0; i--) {
			this.accumulator[i] = bis.readLongBits(2 + this.depth + this.gammaStar, BitStreamConstants.ORDERING_RIGHTMOST_FIRST);
			debug(this.accumulator[i], 2 + this.depth + this.gammaStar, "Read input acc");
		}
		//read the flush tables
		for (int i = 15; i >= 0; i--) {
			TreeTable<TreeTable<Codeword>> rft = reverseFlushTables[i];
			int codeword = 0, codelength = 0;
			while (rft.isTree()) {
				Bit bit = bis.readBit();
				codeword = (codeword << 1) | bit.toInteger();
				codelength++;
				rft = rft.getChild(bit.toInteger());
			}
			codeword = BitTwiddling.reverseBits(codeword, codelength);
			debug(codeword, codelength, "Read flush table code");
			this.activeTables[i] = rft.getValue();
		}
		
		//now we invert the coding operation (always BIP mode)
		for (int t = this.samplesPerBand - 1; t >= 0; t--) {
			for (int b = this.bands - 1; b >= 0; b--) {
				long counterT = this.getHybridCounterValue(t, this.gammaStar, this.gammaZero);
				long counterTm1 = this.getHybridCounterValue(t-1, this.gammaStar, this.gammaZero);
				long accT = this.accumulator[b];
				
				long codeCnt = debugCnts.pop();
				if (counterT != codeCnt) 
					throw new IllegalStateException("(" + codeCnt + "->" + counterT + ")");
				long codeAcc = debugAccs.pop();
				if (accT != codeAcc)
					throw new IllegalStateException("(" + codeAcc + "->" + accT + ")");
				
				int mqi;
				if (t > 0) { //reverse accumulator calculation for next iteration
					//perform high or low entropy decoding
					if (accT*(1l<<14) >= (long) CodeCreator.THRESHOLD[0] * counterT) {
						//was coded on high entropy
						int k = this.getK(counterT, accT);
						mqi = this.reverseLengthLimitedGolombPowerOfTwoDecode(k, bis, this.uMax, this.depth);
						debug(mqi, k, "Read high entropy mqi (" + accT + "," + counterT + ")");
					} else {
						//low entropy
						int codeIndex = this.getCodeIndex(accT, counterT);
						//if current table is root, we need to read a new table
						if (this.activeTables[codeIndex].isRoot()) {
							TreeTable<TreeTable<Codeword>> rt = reverseTables[codeIndex];
							int codeword = 0, codelength = 0;
							while (rt.isTree()) {
								Bit bit = bis.readBit();
								codeword = (codeword << 1) | bit.toInteger();
								codelength++;
								rt = rt.getChild(bit.toInteger());
							}
							codeword = BitTwiddling.reverseBits(codeword, codelength);
							debug(codeword, codelength, "Read flush table code");
							this.activeTables[codeIndex] = rt.getValue();
						}
						//get symbol and update current table
						int inputSymbol = this.activeTables[codeIndex].getParentIndex();
						this.activeTables[codeIndex] = this.activeTables[codeIndex].getParent(); 
						if (inputSymbol == CodeCreator.CODE_X_VAL) {
							int difference = this.reverseLengthLimitedGolombPowerOfTwoDecode(0, bis, this.uMax, this.depth);
							debug(difference, 0, "Read low entropy excess");
							mqi = difference + CodeCreator.INPUTSYMBOLLIMIT[codeIndex] + 1;
						} else { //inputSymbol is mqi
							mqi = inputSymbol;
							debug(mqi, 0, "Input symbol");
						}	
					}
					

					//update accumulator for previous iteration
					this.reverseUpdateAcc(b, mqi, (int) counterTm1);
					//recover lost bit if renormalized
					if (counterTm1 == ((1l<<this.gammaStar) - 1)) {
						Bit bit = bis.readBit();
						if (bit == Bit.BIT_ZERO) 
							this.accumulator[b] += 1;
						
						debug(bit.toInteger(), 1, "Read excess bit from acc");
					}
					
				} else { //raw value is encoded
					mqi = bis.readBits(this.depth, BitStreamConstants.ORDERING_RIGHTMOST_FIRST);
					debug(mqi, this.depth, "Read raw mqi");
				}
				decodedMQI[b*this.samplesPerBand + t] = mqi;

				
				debug(mqi, this.depth, "Decoded mqi");
			}
		}
	}
	
	private void reverseUpdateAcc(int b, int mqi, int counter) {
		if (counter < ((1<<this.gammaStar) - 1)) {
			this.accumulator[b] = this.accumulator[b] - 4*mqi;
		} else {
			this.accumulator[b] = this.accumulator[b]*2 - 4*mqi - 1;
		}
	}



	


}
