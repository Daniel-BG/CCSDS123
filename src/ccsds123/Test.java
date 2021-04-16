package ccsds123;

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
	
	
	static String output = "C:/Users/Daniel/Basurero/output.dat";
	static String output2 = "C:/Users/Daniel/Basurero/output2.dat";
	
	
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
			"--custom_size", "8", "8", "8",
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
		
		
		
		
		//Main.main(argsCompression);
		//Main.main(argsDecompression);
	}
}
