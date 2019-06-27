package ccsds123;

public class Test {
	
	static String input = "C:/Users/Daniel/Hiperspectral images/Reno_Radiance_wIGMGLT/0913-1248_rad.dat";
	//static String input = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Rad/Suwannee_0609-1331_rad.dat";
	//static String input = "C:/Users/Daniel/Hiperspectral images/Beltsville_Radiance_w_IGM/0810_2022_rad.dat";
	static String inputHeader = "C:/Users/Daniel/Hiperspectral images/Reno_Radiance_wIGMGLT/0913-1248_rad.hdr";
	//static String inputHeader = "C:/Users/Daniel/Hiperspectral images/Gulf_Wetlands_Sample_Rad/Suwannee_0609-1331_rad.hdr";
	//static String inputHeader = "C:/Users/Daniel/Hiperspectral images/Beltsville_Radiance_w_IGM/0810_2022_rad.hdr";
	
	
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
				"-i", input,
				"--input_header", inputHeader,
				"-o", output,
				"-k",
				"--bitdepth", "16",
				"--custom_size", "16", "16", "16"
		};

	public static void main(String[] args) {
		Main.main(argsCompare);
		
		//Main.main(argsCompression);
		//Main.main(argsDecompression);
	}
}
