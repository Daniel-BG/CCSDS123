package ccsds123.core;

import java.io.IOException;

import com.jypec.util.bits.BitInputStream;
import com.jypec.util.bits.BitOutputStream;

import ccsds123.util.Sampler;
import ccsds123.util.Utils;

/**
 * Image compression is always done in BIP traversal mode. Increase band, then sample, then line.
 * Input values are assumed possitive
 * @author Daniel
 *
 */
public class DirectCompressor extends Compressor {
	private static final boolean SHOW_PROGRESS = false;
	
	Sampler<Integer> ssmpl 			= new Sampler<Integer>("c_s");
	Sampler<Long> drpsvsmpl 		= new Sampler<Long>("c_drpsv");
	Sampler<Long> psvsmpl	 		= new Sampler<Long>("c_psv");
	Sampler<Long> prsmpl	 		= new Sampler<Long>("c_pr");
	Sampler<Integer> wsmpl			= new Sampler<Integer>("c_w");
	Sampler<Long> wusesmpl			= new Sampler<Long>("c_wuse");
	Sampler<Long> drpesmpl			= new Sampler<Long>("c_drpe");
	Sampler<Long> drsrsmpl			= new Sampler<Long>("c_drsr");
	Sampler<Long> cqbcsmpl			= new Sampler<Long>("c_cqbc");
	Sampler<Long> mevsmpl			= new Sampler<Long>("c_mev");
	Sampler<Long> hrpsvsmpl			= new Sampler<Long>("c_hrpsv");
	Sampler<Long> pcdsmpl			= new Sampler<Long>("c_pcd");
	Sampler<Long> cldsmpl			= new Sampler<Long>("c_cld");
	Sampler<Long> nwdsmpl			= new Sampler<Long>("c_nwd");
	Sampler<Long> wdsmpl			= new Sampler<Long>("c_wd");
	Sampler<Long> ndsmpl			= new Sampler<Long>("c_nd");
	Sampler<Long> lssmpl			= new Sampler<Long>("c_ls");
	Sampler<Long> qismpl			= new Sampler<Long>("c_qi");
	Sampler<Long> srsmpl			= new Sampler<Long>("c_sr");
	Sampler<Long> tsmpl				= new Sampler<Long>("c_ts");
	Sampler<Long> mqismpl			= new Sampler<Long>("c_mqi");
	
	/** DEBUG STUFF */
	private static final String sampleBaseDir = "C:/Users/Daniel/Basurero/out/";
	private static final String sampleExt = ".smp";	
	
	private SampleAdaptiveEntropyCoder entropyCoder;
	
	public DirectCompressor() {
		super();
		
		Sampler.setSamplePath(sampleBaseDir);
		Sampler.setSampleExt(sampleExt);
	}
	

	/**
	 * 
	 * @param block block[band][line][sample]
	 * @param bands
	 * @param lines
	 * @param samples
	 * @param bos
	 * @throws IOException 
	 */
	@Override
	public void doCompress(int [][][] block, int bands, int lines, int samples, BitOutputStream bos) throws IOException {
		this.checkParameterSanity(bands);
		
		//system out the compression parameters
		System.out.println("AbsErr: " + this.getAbsErrVal(0) + " RelErr: " + this.getRelErrVal(0));
		
		int [][][] repBlock = new int[bands][lines][samples];
		int [][][] diffBlock = new int[bands][lines][samples];
		
		////WEIGHT INITIALIZATION 4.6.3.2
		int [][] weights = this.getInitialWeights(bands);
		
		if (entropyCoder == null)
			entropyCoder = new SampleAdaptiveEntropyCoder(this.uMax, this.depth, bands, this.gammaZero, this.gammaStar, this.accumulatorInitializationConstant);
		else
			entropyCoder.reset(this.uMax, this.depth, bands, this.gammaZero, this.gammaStar, this.accumulatorInitializationConstant);
		
		//SampleAdaptiveEntropyCoder entropyCoder = new SampleAdaptiveEntropyCoder(this.uMax, this.depth, bands, this.gammaZero, this.gammaStar, this.accumulatorInitializationConstant);
		
		//LOOPS are interchangeable as long as samples come after lines
		//BIP: lines>samples>bands
		//BIL: lines>bands>sample
		//BSQ: bands>lines>samples
		for (int l = 0; l < lines; l++) {
			for (int s = 0; s < samples; s++) {
				for (int b = 0; b < bands; b++) {
					int t = l*samples + s;
					ssmpl.sample(block[b][l][s]);
					
					if (SHOW_PROGRESS) {
						if ((b+s*bands+l*bands*samples) % 1000000 == 0) {
							System.out.println((b+s*bands+l*bands*samples) / 10000 + "0k / " + (lines*bands*samples) / 1000 + "k");
						}
					}
					//compressing sample block[b][l][s]
					if (t == 0) {
						long doubleResolutionPredSampleValue = drpsvsmpl.sample(this.calcDoubleResolutionSampleValue(b, 0, 0, 0, diffBlock));
						long predictedSampleValue = psvsmpl.sample(this.calcPredictedSampleValue(doubleResolutionPredSampleValue));
						
						long predictionResidual = prsmpl.sample(this.calcPredictionResidual(block[b][0][0], predictedSampleValue));
						long quantizerIndex = qismpl.sample(this.calcQuantizerIndex(predictionResidual, 0, 0));
	
						long sampleRepresentative = srsmpl.sample(this.calcSampleRepresentative(0, 0, 0, block[b][0][0]));
						repBlock[b][0][0] = (int) sampleRepresentative;
						
						long theta = tsmpl.sample(this.calcTheta(0, predictedSampleValue, 0));
						long mappedQuantizerIndex = mqismpl.sample(calcMappedQuantizerIndex(quantizerIndex, theta, doubleResolutionPredSampleValue));
						
						entropyCoder.code((int) mappedQuantizerIndex, 0, b, bos);
						
						mevsmpl.sample(this.calcMaxErrVal(b, predictedSampleValue));

						continue;
					}
					
					////LOCAL SUM BEGIN 4.4
					long localSum = lssmpl.sample(this.calcLocalSum(b, l, s, repBlock, samples));
					////LOCAL SUM END
	
					////LOCAL DIFF BEGIN 4.5
					long northDiff = ndsmpl.sample(this.calcNorthDiff(b, l, s, repBlock, localSum));
					long westDiff = wdsmpl.sample(this.calcWestDiff(b, l, s, repBlock, localSum));
					long northWestDiff = nwdsmpl.sample(this.calcNorthWestDiff(b, l, s, repBlock, localSum));
					////LOCAL DIFF END
					
					//PREDICTED CENTRAL LOCAL DIFFERENCE 4.7.1
					long predictedCentralDiff = pcdsmpl.sample(this.calcPredictedCentralDiff(b, l, s, weights, northDiff, westDiff, northWestDiff, diffBlock));
					//PREDICTED CENTRAL LOCAL DIFFERENCE END
				
					//HR PREDICTED SAMPLE VALUE 4.7.2
					long highResolutionPredSampleValue = hrpsvsmpl.sample(this.calcHighResolutionPredSampleValue(predictedCentralDiff, localSum));
					//DR PREDICTED SAMPLE VALUE 4.7.3
					long doubleResolutionPredSampleValue = drpsvsmpl.sample(this.calcDoubleResolutionSampleValue(b, l, s, highResolutionPredSampleValue, block));
					//PREDICTED SAMMPLE VALUE 4.7.4
					long predictedSampleValue = psvsmpl.sample(this.calcPredictedSampleValue(doubleResolutionPredSampleValue));
					//PRED SAMPLE VALUE END
						
					//PRED RES 4.8.1 + 4.8.2.1
					long predictionResidual = prsmpl.sample(this.calcPredictionResidual(block[b][l][s], predictedSampleValue));
					long maxErrVal = mevsmpl.sample(this.calcMaxErrVal(b, predictedSampleValue));
					long quantizerIndex = qismpl.sample(this.calcQuantizerIndex(predictionResidual, maxErrVal, t));
					
					//DR SAMPLE REPRESENTATIVE AND SAMPLE REPRESENTATIVE 4.9
					long clippedQuantizerBinCenter = cqbcsmpl.sample(this.calcClipQuantizerBinCenter(predictedSampleValue, quantizerIndex, maxErrVal));
					long doubleResolutionSampleRepresentative = drsrsmpl.sample(this.calcDoubleResolutionSampleRepresentative(b, clippedQuantizerBinCenter, quantizerIndex, maxErrVal, highResolutionPredSampleValue));
					
					//DR PRED ERR 4.10.1
					long doubleResolutionPredictionError = drpesmpl.sample(this.calcDoubleResolutionPredictionError(clippedQuantizerBinCenter, doubleResolutionPredSampleValue));
					//WEIGHT UPDATE SCALING EXPONENT 4.10.2
					long weightUpdateScalingExponent = wusesmpl.sample(this.calcWeightUpdateScalingExponent(t, samples));
					//WEIGHT UPDATE 4.10.3
					int windex = 0;
					if (this.fullPredictionMode) {
						int weightExponentOffset = this.getIntraBandWeightExponentOffset(b);
						//north, west, northwest
						weights[b][0] = wsmpl.sample(this.updateWeight(weights[b][0], doubleResolutionPredictionError, northDiff, weightUpdateScalingExponent, weightExponentOffset, t));
						weights[b][1] = wsmpl.sample(this.updateWeight(weights[b][1], doubleResolutionPredictionError, westDiff, weightUpdateScalingExponent, weightExponentOffset, t));
						weights[b][2] = wsmpl.sample(this.updateWeight(weights[b][2], doubleResolutionPredictionError, northWestDiff, weightUpdateScalingExponent, weightExponentOffset, t));
						windex = 3;
					}
					for (int p = 0; p < this.predictionBands; p++) {
						if (b - p > 0) 
							weights[b][windex+p] = wsmpl.sample(this.updateWeight(weights[b][windex+p], doubleResolutionPredictionError, diffBlock[b-p-1][l][s], weightUpdateScalingExponent, getInterBandWeightExponentOffsets(b, p), t));
					}
					
					//MAPPED QUANTIZER INDEX 4.11
					long theta = tsmpl.sample(this.calcTheta(t, predictedSampleValue, maxErrVal));
					long mappedQuantizerIndex = mqismpl.sample(calcMappedQuantizerIndex(quantizerIndex, theta, doubleResolutionPredSampleValue));
					
					//Send to coder to generate the binary output stream
					entropyCoder.code((int) mappedQuantizerIndex, t, b, bos);
					
					long sampleRepresentative = srsmpl.sample(this.calcSampleRepresentative(l, s, doubleResolutionSampleRepresentative, block[b][l][s]));
					repBlock[b][l][s] = (int) sampleRepresentative;
					
					long centralLocalDiff = cldsmpl.sample(this.calcCentralLocalDiff(b, l, s, repBlock, localSum));
					diffBlock[b][l][s] = (int) centralLocalDiff;
				}
			}
		}
		
		ssmpl.export();
		drpsvsmpl.export();
		psvsmpl.export();
		prsmpl.export();
		wsmpl.export();
		wusesmpl.export();
		drpesmpl.export();
		drsrsmpl.export();
		cqbcsmpl.export();
		mevsmpl.export();
		hrpsvsmpl.export();
		pcdsmpl.export();
		cldsmpl.export();
		nwdsmpl.export();
		wdsmpl.export();
		ndsmpl.export();
		lssmpl.export();
		qismpl.export();
		srsmpl.export();
		tsmpl.export();
		mqismpl.export();
	}
	
	
	public int[][][] decompress(int bands, int lines, int samples, BitInputStream bis) throws IOException {
		//helpers
		if (entropyCoder == null)
			entropyCoder = new SampleAdaptiveEntropyCoder(this.uMax, this.depth, bands, this.gammaZero, this.gammaStar, this.accumulatorInitializationConstant);
		else
			entropyCoder.reset(this.uMax, this.depth, bands, this.gammaZero, this.gammaStar, this.accumulatorInitializationConstant);
			
		//SampleAdaptiveEntropyCoder entropyCoder = new SampleAdaptiveEntropyCoder(this.uMax, this.depth, bands, this.gammaZero, this.gammaStar, this.accumulatorInitializationConstant);
		int [][] weights = this.getInitialWeights(bands);
		
		//image will be output here
		int [][][] image = new int[bands][lines][samples];
		
		//auxiliary vars
		int [][][] diffBlock = new int[bands][lines][samples];
		int [][][] repBlock = new int[bands][lines][samples];
		
		for (int l = 0; l < lines; l++) {
			for (int s = 0; s < samples; s++) {
				for (int b = 0; b < bands; b++) {
					int t = l*samples + s;
					
					if (SHOW_PROGRESS) {
						if ((b+s*bands+l*bands*samples) % 1000000 == 0) {
							System.out.println((b+s*bands+l*bands*samples) / 10000 + "0k / " + (lines*bands*samples) / 1000 + "k");
						}
					}
					
					//separate to clearly define first sample processing and other sample processing 
					if (t == 0) {
						//decode first sample
						long mappedQuantizerIndex = mqismpl.unSample((long) entropyCoder.decode(0, b, bis));
						
						long doubleResolutionPredSampleValue = drpsvsmpl.unSample(this.calcDoubleResolutionSampleValue(b, 0, 0, 0, diffBlock));
						long predictedSampleValue = psvsmpl.unSample(this.calcPredictedSampleValue(doubleResolutionPredSampleValue));
						long theta = tsmpl.unSample(this.calcTheta(0, predictedSampleValue, 0));
						
						long maxErrVal = mevsmpl.unSample(this.calcMaxErrVal(b, predictedSampleValue));
						long quantizerIndex = qismpl.unSample(this.deCalcQuantizerIndex(mappedQuantizerIndex, theta, doubleResolutionPredSampleValue, t, predictedSampleValue, maxErrVal));
						
						long predictionResidual = prsmpl.unSample(this.deCalcPredictionResidual(t, quantizerIndex, maxErrVal));
						
						long sample = this.deCalcSample(predictionResidual, predictedSampleValue);
						image[b][0][0] = (int) sample;
	
						long sampleRepresentative = srsmpl.unSample(this.calcSampleRepresentative(0, 0, 0, (int) sample));
						repBlock[b][0][0] = (int) sampleRepresentative;

						continue;
					}
					
					long mappedQuantizerIndex = mqismpl.unSample((long) entropyCoder.decode(t, b, bis));
					
					////LOCAL SUM BEGIN 4.4
					long localSum = lssmpl.unSample(this.calcLocalSum(b, l, s, repBlock, samples));
					////LOCAL SUM END
	
					////LOCAL DIFF BEGIN 4.5
					long northDiff = ndsmpl.unSample(this.calcNorthDiff(b, l, s, repBlock, localSum));
					long westDiff = wdsmpl.unSample(this.calcWestDiff(b, l, s, repBlock, localSum));
					long northWestDiff = nwdsmpl.unSample(this.calcNorthWestDiff(b, l, s, repBlock, localSum));
					////LOCAL DIFF END
					
					//PREDICTED CENTRAL LOCAL DIFFERENCE 4.7.1
					long predictedCentralDiff = pcdsmpl.unSample(this.calcPredictedCentralDiff(b, l, s, weights, northDiff, westDiff, northWestDiff, diffBlock));
					//PREDICTED CENTRAL LOCAL DIFFERENCE END
				
					
					//HR PREDICTED SAMPLE VALUE 4.7.2
					long highResolutionPredSampleValue = hrpsvsmpl.unSample(this.calcHighResolutionPredSampleValue(predictedCentralDiff, localSum));
					//DR PREDICTED SAMPLE VALUE 4.7.3
					long doubleResolutionPredSampleValue = drpsvsmpl.unSample(this.calcDoubleResolutionSampleValue(b, l, s, highResolutionPredSampleValue, image)); //?
					//PREDICTED SAMMPLE VALUE 4.7.4
					long predictedSampleValue = psvsmpl.unSample(this.calcPredictedSampleValue(doubleResolutionPredSampleValue));
					//PRED SAMPLE VALUE END
					
					//UNDO COMPRESSION
					long maxErrVal = mevsmpl.unSample(this.calcMaxErrVal(b, predictedSampleValue));
					long theta = tsmpl.unSample(this.calcTheta(t, predictedSampleValue, maxErrVal));
					
					long quantizerIndex = qismpl.unSample(this.deCalcQuantizerIndex(mappedQuantizerIndex, theta, doubleResolutionPredSampleValue, t, predictedSampleValue, maxErrVal));
					long predictionResidual = prsmpl.unSample(this.deCalcPredictionResidual(t, quantizerIndex, maxErrVal));
					long sample = this.deCalcSample(predictionResidual, predictedSampleValue);
					image[b][l][s] = (int) sample;
					//UNDO COMPRESSION END
									
					//DR SAMPLE REPRESENTATIVE AND SAMPLE REPRESENTATIVE 4.9
					long clippedQuantizerBinCenter = cqbcsmpl.unSample(this.calcClipQuantizerBinCenter(predictedSampleValue, quantizerIndex, maxErrVal));
					long doubleResolutionSampleRepresentative = drsrsmpl.unSample(this.calcDoubleResolutionSampleRepresentative(b, clippedQuantizerBinCenter, quantizerIndex, maxErrVal, highResolutionPredSampleValue));
			
					//DR PRED ERR 4.10.1
					long doubleResolutionPredictionError = drpesmpl.unSample(this.calcDoubleResolutionPredictionError(clippedQuantizerBinCenter, doubleResolutionPredSampleValue));
					//WEIGHT UPDATE SCALING EXPONENT 4.10.2
					long weightUpdateScalingExponent = wusesmpl.unSample(this.calcWeightUpdateScalingExponent(t, samples));
					//WEIGHT UPDATE 4.10.3
					int windex = 0;
					if (this.fullPredictionMode) {
						int weightExponentOffset = this.getIntraBandWeightExponentOffset(b);
						//north, west, northwest
						weights[b][0] = wsmpl.unSample(this.updateWeight(weights[b][0], doubleResolutionPredictionError, northDiff, weightUpdateScalingExponent, weightExponentOffset, t));
						weights[b][1] = wsmpl.unSample(this.updateWeight(weights[b][1], doubleResolutionPredictionError, westDiff, weightUpdateScalingExponent, weightExponentOffset, t));
						weights[b][2] = wsmpl.unSample(this.updateWeight(weights[b][2], doubleResolutionPredictionError, northWestDiff, weightUpdateScalingExponent, weightExponentOffset, t));
						windex = 3;
					}
					for (int p = 0; p < this.predictionBands; p++) {
						if (b - p > 0) 
							weights[b][windex+p] = wsmpl.unSample(this.updateWeight(weights[b][windex+p], doubleResolutionPredictionError, diffBlock[b-p-1][l][s], weightUpdateScalingExponent, getInterBandWeightExponentOffsets(b, p), t));
					}
					

					long sampleRepresentative = srsmpl.unSample(this.calcSampleRepresentative(l, s, doubleResolutionSampleRepresentative, (int) sample));
					repBlock[b][l][s] = (int) sampleRepresentative;
					
					long centralLocalDiff = cldsmpl.unSample(this.calcCentralLocalDiff(b, l, s, repBlock, localSum));
					diffBlock[b][l][s] = (int) centralLocalDiff;
				}
			}
		}
		
		return image;
	}
	

	private long calcLocalSum(int b, int l, int s, int[][][] repBlock, int samples) { //EQ 20, 21, 22, 23
		if (l == 0 && s == 0) 
			throw new IllegalArgumentException("Undefined local sum for t=0");
		
		long localSum = 0;
		switch (this.localSumType) {
			case WIDE_NEIGHBOR_ORIENTED: { //EQ 20
				if (l > 0 && s > 0 && s < samples - 1) {
					localSum = repBlock[b][l][s-1] + repBlock[b][l-1][s-1] + repBlock[b][l-1][s] + repBlock[b][l-1][s+1];
				} else if (l == 0 && s > 0) {
					localSum = repBlock[b][l][s-1] << 2;
				} else if (l > 0 && s == 0) {
					localSum = (repBlock[b][l-1][s] + repBlock[b][l-1][s+1]) << 1;
				} else if (l > 0 && s == samples - 1) {
					localSum = repBlock[b][l][s-1] + repBlock[b][l-1][s-1] + (repBlock[b][l-1][s] << 1);
				} else {
					if (l != 0 || s != 0) {
						throw new IllegalStateException("Should not get here");
					}
				}
				break;
			}
			case NARROW_NEIGHBOR_ORIENTED: { //EQ 21
				if (l > 0 && s > 0 && s < samples - 1) {
					localSum = repBlock[b][l-1][s-1] + (repBlock[b][l-1][s] << 1) + repBlock[b][l-1][s+1];
				} else if (l == 0 && s > 0 && b > 0) {
					localSum = repBlock[b-1][l][s-1];
				} else if (l > 0 && s == 0) {
					localSum = (repBlock[b][l-1][s] + repBlock[b][l-1][s+1]) << 1;
				} else if (l > 0 && s == samples - 1) {
					localSum = (repBlock[b][l-1][s-1] + repBlock[b][l-1][s]) << 1;
				} else if (l == 0 && s > 0 && b == 0) {
					localSum = ParameterCalc.sMid(this.depth) << 2;
				} else {
					if (l != 0 || s != 0) {
						throw new IllegalStateException("Should not get here");
					}
				}
				break;
			}
			case WIDE_COLUMN_ORIENTED: { //EQ 22
				if (l > 0) {
					localSum = repBlock[b][l-1][s] << 2;
				} else if (l == 0 && s > 0) {
					localSum = repBlock[b][l][s-1] << 2;
				} else {
					if (l != 0 || s != 0) {
						throw new IllegalStateException("Should not get here");
					}
				}
				break;
			}
			case NARROW_COLUMN_ORIENTED: { //EQ 23
				if (l > 0) {
					localSum = repBlock[b][l-1][s] << 2;
				} else if (l == 0 && s > 0 && b > 0) {
					localSum = repBlock[b-1][l][s-1] << 2;
				} else if (l == 0 && s > 0 && b == 0) {
					localSum = ParameterCalc.sMid(this.depth) << 2;
				} else {
					if (l != 0 || s != 0) {
						throw new IllegalStateException("Should not get here");
					}
				}
				break;
			}
		}
		return localSum;
	}
	
	private long calcCentralLocalDiff(int b, int l, int s, int[][][] repBlock, long localSum) { //EQ 24
		if (l == 0 && s == 0)
			throw new IllegalArgumentException("Central local diff not defined for t=0");
		long res = (repBlock[b][l][s] << 2) - localSum;
		/*long res;
		if (l == 0)
			res = (repBlock[b][l][s-1] << 2) - localSum;
		else if (s == 0)
			res = (repBlock[b][l-1][s] << 2) - localSum;
		else 
			res = (repBlock[b][l-1][s] << 2) - localSum;*/
		return res;
	}
	
	private long calcNorthDiff (int b, int l, int s, int[][][] repBlock, long localSum) { //EQ 25
		if (l == 0 && s == 0)
			throw new IllegalArgumentException("North diff not defined for t=0");
		
		if (this.fullPredictionMode && (s != 0 || l != 0))
			if (l > 0) 
				return (repBlock[b][l-1][s] << 2) - localSum;
		return 0;
	}
	
	private long calcWestDiff (int b, int l, int s, int[][][] repBlock, long localSum) { //EQ 26
		if (l == 0 && s == 0)
			throw new IllegalArgumentException("West diff not defined for t=0");
		
		if (this.fullPredictionMode && (s != 0 || l != 0)) {
			if (s > 0 && l > 0) {
				return (repBlock[b][l][s-1] << 2) - localSum;
			} else if (s == 0 && l > 0) {
				return (repBlock[b][l-1][s] << 2) - localSum;
			}
		}
		return 0;
	}
	
	private long calcNorthWestDiff (int b, int l, int s, int[][][] repBlock, long localSum) { //EQ 27
		if (l == 0 && s == 0)
			throw new IllegalArgumentException("NorthWest diff not defined for t=0");
		
		if (this.fullPredictionMode && (s != 0 || l != 0)) {
			if (s > 0 && l > 0) {
				return (repBlock[b][l-1][s-1] << 2) - localSum;
			} else if (s == 0 && l > 0) {
				return (repBlock[b][l-1][s] << 2) - localSum;
			}
		}
		return 0;
	}

	
	private long calcPredictedCentralDiff (int b, int l, int s, int[][] weights, long northDiff, long westDiff, long northWestDiff, int[][][] diffBlock) { //EQ 36
		if (l == 0 && s == 0)
			throw new IllegalArgumentException("PredictedCentralDiff not defined for t=0");
		
		long predictedCentralDiff = 0;
		if (b != 0 || this.fullPredictionMode) {
			int windex = 0;
			if (this.fullPredictionMode) {
				predictedCentralDiff += weights[b][0] * northDiff;
				predictedCentralDiff += weights[b][1] * westDiff;
				predictedCentralDiff += weights[b][2] * northWestDiff;
				windex = 3;
			}
			for (int p = 0; p < this.predictionBands; p++) {
				if (b - p > 0) 
					predictedCentralDiff += weights[b][p+windex] * diffBlock[b-p-1][l][s];
			}
			/*for (int p = 0; p < this.predictionBands; p++) {
				if (b - p > 0) {
					if (s == 0 && l == 0) {
						
					} else if (s == 0) {
						predictedCentralDiff += weights[b][p+windex] * diffBlock[b-p-1][l-1][s];
					} else {
						predictedCentralDiff += weights[b][p+windex] * diffBlock[b-p-1][l][s-1];
					}
				}
			}*/
		}
		
		return predictedCentralDiff;
	}
	
	private long calcHighResolutionPredSampleValue(long predictedCentralDiff, long localSum) { // EQ 37
		long highResolutionPredSampleValue = 
				Utils.modR(predictedCentralDiff + ((localSum - (ParameterCalc.sMid(this.depth) << 2)) << this.omega), this.r)
				+ (ParameterCalc.sMid(this.depth) << (this.omega + 2))
				+ (1 << (this.omega + 1));
		return Utils.clamp(
				highResolutionPredSampleValue, 
				ParameterCalc.sMin() << (this.omega + 2), 
				(ParameterCalc.sMax(this.depth) << (this.omega + 2)) + (1 << (this.omega + 1)));
	}
	
	private long calcDoubleResolutionSampleValue(int b, int l, int s, long highResolutionPredSampleValue, int[][][] block) { //EQ 38
		long doubleResolutionPredSampleValue = 0;
		if (s > 0 || l > 0) {
			doubleResolutionPredSampleValue = highResolutionPredSampleValue >> (this.omega + 1);
		} else if (this.predictionBands == 0 || b == 0) {
			doubleResolutionPredSampleValue = ParameterCalc.sMid(this.depth) << 1;
		} else {
			doubleResolutionPredSampleValue = block[b-1][l][s] << 1;;
		}
		return doubleResolutionPredSampleValue;
	}
	
	private long calcPredictedSampleValue(long doubleResolutionPredSampleValue) { //EQ 39
		return doubleResolutionPredSampleValue >> 1;
	}
	
	private long calcPredictionResidual(long sample, long predictedSampleValue) { //EQ 40
		 return sample - predictedSampleValue;
	}
	
	private long calcQuantizerIndex(long predResidual, long maxErrVal, int t) { //EQ 41
		if (t == 0)
			return predResidual;
		return UniformQuantizer.quantize(predResidual, maxErrVal);
	}
	
	private long calcMaxErrVal(int b, long predSampleValue) { //EQ 42, 43, 44, 45
		long maxErrVal = 0;
		if (this.useAbsoluteErrLimit && this.useRelativeErrLimit) {
			maxErrVal = Math.min(this.getAbsErrVal(b), (this.getRelErrVal(b)*Math.abs(predSampleValue)) >> this.depth); //EQ 45
		} else if (this.useRelativeErrLimit) {
			maxErrVal = (this.getRelErrVal(b)*Math.abs(predSampleValue)) >> this.depth; //EQ 44
		} else if (this.useAbsoluteErrLimit) {
			maxErrVal = this.getAbsErrVal(b); //EQ 43
		} else { //no errors
			maxErrVal = 0; //EQ 42
		}
		return maxErrVal;
	}
	
	private long calcSampleRepresentative(int l, int s, long doubleResolutionSampleRepresentative, int sample) { //EQ 46
		if (l > 0 || s > 0) {
			return (doubleResolutionSampleRepresentative + 1) >> 1;
		} else {
			return sample;
		}
	}
	
	private long calcDoubleResolutionSampleRepresentative(int b, long clippedQuantizerBinCenter, long quantizerIndex, long maxErrVal, long highResolutionPredSampleValue) { //EQ 47
		long fm = (1 << this.getResolution(b)) - this.getDamping(b);
		long sm = (clippedQuantizerBinCenter << this.omega) - ((Utils.signum(quantizerIndex)*maxErrVal*this.getOffset(b)) << (this.omega - this.getResolution(b)));
		long add = this.getDamping(b)*highResolutionPredSampleValue - (this.getDamping(b) << (this.omega + 1)); 
		long sby = this.omega + this.getResolution(b) + 1; 
		return (((fm * sm) << 2) + add) >> sby;
	}
	
	private long calcClipQuantizerBinCenter(long predictedSampleValue, long quantizerIndex, long maxErrVal) { //EQ 48
		return Utils.clamp(predictedSampleValue + quantizerIndex*(2*maxErrVal + 1), ParameterCalc.sMin(), ParameterCalc.sMax(this.depth));
	}
	
	private long calcDoubleResolutionPredictionError(long clippedQuantizerBinCenter, long doubleResolutionPredictedSampleValue) { //EQ 49
		return (clippedQuantizerBinCenter << 1) - doubleResolutionPredictedSampleValue;
	}
	
	private long calcWeightUpdateScalingExponent(int t, int samples) { //EQ 50
		return Utils.clamp(this.vmin + ((t - samples) >> this.tIncExp), this.vmin, this.vmax) + this.depth - this.omega;
	}
	
	private int updateWeight(int weight, long doubleResolutionPredictionError, long diff, long weightUpdateScalingExponent, int weightExponentOffset, int t) { //EQ 51,52,53,54
		if (t == 0)
			throw new IllegalArgumentException("Weight updated undefined for t=0");
		//first of all calculate the exponent above to see if its positive or negative
		int exponent = (int) weightUpdateScalingExponent + weightExponentOffset;
		int result = weight;
		if (exponent > 0) {
			result += ((((Utils.signumPlus((int) doubleResolutionPredictionError)*diff) >> exponent) + 1) >> 1);
		} else {
			result += ((((Utils.signumPlus((int) doubleResolutionPredictionError)*diff) << (-exponent)) + 1) >> 1);
		}
		return Utils.clamp(
				result, 
				ParameterCalc.wMin(this.omega), 
				ParameterCalc.wMax(this.omega));
	}
	
	private long calcMappedQuantizerIndex(long quantizerIndex, long theta, long doubleResolutionPredictedSampleValue) { //EQ 55
		if (Math.abs(quantizerIndex) > theta) {
			return Math.abs(quantizerIndex) + theta;
		} else {
			long val;
			if (doubleResolutionPredictedSampleValue % 2 == 0) {
				val = quantizerIndex;
			} else {
				val = -quantizerIndex;
			}
			if (val >= 0 && theta >= val) {
				return Math.abs(quantizerIndex) << 1;
			} else {
				return (Math.abs(quantizerIndex) << 1) - 1;
			}
		}
	}
	
	private long getLowerTheta(int t, long predictedSampleValue, long maxErrVal) { //EQ 56 a
		if (t == 0) 
			return predictedSampleValue - (int) ParameterCalc.sMin();
		else 
			return (predictedSampleValue - ParameterCalc.sMin() + maxErrVal) / (2*maxErrVal + 1);
	}
	
	private long getUpperTheta(int t, long predictedSampleValue, long maxErrVal) { //EQ 56 b
		if (t == 0) 
			return (int) ParameterCalc.sMax(this.depth) - predictedSampleValue;
		else 
			return (ParameterCalc.sMax(this.depth) - predictedSampleValue + maxErrVal) / (2*maxErrVal + 1);
	}
	
	private long calcTheta(int t, long predictedSampleValue, long maxErrVal) { //EQ 56
		return Math.min(getLowerTheta(t, predictedSampleValue, maxErrVal), getUpperTheta(t, predictedSampleValue, maxErrVal));
	}

	private long deCalcQuantizerIndex(long mappedQuantizerIndex, long theta, long doubleResolutionPredictedSampleValue, int t, long predictedSampleValue, long maxErrVal) { //INVERSE OF EQ 55
		if (mappedQuantizerIndex > 2*theta) {
			long absSignedQuantizerIndex = mappedQuantizerIndex - theta;
			//dunno if positive or negative�?�?�? its collapsed
			//positive or negative depending on which limit theta triggered
			long lTheta = getLowerTheta(t, predictedSampleValue, maxErrVal);
			long uTheta = getUpperTheta(t, predictedSampleValue, maxErrVal);
			if (lTheta <= uTheta) {
				return absSignedQuantizerIndex;
			} else {
				return -absSignedQuantizerIndex;
			}
		} else {
			long absSignedQuantizerIndex = (mappedQuantizerIndex + 1) >> 1;
			//assume it is possitive and check if not
			if (mappedQuantizerIndex % 2 == 0) {
				//in this case it went through the first equation. if it holds, it its positive, otherwise
				//it is negative
				if (Utils.minusOneToThe(doubleResolutionPredictedSampleValue)*absSignedQuantizerIndex >= 0
						&& Utils.minusOneToThe(doubleResolutionPredictedSampleValue)*absSignedQuantizerIndex <= theta)
					return absSignedQuantizerIndex;
				return -absSignedQuantizerIndex;
			} else {
				//in this case it went through the second one
				//if the equation holds it is negative, otherwise positive
				if (Utils.minusOneToThe(doubleResolutionPredictedSampleValue)*absSignedQuantizerIndex >= 0
						&& Utils.minusOneToThe(doubleResolutionPredictedSampleValue)*absSignedQuantizerIndex <= theta)
					return -absSignedQuantizerIndex;
				return absSignedQuantizerIndex;
			}
		}
	}
	
	private long deCalcSample(long predictionResidual, long predictedSampleValue) {
		return predictionResidual + predictedSampleValue;
	}

	private long deCalcPredictionResidual(int t, long quantizerIndex, long maxErrVal) {
		if (t == 0)
			return quantizerIndex;
		return UniformQuantizer.dequantize(quantizerIndex, maxErrVal);
	}
	
}
