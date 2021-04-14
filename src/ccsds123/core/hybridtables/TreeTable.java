package ccsds123.core.hybridtables;

import java.util.Iterator;

public class TreeTable<TERMINAL_T> implements Iterable<TreeTable<TERMINAL_T>>{
	private TreeTable<TERMINAL_T>[] children;
	private TERMINAL_T value;
	private TreeTable<TERMINAL_T> parent;
	private int parentIndex;
	private int size;
	
	@SuppressWarnings("unchecked")
	private void init(TreeTable<TERMINAL_T> parent, int size, int parentIndex) {
		this.parent = parent;
		this.parentIndex = parentIndex;
		this.children = (TreeTable<TERMINAL_T>[]) new TreeTable<?>[size];
		this.size = size;
	}
	
	//create nonfinal node
	public TreeTable(TreeTable<TERMINAL_T> parent, int size, int parentIndex) {
		this.init(parent, size, parentIndex);
	}
	
	//create final node
	public TreeTable(TreeTable<TERMINAL_T> parent, TERMINAL_T value, int parentIndex) {
		this.init(parent, 0, parentIndex);
		this.value = value;
	}
	
	public void setValue(TERMINAL_T c) {
		this.value = c;
	}

	public void addTerminalNode(int node, TERMINAL_T c) {
		if (!(this.children[node] == null)) 
			throw new IllegalStateException("Adding to: " + node );
		this.children[node] = new TreeTable<TERMINAL_T>(this, c, node);
	}
	
	public boolean isTree() {
		return this.size != 0;
	}
	
	public boolean isTerminal() {
		return this.size == 0;
	}

	public TreeTable<TERMINAL_T> getNextOrAddDefault(int node) {
		if (!this.isTree()) 
			throw new IllegalStateException();
		TreeTable<TERMINAL_T> next = this.children[node];
		if (next == null)
			next = new TreeTable<TERMINAL_T>(this, this.size, node);
		else
			if (!(next.isTree()))
				throw new IllegalStateException("Size was: " + next.size);
		
		this.children[node] = next;
		return next;
	}
	
	public TreeTable<TERMINAL_T> getChild(int node) {
		return this.children[node];
	}
	
	public String toString() {
		String ret = "[" + (this.value == null ? "null": "....") + "] ";
		for (int i = 0; i < this.size; i++) {
			if (this.children[i] != null)
				ret += i + " : { " + this.children[i] + " } "; 
		}
		
		
		return ret;
	}

	public TERMINAL_T getValue() {
		return this.value;
	}
	
	public TreeTable<TERMINAL_T> getParent() {
		return this.parent;
	}
	
	public int getParentIndex() {
		return this.parentIndex;
	}

	@Override
	public Iterator<TreeTable<TERMINAL_T>> iterator() {
		return new Iterator<TreeTable<TERMINAL_T>>() {
			
			int index = 0; 
			{
				advaceIndex(true);
			}
			
			private void advaceIndex(boolean isFirst) {
				for (index = isFirst ? 0 : index + 1; index < size; index++)
					if (children[index] != null)
						break;
				
			}

			@Override
			public boolean hasNext() {
				return index < size;
			}

			@Override
			public TreeTable<TERMINAL_T> next() {
				int pIndex = index;
				this.advaceIndex(false);
				return children[pIndex];
			}
		};
	}

	public boolean hasValue() {
		return this.value != null;
	}

	public boolean isRoot() {
		return this.parent == null;
	}
	
	
	public boolean checkFullTree(int quantAtStart, int quantAtEnd) {
		if (this.isTerminal()) {
			//check that it has a link
			if (!this.hasValue())
				throw new IllegalStateException("Terminal table does not have link");
			return true;
		} else {
			//must have links in all children
			boolean goodChildren = true;
			try {
				for (int i = 0; i < quantAtStart; i++) {
					goodChildren = goodChildren && this.children[i].checkFullTree(quantAtStart, quantAtEnd);
				}
				for (int i = 0; i < quantAtEnd; i++) {
					goodChildren = goodChildren && this.children[this.size - 1 - i].checkFullTree(quantAtStart, quantAtEnd);
				}
			} catch (NullPointerException npe) {
				npe.printStackTrace();
				throw npe;
			}
			if (!goodChildren)
				throw new IllegalStateException("Table not formed properly");
			return true;
		}
	}
	
	//to build the cool tables
	public int id = 0;
	

}
