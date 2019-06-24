package ccsds123.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;


/**
 * Store input arguments in their parsed form for easier processing
 * @author Daniel
 */
public class InputArguments {
	/** Help was requested */
	public boolean help = false;
	/** Stats were requested */
	public boolean showCompressionStats = false;
	/** Asked to compress */
	public boolean compress = false;
	/** Asked to decompress */
	public boolean decompress = false;
	
	
	//files
	/** Input file path. Null if not set */
	public String input = null;
	/** Input file header. Null if not set */
	public String inputHeader = null;
	/** Output file path. Null if not set */
	public String output = null;
	/** Output file header. Null if not set */
	public String outputHeader = null;
	
	
	
	/**
	 * @param line line where to parse the commands from
	 * @return an InputArguments object filled from the command line
	 * @throws ParseException 
	 */
	public static InputArguments parseFrom(CommandLine line) throws ParseException {
		InputArguments args = new InputArguments();

		args.showCompressionStats = line.hasOption(CCSDSCLI.OPTION_SHOW_STATS);
		args.help = line.hasOption(CCSDSCLI.OPTION_HELP);
		
		args.compress = line.hasOption(CCSDSCLI.OPTION_COMPRESS);
		args.decompress = line.hasOption(CCSDSCLI.OPTION_DECOMPRESS);
		
		args.input = line.getOptionValue(CCSDSCLI.OPTION_INPUT);
		args.output = line.getOptionValue(CCSDSCLI.OPTION_OUTPUT);
		args.inputHeader = line.getOptionValue(CCSDSCLI.OPTION_INPUT_HEADER);
		args.outputHeader = line.getOptionValue(CCSDSCLI.OPTION_OUTPUT_HEADER);
		
		
		return args;
	}
}
