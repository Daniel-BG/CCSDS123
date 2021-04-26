package ccsds123;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;

import ccsds123.util.Sampler;

public class Test {
	
	//static String input = "C:/Users/Daniel/Hiperspectral images/cupriteBSQ/Cuprite";
	//static String input = "C:/Users/Daniel/Hiperspectral images/Reno_Radiance_wIGMGLT/0913-1248_rad.dat";
	//static String input = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Rad/Suwannee_0609-1331_rad.dat";
	//static String input = "C:/Users/Daniel/Hiperspectral images/Beltsville_Radiance_w_IGM/0810_2022_rad.dat";
	static String input = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Rad/Suwannee_0609-1331_rad.dat";
	//static String inputHeader = "C:/Users/Daniel/Hiperspectral images/cupriteBSQ/Cuprite.hdr";
	//static String inputHeader = "C:/Users/Daniel/Hiperspectral images/Reno_Radiance_wIGMGLT/0913-1248_rad.hdr";
	//static String inputHeader = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Rad/Suwannee_0609-1331_rad.hdr";
	//static String inputHeader = "C:/Users/Daniel/Hiperspectral images/Beltsville_Radiance_w_IGM/0810_2022_rad.hdr";
	static String inputHeader = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Rad/Suwannee_0609-1331_rad.hdr";
	
	
	static String output = "C:/Users/Daniel/Basurero/out/output.dat";
	static String output2 = "C:/Users/Daniel/Basurero/out/output2.dat";
	
	
	static String[] argsCompression = 
		{
				"-i", input,
				"--input_header", inputHeader,
				"-o", output,
				"-c",
				"--custom_size", "16", "16", "16"
		};
	
	static String[] argsDecompression =
		{
				"-i", output,
				"-o", output2,
				"-d",
				"--bitdepth", "16",
				"--custom_size", "16", "16", "16"
		};
	
	static String[] argsCompare =
		{
			//"--max_abs_err", "0",
			"--max_rel_err", "0",
			"-i", input,
			"--input_header", inputHeader,
			"-o", output,
			"-k", "--stats",
			"--bitdepth", "16",
			//"--custom_size", "8", "8", "8",
			"--custom_size", "64", "8", "64",
			"--hybrid",
			//"--custom_size", "32", "2", "3",
			//"--custom_size", "360", "64", "64"
		};

	public static void main(String[] args) {
		
		for (int i = 12; i <= 12; i++) {
			argsCompare[1] = Integer.toString((1 << i) >> 1);
			//argsCompare[3] = Integer.toString(1 << i);
			Main.main(argsCompare);
			System.out.println();
		}
		
		BufferedWriter bw;
		BitInputStream bis;
		try {
			bis = new BitInputStream(new FileInputStream(new File(output)));
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(output2))));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		
		try {
			while (true) {
				long nextVal = bis.readLong();
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
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
		//Main.main(argsCompression);
		//Main.main(argsDecompression);
	}
}
