package ccsds123;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.ejml.data.FMatrixRMaj;

import com.jypec.distortion.ImageComparisons;
import com.jypec.img.HyperspectralImageData;
import com.jypec.img.HyperspectralImageIntegerData;
import com.jypec.img.ImageDataType;
import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;
import ccsds123.cli.InputArguments;
import ccsds123.core.Compressor;

public class CCSDS {
	
	public static void compress(Compressor c, HyperspectralImageData hid, String outputFile, InputArguments args) throws IOException {
		
		//Compressor c = new Compressor();
		BitOutputStream bos = new BitOutputStream(new FileOutputStream(new File(outputFile)));
		
		int imgBands, imgLines, imgSamples;
		imgBands	= hid.getNumberOfBands();
		imgLines	= hid.getNumberOfLines();
		imgSamples	= hid.getNumberOfSamples();
		
		//fill block up
		int[][][] image = new int[imgBands][imgLines][imgSamples];
		for (int b = 0; b < imgBands; b++) {
			for (int l = 0; l < imgLines; l++) {
				for (int s = 0; s < imgSamples; s++) {
					image[b][l][s] = hid.getValueAt(b, l, s);
				}
			}
		}
		
		c.compress(image, bos);
		bos.addPadding(8);
		bos.paddingFlush();
		
		if (args.showCompressionStats) {
			System.out.println("Size: " + hid.getBitSize() + " -> " + bos.getBitsOutput());
			System.out.println("Ratio: " + ((double) bos.getBitsOutput() / (double) hid.getBitSize()));
		}
		
		bos.close();
	}
	
	
	public static HyperspectralImageData decompress(Compressor c, InputArguments args) throws IOException {
		//Compressor c = new Compressor();		
		BitInputStream bis = new BitInputStream(new FileInputStream(new File(args.input)));
		
		int[][][] image = c.decompress(bis);
		
		HyperspectralImageData hid = new HyperspectralImageIntegerData(
				new ImageDataType(args.bitDepth, args.signed), args.bands, args.lines, args.samples);
		
		for (int i = 0; i < args.bands; i++) {
			for (int j = 0; j < args.lines; j++) {
				for (int k = 0; k < args.samples; k++) {
					hid.setValueAt(image[i][j][k], i, j, k);
				}
			}
		}
		
		return hid;
	}
	
	public static void compare(Compressor c, HyperspectralImageData hid, InputArguments args) throws IOException {
		CCSDS.compress(c, hid, args.output, args);
		args.input = args.output;
		args.bands = hid.getNumberOfBands();
		args.lines = hid.getNumberOfLines();
		args.samples = hid.getNumberOfSamples();
		args.bitDepth = hid.getDataType().getBitDepth();
		args.signed = hid.getDataType().isSigned();
		HyperspectralImageData hidRes = CCSDS.decompress(c, args);
		
		FMatrixRMaj fdm = hid.tofloatMatrix();
		FMatrixRMaj sdm = hidRes.tofloatMatrix();
		hidRes = null; //garbage collect
		float dynRange = hid.getDataType().getDynamicRange();
		
		//output metrics
		System.out.println("PSNR: " + ImageComparisons.rawPSNR(fdm, sdm, dynRange));
		//System.out.println("Normalized PSNR is: " + ImageComparisons.normalizedPSNR(fdm, sdm));
		//System.out.println("powerSNR is: " + ImageComparisons.powerSNR(fdm, sdm));
		System.out.println("SNR: " + ImageComparisons.SNR(fdm, sdm));
		System.out.println("MSE: " + ImageComparisons.MSE(fdm, sdm));
		//System.out.println("maxSE is: " + ImageComparisons.maxSE(fdm, sdm));
		//System.out.println("MSR is: " + ImageComparisons.MSR(fdm, sdm));
		//System.out.println("SSIM is: " + ImageComparisons.SSIM(fdm, sdm, dynRange));
	}
	
}
