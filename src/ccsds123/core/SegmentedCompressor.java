package ccsds123.core;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.jypec.util.Pair;
import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;

public class SegmentedCompressor extends Compressor {
	@Override
	public int[][][] decompress(int bands, int lines, int samples, BitInputStream bis) throws IOException {
		DirectCompressor c = new DirectCompressor();
		return c.decompress(bands, lines, samples, bis);
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

		public int getT(int samples) {
			return this.line * samples + this.sample;
		}
	}
	
	
	Queue<Coordinate> coordQueue;
	Queue<Integer> sampleQueue;
	

	@Override
	public void doCompress(int[][][] block, int bands, int lines, int samples, BitOutputStream bos) throws IOException {
		coordQueue = new LinkedList<Coordinate>();
		sampleQueue = new LinkedList<Integer>();
		SampleAdaptiveEntropyCoder entropyCoder = new SampleAdaptiveEntropyCoder(this.uMax, this.depth, bands, this.gammaZero, this.gammaStar, this.accumulatorInitializationConstant);
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
				sampleQueue.add(block[z][y][x]);
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
		Queue<Pair<Long, Coordinate>> 	westQueue 		= new LinkedList<Pair<Long, Coordinate>>();
		Queue<Pair<Long, Coordinate>> 	northQueue 		= new LinkedList<Pair<Long, Coordinate>>();
		Queue<Pair<Long, Coordinate>> 	northEastQueue 	= new LinkedList<Pair<Long, Coordinate>>();
		Queue<Pair<Long, Coordinate>> 	northWestQueue 	= new LinkedList<Pair<Long, Coordinate>>();
		Queue<Pair<long[], Coordinate>>	diffQueue 		= new LinkedList<Pair<long[], Coordinate>>();
		Queue<Integer> firstPixelQueueDRPSV = new LinkedList<Integer>();
		
		int[][][] sarr = new int[bands][lines][samples];
		long[][][] drpsvarr = new long[bands][lines][samples];
		long[][][] psvarr = new long[bands][lines][samples];
		long[][][] prarr = new long[bands][lines][samples];
		Queue<?>[][][] warr = new Queue[bands][lines][samples];
		long[][][] wusearr = new long[bands][lines][samples];
		long[][][] dprearr = new long[bands][lines][samples];
		long[][][] drsrarr = new long[bands][lines][samples];
		long[][][] cqbcarr = new long[bands][lines][samples];
		long[][][] mevarr = new long[bands][lines][samples];
		long[][][] hrpsvarr = new long[bands][lines][samples];
		long[][][] pcdarr = new long[bands][lines][samples];
		long[][][] cldarr = new long[bands][lines][samples];
		long[][][] nwdarr = new long[bands][lines][samples];
		long[][][] wdarr = new long[bands][lines][samples];
		long[][][] ndarr = new long[bands][lines][samples];
		long[][][] lsarr = new long[bands][lines][samples];
		long[][][] qiarr = new long[bands][lines][samples];
		long[][][] srarr = new long[bands][lines][samples];
		long[][][] tarr = new long[bands][lines][samples];
		long[][][] mqiarr = new long[bands][lines][samples];


		//keep taking things till there are no more
		while (!coordQueue.isEmpty()) {
			Coordinate currCoord = coordQueue.remove();
			int currSample = sampleQueue.remove();
			
			////NEIGHBORHOOD BEGIN 4.1
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
			////NEIGHBORHOOD END 4.1

			
			////LOCAL SUM BEGIN 4.4
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
				default: {
					throw new IllegalStateException("Unimplemented!");
				}
			}
			////LOCAL SUM END
			
			
			////LOCAL DIFF BEGIN 4.5
			long northDiff, westDiff, northWestDiff;
			if (!currCoord.firstLine() && !currCoord.firstSample()) {
				northDiff = (northRep << 2) - localSum;
				westDiff = (westRep << 2) - localSum;
				northWestDiff = (northWestRep << 2) - localSum;
			} else if (currCoord.firstLine() && !currCoord.firstSample()) {
				northDiff = (northRep << 2) - localSum;
				westDiff = (northRep << 2) - localSum;
				northWestDiff = (northRep << 2) - localSum;
			} else {
				northDiff = 0;
				westDiff = 0;
				northWestDiff = 0;
			}
			////LOCAL DIFF END
			
			
			////PREDICTED CENTRAL LOCAL DIFFERENCE 4.7.1
			//TAKE DIFFERENCES
			long [] diffs = new long[this.predictionBands];
			if (!currCoord.firstBand() && !currCoord.firstT()) {
				Pair<long[], Coordinate> pp = diffQueue.remove();
				diffs = pp.first();
				Coordinate diffCoord = pp.second();
				//check coordinate is below us
				if (diffCoord.band != currCoord.band - 1 || diffCoord.sample != currCoord.sample || diffCoord.line != currCoord.line) {
					throw new IllegalStateException("WRONG");
				}
			}
			//TAKE WEIGTHS
			int[] localWeights = weights[currCoord.band];
			//CALCULATE PCD
			long predictedCentralDiff = 0;
			int windex = 0;
			if (this.fullPredictionMode) {
				predictedCentralDiff += localWeights[0] * northDiff;
				predictedCentralDiff += localWeights[1] * westDiff;
				predictedCentralDiff += localWeights[2] * northWestDiff;
				windex = 3;
			}
			for (int p = 0; p < this.predictionBands; p++) {
				if (currCoord.band - p > 0) 
					predictedCentralDiff += localWeights[p+windex] * diffs[p];
			}
			////PREDICTED CENTRAL LOCAL DIFFERENCE END 4.7.1
			

			//HR PREDICTED SAMPLE VALUE 4.7.2
			long highResolutionPredSampleValue = this.calcHighResolutionPredSampleValue(predictedCentralDiff, localSum);
			//DR PREDICTED SAMPLE VALUE 4.7.3
			long doubleResolutionPredSampleValue = 0;
			if (!currCoord.firstSample() || !currCoord.firstLine()) {
				doubleResolutionPredSampleValue = highResolutionPredSampleValue >> (this.omega + 1);
			} else if (this.predictionBands == 0 || currCoord.firstBand()) {
				doubleResolutionPredSampleValue = ParameterCalc.sMid(this.depth) << 1;
			} else {
				doubleResolutionPredSampleValue = firstPixelQueueDRPSV.remove() << 1;
			}
			//PREDICTED SAMMPLE VALUE 4.7.4
			long predictedSampleValue = this.calcPredictedSampleValue(doubleResolutionPredSampleValue);
			
			
			//PRED RES 4.8.1 + 4.8.2.1
			long predictionResidual = this.calcPredictionResidual(currSample, predictedSampleValue);
			long maxErrVal = this.calcMaxErrVal(currCoord.band, predictedSampleValue);
			long quantizerIndex = this.calcQuantizerIndex(predictionResidual, maxErrVal, currCoord.getT(samples));
			
			//DR SAMPLE REPRESENTATIVE AND SAMPLE REPRESENTATIVE 4.9
			long clippedQuantizerBinCenter = this.calcClipQuantizerBinCenter(predictedSampleValue, quantizerIndex, maxErrVal);
			long doubleResolutionSampleRepresentative = this.calcDoubleResolutionSampleRepresentative(currCoord.band, clippedQuantizerBinCenter, quantizerIndex, maxErrVal, highResolutionPredSampleValue);
			
			//DR PRED ERR 4.10.1
			long doubleResolutionPredictionError = this.calcDoubleResolutionPredictionError(clippedQuantizerBinCenter, doubleResolutionPredSampleValue);
			//WEIGHT UPDATE SCALING EXPONENT 4.10.2
			long weightUpdateScalingExponent = this.calcWeightUpdateScalingExponent(currCoord.getT(samples), samples);
			
			
			//WEIGHT UPDATE 4.10.3
			LinkedList<Integer> cwl = new LinkedList<Integer>();
			if (currCoord.getT(samples) > 0) {
				windex = 0;
				if (this.fullPredictionMode) {
					int weightExponentOffset = this.getIntraBandWeightExponentOffset(currCoord.band);
					//north, west, northwest
					localWeights[0] = this.updateWeight(localWeights[0], doubleResolutionPredictionError, northDiff, weightUpdateScalingExponent, weightExponentOffset, currCoord.getT(samples));
					localWeights[1] = this.updateWeight(localWeights[1], doubleResolutionPredictionError, westDiff, weightUpdateScalingExponent, weightExponentOffset, currCoord.getT(samples));
					localWeights[2] = this.updateWeight(localWeights[2], doubleResolutionPredictionError, northWestDiff, weightUpdateScalingExponent, weightExponentOffset, currCoord.getT(samples));
					cwl.add(localWeights[0]);
					cwl.add(localWeights[1]);
					cwl.add(localWeights[2]);
					windex = 3;
				}
				for (int p = 0; p < this.predictionBands; p++) {
					if (currCoord.band - p > 0) { 
						localWeights[windex+p] = this.updateWeight(localWeights[windex+p], doubleResolutionPredictionError, diffs[p], weightUpdateScalingExponent, getInterBandWeightExponentOffsets(currCoord.band, p), currCoord.getT(samples));
						cwl.add(localWeights[windex+p]);
					}
				}
			}
			
			//MAPPED QUANTIZER INDEX 4.11
			long theta = this.calcTheta(currCoord.getT(samples), predictedSampleValue, maxErrVal);
			long mappedQuantizerIndex = this.calcMappedQuantizerIndex(quantizerIndex, theta, doubleResolutionPredSampleValue);
			
			//CALCULATE SAMPLE REPRESENTATIVE AND NEXT DIFFERENCE
			long currRep, currDif;
			if (currCoord.getT(samples) == 0) {
				currRep = this.calcSampleRepresentative(0, 0, 0, currSample);
				currDif = 0;
			} else {
				currRep = this.calcSampleRepresentative(currCoord.line, currCoord.sample, doubleResolutionSampleRepresentative, currSample);
				currDif = this.calcCentralLocalDiff(currCoord.line, currCoord.sample, currRep, localSum);
			}
			
			
			
			
			//GET THINGS BACK TO THEIR QUEUES
			/* Curr -> W
			 * W -> NE
			 * NE -> N
			 * N -> NW
			 */			
			//where does my neighborhood go??
			//west, north, northwest, northeast, current
			if (!currCoord.lastLine(lines) || !currCoord.lastSample(samples)) {
				westQueue.add(new Pair<>(currRep, currCoord));
			} 
			
			if (!currCoord.lastLine(lines) && !currCoord.firstSample()) {
				northEastQueue.add(new Pair<>(westRep, westCoord));
			} else if (!currCoord.firstLine() && currCoord.firstSample()) {
				northEastQueue.add(new Pair<>(nextWestRep, nextWestCoord)); //convert last west into northeast
			}
			
			if (!currCoord.lastLine(lines) && currCoord.lastSample(samples)) {
				northQueue.add(new Pair<>(nextNorthEastRep, nextNorthEastCoord)); //convert first northeast into north
			} else if (!currCoord.firstLine() && !currCoord.lastSample(samples)) {
				northQueue.add(new Pair<>(northEastRep, northEastCoord));
			}
			
			if (!currCoord.firstLine() && !currCoord.lastSample(samples)) {
				northWestQueue.add(new Pair<>(northRep, northCoord));
			}
			
			
			if (!currCoord.lastBand(bands) && !currCoord.firstT()) {
				for (int i = diffs.length-1; i > 0; i--) {
					diffs[i] = diffs[i-1];
				}
				diffs[0] = currDif;
				
				diffQueue.add(new Pair<long[], Coordinate>(diffs, currCoord));
			}
			
			if (currCoord.firstLine() && currCoord.firstSample() && !currCoord.lastBand(bands) && this.predictionBands > 0) {
				firstPixelQueueDRPSV.add(currSample);
			}
			
			
			sarr[currCoord.band][currCoord.line][currCoord.sample] = currSample;
			drpsvarr[currCoord.band][currCoord.line][currCoord.sample] = doubleResolutionPredSampleValue;
			psvarr[currCoord.band][currCoord.line][currCoord.sample] = predictedSampleValue;
			prarr[currCoord.band][currCoord.line][currCoord.sample] = predictionResidual;
			dprearr[currCoord.band][currCoord.line][currCoord.sample] = doubleResolutionPredictionError;
			drsrarr[currCoord.band][currCoord.line][currCoord.sample] = doubleResolutionSampleRepresentative;
			cqbcarr[currCoord.band][currCoord.line][currCoord.sample] = clippedQuantizerBinCenter;
			mevarr[currCoord.band][currCoord.line][currCoord.sample] = maxErrVal;
			hrpsvarr[currCoord.band][currCoord.line][currCoord.sample] = highResolutionPredSampleValue;
			pcdarr[currCoord.band][currCoord.line][currCoord.sample] = predictedCentralDiff;
			nwdarr[currCoord.band][currCoord.line][currCoord.sample] = northWestDiff;
			wdarr[currCoord.band][currCoord.line][currCoord.sample] = westDiff;
			ndarr[currCoord.band][currCoord.line][currCoord.sample] = northDiff;
			lsarr[currCoord.band][currCoord.line][currCoord.sample] = localSum;
			qiarr[currCoord.band][currCoord.line][currCoord.sample] = quantizerIndex;
			srarr[currCoord.band][currCoord.line][currCoord.sample] = currRep;
			tarr[currCoord.band][currCoord.line][currCoord.sample] = theta;
			cldarr[currCoord.band][currCoord.line][currCoord.sample] = currDif;
			wusearr[currCoord.band][currCoord.line][currCoord.sample] = weightUpdateScalingExponent;
			warr[currCoord.band][currCoord.line][currCoord.sample] = cwl;
			//SEND TO ENCODER
			mqiarr[currCoord.band][currCoord.line][currCoord.sample] = mappedQuantizerIndex;
		}
		
		//compress and all the other stuff from the residuals
		System.out.println("State of queues \n\t W: " + westQueue.size() +
											"\n\tNE: " + northEastQueue.size() +
											"\n\t N: " + northQueue.size() +  
											"\n\tNW: " + northWestQueue.size() +
											"\n\tDF: " + diffQueue.size() +
											"\n\tSQ: " + sampleQueue.size() + 
											"\n\tCQ: " + coordQueue.size()+
											"\n\tFP: " + firstPixelQueueDRPSV.size());
		
		for (int i = 0; i < lines; i++) {
			for (int j = 0; j < samples; j++) {
				for (int k = 0; k < bands; k++) {
					entropyCoder.code((int) mqiarr[k][i][j], i*samples+j, k, bos);
					
					if (i > 0 || j > 0) {
						cldsmpl.sample(cldarr[k][i][j]);
						cqbcsmpl.sample(cqbcarr[k][i][j]);
						drpesmpl.sample(dprearr[k][i][j]);
						drsrsmpl.sample(drsrarr[k][i][j]);
						hrpsvsmpl.sample(hrpsvarr[k][i][j]);
						lssmpl.sample(lsarr[k][i][j]);
						ndsmpl.sample(ndarr[k][i][j]);
						nwdsmpl.sample(nwdarr[k][i][j]);
						pcdsmpl.sample(pcdarr[k][i][j]);
						wdsmpl.sample(wdarr[k][i][j]);
						wusesmpl.sample(wusearr[k][i][j]);
					}
					
					drpsvsmpl.sample(drpsvarr[k][i][j]);
					mevsmpl.sample(mevarr[k][i][j]);
					mqismpl.sample(mqiarr[k][i][j]);
					prsmpl.sample(prarr[k][i][j]);
					psvsmpl.sample(psvarr[k][i][j]);
					qismpl.sample(qiarr[k][i][j]);
					ssmpl.sample(sarr[k][i][j]);
					srsmpl.sample(srarr[k][i][j]);
					tsmpl.sample(tarr[k][i][j]);
					Queue<Integer> cwl = (Queue<Integer>) warr[k][i][j];
					while (!cwl.isEmpty())
						wsmpl.sample(cwl.remove());					

				}
			}
		}
		
		
		
		
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




/*

if (currCoord.firstLine()) {
if (currCoord.firstSample()) {
	//westQueue.add(new Pair<>(currRep, currCoord));
} else if (!currCoord.lastSample(samples)) {
	//northEastQueue.add(new Pair<>(westRep, westCoord));
	//westQueue.add(new Pair<>(currRep, currCoord));
} else {
	//northEastQueue.add(new Pair<>(westRep, westCoord));
	//northQueue.add(new Pair<>(nextNorthEastRep, nextNorthEastCoord)); //convert first northeast into north
	//westQueue.add(new Pair<>(currRep, currCoord));
}
} else if (!currCoord.lastLine(lines)) {
if (currCoord.firstSample()) {
	//northEastQueue.add(new Pair<>(nextWestRep, nextWestCoord)); //convert last west into northeast 
	//northQueue.add(new Pair<>(northEastRep, northEastCoord));
	//northWestQueue.add(new Pair<>(northRep, northCoord));
	//westQueue.add(new Pair<>(currRep, currCoord));
} else if (!currCoord.lastSample(samples)) {
	//northEastQueue.add(new Pair<>(westRep, westCoord));
	//northQueue.add(new Pair<>(northEastRep, northEastCoord));
	//northWestQueue.add(new Pair<>(northRep, northCoord));
	//westQueue.add(new Pair<>(currRep, currCoord));
} else {
	//northEastQueue.add(new Pair<>(westRep, westCoord));
	//northQueue.add(new Pair<>(nextNorthEastRep, nextNorthEastCoord)); //convert first northeast into north
	//westQueue.add(new Pair<>(currRep, currCoord));
}
} else {
if (currCoord.firstSample()) {
	//northEastQueue.add(new Pair<>(nextWestRep, nextWestCoord)); //convert last west into northeast 
	//northQueue.add(new Pair<>(northEastRep, northEastCoord));
	//northWestQueue.add(new Pair<>(northRep, northCoord));
	//westQueue.add(new Pair<>(currRep, currCoord));
} else if (!currCoord.lastSample(samples)) {
	//northQueue.add(new Pair<>(northEastRep, northEastCoord));
	//northWestQueue.add(new Pair<>(northRep, northCoord));
	//westQueue.add(new Pair<>(currRep, currCoord));
} else {
	
}
}

*/