package ccsds123.memtests;

import ccsds123.memtests.ThreeDCoordinate.AdvancementType;

public class Board {
	private enum BoardState {
		SET, UNSET, NONE;

		@Override
		public String toString() {
			switch(this) {
			case SET:
				return "S";
			case UNSET:
				return "U";
			case NONE: 
				return "N";
			default:
				throw new IllegalStateException("NOT IMPLEMENTED");
			}
		}
	};
	
	private BoardState[][] board;
	private int setCount;
	private int unSetCount;
	private int noneCount;
	private int width, height;
	
	
	public Board(int width, int height) {
		this.board = new BoardState[height][width];
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				this.board[i][j] = BoardState.NONE;
			}
		}
		this.width = width;
		this.height = height;
		this.noneCount = width * height;
		this.setCount = 0;
		this.unSetCount = 0;
	}
	
	private void remove(BoardState boardState) {
		switch (boardState) {
		case SET:
			this.setCount--;
			break;
		case UNSET:
			this.unSetCount--;
			break;
		case NONE:
			this.noneCount--;
			break;
		default:
			throw new IllegalStateException("GELLO");
		}
	}
	
	private void add(BoardState boardState) {
		switch (boardState) {
		case SET:
			this.setCount++;
			break;
		case UNSET:
			this.unSetCount++;
			break;
		case NONE:
			this.noneCount++;
			break;
		default:
			throw new IllegalStateException("GELLO");
		}
	}
	
	private void set(ThreeDCoordinate bc, BoardState state) {
		this.remove(this.board[bc.getZ()][bc.getTMod()]);
		this.board[bc.getZ()][bc.getTMod()] = state;
		this.add(state);
	}
	
	public boolean safeSet(ThreeDCoordinate bc) {
		if (this.board[bc.getZ()][bc.getTMod()] == BoardState.SET) {
			return false;
		} 
		this.set(bc, BoardState.SET);
		return true;
	}
	
	public boolean safeUnSet(ThreeDCoordinate bc) {
		if (this.board[bc.getZ()][bc.getTMod()] != BoardState.SET) {
			return false;
		}
		this.set(bc, BoardState.UNSET);
		return true;
	}
		
	private void checkCount() {
		if (this.totalCount() != this.width * this.height)
			throw new IllegalStateException("ERRR");
	}
	
	private int totalCount() {
		return this.setCount + this.unSetCount + this.noneCount;
	}
	
	private String getState() {
		String ret = "SET: " + this.setCount + " UNSET: " + this.unSetCount + " NONE: " + this.noneCount;
		ret += " (TOTAL: " + (this.totalCount()) + ")";
		return ret;
	}
	
	private void printBoard() {
		String ret = "";
		for (int i = height - 1; i >= 0; i--) {
			for (int j = 0; j < width; j++) {
				ret += this.board[i][j];
			}
			ret += "\n";
		}
		System.out.println(ret);
	}


	
	public static final int EXTRA_PRELOAD = 8; //up to 6 in practice (3x3->7 (1+6)
	
	public static void main(String[] args) {
		Board.checkWriteRead(ThreeDCoordinate.AdvancementType.VERTICAL, ThreeDCoordinate.AdvancementType.DIAGONAL);
		//Board.checkWriteRead(ThreeDCoordinate.AdvancementType.DIAGONAL, ThreeDCoordinate.AdvancementType.VERTICAL);
	}

	private static void checkWriteRead(AdvancementType atWrite, AdvancementType atRead) {
		int size = 4;
		Board board = new Board(size*4, size*4);
		ThreeDCoordinate wCoord = new ThreeDCoordinate(0, 0, 0, 1, size*4, size, atWrite);
		ThreeDCoordinate rCoord = new ThreeDCoordinate(0, 0, 0, 1, size*4, size, atRead);
		
		int load = (size-1)*(size-2)/2 + EXTRA_PRELOAD;
		
		//preload so that then it pipelines greatly
		while (load-->0) {
			//System.out.println("Set @ " + wCoord);
			if (!board.safeSet(wCoord)) {
				throw new IllegalStateException();
			}
			wCoord.nextCoord();
		}
		
		boolean endVert = false, endDiag = false;
		int iters = 0;
		while(!endVert || !endDiag) {
			iters++;
			if (!endVert) {
				if (board.safeSet(wCoord)) {
					//System.out.println("Set @ " + wCoord);
					if (!wCoord.nextCoord())
						endVert = true;
				}
			}
			board.checkCount();
			if (!endDiag) {
				if (board.safeUnSet(rCoord)) {
					//System.out.println("UnSet @ " + rCoord);
					if (!rCoord.nextCoord())
						endDiag = true;
					//System.out.println("Next UnSet @ " + rCoord);
				}
			}
			board.checkCount();
			System.out.println("IT: " + iters + "->" + board.getState());
			//board.printBoard();
			
		}
	
		board.printBoard();
		
		/*while(board.safeSet(vert))
			vert.nextCoord();
		
		System.out.println("IT: " + iters + "->" + board.getState());
		board.printBoard();*/
	}

}
