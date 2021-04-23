package ccsds123.core;

import java.io.IOException;

import com.jypec.util.bits.Bit;
import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;
import com.jypec.util.bits.BitStreamConstants;
import com.jypec.util.bits.BitTwiddling;

import ccsds123.core.hybridtables.CodeCreator;
import ccsds123.core.hybridtables.Codeword;
import ccsds123.core.hybridtables.TreeTable;

/**
 * 
 * implements 5.4.3.3 of the standard, the HYBRID entropy coder
 * @author Daniel
 *
 */
public class HybridEntropyCoder extends EntropyCoder {
	
	public HybridEntropyCoder(SamplingUnit su, CompressorParameters cp) {
		super(su, cp);
	}
	private long[] accumulator;
	private TreeTable<Codeword>[] baseTables;
	private TreeTable<Codeword>[] activeTables;
	
	@SuppressWarnings("unchecked")
	@Override
	public void reset() {
		this.accumulator = new long[this.cp.bands];
		for (int b = 0; b < accumulator.length; b++) {
			accumulator[b] = this.cp.getHInitialAcc(b);
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
		//if (k < 2)
		//	throw new IllegalStateException("Should not be 1, check eq 66");
		
		return Math.min(k, Math.max(this.cp.depth-2, 2));
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
	
	@Override
	public void code(int mappedQuantizerIndex, int t, int b, BitOutputStream bos) throws IOException {
		//generate counter for current iteration
		long counterT = this.getCounterValue(t);
		long counterTp1 = this.getCounterValue(t+1);
		long accT;
		debug(mappedQuantizerIndex, this.cp.depth, "Coding mqi");

		if (t == 0) {
			//code raw mqi value
			debug(mappedQuantizerIndex, this.cp.depth, "Write raw MQI");
			bos.writeBits(mappedQuantizerIndex, this.cp.depth, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
			accT = this.accumulator[b];
		} else {
			//output last acc bit if we are losing it
			int flushBit = (int) (this.accumulator[b] & 0x1);
			if (counterT == ((1l<<this.cp.gammaStar) - 1)) {
				flushBit |= 0x2;
			}
			
			//update accumulator for current iteration
			this.updateAcc(b, mappedQuantizerIndex, (int) counterT);
			accT = this.accumulator[b];

			boolean isHighEntropy = accT<<14 >= (long) CodeCreator.THRESHOLD[0] * counterTp1;
			int k = this.getK(counterTp1, accT);
			int codeIndex = this.getCodeIndex(accT, counterTp1);
			int inputSymbol = mappedQuantizerIndex <= CodeCreator.INPUTSYMBOLLIMIT[codeIndex] ? mappedQuantizerIndex : CodeCreator.CODE_X_VAL;
			int codeQuant = mappedQuantizerIndex - CodeCreator.INPUTSYMBOLLIMIT[codeIndex] - 1;
			TreeTable<Codeword> currentTable = this.activeTables[codeIndex];
			TreeTable<Codeword> entry = currentTable.getChild(inputSymbol);
			boolean isTree = entry.isTree();
			TreeTable<Codeword> nextTable;
			nextTable = isTree ? entry : this.baseTables[codeIndex];
			Codeword cw = entry.getValue();
			int nextTableId = nextTable.id;
			int cwVal = cw.getValue();
			int cwBits = cw.getBits();
			if (isTree) {
				cwVal = nextTable.id;				//unused
				cwBits = 0;							//unused
			}

			
			
			//output last acc bit if we are losing it
			if ((flushBit & 0x2) != 0) {
				Bit bit = flushBit % 2 == 0 ? Bit.BIT_ZERO : Bit.BIT_ONE;
				bos.writeBit(bit);
				debug(bit.toInteger(), this.cp.depth, "Write excess bit from acc: " + Long.toHexString(this.accumulator[b]));
			}
			
			//perform high or low entropy coding
			if (isHighEntropy) {
				//high entropy
				debug(mappedQuantizerIndex, k, "Write High entropy MQI + (" + accT + "," + counterTp1 + ")");
				this.reverseLengthLimitedGolombPowerOfTwoCode(mappedQuantizerIndex, k, bos);
			} else {
				//low entropy
				if (inputSymbol == CodeCreator.CODE_X_VAL) {
					debug(codeQuant, 0, "Write low entropy excess");
					this.reverseLengthLimitedGolombPowerOfTwoCode(codeQuant, 0, bos);
				}
				
				if (!isTree) { 	//output final code and reset table
					debug(cwVal, cwBits, "Write table codeword");
					bos.writeBits(cwVal, cwBits, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
				}
				this.activeTables[codeIndex] = nextTable;
			}
			
			
			this.su.ksmpl.sample(k);
			this.su.cismpl.sample(codeIndex);
			this.su.issmpl.sample(inputSymbol);
			this.su.cqsmpl.sample(codeQuant);
			this.su.fbsmpl.sample(flushBit);
			this.su.ihesmpl.sample(isHighEntropy?1:0);
			this.su.itsmpl.sample(isTree?1:0);
			this.su.ntidsmpl.sample(nextTableId);
			this.su.cwbsmpl.sample(cwBits);
			this.su.cwvsmpl.sample(cwVal);
			this.su.accsmpl.sample(accT);
			this.su.cntsmpl.sample(counterTp1);
			this.su.ctidsmpl.sample(currentTable.id);
		}
		

		
		if (t == this.cp.samplesPerBand - 1 && b == this.cp.bands - 1) {//last sample, flush things
			//flush all active tables with their flush codes
			for (int i = 0; i < 16; i++) {
				Codeword flushWord = this.activeTables[i].getValue();
				debug(flushWord.getValue(), flushWord.getBits(), "Flush table");
				bos.writeBits(flushWord.getValue(), flushWord.getBits(), BitStreamConstants.ORDERING_LEFTMOST_FIRST);
			}
			//flush accumulators
			for (int i = 0; i < this.cp.bands; i++) {
				debug(this.accumulator[i], 2 + this.cp.depth + this.cp.gammaStar, "Flush acc");
				bos.writeBits(this.accumulator[i], 2 + this.cp.depth + this.cp.gammaStar, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
			}
			//flush a single '1' bit so that the end can be identified when reading backwards
			debug(1, 1, "Mark EOS");
			bos.writeBit(Bit.BIT_ONE);
		}
	}
	
	private void updateAcc(int b, int mqi, int counter) {
		this.accumulator[b] += 4*mqi;
		if (counter == ((1<<this.cp.gammaStar) - 1)) {
			this.accumulator[b] = (this.accumulator[b] + 1) >> 1;
		}
	}
	
	private int[] decodedMQI;
	@Override
	public int decode(int t, int b, BitInputStream bis) throws IOException {
		if (t == 0 && b == 0)
			this.fullyDecode(bis);
		return decodedMQI[b*this.cp.samplesPerBand + t];
	}
	
	private void fullyDecode(BitInputStream inputBis) throws IOException {
		BitInputStream bis = inputBis.createReverseStream();
		//initialize decoding things
		TreeTable<TreeTable<Codeword>>[] reverseTables = CodeCreator.getReverseTables();
		TreeTable<TreeTable<Codeword>>[] reverseFlushTables = CodeCreator.getReverseFlushTables();
		decodedMQI = new int[this.cp.bands*this.cp.samplesPerBand];
		
		//first read until the first one
		while (bis.readBit() == Bit.BIT_ZERO)
			debug(0, 1, "Read input padding");
		debug(1, 1, "Read end of input padding");
		//read the accumulators
		this.accumulator = new long[this.cp.bands];
		for (int i = this.cp.bands - 1; i >= 0; i--) {
			this.accumulator[i] = bis.readLongBits(2 + this.cp.depth + this.cp.gammaStar, BitStreamConstants.ORDERING_RIGHTMOST_FIRST);
			debug(this.accumulator[i], 2 + this.cp.depth + this.cp.gammaStar, "Read input acc");
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
		for (int t = this.cp.samplesPerBand - 1; t >= 0; t--) {
			for (int b = this.cp.bands - 1; b >= 0; b--) {
				long counterTp1 = this.getCounterValue(t+1);
				long counterT = this.getCounterValue(t);
				long accT = this.accumulator[b];

				
				int mqi;
				if (t > 0) { //reverse accumulator calculation for next iteration
					this.su.cntsmpl.reverseUnSample(counterTp1);
					this.su.accsmpl.reverseUnSample(accT);
					//perform high or low entropy decoding
					if (accT*(1l<<14) >= (long) CodeCreator.THRESHOLD[0] * counterTp1) {
						//was coded on high entropy
						int k = this.getK(counterTp1, accT);
						mqi = this.reverseLengthLimitedGolombPowerOfTwoDecode(k, bis);
						debug(mqi, k, "Read high entropy mqi (" + accT + "," + counterTp1 + ")");
					} else {
						//low entropy
						int codeIndex = this.getCodeIndex(accT, counterTp1);
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
							int difference = this.reverseLengthLimitedGolombPowerOfTwoDecode(0, bis);
							debug(difference, 0, "Read low entropy excess");
							mqi = difference + CodeCreator.INPUTSYMBOLLIMIT[codeIndex] + 1;
						} else { //inputSymbol is mqi
							mqi = inputSymbol;
							debug(mqi, 0, "Input symbol");
						}	
					}
					

					//update accumulator for previous iteration
					this.reverseUpdateAcc(b, mqi, (int) counterT);
					//recover lost bit if renormalized
					if (counterT == ((1l<<this.cp.gammaStar) - 1)) {
						Bit bit = bis.readBit();
						if (bit == Bit.BIT_ZERO) 
							this.accumulator[b] += 1;
						
						debug(bit.toInteger(), 1, "Read excess bit from acc");
					}
					
				} else { //raw value is encoded
					mqi = bis.readBits(this.cp.depth, BitStreamConstants.ORDERING_RIGHTMOST_FIRST);
					debug(mqi, this.cp.depth, "Read raw mqi");
				}
				decodedMQI[b*this.cp.samplesPerBand + t] = mqi;

				
				debug(mqi, this.cp.depth, "Decoded mqi");
			}
		}
	}
	
	private void reverseUpdateAcc(int b, int mqi, int counter) {
		if (counter < ((1<<this.cp.gammaStar) - 1)) {
			this.accumulator[b] = this.accumulator[b] - 4*mqi;
		} else {
			this.accumulator[b] = this.accumulator[b]*2 - 4*mqi - 1;
		}
	}



	


}
