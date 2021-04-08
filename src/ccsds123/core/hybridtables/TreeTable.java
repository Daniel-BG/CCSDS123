package ccsds123.core.hybridtables;

import java.util.Iterator;

public class TreeTable<TERMINAL_T> implements Iterable<TableEntry<TERMINAL_T, TreeTable<TERMINAL_T>>>{
	private TableEntry<TERMINAL_T, TreeTable<TERMINAL_T>>[] table;
	private TERMINAL_T value;
	private TreeTable<TERMINAL_T> parent;
	private int size;
	
	@SuppressWarnings("unchecked")
	public TreeTable(TreeTable<TERMINAL_T> parent, int size) {
		this.parent = parent;
		this.table = (TableEntry<TERMINAL_T, TreeTable<TERMINAL_T>>[]) new TableEntry<?, ?>[size];
		this.size = size;
	}
	
	public void setCodeword(TERMINAL_T c) {
		this.value = c;
	}

	public void addTerminalNode(int node, TERMINAL_T c) {
		if (!(this.table[node] == null)) 
			throw new IllegalStateException();
		this.table[node] = new TableEntry<TERMINAL_T, TreeTable<TERMINAL_T>>(c, null);
	}

	public TreeTable<TERMINAL_T> getNextOrAddDefault(int node) {
		TableEntry<TERMINAL_T, TreeTable<TERMINAL_T>> next = this.table[node];
		if (next == null)
			next = new TableEntry<TERMINAL_T, TreeTable<TERMINAL_T>>(null, new TreeTable<TERMINAL_T>(this, this.size));
		else
			if (!(next.isTree()))
				throw new IllegalStateException();
		
		this.table[node] = next;
		return next.getTree();
	}
	
	public TableEntry<TERMINAL_T, TreeTable<TERMINAL_T>> getEntry(int node) {
		return this.table[node];
	}
	
	public String toString() {
		String ret = "[" + this.value + "] ";
		for (int i = 0; i < this.size; i++) {
			if (this.table[i] != null)
				ret += i + " : { " + this.table[i] + " } "; 
		}
		
		
		return ret;
	}

	public TERMINAL_T getValue() {
		return this.value;
	}
	
	public TreeTable<TERMINAL_T> getParent() {
		return this.parent;
	}

	@Override
	public Iterator<TableEntry<TERMINAL_T, TreeTable<TERMINAL_T>>> iterator() {
		return new Iterator<TableEntry<TERMINAL_T, TreeTable<TERMINAL_T>>>() {
			
			int index = 0; 
			{
				advaceIndex(true);
			}
			
			private void advaceIndex(boolean isFirst) {
				for (int i = isFirst ? 0 : index + 1; i < size; i++)
					if (table[i] != null)
						break;
			}

			@Override
			public boolean hasNext() {
				return index < size;
			}

			@Override
			public TableEntry<TERMINAL_T, TreeTable<TERMINAL_T>> next() {
				int pIndex = index;
				this.advaceIndex(false);
				return table[pIndex];
			}
		};
	}
}
