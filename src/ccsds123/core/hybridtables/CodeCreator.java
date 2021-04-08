package ccsds123.core.hybridtables;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jypec.util.Pair;

public class CodeCreator {
	
	public static final char CODE_X_HEX = 'D';
	public static final int CODE_X_VAL = 13;
	public static final int CODE_AMOUNT = 14;
	
	@SuppressWarnings("unchecked")
	static TreeTable<Codeword>[] tables = (TreeTable<Codeword>[]) new TreeTable<?>[16];
	static TreeTable<TreeTable<Codeword>> reverseTable;
	
	static {
		//create mqi->code tables
		for (int i = 0; i < 16; i++)
			tables[i] = createTable(RawCodeTables.lecs[i], RawCodeTables.flecs[i]);
		//create code->codetable tables
		/*reverseTable = new TreeTable<TreeTable<Codeword>>(null, 2);
		
		Stack<TreeTable<Codeword>> tts = new Stack<TreeTable<Codeword>>();
		for (TreeTable<Codeword> tt: tables) {
			tts.push(tt);
		}
		while (!tts.isEmpty()) {
			TreeTable<Codeword> currentTable = tts.pop();
			//add terminal code
			Codeword val = currentTable.getValue();
			addReverseTableEntry(reverseTable, val);
		}*/
	}
	
	public static void main(String[] args) {
		for (TreeTable<Codeword> ct: tables) 
			System.out.println(ct);
	}
	
	public static TreeTable<Codeword>[] getCodeTables() {
		return tables;
	}
	
	private static List<Pair<String, String>> parseInput(String rawInputTable) {
		//separate all input prefix and output prefix
		String basePattern = "([^,\\s]+|[^,]+,\\s[^\\s]+)\\s([^\\s]+)[\\s]?";
		List<Pair<String, String>> allMatches = new ArrayList<Pair<String, String>>();
		Matcher mbase = Pattern.compile(basePattern).matcher(rawInputTable);
		while (mbase.find()) {
			String input = mbase.group(1);
			input = input.replace('X', CODE_X_HEX); //replace X with D's to parse later better
			if (input.contains("null")) //replace nulls with empty sequences
				input = "";
			String output = mbase.group(2);
			allMatches.add(new Pair<String, String>(input, output));
		}
		
		//postprocess step by step	
		//first replace 'r' by all its values
		List<Pair<String, String>> allMatchesTemp = new ArrayList<Pair<String, String>>();
		for (Pair<String, String> entry: allMatches) {
			
			Pattern patternR = Pattern.compile("0\\^\\{r}([^,]+)?,\\s(\\d+)≤r≤(\\d+)");
			Matcher m = patternR.matcher(entry.first());
			if (m.matches()) {
				//need to match the second part as well
				Pattern patternRsec = Pattern.compile("<(\\d+).h\\(([0-9A-F]+)\\+(\\d+)?r\\)>");
				Matcher m2 = patternRsec.matcher(entry.second()); 
				if (!m2.matches()) {
					throw new IllegalStateException("Error matching");
				}
				//need to re-generate all of the new possibilities
				String firstPattern = m.group(1) == null ? "" : m.group(1);
				int lowBound = Integer.parseInt(m.group(2));
				int highBound = Integer.parseInt(m.group(3));
				int outBits = Integer.parseInt(m2.group(1));
				int base = Integer.parseInt(m2.group(2), 16);
				int multiplier = m2.group(3) == null ? 1 : Integer.parseInt(m2.group(3));
				for (int i = lowBound; i <= highBound; i++) {
					String inputCode = createZeroString(i) + firstPattern;
					int outputCodeNum = base + multiplier*i;
					String outputCode = "" + outBits + "'h" + Integer.toHexString(outputCodeNum);
					allMatchesTemp.add(new Pair<String, String>(inputCode, outputCode));
				}
			} else {
				allMatchesTemp.add(entry);
			}
		}
		allMatches = allMatchesTemp;
		
		//now replace 0^{\d+} occurences by the raw value
		for (Pair<String, String> entry: allMatches) {
			Pattern fixedRepPattern = Pattern.compile("0\\^\\{(\\d+)\\}(.*)");
			Matcher mfrep = fixedRepPattern.matcher(entry.first());
			if (mfrep.matches()) {
				int numzeros = Integer.parseInt(mfrep.group(1));
				String input = "";
				for (int i = 0; i < numzeros; i++) 
					input += "0";
				
				input += mfrep.group(2); 
				entry.setFirst(input);
			}
		}
		
		return allMatches;
	}
	
	private static TreeTable<Codeword> createTable(String TreeTable, String terminalTable) {

		List<Pair<String, String>> allMatches = parseInput(TreeTable);
		 
		TreeTable<Codeword> ct = new TreeTable<Codeword>(null, CODE_AMOUNT);
		for (Pair<String, String> entry: allMatches) {
			String sequence = entry.first();
			String code = entry.second();
			//System.out.println(sequence + "->" + code);
			TreeTable<Codeword> currentTable = ct;
			for (int i = 0; i < sequence.length(); i++) {
				int node = Integer.parseInt(""+sequence.charAt(i), 16);
				if (i == sequence.length() - 1) {
					//this is the last, add code to code table
					Codeword cw = new Codeword(code);
					currentTable.addTerminalNode(node, cw);
				} else {
					//go through the table
					currentTable = currentTable.getNextOrAddDefault(node);
				}
			}
		}
		
		allMatches = parseInput(terminalTable);
		for (Pair<String, String> entry: allMatches) {
			String sequence = entry.first();
			String code = entry.second();
			//System.out.println(sequence + "->" + code);
			TreeTable<Codeword> currentTable = ct;
			for (int i = 0; i < sequence.length(); i++) {
				int node = Integer.parseInt(""+sequence.charAt(i), 16);
				currentTable = currentTable.getNextOrAddDefault(node);
			}
			currentTable.setCodeword(new Codeword(code));
		}

		return ct;
	}
	
	private static String createZeroString(int length) {
		String ret = "";
		for (int i = 0; i < length; i++) {
			ret += "0";
		}
		return ret;
	}
	
}

/*
 * 
 * 	
	/*
	 * ([0-9A-Z]+)\ (\d+)['’]h([0-9A-Z]+)\s*
	 * lowent_0_code.add(new Pair<Codeword, Codeword>(new Codeword("$1"), new Codeword($2, "$3")));\n
	 * ([^,\s]+|[^,]+,\s[^\s]+)\s([^\s]+)[\s]?
	 *
 * 			String fixedRepPattern = "0\\^\\{(\\d+)\\}(.*)";
			Matcher mfrep = Pattern.compile(fixedRepPattern).matcher(input);
			if (mfrep.matches()) {
				int numzeros = Integer.parseInt(mfrep.group(1));
				input = "";
				for (int i = 0; i < numzeros; i++) 
					input += "0";
				
				input += mfrep.group(2); 
			}
			
	*/
