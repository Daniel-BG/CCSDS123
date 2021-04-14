package ccsds123.core.hybridtables;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jypec.util.Pair;
import com.jypec.util.bits.Bit;
import com.jypec.util.bits.BitTwiddling;

public class CodeCreator {
	
	public static final char CODE_X_HEX = 'D';
	public static final int CODE_X_VAL = 13;
	public static final int CODE_AMOUNT = 14;
	
	public static final int[] INPUTSYMBOLLIMIT = {	12, 	10, 	8, 		6, 
			6, 		4, 		4, 		4,
			2,		2, 		2, 		2, 
			2, 		2, 		2, 		0};

	public static final int[] THRESHOLD = 		  {	303336, 225404, 166979, 128672, 
				95597, 	69670, 	50678, 	34898, 
				23331, 	14935, 	9282, 	5510, 
				3195, 	1928, 	1112, 	408};
	
	@SuppressWarnings("unchecked")
	static TreeTable<Codeword>[] tables = (TreeTable<Codeword>[]) new TreeTable<?>[16];
	@SuppressWarnings("unchecked")
	static TreeTable<TreeTable<Codeword>>[] reverseTables = (TreeTable<TreeTable<Codeword>>[]) new TreeTable<?>[16];
	@SuppressWarnings("unchecked")
	static TreeTable<TreeTable<Codeword>>[] reverseFlushTables = (TreeTable<TreeTable<Codeword>>[]) new TreeTable<?>[16];
	
	
	static {
		//create tables
		for (int i = 0; i < 16; i++) {
			tables[i] = createTable(RawCodeTables.lecs[i], RawCodeTables.flecs[i]);
			reverseTables[i] = createReverseTable(tables[i], false);
			reverseFlushTables[i] = createReverseTable(tables[i], true);
		}
	}
	
	public static void checkAllTableIntegrity() {
		//check full tree table integrity
		for (int i = 0; i < 16; i++) {
			tables[i].checkFullTree(INPUTSYMBOLLIMIT[i], 1);
			reverseTables[i].checkFullTree(2, 0);
			reverseFlushTables[i].checkFullTree(2, 0);
		}
		System.out.println("All tables are full trees");
		
		//check that every node is reachable in reverse and only once
		for (int i = 0; i < 16; i++) {
			Set<TreeTable<Codeword>> dtset = new HashSet<>();
			Stack<TreeTable<Codeword>> dtstack = new Stack<>();
			//create set of unique tables
			dtstack.push(tables[i]);
			while(!dtstack.isEmpty()) {
				TreeTable<Codeword> ctt = dtstack.pop();
				if (dtset.contains(ctt))
					throw new IllegalStateException();
				dtset.add(ctt);
				for (TreeTable<Codeword> tt: ctt)
					dtstack.push(tt);
			}
			//check that all are reachable
			Stack<TreeTable<TreeTable<Codeword>>> reverseStack = new Stack<>();
			reverseStack.push(reverseTables[i]);
			reverseStack.push(reverseFlushTables[i]);
			while (!reverseStack.isEmpty()) {
				TreeTable<TreeTable<Codeword>> crt = reverseStack.pop();
				if (crt.isTerminal()) {
					//remove its object from the dtset
					if (!dtset.contains(crt.getValue()))
						throw new IllegalStateException();
					dtset.remove(crt.getValue());
				} else {
					for (TreeTable<TreeTable<Codeword>> child: crt)
						reverseStack.push(child);
				}
			}
			//check that all were visited
			if (!dtset.isEmpty())
				throw new IllegalStateException();
		}
		
		System.out.println("All reverse tables correctly point to all direct tables");
	}
	
	private static final long CODE_FLUSH = 0xf;
	private static final long CODE_NONE = 0xb;
	private static final long CODE_TERMINAL = 0xc;
	private static final long CODE_NEXTTAB  = 0x4;
	
	private static void generateVHDLRepr() {
		//generate IDs for each and every table
		Queue<TreeTable<Codeword>> tablequeue = new LinkedList<TreeTable<Codeword>>();
		for (int i = 0; i < 16; i++) {
			tablequeue.add(tables[i]);
		}
		int tableIndex = 0;
		int maxCodeBits = 0;
		while (!tablequeue.isEmpty()) {
			TreeTable<Codeword> tq = tablequeue.poll();
			if (!tq.isTerminal()) {
				tq.id = tableIndex;
				tableIndex++;
			}
			for (TreeTable<Codeword> child: tq)
				tablequeue.add(child);
			maxCodeBits = Math.max(maxCodeBits, tq.getValue().getBits());
		}
		
		int necessaryRefBits = BitTwiddling.bitsOf(tableIndex);
		int entryBits = Math.max(necessaryRefBits, maxCodeBits);
		int entryLengthBits = BitTwiddling.bitsOf(entryBits);
		int totalBitsPerEntry = 32; //entryBits + entryLengthBits + 1; //1 for either reference/code
		if (entryBits + entryLengthBits > 28) 
			throw new IllegalStateException("Code is thought for up to 28 bits of entry plus 4 of entry code, change it if this exception is raised");
		
		System.out.println("Total tables: " + tableIndex + " needing " + (CODE_AMOUNT + 1) + " entries and "+ totalBitsPerEntry + " bits per entry ");
		BigInteger[] binTables = new BigInteger[tableIndex];
		
		tablequeue = new LinkedList<TreeTable<Codeword>>();
		for (int i = 0; i < 16; i++) {
			tablequeue.add(tables[i]);
		}
		while (!tablequeue.isEmpty()) {
			TreeTable<Codeword> tq = tablequeue.poll();
			//TODO add the code for the current table
			if (tq.isTerminal())
				throw new IllegalStateException("We are not building terminals here, tables need to always have children");
			long code = tq.getValue().getValue();
			code |= ((long) tq.getValue().getBits()) << entryLengthBits;
			code |= CODE_FLUSH << (totalBitsPerEntry - 4);
			BigInteger base = BigInteger.valueOf(code);
			
			for (int i = 0; i < CODE_AMOUNT; i++) {
				TreeTable<Codeword> child = tq.getChild(i);
				long nextCode;
				if (child == null) {
					//shift base and continue
					nextCode = CODE_NONE << (totalBitsPerEntry - 4);
				} else if (child.isTerminal()) {
					//add terminal thingy to the table
					nextCode = child.getValue().getValue();
					nextCode |= ((long) child.getValue().getBits()) << entryLengthBits;
					nextCode |= CODE_TERMINAL << (totalBitsPerEntry - 4);
				} else {
					tablequeue.add(child);
					//add reference to node to the table
					nextCode = child.id;
					nextCode |= CODE_NEXTTAB << (totalBitsPerEntry - 4);
				}
				base = base.shiftLeft(totalBitsPerEntry);
				base = base.or(BigInteger.valueOf(nextCode));
			}
			//save the current table entry
			binTables[tq.id] = base;
		}
		//output this shiat
		for (BigInteger bi: binTables)
			System.out.println("x\"" + bi.toString(16) + "\",");
		
	}
	
	public static void main(String[] args) {
		//print and check integrity
		for (int i = 0; i < 16; i++) {
			System.out.println("Table for " + i + ": " + tables[i]);
			System.out.println("Rtabl for " + i + ": " + reverseTables[i]);
			System.out.println("RFtab for " + i + ": " + reverseFlushTables[i]);
		}
		System.out.println("All tables built");
		
		checkAllTableIntegrity();
		generateVHDLRepr();
	}
	


	public static TreeTable<Codeword>[] getCodeTables() {
		return tables;
	}
	
	public static TreeTable<TreeTable<Codeword>>[] getReverseFlushTables() {
		return reverseFlushTables;
	}
	
	public static TreeTable<TreeTable<Codeword>>[] getReverseTables() {
		return reverseTables;
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
					outputCodeNum = BitTwiddling.reverseBits(outputCodeNum, outBits);
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
		 
		TreeTable<Codeword> ct = new TreeTable<Codeword>(null, CODE_AMOUNT, 0);
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
			currentTable.setValue(new Codeword(code));
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
	
	
	private static TreeTable<TreeTable<Codeword>> createReverseTable(TreeTable<Codeword> tabs, boolean flush) {
		TreeTable<TreeTable<Codeword>> rt = new TreeTable<TreeTable<Codeword>>(null, 2, 0);
				
		Stack<TreeTable<Codeword>> tts = new Stack<TreeTable<Codeword>>();
		tts.push(tabs);
		
		while (!tts.isEmpty()) {
			TreeTable<Codeword> currentTable = tts.pop();
			//add terminal code only if table is terminal
			if (!flush && !currentTable.isTree() || flush & currentTable.isTree()) {
				Codeword val = currentTable.getValue();
				//System.out.println("Adding cw: " + val);
				addReverseTableEntry(rt, val, currentTable);
			}
			for (TreeTable<Codeword> child: currentTable) {
				tts.push(child);
			}
			
		}
		return rt;
	}
	
	private static void addReverseTableEntry(TreeTable<TreeTable<Codeword>> rt, Codeword val, TreeTable<Codeword> reference) {
		Iterator<Bit> itbit = val.reverseIterator();
		Bit bit = itbit.next();

		int cnt = 0;
		int sfx = 0;
		//go deep until it is terminal
		while(itbit.hasNext()) {
			sfx |= (bit.toInteger() << cnt);
			cnt++;
			try {
				rt = rt.getNextOrAddDefault(bit.toInteger());
			} catch (Exception e) {
				System.err.println("<" + e.getMessage() + "> "+ val + " has a suffix already processed: " + cnt + "'h" + Integer.toHexString(sfx));
				return;
			}
			bit = itbit.next();
		}
		//add the terminal node that references the input codeword table
		try {
			rt.addTerminalNode(bit.toInteger(), reference);
		} catch (Exception e) {
			System.err.println(reference.getValue() + " is a suffix of an already processed code");
			return;
		}
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
