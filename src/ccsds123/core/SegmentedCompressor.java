package ccsds123.core;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import com.jypec.util.Pair;
import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;

public class SegmentedCompressor extends Compressor {
	@Override
	public int[][][] decompress(int bands, int lines, int samples, BitInputStream bis) throws IOException {
		throw new IllegalArgumentException("Decompress w/ normal compressor");
	}
	
	
	private class Coordinate {
		int band, line, sample;
		
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
	}
	
	
	Queue<Coordinate> coordQueue;
	

	@Override
	public void doCompress(int[][][] block, int bands, int lines, int samples, BitOutputStream bos) throws IOException {
		coordQueue = new LinkedList<Coordinate>();
		//add all coords to coordQueue following a diagonal pattern
		int maxT = lines*samples-1;
		int topZ = 1;
		int bottomZ = 0;
		int tStart = 0;
		while(topZ != bottomZ) {
			for (int i = topZ-1; i >= bottomZ; i--) {
				int z = i;
				int t = tStart + (topZ - 1) - i;
				int x = t%samples;
				int y = t/samples;
				coordQueue.add(new Coordinate(z, y, x));
				
				if (t == maxT) {
					bottomZ++;
					break;
				}
			}
			if (topZ < bands) {
				topZ++;
			} else {
				tStart++;
			}
		}
		
		//initializations
		int [][] weights = this.getInitialWeights(bands);
		
		//queues
		Queue<Pair<Long, Coordinate>> westQueue = new LinkedList<Pair<Long, Coordinate>>();
		Queue<Pair<Long, Coordinate>> northQueue = new LinkedList<Pair<Long, Coordinate>>();
		Queue<Pair<Long, Coordinate>> northEastQueue = new LinkedList<Pair<Long, Coordinate>>();
		Queue<Pair<Long, Coordinate>> northWestQueue = new LinkedList<Pair<Long, Coordinate>>();
		
		//keep taking things till there are no more
		while (!coordQueue.isEmpty()) {
			Coordinate currCoord = coordQueue.remove();
			
			//TAKE REPRESENTATIVE NEIGHBORHOOD 
			Long westRep, northWestRep, northRep, northEastRep, nextNorthEastRep, nextWestRep;
			Coordinate dummy = new Coordinate(-3, -3, -3);
			Coordinate westCoord = dummy, northWestCoord = dummy, northCoord = dummy, northEastCoord = dummy, nextNorthEastCoord = dummy, nextWestCoord = dummy;
			Pair<Long, Coordinate> qData;
			//west sample
			if (currCoord.firstSample()) {
				if (currCoord.firstLine()) {
					westRep = 0l;
					nextWestRep = 0l;
				} else {
					westRep = 0l;
					qData = westQueue.remove();
					nextWestRep = qData.first(); //use to transform last west sample into northeast
					nextWestCoord = qData.second();
				}
			} else { 
				qData = westQueue.remove();
				westRep = qData.first();
				westCoord = qData.second();
				nextWestRep = 0l;
			}
			//north sample
			if (currCoord.firstLine()) {
				northRep = 0l;
			} else {
				qData = northQueue.remove();
				northRep = qData.first();
				northCoord = qData.second();
			}
			//north east sample
			if (currCoord.firstLine() && !currCoord.lastSample(samples)) {
				northEastRep = 0l;
				nextNorthEastRep = 0l;
			} else if (currCoord.lastSample(samples)){
				northEastRep = 0l;
				if (currCoord.lastLine(lines)) {
					nextNorthEastRep = 0l;
				} else {
					qData = northEastQueue.remove();
					nextNorthEastRep = qData.first();
					nextNorthEastCoord = qData.second();
				}
			} else {
				nextNorthEastRep = 0l;
				qData = northEastQueue.remove();
				northEastRep = qData.first();
				northEastCoord = qData.second();
			}
			//north west sample
			if (currCoord.firstLine() || currCoord.firstSample()) {
				northWestRep = 0l;
			} else {
				qData = northWestQueue.remove();
				northWestRep = qData.first();
				northWestCoord = qData.second();
			}

			
			//CREATE LOCAL SUM
			long localSum;
			switch (this.localSumType) {
				case WIDE_NEIGHBOR_ORIENTED: { //EQ 20
					if (!currCoord.firstLine() && !currCoord.firstSample() && !currCoord.lastSample(samples)) {
						this.checkCoordinates(currCoord, westCoord, northWestCoord, northCoord, northEastCoord);
						localSum = westRep + northRep + northEastRep + northWestRep;
					} else if (currCoord.firstLine() && !currCoord.firstSample()) {
						this.checkCoordinates(currCoord, westCoord, null, null, null);
						localSum = westRep << 2;
					} else if (!currCoord.firstLine() && currCoord.firstSample()) {
						this.checkCoordinates(currCoord, null, null, northCoord, northEastCoord);
						localSum = (northRep + northEastRep) << 1;
					} else if (!currCoord.firstLine() && currCoord.lastSample(samples)) {
						this.checkCoordinates(currCoord, westCoord, northWestCoord, northCoord, null);
						localSum = westRep + northWestRep + (northRep << 1);
					} else if (currCoord.firstT()){
						localSum = 0;
					} else {
						throw new IllegalStateException("Should not get here");
					}
					break;
				}
				case NARROW_NEIGHBOR_ORIENTED: { //EQ 21
					throw new IllegalStateException("Unimplemented!");
					/*if (!currCoord.firstLine() && !currCoord.firstSample() && !currCoord.lastSample(samples)) {
						localSum = northWestRep + (northRep << 1) + northEastRep;
					} else if (currCoord.firstLine() && !currCoord.firstSample() && !currCoord.firstBand()) {
						localSum = westUnderRep;
					} else if (!currCoord.firstLine() && currCoord.firstSample()) {
						localSum = (northRep + northEastRep) << 1;
					} else if (!currCoord.firstLine() && currCoord.lastSample(samples)) {
						localSum = (northWestRep + northRep) << 1;
					} else if (currCoord.firstLine() && !currCoord.firstSample() && currCoord.firstBand()) {
						localSum = ParameterCalc.sMid(this.depth) << 2;
					} else if (currCoord.firstT()) {
						localSum = 0;
					} else {
						throw new IllegalStateException("Should not get here");
					}
					break;*/
				}
				case WIDE_COLUMN_ORIENTED: { //EQ 22
					if (!currCoord.firstLine()) {
						this.checkCoordinates(currCoord, null, null, northCoord, null);
						localSum = northRep << 2;
					} else if (currCoord.firstLine() && !currCoord.firstSample()) {
						this.checkCoordinates(currCoord, westCoord, null, null, null);
						localSum = westRep << 2;
					} else  if (currCoord.firstT()) {
						localSum = 0; 
					} else {						
						throw new IllegalStateException("Should not get here");
					}
					break;
				}
				case NARROW_COLUMN_ORIENTED: { //EQ 23
					throw new IllegalStateException("Unimplemented!");
					/*if (!currCoord.firstLine()) {
						localSum = northRep << 2;
					} else if (currCoord.firstLine() && !currCoord.firstSample() && !currCoord.firstBand()) {
						localSum = westUnderRep << 2;
					} else if (currCoord.firstLine() && !currCoord.firstSample() && currCoord.firstBand()) {
						localSum = ParameterCalc.sMid(this.depth) << 2;
					} else if (currCoord.firstT()) {
						localSum = 0;
					} else {
						throw new IllegalStateException("Should not get here");
					}
					break;*/
				}
			}
			
			
			
			
			
			
			
			
			
			long currRep = -1234566789;
			
			
			//GET THINGS BACK TO THEIR QUEUES
			/* N -> NW
			 * NE -> N
			 * Curr -> W
			 * W -> WU
			 * WU -> NE
			 */			
			//where does my neighborhood go??
			//west, north, northwest, northeast, current
			if (currCoord.firstLine()) {
				if (currCoord.firstSample()) {
					westQueue.add(new Pair<>(currRep, currCoord));
				} else if (!currCoord.lastSample(samples)) {
					northEastQueue.add(new Pair<>(westRep, westCoord));
					westQueue.add(new Pair<>(currRep, currCoord));
				} else {
					northEastQueue.add(new Pair<>(westRep, westCoord));
					northQueue.add(new Pair<>(nextNorthEastRep, nextNorthEastCoord)); //convert first northeast into north
					westQueue.add(new Pair<>(currRep, currCoord));
				}
			} else if (!currCoord.lastLine(lines)) {
				if (currCoord.firstSample()) {
					northEastQueue.add(new Pair<>(nextWestRep, nextWestCoord)); //convert last west into northeast 
					northQueue.add(new Pair<>(northEastRep, northEastCoord));
					northWestQueue.add(new Pair<>(northRep, northCoord));
					westQueue.add(new Pair<>(currRep, currCoord));
				} else if (!currCoord.lastSample(samples)) {
					northEastQueue.add(new Pair<>(westRep, westCoord));
					northQueue.add(new Pair<>(northEastRep, northEastCoord));
					northWestQueue.add(new Pair<>(northRep, northCoord));
					westQueue.add(new Pair<>(currRep, currCoord));
				} else {
					northEastQueue.add(new Pair<>(westRep, westCoord));
					northQueue.add(new Pair<>(nextNorthEastRep, nextNorthEastCoord)); //convert first northeast into north
					westQueue.add(new Pair<>(currRep, currCoord));
				}
			} else {
				if (currCoord.firstSample()) {
					northEastQueue.add(new Pair<>(nextWestRep, nextWestCoord)); //convert last west into northeast 
					northQueue.add(new Pair<>(northEastRep, northEastCoord));
					northWestQueue.add(new Pair<>(northRep, northCoord));
					westQueue.add(new Pair<>(currRep, currCoord));
				} else if (!currCoord.lastSample(samples)) {
					northQueue.add(new Pair<>(northEastRep, northEastCoord));
					northWestQueue.add(new Pair<>(northRep, northCoord));
					westQueue.add(new Pair<>(currRep, currCoord));
				} else {
					
				}
			}
		}
		
		//compress and all the other stuff from the residuals
		System.out.println("State of queues \n\t W: " + westQueue.size() +
											"\n\tNE: " + northEastQueue.size() +
											"\n\t N: " + northQueue.size() +  
											"\n\tNW: " + northWestQueue.size());
		
		throw new IllegalStateException("NOT IMPL");
	}
	
	
	private void checkCoordinates(Coordinate current, Coordinate west, Coordinate northWest, Coordinate north, Coordinate northEast) {
		if (west != null)
			if (current.band != west.band || current.line != west.line || current.sample-1 != west.sample) 
				throw new IllegalStateException("FAIL @ west: (" + current + " -> " + west + ")");
		if (northWest != null)
			if (current.band != northWest.band || current.line-1 != northWest.line || current.sample-1 != northWest.sample) 
				throw new IllegalStateException("FAIL @ northWest: (" + current + " -> " + northWest + ")");
		if (north != null)
			if (current.band != north.band || current.line-1 != north.line || current.sample != north.sample) 
				throw new IllegalStateException("FAIL @ north: (" + current + " -> " + north + ")");
		if (northEast != null)
			if (current.band != northEast.band || current.line-1 != northEast.line || current.sample+1 != northEast.sample) 
				throw new IllegalStateException("FAIL @ northEast: (" + current + " -> " + northEast + ")");
	}

}
