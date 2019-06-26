package ccsds123.cli;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Anything related to command line interface stuff goes here
 * @author Daniel
 */
public class CCSDSCLI {
	/** Help option constant. Use for retrieving arguments and/or flags */
	public static final String OPTION_HELP = "help";
	/** Show stats about compression */
	public static final String OPTION_SHOW_STATS = "stats";
	/** Compress/Decompress */
	public static final String OPTION_COMPRESS = "compress";
	public static final String OPTION_DECOMPRESS = "decompress";
	/** Input/Output file options */
	public static final String OPTION_INPUT = "input";
	public static final String OPTION_INPUT_HEADER = "input_header";
	public static final String OPTION_OUTPUT = "output";
	public static final String OPTION_OUTPUT_HEADER = "output_header";
	/** Custom size use */
	public static final String OPTION_CUSTOM_SIZE = "custom_size";
	
	/* Options for jypec */
	private static Options ccsds123Options;
	/* Only one instance */
	static {
		/* flags */
		Option help				= new Option("h", OPTION_HELP, false, "print this message");
		Option compressionStats = new Option(null, OPTION_SHOW_STATS, false, "show compression stats");
		Option compress			= new Option("c", OPTION_COMPRESS, false, "compress image");
		Option decompress		= new Option("d", OPTION_DECOMPRESS, false, "decompress image");
		
		/* input output files */
		Option input = Option
				.builder("i")
				.argName("file")
				.desc("input file")
				.hasArg()
				.longOpt(OPTION_INPUT)
				.required()
				.build();
		
		Option inputHeader = Option
				.builder()
				.argName("file")
				.desc("input file header location")
				.hasArg()
				.longOpt(OPTION_INPUT_HEADER)
				.build();
		
		Option output = Option
				.builder("o")
				.argName("file")
				.desc("output file")
				.hasArg()
				.longOpt(OPTION_OUTPUT)
				.required()
				.build();
		
		Option outputHeader = Option
				.builder()
				.argName("file")
				.desc("output file header location")
				.hasArg()
				.longOpt(OPTION_OUTPUT_HEADER)
				.build();
		
		Option customSize = Option
				.builder()
				.argName("dimensions")
				.desc("custom number of bands, lines, samples")
				.numberOfArgs(3)
				.longOpt(OPTION_CUSTOM_SIZE)
				.build();
		
		ccsds123Options = new Options();
		
		ccsds123Options.addOption(output);
		ccsds123Options.addOption(input);
		ccsds123Options.addOption(help);
		ccsds123Options.addOption(compressionStats);
		ccsds123Options.addOption(inputHeader);
		ccsds123Options.addOption(outputHeader);
		ccsds123Options.addOption(compress);
		ccsds123Options.addOption(decompress);
		ccsds123Options.addOption(customSize);
	}
	
	
	/**
	 * testing
	 * @return the options for the jypec cli
	 */
	public static Options getOptions() {
		return ccsds123Options;
	}

	/**
	 * Prints the help for the command line interface of jypec
	 */
	public static void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "jypec", CCSDSCLI.getOptions());
	}

}
