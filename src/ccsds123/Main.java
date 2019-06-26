package ccsds123;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import ccsds123.cli.CCSDSCLI;
import ccsds123.cli.InputArguments;


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
	        //go through options
	        if (iArgs.help) {
	        	printHelp();
	        } else if (iArgs.compress) {
	        	try {
					CCSDS.compress(iArgs);
				} catch (IOException e) {
					e.printStackTrace();
				}
	        } else if (iArgs.decompress) {
	        	try {
					CCSDS.decompress(iArgs);
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
