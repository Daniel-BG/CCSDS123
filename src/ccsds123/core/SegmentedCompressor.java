package ccsds123.core;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import com.jypec.util.Pair;
import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;

public class SegmentedCompressor extends Compressor {
	
	public SegmentedCompressor(EntropyCoder ec, CompressorParameters cp, SamplingUnit su) {
		super(ec, cp, su);
	}


	@Override
	public int[][][] decompress(BitInputStream bis) throws IOException {
		DirectCompressor c = new DirectCompressor(this.entropyCoder, this.parameters, this.su);
		return c.decompress(bis);
	}
	
	

	
	
	Queue<Coordinate> coordQueue;
	Queue<Integer> sampleQueue;
	

	@Override
	public void doCompress(int[][][] block, BitOutputStream bos) throws IOException {
		coordQueue = new LinkedList<Coordinate>();
		sampleQueue = new LinkedList<Integer>();
		this.entropyCoder.reset();
		//add all coords to coordQueue following a diagonal pattern
		Iterator<Coordinate> coordIter = Coordinate.getDiagonalIterator(this.parameters.bands, this.parameters.lines, this.parameters.samples);
		while (coordIter.hasNext()) {
			Coordinate currCoord = coordIter.next();
			coordQueue.add(currCoord);
			sampleQueue.add(block[currCoord.band][currCoord.line][currCoord.sample]);
		}
		//initializations
		
		
		//queues
		Queue<Pair<Long, Coordinate>> 	westQueue 		= new LinkedList<Pair<Long, Coordinate>>();
		Queue<Pair<Long, Coordinate>> 	northQueue 		= new LinkedList<Pair<Long, Coordinate>>();
		Queue<Pair<Long, Coordinate>> 	northEastQueue 	= new LinkedList<Pair<Long, Coordinate>>();
		Queue<Pair<Long, Coordinate>> 	northWestQueue 	= new LinkedList<Pair<Long, Coordinate>>();
		Queue<Pair<long[], Coordinate>>	diffQueue 		= new LinkedList<Pair<long[], Coordinate>>();
		Queue<Pair<int[], Coordinate>>	initialWeightQueue 	= new LinkedList<Pair<int[], Coordinate>>();
		Queue<Pair<int[], Coordinate>>	weightQueue 	= new LinkedList<Pair<int[], Coordinate>>();
		Queue<Integer> firstPixelQueueDRPSV = new LinkedList<Integer>();
		
		int[][][] sarr 		= new int[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] drpsvarr = new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] psvarr 	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] prarr 	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		Queue<?>[][][] warr = new Queue[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] wusearr 	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] dprearr 	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] drsrarr 	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] cqbcarr 	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] mevarr 	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] hrpsvarr = new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] pcdarr	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] cldarr	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] nwdarr	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] wdarr	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] ndarr 	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] lsarr	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] qiarr	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] srarr 	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] tarr 	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] mqiarr 	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] wrarr	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] nwrarr	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] nerarr	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		long[][][] nrarr	= new long[this.parameters.bands][this.parameters.lines][this.parameters.samples];
		
		
		int [][] initialWeights = this.parameters.getInitialWeights();
		for (int i = 0; i < initialWeights.length; i++) {
			initialWeightQueue.add(new Pair<int[], Coordinate>(initialWeights[i], new Coordinate(i, 0, 0)));
		}


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
			if (currCoord.firstLine() && !currCoord.lastSample(this.parameters.samples)) {
				northEastRep = 0l;
				nextNorthEastRep = 0l;
			} else if (currCoord.lastSample(this.parameters.samples)){
				northEastRep = 0l;
				if (currCoord.lastLine(this.parameters.lines)) {
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
			switch (this.parameters.localSumType) {
				case WIDE_NEIGHBOR_ORIENTED: { //EQ 20
					if (!currCoord.firstLine() && !currCoord.firstSample() && !currCoord.lastSample(this.parameters.samples)) {
						this.checkCoordinates(currCoord, westCoord, northWestCoord, northCoord, northEastCoord);
						localSum = westRep + northRep + northEastRep + northWestRep;
					} else if (currCoord.firstLine() && !currCoord.firstSample()) {
						this.checkCoordinates(currCoord, westCoord, null, null, null);
						localSum = westRep << 2;
					} else if (!currCoord.firstLine() && currCoord.firstSample()) {
						this.checkCoordinates(currCoord, null, null, northCoord, northEastCoord);
						localSum = (northRep + northEastRep) << 1;
					} else if (!currCoord.firstLine() && currCoord.lastSample(this.parameters.samples)) {
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
			if (!currCoord.firstSample() && !currCoord.firstLine()) {
				northDiff = (northRep << 2) - localSum;
				westDiff = (westRep << 2) - localSum;
				northWestDiff = (northWestRep << 2) - localSum;
			} else if (currCoord.firstSample() && !currCoord.firstLine()) {
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
			long [] diffs = new long[this.parameters.predictionBands];
			if (!currCoord.firstBand()) {
				Pair<long[], Coordinate> pp = diffQueue.remove();
				diffs = pp.first();
				Coordinate diffCoord = pp.second();
				//check coordinate is below us
				if (diffCoord.band != currCoord.band - 1 || diffCoord.sample != currCoord.sample || diffCoord.line != currCoord.line) {
					throw new IllegalStateException("WRONG");
				}
			}
			//TAKE WEIGTHS
			int[] localWeights;
			Pair<int[], Coordinate> wcp;
			if (currCoord.firstT()) {
				wcp = initialWeightQueue.remove();
			} else {
				wcp = weightQueue.remove();
			}
			//check weight coordinate
			Coordinate weightCoord = wcp.second();
			if (weightCoord.band != currCoord.band) {
				throw new IllegalStateException("WRONG");
			}
			localWeights = wcp.first();
			
			//CALCULATE PCD
			int windex = 0;
			long predictedCentralDiff = 0;
			//if (!currCoord.firstBand() || this.fullPredictionMode) {
			if (this.parameters.fullPredictionMode) {
				predictedCentralDiff += localWeights[0] * northDiff;
				predictedCentralDiff += localWeights[1] * westDiff;
				predictedCentralDiff += localWeights[2] * northWestDiff;
				windex = 3;
			}
			for (int p = 0; p < this.parameters.predictionBands; p++) {
				//if (currCoord.band - p > 0) 
				predictedCentralDiff += localWeights[p+windex] * diffs[p];
			}
			//}
			////PREDICTED CENTRAL LOCAL DIFFERENCE END 4.7.1
			

			//HR PREDICTED SAMPLE VALUE 4.7.2
			long highResolutionPredSampleValue = this.calcHighResolutionPredSampleValue(predictedCentralDiff, localSum);
			//DR PREDICTED SAMPLE VALUE 4.7.3
			long doubleResolutionPredSampleValue = 0;
			if (!currCoord.firstSample() || !currCoord.firstLine()) {
				doubleResolutionPredSampleValue = highResolutionPredSampleValue >> (this.parameters.omega + 1);
			} else if (this.parameters.predictionBands == 0 || currCoord.firstBand()) {
				doubleResolutionPredSampleValue = ParameterCalc.sMid(this.parameters.depth) << 1;
			} else {
				doubleResolutionPredSampleValue = firstPixelQueueDRPSV.remove() << 1;
			}
			//PREDICTED SAMMPLE VALUE 4.7.4
			long predictedSampleValue = this.calcPredictedSampleValue(doubleResolutionPredSampleValue);
			
			
			//PRED RES 4.8.1 + 4.8.2.1
			long predictionResidual = this.calcPredictionResidual(currSample, predictedSampleValue);
			long maxErrVal = this.calcMaxErrVal(currCoord.band, predictedSampleValue, currCoord.getT(this.parameters.samples));
			long quantizerIndex = this.calcQuantizerIndex(predictionResidual, maxErrVal, currCoord.getT(this.parameters.samples));
			
			//DR SAMPLE REPRESENTATIVE AND SAMPLE REPRESENTATIVE 4.9
			long clippedQuantizerBinCenter = this.calcClipQuantizerBinCenter(predictedSampleValue, quantizerIndex, maxErrVal);
			long doubleResolutionSampleRepresentative = this.calcDoubleResolutionSampleRepresentative(currCoord.band, clippedQuantizerBinCenter, quantizerIndex, maxErrVal, highResolutionPredSampleValue);
			
			//DR PRED ERR 4.10.1
			long doubleResolutionPredictionError = this.calcDoubleResolutionPredictionError(clippedQuantizerBinCenter, doubleResolutionPredSampleValue);
			//WEIGHT UPDATE SCALING EXPONENT 4.10.2
			long weightUpdateScalingExponent = this.calcWeightUpdateScalingExponent(currCoord.getT(this.parameters.samples), this.parameters.samples);
			
			
			//WEIGHT UPDATE 4.10.3
			LinkedList<Integer> cwl = new LinkedList<Integer>();
			if (currCoord.getT(this.parameters.samples) > 0) {
				windex = 0;
				if (this.parameters.fullPredictionMode) {
					int weightExponentOffset = this.parameters.getIntraBandWeightExponentOffset(currCoord.band);
					//north, west, northwest
					localWeights[0] = this.updateWeight(localWeights[0], doubleResolutionPredictionError, northDiff, weightUpdateScalingExponent, weightExponentOffset, currCoord.getT(this.parameters.samples));
					localWeights[1] = this.updateWeight(localWeights[1], doubleResolutionPredictionError, westDiff, weightUpdateScalingExponent, weightExponentOffset, currCoord.getT(this.parameters.samples));
					localWeights[2] = this.updateWeight(localWeights[2], doubleResolutionPredictionError, northWestDiff, weightUpdateScalingExponent, weightExponentOffset, currCoord.getT(this.parameters.samples));
					cwl.add(localWeights[0]);
					cwl.add(localWeights[1]);
					cwl.add(localWeights[2]);
					windex = 3;
				}
				for (int p = 0; p < this.parameters.predictionBands; p++) {
					localWeights[windex+p] = this.updateWeight(localWeights[windex+p], doubleResolutionPredictionError, diffs[p], weightUpdateScalingExponent, this.parameters.getInterBandWeightExponentOffsets(currCoord.band, p), currCoord.getT(this.parameters.samples));
					if (currCoord.band - p > 0) { 
						//add only the ones we need to be checking, the others are cancelled by differences being zero
						cwl.add(localWeights[windex+p]);
					}
				}
			}
			//save weights if needed
			if (!currCoord.lastT(this.parameters.samples, this.parameters.lines)) {
				weightQueue.add(wcp);
			}
			
			//MAPPED QUANTIZER INDEX 4.11
			long theta = this.calcTheta(currCoord.getT(this.parameters.samples), predictedSampleValue, maxErrVal);
			long mappedQuantizerIndex = this.calcMappedQuantizerIndex(quantizerIndex, theta, doubleResolutionPredSampleValue);
			
			//CALCULATE SAMPLE REPRESENTATIVE AND NEXT DIFFERENCE
			long currRep = this.calcSampleRepresentative(currCoord.line, currCoord.sample, doubleResolutionSampleRepresentative, currSample);
			long currDif = this.calcCentralLocalDiff(currCoord.line, currCoord.sample, currRep, localSum);
			
			
			
			
			
			//GET THINGS BACK TO THEIR QUEUES
			/* Curr -> W
			 * W -> NE
			 * NE -> N
			 * N -> NW
			 */			
			//where does my neighborhood go??
			//west, north, northwest, northeast, current
			if (!currCoord.lastLine(this.parameters.lines) || !currCoord.lastSample(this.parameters.samples)) {
				westQueue.add(new Pair<>(currRep, currCoord));
			} 
			
			if (!currCoord.lastLine(this.parameters.lines) && !currCoord.firstSample()) {
				northEastQueue.add(new Pair<>(westRep, westCoord));
			} else if (!currCoord.firstLine() && currCoord.firstSample()) {
				northEastQueue.add(new Pair<>(nextWestRep, nextWestCoord)); //convert last west into northeast
			}
			
			if (!currCoord.lastLine(this.parameters.lines) && currCoord.lastSample(this.parameters.samples)) {
				northQueue.add(new Pair<>(nextNorthEastRep, nextNorthEastCoord)); //convert first northeast into north
			} else if (!currCoord.firstLine() && !currCoord.lastSample(this.parameters.samples)) {
				northQueue.add(new Pair<>(northEastRep, northEastCoord));
			}
			
			if (!currCoord.firstLine() && !currCoord.lastSample(this.parameters.samples)) {
				northWestQueue.add(new Pair<>(northRep, northCoord));
			}
			
			
			if (!currCoord.lastBand(this.parameters.bands)) {
				for (int i = diffs.length-1; i > 0; i--) {
					diffs[i] = diffs[i-1];
				}
				diffs[0] = currDif;
				
				diffQueue.add(new Pair<long[], Coordinate>(diffs, currCoord));
			}
			
			if (currCoord.firstLine() && currCoord.firstSample() && !currCoord.lastBand(this.parameters.bands) && this.parameters.predictionBands > 0) {
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
			wrarr[currCoord.band][currCoord.line][currCoord.sample] = westRep;
			nwrarr[currCoord.band][currCoord.line][currCoord.sample] = northWestRep;
			nerarr[currCoord.band][currCoord.line][currCoord.sample] = northEastRep;
			nrarr[currCoord.band][currCoord.line][currCoord.sample] = northRep;
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

		
		for (int i = 0; i < this.parameters.lines; i++) {
			for (int j = 0; j < this.parameters.samples; j++) {
				for (int k = 0; k < this.parameters.bands; k++) {
					this.entropyCoder.code((int) mqiarr[k][i][j], i*this.parameters.samples+j, k, bos);

					su.wrsmpl.sample(wrarr[k][i][j]);
					su.nrsmpl.sample(nrarr[k][i][j]);
					su.nwrsmpl.sample(nwrarr[k][i][j]);
					su.nersmpl.sample(nerarr[k][i][j]);
					
					
					su.cldsmpl.sample(cldarr[k][i][j]);
					su.cqbcsmpl.sample(cqbcarr[k][i][j]);
					su.drpesmpl.sample(dprearr[k][i][j]);
					su.drsrsmpl.sample(drsrarr[k][i][j]);
					su.hrpsvsmpl.sample(hrpsvarr[k][i][j]);
					su.lssmpl.sample(lsarr[k][i][j]);
					su.ndsmpl.sample(ndarr[k][i][j]);
					su.nwdsmpl.sample(nwdarr[k][i][j]);
					su.pcdsmpl.sample(pcdarr[k][i][j]);
					su.wdsmpl.sample(wdarr[k][i][j]);
					su.wusesmpl.sample(wusearr[k][i][j]);
					
					
					su.drpsvsmpl.sample(drpsvarr[k][i][j]);
					su.mevsmpl.sample(mevarr[k][i][j]);
					su.mqismpl.sample(mqiarr[k][i][j]);
					su.prsmpl.sample(prarr[k][i][j]);
					su.psvsmpl.sample(psvarr[k][i][j]);
					su.qismpl.sample(qiarr[k][i][j]);
					su.ssmpl.sample(sarr[k][i][j]);
					su.srsmpl.sample(srarr[k][i][j]);
					su.tsmpl.sample(tarr[k][i][j]);
					@SuppressWarnings("unchecked")
					Queue<Integer> cwl = (Queue<Integer>) warr[k][i][j];
					while (!cwl.isEmpty())
						su.wsmpl.sample(cwl.remove());					

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