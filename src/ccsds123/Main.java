package ccsds123;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import com.jypec.img.HyperspectralImage;
import com.jypec.img.HyperspectralImageData;
import com.jypec.util.io.HyperspectralImageReader;

import ccsds123.cli.CCSDSCLI;
import ccsds123.cli.InputArguments;
import ccsds123.core.SegmentedCompressor;
import ccsds123.core.Compressor;
import ccsds123.core.Constants;
import ccsds123.core.DirectCompressor;


public class Main {
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    //create the parser
	    CommandLineParser parser = new DefaultParser();
	    try {
	        //parse the command line arguments
	        CommandLine line = parser.parse( CCSDSCLI.getOptions(), args );
	        InputArguments iArgs = InputArguments.parseFrom(line);
	        Compressor c = new SegmentedCompressor();
	        //Compressor c = new DirectCompressor();
	        
	        //set compressor parameters
	        int[] absErr = new int[1];
	        int[] relErr = new int[1];
	        absErr[0] = iArgs.max_abs_err;
	        relErr[0] = iArgs.max_rel_err;
	        c.setErrors(Constants.DEFAULT_ABSOLUTE_ERROR_LIMIT_BIT_DEPTH, Constants.DEFAULT_RELATIVE_ERROR_LIMIT_BIT_DEPTH, absErr, relErr, true, true);
	        
	        
	        //go through options
	        if (iArgs.help) {
	        	printHelp();
	        } else if (iArgs.compress || iArgs.compare) {
	        	try {
	        		//read input image
	        		HyperspectralImage hi = HyperspectralImageReader.read(iArgs.input, iArgs.inputHeader, true);	   
	        		HyperspectralImageData hid = hi.getData();
	        		if (iArgs.useCustomSize)
	        			hid = hid.resize(iArgs.bands, iArgs.lines, iArgs.samples);
	        		
	        		if (iArgs.compress)
	        			CCSDS.compress(c, hid, iArgs.output, iArgs);
	        		else
	        			CCSDS.compare(c, hid, iArgs);
				} catch (IOException e) {
					e.printStackTrace();
				}
	        } else if (iArgs.decompress) {
	        	try {
					CCSDS.decompress(c, iArgs);
				} catch (IOException e) {
					e.printStackTrace();
				}
	        } else {
	        	throw new ParseException("Missing options -c -d, i don't know what to do");
	        }
	    }
	    catch( ParseException exp ) {
	        System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
	        printHelp();
	    }
	}
	
	
	/**
	 * Prints help for the command line interface
	 */
	private static void printHelp() {
		CCSDSCLI.printHelp();
	}
	
}
