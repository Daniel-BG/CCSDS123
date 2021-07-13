package ccsds123;

import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

public class Dec2Hex {
	
	public static void main(String[] args) {
		
		
		try {
			//String inputFile = "C:/Users/Daniel/Basurero/out/output.mif";
			String inputFile = "C:/Users/Daniel/Basurero/out/c_s.mif";
			String outputFile = inputFile + ".hex";
			Scanner sc = new Scanner(new File(inputFile));
			FileWriter myWriter = new FileWriter(outputFile);
			
			while(sc.hasNextLong())
			{
				long l = sc.nextLong();
				myWriter.write(Long.toHexString(l)+"\n");
			}
			
			sc.close();
			myWriter.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
