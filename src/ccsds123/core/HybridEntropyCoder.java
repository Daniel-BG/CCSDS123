package ccsds123.core;

import java.io.IOException;

import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;

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
	
/*	00 5'h19 09 8’h3B 5 4'h6
	010 8'h57 0A 8’hBB 6 4'hE
	011 8'hD7 0B 9’h00F 70 7'h0B
	012 8'h37 0C 9’h10F 71 7'h4B
	013 9'h0AF 0X 8’h7B 72 7'h2B
	014 9'h1AF 1 3’h0 73 8'h47
	015 9'h06F 2 3’h4 74 8'hC7
	016 9'h16F 3 3’h2 75 8'h27
	017 10'h03F 40 6’h1D 76 8'hA7
	018 10'h23F 41 6’h3D 77 9'h0CF
	019 11'h17F 42 6’h03 78 9'h1CF
	01A 11'h57F 43 6’h23 79 10'h15F
	01B 12'h1FF 440 9’h09F 7A 10'h35F
	01C 12'h9FF 441 9’h19F 7B 11'h07F
	01X 11'h37F 442 9’h05F 7C 11'h47F
	020 8'hB7 443 10’h0BF 7X 10'h0DF
	021 8'h77 444 10’h2BF 80 7'h6B
	022 8'hF7 445 10’h1BF 81 7'h1B
	023 9'h0EF 446 10’h3BF 82 7'h5B
	024 9'h1EF 447 11’h2FF 83 8'h67
	025 9'h01F 448 11’h6FF 84 8'hE7
	026 9'h11F 449 12’h3FF 85 8'h17
	027 10'h13F 44A 12’hBFF 86 8'h97
	028 10'h33F 44B 13’h0FFF 87 9'h02F
	029 11'h77F 44C 13’h1FFF 88 9'h12F
	02A 11'h0FF 44X 12’h7FF 89 10'h2DF
	02B 12'h5FF 45 7’h33 8A 10'h1DF
	02C 12'hDFF 46 7’h73 8B 11'h27F
	02X 11'h4FF 47 8’hFB 8C 11'h67F
	03 6'h15 48 8’h07 8X 10'h3DF
	04 6'h35 49 9’h08F 9 5'h01
	05 6'h0D 4A 9’h18F A 5'h11
	06 6'h2D 4B 9’h04F B 6'h05
	07 7'h13 4C 9’h14F C 6'h25
	08 7'h53 4X 8’h87 X 5'h09*/

	@Override
	public void code(int mappedQuantizerIndex, int t, int b, BitOutputStream bos) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int decode(int t, int b, BitInputStream bis) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
