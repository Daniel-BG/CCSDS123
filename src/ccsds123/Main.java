package ccsds123;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import ccsds123.cli.CCSDSCLI;
import ccsds123.cli.InputArguments;
import ccsds123.core.SegmentedCompressor;
import hyppo.data.HyperspectralImage;
import hyppo.io.HyperspectralImageDataWriter;
import hyppo.io.HyperspectralImageReader;
import hyppo.io.HyperspectralImageWriter;
import hyppo.io.headerio.enums.BandOrdering;
import hyppo.io.headerio.enums.ByteOrdering;
import ccsds123.core.Compressor;
import ccsds123.core.CompressorParameters;
import ccsds123.core.Constants;
import ccsds123.core.EntropyCoder;
import ccsds123.core.HybridEntropyCoder;
import ccsds123.core.SampleAdaptiveEntropyCoder;
import ccsds123.core.SamplingUnit;


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
	        //parse compressor parameters and create compression/decompression stuff
	        SamplingUnit su = new SamplingUnit();
	        CompressorParameters cp = new CompressorParameters();
    		int[] absErr = new int[1];
	        int[] relErr = new int[1];
	        absErr[0] = iArgs.max_abs_err;
	        relErr[0] = iArgs.max_rel_err;
	        cp.setDefaults();
	        cp.setErrors(Constants.DEFAULT_ABSOLUTE_ERROR_LIMIT_BIT_DEPTH, Constants.DEFAULT_RELATIVE_ERROR_LIMIT_BIT_DEPTH, absErr, relErr, iArgs.use_max_abs_err, iArgs.use_max_rel_err);
    		EntropyCoder ec;
	        if (iArgs.use_hybrid)
	        	ec = new HybridEntropyCoder(su, cp);
	        else
	        	ec = new SampleAdaptiveEntropyCoder(su, cp);
	        Compressor c = new SegmentedCompressor(ec, cp, su);
	        
	        
	        //go through options
	        if (iArgs.help) {
	        	printHelp();
	        } else if (iArgs.compress || iArgs.compare) {
	        	try {
	        		//read input image
	        		HyperspectralImage hi = HyperspectralImageReader.read(iArgs.input, iArgs.inputHeader, true);	   
	        		//preprocess input image
	        		if (iArgs.useCustomSize) {
	        			if (iArgs.bands > hi.getData().getNumberOfBands()) {
	        				iArgs.bands = hi.getData().getNumberOfBands();
	        				System.out.println("Custom band size too big, resized to: " + iArgs.bands);
	        			}
	        			if (iArgs.lines > hi.getData().getNumberOfLines()) {
	        				iArgs.lines = hi.getData().getNumberOfLines();
	        				System.out.println("Custom line size too big, resized to: " + iArgs.lines);
	        			}
	        			if (iArgs.samples > hi.getData().getNumberOfSamples()) {
	        				iArgs.samples = hi.getData().getNumberOfSamples();
	        				System.out.println("Custom sample size too big, resized to: " + iArgs.samples);
	        			}
	        			hi.resize(iArgs.bands, iArgs.lines, iArgs.samples);
	        		}
	        		if (hi.getData().getDataType().isSigned() || hi.getData().getDataType().isFloating()) {
	        			System.out.println("Normalizing image in format " + hi.getData() + " from " + hi.getData().getDataType() + " to work with comressor @Main.java");
	        			hi.normalize();
	        		}
	        		
	        		
	        		HyperspectralImageDataWriter.writeImageData(hi.getData(), 0, iArgs.output + ".rawin.bin", BandOrdering.BIP, ByteOrdering.LITTLE_ENDIAN);
	        		//prepare for compression
	        		cp.setSize(hi.getData().getNumberOfSamples(), hi.getData().getNumberOfLines(), hi.getData().getNumberOfBands());

	        		if (iArgs.compress)
	        			CCSDS.compress(c, hi.getData(), iArgs.output, iArgs);
	        		else
	        			CCSDS.compare(c, hi, iArgs);
				} catch (IOException e) {
					e.printStackTrace();
				}
	        } else if (iArgs.decompress) {
	        	try {
	        		cp.setSize(iArgs.samples, iArgs.lines, iArgs.bands);
	        		//read header to decompress
					HyperspectralImage hi = CCSDS.decompress(c, iArgs);
					HyperspectralImageWriter hiw = new HyperspectralImageWriter();
					//hiw.write(hi, iArgs);
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
