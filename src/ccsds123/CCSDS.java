package ccsds123;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import org.ejml.data.DMatrixRMaj;


/*import com.jypec.distortion.ImageComparisons;
import com.jypec.img.HeaderConstants;
import com.jypec.img.HyperspectralImage;
import com.jypec.img.HyperspectralImageData;
import com.jypec.img.HyperspectralImageIntegerData;
import com.jypec.img.ImageDataType;
import com.jypec.img.ImageHeaderData;
import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;
import com.jypec.util.io.HyperspectralImageWriter;
import com.jypec.util.io.headerio.enums.BandOrdering;
import com.jypec.util.io.headerio.enums.ByteOrdering;*/

import ccsds123.cli.InputArguments;
import ccsds123.core.Compressor;
import ccsds123.core.CompressorParameters;
import hyppo.data.HeaderConstants;
import hyppo.data.HyperspectralImage;
import hyppo.data.HyperspectralImageData;
import hyppo.data.HyperspectralImageIntegerData;
import hyppo.data.ImageHeaderData;
import hyppo.data.imageDataTypes.ImageDataType;
import hyppo.io.headerio.enums.BandOrdering;
import hyppo.io.headerio.enums.ByteOrdering;
import hyppo.util.ImageComparisons;
import javelin.bits.BitInputStream;
import javelin.bits.BitOutputStream;


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
					image[b][l][s] = (int) hid.getValueAt(b, l, s);
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
		
		//printVHDLTestParameters(c.getParamters());
		//printCTestParameters(c.getParamters());
	}
	
	public static void printCTestParameters(CompressorParameters compressorParameters) {
		String base = 
			"	//<Autogen by CCSDS.java>\r\n" +
			"	XCCSDS_Out32(BaseAddress + CCSDS_REG_CFG_SAMPLES_LOCALADDR, <SAMPLES>);\r\n" + 
			"	XCCSDS_Out32(BaseAddress + CCSDS_REG_CFG_MAX_X_LOCALADDR, <SAMPLES> - 1);\r\n" + 
			"	XCCSDS_Out32(BaseAddress + CCSDS_REG_CFG_MAX_Y_LOCALADDR, <LINES> - 1);\r\n" + 
			"	XCCSDS_Out32(BaseAddress + CCSDS_REG_CFG_MAX_Z_LOCALADDR, <BANDS> - 1);\r\n" + 
			"	XCCSDS_Out32(BaseAddress + CCSDS_REG_CFG_MAX_T_LOCALADDR, <SAMPLES>*<LINES> - 1);\r\n" +  
			"	XCCSDS_Out32(BaseAddress + CCSDS_REG_CFG_MIN_PRELOAD_VALUE_LOCALADDR, ((<BANDS>-1)*(<BANDS>-2))/2+2);\r\n" + 
			"	XCCSDS_Out32(BaseAddress + CCSDS_REG_CFG_MAX_PRELOAD_VALUE_LOCALADDR, ((<BANDS>-1)*(<BANDS>-2))/2+6);\r\n" + 
			"	XCCSDS_Out32(BaseAddress + CCSDS_REG_BYTENO_LOCALADDR, <SAMPLES> * <LINES> * <BANDS> * CCSDS_INPUT_BYTE_WIDTH);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_P_LOCALADDR, 				<P>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_TINC_LOCALADDR, 				<TINC>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_VMAX_LOCALADDR, 				<VMAX>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_VMIN_LOCALADDR, 				<VMIN>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_DEPTH_LOCALADDR, 			<D>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_OMEGA_LOCALADDR, 			<OMEGA>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_WEO_LOCALADDR, 				<WEO>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_USE_ABS_ERR_LOCALADDR, 		<USEABSERR>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_USE_REL_ERR_LOCALADDR,		<USERELERR>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_ABS_ERR_LOCALADDR, 			<ABSERR>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_REL_ERR_LOCALADDR, 			<RELERR>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_SMAX_LOCALADDR, 				<SMAX>);\r\n" +
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_SMID_LOCALADDR, 				<SMID>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_RESOLUTION_LOCALADDR, 		<RES>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_DAMPING_LOCALADDR, 			<DAMP>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_OFFSET_LOCALADDR, 			<OFF>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_INITIAL_COUNTER_LOCALADDR,	<ICNT>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_FINAL_COUNTER_LOCALADDR, 	<FCNT>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_GAMMA_STAR_LOCALADDR, 		<GSTAR>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_U_MAX_LOCALADDR, 			<UMAX>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_IACC_LOCALADDR, 				<IACC>);\r\n" + 
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_WIDE_SUM_LOCALADDR, 			<WIDESUM>);\r\n" +
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_NEIGHBOR_SUM_LOCALADDR, 		<NEIGHSUM>);\r\n" +
			"	XCCSDS_SetParam(BaseAddress, CCSDS_REG_CFG_FULL_PREDICTION_LOCALADDR, 	<FULLPRED>);\r\n" +
			"	//</Autogen by CCSDS.java>";
		base = replaceParams(base, compressorParameters);
		System.out.println(base);
	}
	
	public static void printVHDLTestParameters(CompressorParameters compressorParameters) {
		String base = 
			"		--<! autogenerated by CCSDS.java>\r\n" + 
			"		constant C_SAMPLES: integer := <SAMPLES>;\r\n" + 
			"		constant C_LINES: integer := <LINES>;\r\n" + 
			"		constant C_BANDS: integer := <BANDS>;\r\n" + 
			"		--</! autogenerated by CCSDS.java>\r\n" + 
			"		--<! autogenerated by CCSDS.java>\r\n" + 
			"		--image size constants\r\n" + 
			"		cfg_samples 		<= std_logic_vector(to_unsigned(C_SAMPLES, CONST_MAX_SAMPLES_BITS));\r\n" + 
			"		cfg_max_x			<= std_logic_vector(to_unsigned(C_SAMPLES-1, CONST_MAX_X_VALUE_BITS));\r\n" + 
			"		cfg_max_y 			<= std_logic_vector(to_unsigned(C_LINES-1, CONST_MAX_Y_VALUE_BITS));\r\n" + 
			"		cfg_max_z 			<= std_logic_vector(to_unsigned(C_BANDS-1, CONST_MAX_Z_VALUE_BITS));\r\n" + 
			"		cfg_max_t 			<= std_logic_vector(to_unsigned(C_SAMPLES*C_LINES - 1, CONST_MAX_T_VALUE_BITS));\r\n" + 
			"		--architecture constants\r\n" + 
			"		cfg_min_preload_value <= std_logic_vector(to_unsigned(((C_BANDS-1)*(C_BANDS-2))/2+2, CONST_MAX_Z_VALUE_BITS*2)); --((cfg_max_z)*(cfg_max_z-1))/2 + 2\r\n" + 
			"		cfg_max_preload_value <= std_logic_vector(to_unsigned(((C_BANDS-1)*(C_BANDS-2))/2+6, CONST_MAX_Z_VALUE_BITS*2)); --((cfg_max_z)*(cfg_max_z-1))/2 + 6\r\n" + 
			"		--algorithm constants\r\n" + 
			"		cfg_p 				<= std_logic_vector(to_unsigned(<P>, CONST_MAX_P_WIDTH_BITS));\r\n" + 
			"		cfg_tinc 			<= std_logic_vector(to_unsigned(<TINC>, CONST_TINC_BITS));\r\n" + 
			"		cfg_vmax			<= std_logic_vector(to_unsigned(<VMAX>, CONST_VMINMAX_BITS));\r\n" + 
			"		cfg_vmin			<= std_logic_vector(to_signed(<VMIN>, CONST_VMINMAX_BITS));\r\n" + 
			"		cfg_depth 			<= std_logic_vector(to_unsigned(<D>, CONST_MAX_DATA_WIDTH_BITS));\r\n" + 
			"		cfg_omega 			<= std_logic_vector(to_unsigned(<OMEGA>, CONST_MAX_OMEGA_WIDTH_BITS));\r\n" + 
			"		cfg_weo 			<= std_logic_vector(to_unsigned(<WEO>, CONST_WEO_BITS));\r\n" + 
			"		cfg_use_abs_err 	<= '<USEABSERR>';\r\n" + 
			"		cfg_use_rel_err 	<= '<USERELERR>';\r\n" + 
			"		cfg_abs_err			<= std_logic_vector(to_unsigned(<ABSERR>, CONST_ABS_ERR_BITS));\r\n" + 
			"		cfg_rel_err			<= std_logic_vector(to_unsigned(<RELERR>, CONST_REL_ERR_BITS));\r\n" + 
			"		cfg_smax			<= std_logic_vector(to_unsigned(<SMAX>, CONST_MAX_DATA_WIDTH));\r\n" + 
			"		cfg_resolution 		<= std_logic_vector(to_unsigned(<RES>, CONST_RES_BITS));\r\n" + 
			"		cfg_damping 		<= std_logic_vector(to_unsigned(<DAMP>, CONST_DAMPING_BITS));\r\n" + 
			"		cfg_offset 			<= std_logic_vector(to_unsigned(<OFF>, CONST_OFFSET_BITS));\r\n" + 
			"		cfg_initial_counter <= std_logic_vector(to_unsigned(<ICNT>, CONST_MAX_COUNTER_BITS)); \r\n" + 
			"		cfg_final_counter 	<= std_logic_vector(to_unsigned(<FCNT>, CONST_MAX_COUNTER_BITS)); --2**gamma_star - 1\r\n" + 
			"		cfg_gamma_star 		<= std_logic_vector(to_unsigned(<GSTAR>, CONST_MAX_GAMMA_STAR_BITS));\r\n" + 
			"		cfg_u_max 			<= std_logic_vector(to_unsigned(<UMAX>, CONST_U_MAX_BITS));\r\n" + 
			"		cfg_iacc 			<= std_logic_vector(to_unsigned(<IACC>,  CONST_MAX_HR_ACC_BITS)); --4*(1 << this.gammaZero)*meanMQIestimate (5)\r\n" + 
			"		cfg_smid			<= std_logic_vector(to_unsigned(<SMID>, CONST_MAX_DATA_WIDTH));\r\n" +
			"		cfg_wide_sum		<= '<WIDESUM>';\r\n" +
			"		cfg_neighbor_sum	<= '<NEIGHSUM>';\r\n" +
			"		cfg_full_pred		<= '<FULLPRED>';\r\n" +
			"		--</! autogenerated by CCSDS.java>";
		base = replaceParams(base, compressorParameters);
		System.out.println(base);
		
	}
	
	public static String replaceParams(String base, CompressorParameters compressorParameters) {
		base = base.replace("<SAMPLES>", ""+compressorParameters.samples);
		base = base.replace("<LINES>", ""+compressorParameters.lines);
		base = base.replace("<BANDS>", ""+compressorParameters.bands);
		base = base.replace("<P>", ""+compressorParameters.predictionBands);
		base = base.replace("<TINC>", ""+compressorParameters.tIncExp);
		base = base.replace("<VMAX>", ""+compressorParameters.vmax);
		base = base.replace("<VMIN>", ""+compressorParameters.vmin);
		base = base.replace("<D>", ""+compressorParameters.depth);
		base = base.replace("<OMEGA>", ""+compressorParameters.omega);
		base = base.replace("<WEO>", ""+compressorParameters.getInterBandWeightExponentOffsets(0, 0));
		base = base.replace("<USEABSERR>", compressorParameters.useAbsoluteErrLimit?"1":"0");
		base = base.replace("<USERELERR>", compressorParameters.useRelativeErrLimit?"1":"0");
		base = base.replace("<FULLPRED>", compressorParameters.fullPredictionMode?"1":"0");
		base = base.replace("<WIDESUM>", compressorParameters.localSumType.isWide()?"1":"0");
		base = base.replace("<NEIGHSUM>", compressorParameters.localSumType.isNeighbor()?"1":"0");
		base = base.replace("<ABSERR>", ""+compressorParameters.getAbsErrVal(0));
		base = base.replace("<RELERR>", ""+compressorParameters.getRelErrVal(0));
		base = base.replace("<SMAX>", ""+((1<<compressorParameters.depth) - 1));
		base = base.replace("<SMID>", ""+(1<<(compressorParameters.depth - 1)));
		base = base.replace("<RES>", ""+compressorParameters.getResolution(0));
		base = base.replace("<DAMP>", ""+compressorParameters.getDamping(0));
		base = base.replace("<OFF>", ""+compressorParameters.getOffset(0));
		base = base.replace("<ICNT>", ""+(1 << compressorParameters.gammaZero));
		base = base.replace("<FCNT>", ""+((1 << compressorParameters.gammaStar) - 1));
		base = base.replace("<GSTAR>", ""+compressorParameters.gammaStar);
		base = base.replace("<UMAX>", ""+compressorParameters.uMax);
		base = base.replace("<IACC>",  "" + compressorParameters.getHInitialAcc(0));
		return base;
	}
	
	
	public static HyperspectralImage decompress(Compressor c, InputArguments args) throws IOException {
		//Compressor c = new Compressor();		
		BitInputStream bis = new BitInputStream(new FileInputStream(new File(args.input)));
		
		int[][][] image = c.decompress(bis);
		
		HyperspectralImageData hid = new HyperspectralImageIntegerData(
				ImageDataType.fromParams(args.bitDepth, args.signed, args.floating), args.bands, args.lines, args.samples);
		
		for (int i = 0; i < args.bands; i++) {
			for (int j = 0; j < args.lines; j++) {
				for (int k = 0; k < args.samples; k++) {
					hid.setValueAt(image[i][j][k], i, j, k);
				}
			}
		}
		ImageHeaderData ihd = new ImageHeaderData();
		ihd.put(HeaderConstants.HEADER_BANDS, args.bands);
		ihd.put(HeaderConstants.HEADER_SAMPLES, args.samples);
		ihd.put(HeaderConstants.HEADER_LINES, args.lines);
		ihd.put(HeaderConstants.HEADER_BYTE_ORDER, ByteOrdering.LITTLE_ENDIAN);
		ihd.put(HeaderConstants.HEADER_DATA_TYPE, 12);
		ihd.put(HeaderConstants.HEADER_INTERLEAVE, BandOrdering.BIP);
		HyperspectralImage hi = new HyperspectralImage(hid, ihd);
		return hi;
	}
	
	public static void compare(Compressor c, HyperspectralImage hi, InputArguments args) throws IOException {
		HyperspectralImageData hid = hi.getData();
		CCSDS.compress(c, hid, args.output, args);
		//introduce a random bitflip in the input
		//System.out.println("Introducing error in output @CCSDS.java. Remove if normal operation is desired");
		//randomBitFlip(args.output);
		////////
		args.input = args.output;
		args.bands = hid.getNumberOfBands();
		args.lines = hid.getNumberOfLines();
		args.samples = hid.getNumberOfSamples();
		args.bitDepth = hid.getDataType().getBitDepth();
		args.signed = hid.getDataType().isSigned();
		args.floating = hid.getDataType().isFloating();
		HyperspectralImage hidRes = CCSDS.decompress(c, args);
		
		DMatrixRMaj fdm = hid.toDoubleMatrix();
		DMatrixRMaj sdm = hidRes.getData().toDoubleMatrix();
		hidRes = null; //garbage collect
		double dynRange = hid.getDataType().getDynamicRange();
		
		//output metrics
		System.out.println("PSNR: " + ImageComparisons.rawPSNR(fdm, sdm, (float) dynRange));
		//System.out.println("Normalized PSNR is: " + ImageComparisons.normalizedPSNR(fdm, sdm));
		//System.out.println("powerSNR is: " + ImageComparisons.powerSNR(fdm, sdm));
		System.out.println("SNR: " + ImageComparisons.SNR(fdm, sdm));
		System.out.println("MSE: " + ImageComparisons.MSE(fdm, sdm));
		//System.out.println("maxSE is: " + ImageComparisons.maxSE(fdm, sdm));
		//System.out.println("MSR is: " + ImageComparisons.MSR(fdm, sdm));
		//System.out.println("SSIM is: " + ImageComparisons.SSIM(fdm, sdm, dynRange));
	}
	
	
	public static void randomBitFlip(String filePath) {
		try {
			File file = new File(filePath);
	        FileInputStream fl = new FileInputStream(file);
	        byte[] arr = new byte[(int)file.length()];
	        fl.read(arr);
	        fl.close();
	        
	        Random r = new Random();
	        int pos = r.nextInt(arr.length*8);
	        System.out.println("Introducing error at pos: " + pos);
	        arr[pos/8] ^= ((0x1) << (pos % 8));
	        
	        
	        File outputFile = new File(filePath);
	        FileOutputStream os = new FileOutputStream(outputFile);
	        os.write(arr);
	        os.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
}
