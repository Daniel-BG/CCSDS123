package ccsds123.core;

import java.io.IOException;

import com.jypec.util.bits.Bit;
import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;
import com.jypec.util.bits.BitStreamConstants;

import ccsds123.core.hybridtables.CodeCreator;
import ccsds123.core.hybridtables.Codeword;
import ccsds123.core.hybridtables.TableEntry;
import ccsds123.core.hybridtables.TreeTable;
import ccsds123.util.Utils;

/**
 * 
 * implements 5.4.3.3 of the standard, the HYBRID entropy coder
 * @author Daniel
 *
 */
public class HybridEntropyCoder extends EntropyCoder {
	
	
	private static final int[] INPUTSYMBOLLIMIT = {	12, 	10, 	8, 		6, 
													6, 		4, 		4, 		4,
													2,		2, 		2, 		2, 
													2, 		2, 		2, 		0};

	private static final int[] THRESHOLD = 		  {	303336, 225404, 166979, 128672, 
													95597, 	69670, 	50678, 	34898, 
													23331, 	14935, 	9282, 	5510, 
													3195, 	1928, 	1112, 	408};
	
	private int uMax;
	private int gammaStar, gammaZero;
	private long[] accumulator;
	private int depth;
	private TreeTable<Codeword>[] baseTables;
	private TreeTable<Codeword>[] activeTables;
	private int bands;
	private int samplesPerBand;
	
	public HybridEntropyCoder(int uMax, int gammaStar, int gammaZero, int depth, int bands, int samplesPerBand, int [] accumulatorInitializationConstant) {
		this.uMax = uMax; //8 <= umax <= 32
		this.gammaStar = gammaStar;
		this.gammaZero = gammaZero;
		this.depth = depth;
		this.bands = bands;
		this.samplesPerBand = samplesPerBand;
		int meanMQIestimate = 5;
		for (int b = 0; b < accumulator.length; b++) {
			accumulator[b] = Utils.getVectorValue(accumulatorInitializationConstant, b, 4*(1 << gammaZero)*meanMQIestimate);
		}
		this.baseTables = CodeCreator.getCodeTables();
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
			if (acc<<14 < (long) counter * (long) THRESHOLD[i])
				codeIndex = i;
			else
				break;
		}
		return codeIndex;
	}
	
	@Override
	public void code(int mappedQuantizerIndex, int t, int b, BitOutputStream bos) throws IOException {
		//generate counter for current iteration
		long counterT = this.getCounterValue(t+1, this.gammaStar, this.gammaZero);
		long accT = this.accumulator[b];
		if (t == 0) {
			//code raw mqi value
			bos.writeBits(mappedQuantizerIndex, this.depth, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
		} else {
			//perform high or low entropy coding
			if (accT*(1l<<14) >= (long) THRESHOLD[0] * counterT) {
				//high entropy
				int k = this.getK(counterT, accT);
				this.reverseLengthLimitedGolombPowerOfTwoCode(mappedQuantizerIndex, k, bos, this.uMax, this.depth);
			} else {
				//low entropy
				int codeIndex = this.getCodeIndex(accT, counterT);
				int inputSymbol = mappedQuantizerIndex <= INPUTSYMBOLLIMIT[codeIndex] ? mappedQuantizerIndex : CodeCreator.CODE_X_VAL;
				if (inputSymbol == CodeCreator.CODE_X_VAL) {
					int codeQuant = mappedQuantizerIndex - INPUTSYMBOLLIMIT[codeIndex] - 1;
					this.reverseLengthLimitedGolombPowerOfTwoCode(codeQuant, 0, bos, this.uMax, this.depth);
				}
				TreeTable<Codeword> currentTable = this.activeTables[codeIndex];
				TableEntry<Codeword, TreeTable<Codeword>> entry = currentTable.getEntry(inputSymbol); 
				if (entry.isTree()) { //go to next node
					this.activeTables[codeIndex] = entry.getTree(); 
				} else {	//output final code and reset table
					Codeword cw = entry.getTerminal();
					bos.writeBits(cw.getValue(), cw.getBits(), BitStreamConstants.ORDERING_LEFTMOST_FIRST);
					this.activeTables[codeIndex] = this.baseTables[codeIndex];
				}
			}
			if (t < this.samplesPerBand - 1) { //don't update on last sample, no one is using it
				//if on update iteration, output last bit of hracc before losing it
				if (counterT < ((1l<<this.gammaStar) - 1)) {
					if (accT% 2 == 0)
						bos.writeBit(Bit.BIT_ZERO);
					else
						bos.writeBit(Bit.BIT_ONE);
				}
				//update accumulator for current iteration
				this.updateAcc(b, mappedQuantizerIndex, (int) counterT);
			} else if (b == this.bands - 1) {//last sample, flush things
				//flush all active tables with their flush codes
				for (int i = 0; i < 16; i++) {
					Codeword flushWord = this.activeTables[i].getValue();
					bos.writeBits(flushWord.getValue(), flushWord.getBits(), BitStreamConstants.ORDERING_LEFTMOST_FIRST);
				}
				//flush accumulators
				for (int i = 0; i < this.bands; i++) {
					bos.writeBits(this.accumulator[i], 2 + this.depth + this.gammaStar, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
				}
				//flush a single '1' bit so that the end can be identified when reading backwards
				bos.writeBit(Bit.BIT_ONE);
			}
			
		}
	}

	@Override
	public int decode(int t, int b, BitInputStream bis) throws IOException {
		throw new IllegalStateException();
	}
	
	private void updateAcc(int b, int mqi, int counter) {
		if (counter < ((1<<this.gammaStar) - 1)) {
			this.accumulator[b] = this.accumulator[b] + 4*mqi;
		} else {
			this.accumulator[b] = (this.accumulator[b] + 4*mqi + 1) >> 1;
		}
	}

}
