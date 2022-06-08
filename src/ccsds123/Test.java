package ccsds123;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Test {
	
	static String input0 = "C:/Users/Daniel/Hiperspectral images/cupriteBSQ/Cuprite";
	static String input1 = "C:/Users/Daniel/Hiperspectral images/Reno_Radiance_wIGMGLT/0913-1248_rad.dat";
	static String input2 = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Rad/Suwannee_0609-1331_rad.dat";
	static String input3 = "C:/Users/Daniel/Hiperspectral images/Beltsville_Radiance_w_IGM/0810_2022_rad.dat";
	//static String input = "C:/Users/Daniel/Hiperspectral images/TestPattern/test_ptn_x100y36z17_16u.bip";
	static String inputHeader0 = "C:/Users/Daniel/Hiperspectral images/cupriteBSQ/Cuprite.hdr";
	static String inputHeader1 = "C:/Users/Daniel/Hiperspectral images/Reno_Radiance_wIGMGLT/0913-1248_rad.hdr";
	static String inputHeader2 = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Rad/Suwannee_0609-1331_rad.hdr";
	static String inputHeader3 = "C:/Users/Daniel/Hiperspectral images/Beltsville_Radiance_w_IGM/0810_2022_rad.hdr";
	//static String inputHeader = "C:/Users/Daniel/Hiperspectral images/TestPattern/test_ptn_x100y36z17_16u.bip.hdr";
	
	static String[] inputs = {input0, input1, input2, input3};
	static String[] inputHeaders = {inputHeader0, inputHeader1, inputHeader2, inputHeader3};
	
	//static String input4 = "C:/Users/Daniel/Hiperspectral images/Beltsville_Radiance_w_IGM/0810_2022_IGM.dat";
	//static String inputHeader4 = "C:/Users/Daniel/Hiperspectral images/Beltsville_Radiance_w_IGM/0810_2022_IGM.hdr";
	//static String input2 = "C:/Users/Daniel/Hiperspectral images/Beltsville_Reflectance_w_IGM/0810_2022_IGM.dat";
	//static String inputHeader2 = "C:/Users/Daniel/Hiperspectral images/Beltsville_Reflectance_w_IGM/0810_2022_IGM.hdr";
	
	//static String input1 = "C:/Users/Daniel/Hiperspectral images/Beltsville_Radiance_w_IGM/0810_2022_rad.dat";
	//static String inputHeader1 = "C:/Users/Daniel/Hiperspectral images/Beltsville_Radiance_w_IGM/0810_2022_rad.hdr";
	/*static String input3 = "C:/Users/Daniel/Hiperspectral images/Beltsville_Reflectance_w_IGM/0810_2022_ref.dat";
	static String inputHeader3 = "C:/Users/Daniel/Hiperspectral images/Beltsville_Reflectance_w_IGM/0810_2022_ref.hdr";
	static String input4 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_hawaii_f011020t01p03r05_sc01.uncal-u16be-224x512x614.raw";
	static String inputHeader4 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_hawaii_f011020t01p03r05_sc01.uncal-u16be-224x512x614.raw.hdr";
	static String input5 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_maine_f030828t01p00r05_sc10.uncal-u16be-224x512x680.raw";
	static String inputHeader5 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_maine_f030828t01p00r05_sc10.uncal-u16be-224x512x680.raw.hdr";
	static String input6 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_yellowstone_f060925t01p00r12_sc00.cal-s16be-224x512x677.raw";
	static String inputHeader6 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_yellowstone_f060925t01p00r12_sc00.cal-s16be-224x512x677.raw.hdr";
	static String input7 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_yellowstone_f060925t01p00r12_sc00.uncal-u16be-224x512x680.raw";
	static String inputHeader7 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_yellowstone_f060925t01p00r12_sc00.uncal-u16be-224x512x680.raw.hdr";
	static String input8 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_yellowstone_f060925t01p00r12_sc03.cal-s16be-224x512x677.raw";
	static String inputHeader8 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_yellowstone_f060925t01p00r12_sc03.cal-s16be-224x512x677.raw.hdr";
	static String input9 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_yellowstone_f060925t01p00r12_sc03.uncal-u16be-224x512x680.raw";
	static String inputHeader9 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_yellowstone_f060925t01p00r12_sc03.uncal-u16be-224x512x680.raw.hdr";
	static String input10 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_yellowstone_f060925t01p00r12_sc10.cal-s16be-224x512x677.raw";
	static String inputHeader10 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_yellowstone_f060925t01p00r12_sc10.cal-s16be-224x512x677.raw.hdr";
	static String input11 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_yellowstone_f060925t01p00r12_sc11.cal-s16be-224x512x677.raw";
	static String inputHeader11 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_yellowstone_f060925t01p00r12_sc11.cal-s16be-224x512x677.raw.hdr";
	static String input12 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_yellowstone_f060925t01p00r12_sc18.cal-s16be-224x512x677.raw";
	static String inputHeader12 = "C:/Users/Daniel/Hiperspectral images/CCSDS 123 suite/AVIRIS/aviris_yellowstone_f060925t01p00r12_sc18.cal-s16be-224x512x677.raw.hdr";
	static String input13 = "C:/Users/Daniel/Hiperspectral images/cupriteBSQ/Cuprite";
	static String inputHeader13 = "C:/Users/Daniel/Hiperspectral images/cupriteBSQ/Cuprite.hdr";
	static String input14 = "C:/Users/Daniel/Hiperspectral images/Cuprite_Radiance/f080611t01p00r06rdn_c_sc01_ort_img";
	static String inputHeader14 = "C:/Users/Daniel/Hiperspectral images/Cuprite_Radiance/f080611t01p00r06rdn_c_sc01_ort_img.hdr";
	static String input15 = "C:/Users/Daniel/Hiperspectral images/Cuprite_radiance_wIGMGLT/0614-1124_GLT.dat";
	static String inputHeader15 = "C:/Users/Daniel/Hiperspectral images/Cuprite_radiance_wIGMGLT/0614-1124_GLT.hdr";
	static String input16 = "C:/Users/Daniel/Hiperspectral images/Cuprite_radiance_wIGMGLT/0614-1124_IGM.dat";
	static String inputHeader16 = "C:/Users/Daniel/Hiperspectral images/Cuprite_radiance_wIGMGLT/0614-1124_IGM.hdr";
	static String input17 = "C:/Users/Daniel/Hiperspectral images/Cuprite_radiance_wIGMGLT/0614-1124_rad.dat";
	static String inputHeader17 = "C:/Users/Daniel/Hiperspectral images/Cuprite_radiance_wIGMGLT/0614-1124_rad.hdr";
	static String input18 = "C:/Users/Daniel/Hiperspectral images/Cuprite_Reflectance/f970619t01p02_r02_sc01.a.rfl";
	static String inputHeader18 = "C:/Users/Daniel/Hiperspectral images/Cuprite_Reflectance/f970619t01p02_r02_sc01.a.hdr";
	static String input19 = "C:/Users/Daniel/Hiperspectral images/Cuprite_Reflectance/f970619t01p02_r02_sc02.a.rfl";
	static String inputHeader19 = "C:/Users/Daniel/Hiperspectral images/Cuprite_Reflectance/f970619t01p02_r02_sc02.a.hdr";
	static String input20 = "C:/Users/Daniel/Hiperspectral images/Cuprite_Reflectance/f970619t01p02_r02_sc03.a.rfl";
	static String inputHeader20 = "C:/Users/Daniel/Hiperspectral images/Cuprite_Reflectance/f970619t01p02_r02_sc03.a.hdr";
	static String input21 = "C:/Users/Daniel/Hiperspectral images/Cuprite_Reflectance/f970619t01p02_r02_sc04.a.rfl";
	static String inputHeader21 = "C:/Users/Daniel/Hiperspectral images/Cuprite_Reflectance/f970619t01p02_r02_sc04.a.hdr";
	static String input22 = "C:/Users/Daniel/Hiperspectral images/Cuprite_Reflectance/f970619t01p02_r02_sc05.a.rfl";
	static String inputHeader22 = "C:/Users/Daniel/Hiperspectral images/Cuprite_Reflectance/f970619t01p02_r02_sc05.a.hdr";
	static String input23 = "C:/Users/Daniel/Hiperspectral images/Cuprite_reflectance_wIGMGLT/0614-1124_GLT.dat";
	static String inputHeader23 = "C:/Users/Daniel/Hiperspectral images/Cuprite_reflectance_wIGMGLT/0614-1124_GLT.hdr";
	static String input24 = "C:/Users/Daniel/Hiperspectral images/Cuprite_reflectance_wIGMGLT/0614-1124_IGM.dat";
	static String inputHeader24 = "C:/Users/Daniel/Hiperspectral images/Cuprite_reflectance_wIGMGLT/0614-1124_IGM.hdr";
	static String input25 = "C:/Users/Daniel/Hiperspectral images/Cuprite_reflectance_wIGMGLT/0614-1124_ref.dat";
	static String inputHeader25 = "C:/Users/Daniel/Hiperspectral images/Cuprite_reflectance_wIGMGLT/0614-1124_ref.hdr";
	static String input26 = "C:/Users/Daniel/Hiperspectral images/DeepHorizon_OilSpill/0612-1615_glt_sub.dat";
	static String inputHeader26 = "C:/Users/Daniel/Hiperspectral images/DeepHorizon_OilSpill/0612-1615_glt_sub.hdr";
	static String input27 = "C:/Users/Daniel/Hiperspectral images/DeepHorizon_OilSpill/0612-1615_igm_sub.dat";
	static String inputHeader27 = "C:/Users/Daniel/Hiperspectral images/DeepHorizon_OilSpill/0612-1615_igm_sub.hdr";
	static String input28 = "C:/Users/Daniel/Hiperspectral images/DeepHorizon_OilSpill/0612-1615_rad_sub.dat";
	static String inputHeader28 = "C:/Users/Daniel/Hiperspectral images/DeepHorizon_OilSpill/0612-1615_rad_sub.hdr";
	static String input29 = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Rad/Suwannee_0609-1331_glt.dat";
	static String inputHeader29 = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Rad/Suwannee_0609-1331_glt.hdr";
	static String input30 = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Rad/Suwannee_0609-1331_igm.dat";
	static String inputHeader30 = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Rad/Suwannee_0609-1331_igm.hdr";
	static String input31 = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Rad/Suwannee_0609-1331_rad.dat";
	static String inputHeader31 = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Rad/Suwannee_0609-1331_rad.hdr";
	static String input32 = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Ref2/Suwannee_0609-1331_glt.dat";
	static String inputHeader32 = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Ref2/Suwannee_0609-1331_glt.hdr";
	static String input33 = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Ref2/Suwannee_0609-1331_igm.dat";
	static String inputHeader33 = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Ref2/Suwannee_0609-1331_igm.hdr";
	static String input34 = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Ref2/Suwannee_0609-1331_ref.dat";
	static String inputHeader34 = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Ref2/Suwannee_0609-1331_ref.hdr";
	static String input35 = "C:/Users/Daniel/Hiperspectral images/JasperRidge/Radiance/AVJRBP_RAD.bsq";
	static String inputHeader35 = "C:/Users/Daniel/Hiperspectral images/JasperRidge/Radiance/AVJRBP_RAD.hdr";
	static String input36 = "C:/Users/Daniel/Hiperspectral images/JasperRidge/Reflectance/AVJRBP_REF.bsq";
	static String inputHeader36 = "C:/Users/Daniel/Hiperspectral images/JasperRidge/Reflectance/AVJRBP_REF.hdr";
	static String input37 = "C:/Users/Daniel/Hiperspectral images/Low_Altitude/f960705t01p02_r02c_img";
	static String inputHeader37 = "C:/Users/Daniel/Hiperspectral images/Low_Altitude/f960705t01p02_r02c_img.hdr";
	static String input38 = "C:/Users/Daniel/Hiperspectral images/Moffet_Field_Radiance/f080611t01p00r07rdn_c_obs";
	static String inputHeader38 = "C:/Users/Daniel/Hiperspectral images/Moffet_Field_Radiance/f080611t01p00r07rdn_c_obs.hdr";
	static String input39 = "C:/Users/Daniel/Hiperspectral images/Moffet_Field_Radiance/f080611t01p00r07rdn_c_obs_ort";
	static String inputHeader39 = "C:/Users/Daniel/Hiperspectral images/Moffet_Field_Radiance/f080611t01p00r07rdn_c_obs_ort.hdr";
	static String input40 = "C:/Users/Daniel/Hiperspectral images/Moffet_Field_Radiance/f080611t01p00r07rdn_c_ort_glt";
	static String inputHeader40 = "C:/Users/Daniel/Hiperspectral images/Moffet_Field_Radiance/f080611t01p00r07rdn_c_ort_glt.hdr";
	static String input41 = "C:/Users/Daniel/Hiperspectral images/Moffet_Field_Radiance/f080611t01p00r07rdn_c_ort_igm";
	static String inputHeader41 = "C:/Users/Daniel/Hiperspectral images/Moffet_Field_Radiance/f080611t01p00r07rdn_c_ort_igm.hdr";
	static String input42 = "C:/Users/Daniel/Hiperspectral images/Moffet_Field_Radiance/f080611t01p00r07rdn_c_sc01_ort_img";
	static String inputHeader42 = "C:/Users/Daniel/Hiperspectral images/Moffet_Field_Radiance/f080611t01p00r07rdn_c_sc01_ort_img.hdr";
	static String input43 = "C:/Users/Daniel/Hiperspectral images/RedSea_Radiance_w_IGMGLT/0509-0440_GLT.dat";
	static String inputHeader43 = "C:/Users/Daniel/Hiperspectral images/RedSea_Radiance_w_IGMGLT/0509-0440_GLT.hdr";
	static String input44 = "C:/Users/Daniel/Hiperspectral images/RedSea_Radiance_w_IGMGLT/0509-0440_IGM.dat";
	static String inputHeader44 = "C:/Users/Daniel/Hiperspectral images/RedSea_Radiance_w_IGMGLT/0509-0440_IGM.hdr";
	static String input45 = "C:/Users/Daniel/Hiperspectral images/RedSea_Radiance_w_IGMGLT/0509-0440_rad.dat";
	static String inputHeader45 = "C:/Users/Daniel/Hiperspectral images/RedSea_Radiance_w_IGMGLT/0509-0440_rad.hdr";
	static String input46 = "C:/Users/Daniel/Hiperspectral images/Reno_Radiance_wIGMGLT/0913-1248_GLT.dat";
	static String inputHeader46 = "C:/Users/Daniel/Hiperspectral images/Reno_Radiance_wIGMGLT/0913-1248_GLT.hdr";
	static String input47 = "C:/Users/Daniel/Hiperspectral images/Reno_Radiance_wIGMGLT/0913-1248_IGM.dat";
	static String inputHeader47 = "C:/Users/Daniel/Hiperspectral images/Reno_Radiance_wIGMGLT/0913-1248_IGM.hdr";
	static String input48 = "C:/Users/Daniel/Hiperspectral images/Reno_Radiance_wIGMGLT/0913-1248_rad.dat";
	static String inputHeader48 = "C:/Users/Daniel/Hiperspectral images/Reno_Radiance_wIGMGLT/0913-1248_rad.hdr";
	static String input49 = "C:/Users/Daniel/Hiperspectral images/Reno_Reflectance_wIGMGLT/0913-1248_ref.dat";
	static String inputHeader49 = "C:/Users/Daniel/Hiperspectral images/Reno_Reflectance_wIGMGLT/0913-1248_ref.hdr";
	static String input50 = "C:/Users/Daniel/Hiperspectral images/TestPattern/test_ptn_x100y36z17_16u.bip";
	static String inputHeader50 = "C:/Users/Daniel/Hiperspectral images/TestPattern/test_ptn_x100y36z17_16u.bip.hdr";
	static String input51 = "C:/Users/Daniel/Hiperspectral images/WTC/SubsetWTC.bsq";
	static String inputHeader51 = "C:/Users/Daniel/Hiperspectral images/WTC/SubsetWTC.hdr";
	static String[] inputs = {input3,input4,input5,input6,input7,input8,input9,input10,input11,input12,input13,input14,input15,input16,input17,input18,input19,input20,input21,input22,input23,input24,input25,input26,input27,input28,input29,input30,input31,input32,input33,input34,input35,input36,input37,input38,input39,input40,input41,input42,input43,input44,input45,input46,input47,input48,input49,input50,input51};
	static String[] inputHeaders = {inputHeader3,inputHeader4,inputHeader5,inputHeader6,inputHeader7,inputHeader8,inputHeader9,inputHeader10,inputHeader11,inputHeader12,inputHeader13,inputHeader14,inputHeader15,inputHeader16,inputHeader17,inputHeader18,inputHeader19,inputHeader20,inputHeader21,inputHeader22,inputHeader23,inputHeader24,inputHeader25,inputHeader26,inputHeader27,inputHeader28,inputHeader29,inputHeader30,inputHeader31,inputHeader32,inputHeader33,inputHeader34,inputHeader35,inputHeader36,inputHeader37,inputHeader38,inputHeader39,inputHeader40,inputHeader41,inputHeader42,inputHeader43,inputHeader44,inputHeader45,inputHeader46,inputHeader47,inputHeader48,inputHeader49,inputHeader50,inputHeader51};*/
	
	static String input = input0;
	static String inputHeader = inputHeader0;
	
	static String output = "C:/Users/Daniel/Basurero/out/output.dat";
	static String output_ref = "C:/Users/Daniel/Basurero/out/output_ref.dat";
	static String output_inv = "C:/Users/Daniel/Basurero/out/output_inv.dat";
	static String output2 = "C:/Users/Daniel/Basurero/out/output.mif";
	//static String output2 = "C:/Users/Daniel/Basurero/out/output_rec.dat";//"C:/Users/Daniel/Basurero/out/output.mif";
	
	
	static String[] argsCompression = 
		{
				"-i", input,
				"--input_header", inputHeader,
				"-o", output,
				"--bitdepth", "16",
				"--hybrid",
				"-c",
				"--max_abs_err", "1",
				//"--max_rel_err", "4096",
				"--custom_size", "32", "64", "128"
		};
	
	static String[] argsDecompression =
		{
				"-i", output,
				"-o", output2,
				"-d",
				"--hybrid",
				"--max_abs_err", "1",
				"--bitdepth", "16",
				"--custom_size", "32", "64", "128"
		};
	
	static String[] argsCompare =
		{
			//"--max_abs_err", "8",
			//"--max_rel_err", "32",
			"-i", input,
			"--input_header", inputHeader,
			"-o", output,
			"-k", "--stats",
			"--bitdepth", "16",
			"--custom_size", "32", "64", "128",
			//"--custom_size", "8", "8", "8",
			//"--custom_size", "128", "16", "16",
			//"--custom_size", "64", "32", "16",
			//"--custom_size", "16", "16", "16",
			//"--custom_size", "128", "64", "64",
			"--hybrid",
			//"--custom_size", "32", "2", "3",
			//"--custom_size", "360", "64", "64"
		};
	
	public static void printArgs(String[] args) {
		String str = "";
		for (String s: args)
			str += s + " ";
		System.out.println(str);
	}

	public static void main(String[] args) {
		//Main.main(argsCompression);
		//Main.main(argsDecompression);
		Main.main(argsCompare);
	}
	
	static String[] errTypes = {"--max_abs_err"}; //, "--max_rel_err"};
	
	public static void batchProcess(String[] args) {
		int tp= 0, tn = 0, fp = 0, fn = 0, tpl = 0;
		for (int image = 0; image < inputs.length; image++) {
			for (String errType: errTypes) {
				for (int i = 0; i <= 0; i++) {
					argsCompare[0] = errType;
					argsCompare[1] = Integer.toString((1 << i) >> 1);
					argsCompare[3] = inputs[image];
					argsCompare[5] = inputHeaders[image];
					printArgs(argsCompare);
					//argsCompare[3] = Integer.toString(1 << i);
					try {
						Main.main(argsCompare);
					} catch (Exception e) {
						e.printStackTrace();
					}
					//long checkSumA = 0xEDB1D90146193476l; //lossless
					//long checkSumB = 0xE0F31AD11F0EAC00l;
					/*
					int  checkLen  = 4880*8+8;
					//long checkSumA = 0x046E0005160003D3l;
					long checkSumB = 0x0004360006B58000l;
					
					
					try {
						boolean match = compareByMemoryMappedFiles(output, output_ref);
		
						RandomAccessFile raf = new RandomAccessFile(new File(output), "r");
						long len = raf.length(); //should be *8
						raf.seek(len - 16);
						long lastBytesA = raf.readLong();
						long lastBytesB = raf.readLong();
						boolean fastMatch = len == checkLen && lastBytesB == checkSumB; // && lastBytesA == checkSumA;
						
						if (match && fastMatch)
							tn++;
						if (!match && fastMatch) {
							fn++;
						}
						if (match && !fastMatch)
							fp++; //should not get here
						if (!match && !fastMatch)
							tp++;
						if (len != checkLen)
							tpl++;
						
						System.out.println("Processed: " + i + " (tn: " + tn + ", fn: " + fn + ", fp: " + fp + ", tp: " + tp + " [l:" + tpl + "])");
						raf.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
		
					//System.out.println();
				}
			}
		}
		/*
		BufferedWriter bw;
		BitInputStream bis;
		BitOutputStream bos;
		try {
			bis = new BitInputStream(new FileInputStream(new File(output)));
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(output2))));
			bos = new BitOutputStream(new FileOutputStream(new File(output_inv)));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		
		try {
			while (true) {
				int nextVal1 = bis.readInt();
				int nextVal2 = bis.readInt();
				long nextVal = (((long) nextVal1) << 32) | (((long) nextVal2) & 0xffffffffl);
				bos.writeBits(Integer.reverseBytes(nextVal1), 32, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
				bos.writeBits(Integer.reverseBytes(nextVal2), 32, BitStreamConstants.ORDERING_LEFTMOST_FIRST);
				bw.write(Long.toString(nextVal));
				bw.newLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		} finally {
			try {
				bw.close();
				bis.close();
				bos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
		
		
		
	}
	
	public static boolean compareByMemoryMappedFiles(String path1, String path2) throws IOException {
		byte[] b1 = Files.readAllBytes(Paths.get(path1));
		byte[] b2 = Files.readAllBytes(Paths.get(path2));
		return Arrays.equals(b1, b2);
	}
}
