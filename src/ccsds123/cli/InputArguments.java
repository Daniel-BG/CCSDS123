package ccsds123.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import ccsds123.core.Constants;


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
	/** signed/floating samples (Decompression) */
	public boolean signed;
	public boolean floating = false;
	
	//custom algorithm stuff
	public int max_abs_err = Constants.DEFAULT_ABS_ERR_VALUE;
	public int max_rel_err = Constants.DEFAULT_REL_ERR_VALUE;
	public boolean use_max_abs_err = false;
	public boolean use_max_rel_err = false;
	public boolean use_hybrid = false;
	
	
	
	
	/**
	 * @param line line where to parse the commands from
	 * @return an InputArguments object filled from the command line
	 * @throws ParseException 
	 */
	public static InputArguments parseFrom(CommandLine line) throws ParseException {
		InputArguments args = new InputArguments();
		
		args.use_hybrid = line.hasOption(CCSDSCLI.OPTION_HYBRID);

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
		
		//parse algorithm values
		if (line.hasOption(CCSDSCLI.OPTION_MAX_ABS_ERR)) {
			args.max_abs_err = Integer.parseInt(line.getOptionValue(CCSDSCLI.OPTION_MAX_ABS_ERR));
			args.use_max_abs_err = true;
		} else {
			args.use_max_abs_err = false;
		}
		
		if (line.hasOption(CCSDSCLI.OPTION_MAX_REL_ERR)) {
			args.max_rel_err = Integer.parseInt(line.getOptionValue(CCSDSCLI.OPTION_MAX_REL_ERR));
			args.use_max_rel_err = true;
		} else {
			args.use_max_rel_err = false;
		}
		
		return args;
	}
}
