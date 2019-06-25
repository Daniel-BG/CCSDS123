package ccsds123;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.jypec.img.HyperspectralImage;
import com.jypec.img.HyperspectralImageData;
import com.jypec.util.bits.BitOutputStream;
import com.jypec.util.io.HyperspectralImageReader;

import ccsds123.cli.InputArguments;
import ccsds123.core.Compressor;

public class CCSDS {

	//todo add commons-cli and others here
	
	public static void compress(InputArguments args) throws IOException {
		
		Compressor c = new Compressor();
		HyperspectralImage hi = HyperspectralImageReader.read(args.input, args.inputHeader, true);
		
		BitOutputStream bos = new BitOutputStream(new FileOutputStream(new File(args.output)));
		
		HyperspectralImageData hid = hi.getData();
		int imgBands	= hid.getNumberOfBands();
		int imgLines	= hid.getNumberOfLines();
		int imgSamples	= hid.getNumberOfSamples();
		
		//fill block up
		int[][][] image = new int[imgBands][imgLines][imgSamples];
		for (int b = 0; b < imgBands; b++) {
			for (int l = 0; l < imgLines; l++) {
				for (int s = 0; s < imgSamples; s++) {
					image[b][l][s] = hid.getValueAt(b, l, s);
				}
			}
		}
		
		c.compress(image, imgBands, imgLines, imgSamples, bos);
		
	}
	
}
