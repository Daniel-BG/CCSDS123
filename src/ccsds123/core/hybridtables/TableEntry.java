package ccsds123.core.hybridtables;

public class TableEntry<TERMINAL_T, TREE_T> {
	private static enum TableEntryType {
		TERMINAL, TREE
	}
	private TERMINAL_T c;
	private TREE_T ct;
	private TableEntryType ctet;
	public TableEntry(TERMINAL_T c, TREE_T ct) {
		if (c == null && ct == null || c != null && ct != null)
			throw new IllegalArgumentException();
		if (c != null) {
			this.c = c;
			this.ctet = TableEntryType.TERMINAL;
		} else {
			this.ct = ct;
			this.ctet = TableEntryType.TREE;
		}
	}
	
	public boolean isTree() {
		return this.ctet == TableEntryType.TREE;
	}
	public TREE_T getTree() {
		if (this.ct == null)
			throw new IllegalStateException();
		return this.ct;
	}
	
	public TERMINAL_T getTerminal() {
		if (this.c == null)
			throw new IllegalStateException();
		return this.c;
	}
	
	public String toString() {
		if (this.isTree()) {
			return ct.toString();
		} else {
			return c.toString();
		}
	}
}
