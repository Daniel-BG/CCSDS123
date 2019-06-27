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
	/** Asked to compare */
	public boolean compare;
	
	
	//files
	/** Input file path. Null if not set */
	public String input = null;
	/** Input file header. Null if not set */
	public String inputHeader = null;
	/** Output file path. Null if not set */
	public String output = null;
	/** Output file header. Null if not set */
	public String outputHeader = null;
	
	//image size
	/** use custom image size */
	public boolean useCustomSize = false;
	/** number of bands in custom size */
	public int bands = 0;
	/** number of lines in custom size */
	public int lines = 0;
	/** number of samples in custom size */
	public int samples = 0;
	/** bitdepth (Decompression) */
	public int bitDepth;
	/** signed samples (Decompression) */
	public boolean signed;
	
	
	
	
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
		args.compare = line.hasOption(CCSDSCLI.OPTION_COMPARE);
		
		args.input = line.getOptionValue(CCSDSCLI.OPTION_INPUT);
		args.output = line.getOptionValue(CCSDSCLI.OPTION_OUTPUT);
		args.inputHeader = line.getOptionValue(CCSDSCLI.OPTION_INPUT_HEADER);
		args.outputHeader = line.getOptionValue(CCSDSCLI.OPTION_OUTPUT_HEADER);
		
		String[] sizeArgs = line.getOptionValues(CCSDSCLI.OPTION_CUSTOM_SIZE);
		if (sizeArgs != null) {
			args.useCustomSize = true;
			args.bands   = Integer.parseInt(sizeArgs[0]);
			args.lines   = Integer.parseInt(sizeArgs[1]);
			args.samples = Integer.parseInt(sizeArgs[2]);
		} else {
			args.useCustomSize = false;
		}
		
		if (line.hasOption(CCSDSCLI.OPTION_BITDEPTH))
			args.bitDepth = Integer.parseInt(line.getOptionValue(CCSDSCLI.OPTION_BITDEPTH));
		args.signed   = line.hasOption(CCSDSCLI.OPTION_SIGNED);
		
		return args;
	}
}
