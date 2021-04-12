package ccsds123.core.hybridtables;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jypec.util.bits.Bit;

public class Codeword{
	
	private int value = 0;	//value of the coderword (up to 64 bits)
	private int bits = 0; 	//amount of bits used by the codeword
	
	public Codeword (int bits, int value) {
		if (bits > 31)
			throw new IllegalArgumentException("Too long of a codeword");
	
		this.bits = bits;
		this.value = value;
	}

	public Codeword(String code) {
		Pattern p = Pattern.compile("(\\d+)'h([0-9a-fA-F]+)");
		Matcher m = p.matcher(code);
		if (m.matches()) {
			this.bits = Integer.parseInt(m.group(1));
			this.value = Integer.parseInt(m.group(2), 16);
		} else {
			throw new IllegalArgumentException("Didnt match: " + code);
		}
	}
	
	public String toString() {
		return bits + "'h" + Integer.toHexString(this.value);
	}
	
	public int getValue() {
		return this.value;
	}
	
	public int getBits() {
		return this.bits;
	}

	public Iterator<Bit> reverseIterator() {
		return new Iterator<Bit>() {
			int bitsLeft = bits;
			int valueLeft = value;
			@Override
			public boolean hasNext() {
				return bitsLeft > 0;
			}

			@Override
			public Bit next() {
				Bit bit = Bit.fromInteger(valueLeft & 0x1); 
				valueLeft >>= 1;
				bitsLeft -= 1;
				return bit;
			}
			
		};
	}
	
	public Iterator<Bit> iterator() {
		return new Iterator<Bit>() {
			int bitsLeft = bits;
			int valueLeft = value;
			@Override
			public boolean hasNext() {
				return bitsLeft > 0;
			}

			@Override
			public Bit next() {
				Bit bit = Bit.fromInteger(valueLeft & (0x1 << (bitsLeft - 1))); 
				bitsLeft -= 1;
				return bit;
			}
			
		};
	}
	
}