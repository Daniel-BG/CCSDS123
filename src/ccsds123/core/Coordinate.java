package ccsds123.core;

import java.util.Iterator;

public class Coordinate {
	public int band, line, sample;
	
	public Coordinate(int band, int line, int sample) {
		this.band = band;
		this.line = line;
		this.sample = sample;
	}
	
	public boolean firstLine() {
		return this.line == 0;
	}
	
	public boolean firstSample() {
		return this.sample == 0;
	}
	
	public boolean firstBand() {
		return this.band == 0;
	}
	
	public boolean firstT() {
		return this.firstLine() && this.firstSample();
	}
	
	public boolean lastT(int samples, int lines) {
		return this.lastLine(lines) && this.lastSample(samples);
	}
	
	public boolean lastLine(int lines) {
		return this.line == lines - 1;
	}
	
	public boolean lastSample(int samples) {
		return this.sample == samples - 1;
	}
	
	public boolean lastBand(int bands) {
		return this.band == bands - 1;
	}
	
	@Override
	public String toString() {
		return "Z: " + band + " Y: " + line + " X: " + sample;
	}

	public int getT(int samples) {
		return this.line * samples + this.sample;
	}
	
	
	public static Iterator<Coordinate> getDiagonalIterator(int inBands, int inLines, int inSamples) {
		return new Iterator<Coordinate>() {
			int maxT 	= inLines*inSamples - 1;
			int samples = inSamples;
			int bands 	= inBands;
			
			int topZ = 1;
			int bottomZ = 0;
			int tStart = 0;
			int i = topZ - 1;
			boolean hasNext = true;
			
			{
				
			}

			@Override
			public boolean hasNext() {
				return hasNext;
			}

			@Override
			public Coordinate next() {
				Coordinate nextCoord;
				boolean endLoop = false;
				
				int z = i;
				int t = tStart + (topZ - 1) - i;
				int x = t%this.samples;
				int y = t/this.samples;
				nextCoord = new Coordinate(z, y, x); 
				if (t == maxT) {
					bottomZ++;
					endLoop = true;
				} else {
					i--;
					if (i < bottomZ) {
						endLoop = true;
					}
				}
				
				if (endLoop) {
					if (topZ < bands) {
						topZ++;
					} else {
						tStart++;
					}
					i = topZ - 1;
					if (topZ == bottomZ)
						hasNext = false;
				}
			
				return nextCoord;
			}
			
		};
	}


}

/*
 * 		coordQueue = new LinkedList<Coordinate>();
		LinkedList<Coordinate> outputCoordQueue = new LinkedList<Coordinate>();
		sampleQueue = new LinkedList<Integer>();
		this.entropyCoder.reset();
		//add all coords to coordQueue following a diagonal pattern
		int maxT = this.parameters.lines*this.parameters.samples-1;
		int topZ = 1;
		int bottomZ = 0;
		int tStart = 0;
		while(topZ != bottomZ) {
			for (int i = topZ-1; i >= bottomZ; i--) {
				int z = i;
				int t = tStart + (topZ - 1) - i;
				int x = t%this.parameters.samples;
				int y = t/this.parameters.samples;
				Coordinate currCoord = new Coordinate(z, y, x);
				coordQueue.add(currCoord);
				outputCoordQueue.add(currCoord);
				sampleQueue.add(block[z][y][x]);
				if (t == maxT) {
					bottomZ++;
					break;
				}
			}
			if (topZ < this.parameters.bands) {
				topZ++;
			} else {
				tStart++;
			}
		}
		*/
