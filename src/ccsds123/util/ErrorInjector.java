package ccsds123.util;

import java.util.Random;

public class ErrorInjector {
	
	private int numErrors, numSamples;
	private int[] sampleError;
	private int[] bitError;
	private int injectIndex, errorIndex;
	
	/**
	 * Create and configure an error injector. 
	 * It will inject numErrors bitFlips randomly 
	 * distributed among the numSamples and bitsPerSample 
	 * given, always the same if the seed is specified and equal.
	 * Errors can potentially cancel each other if they randomly 
	 * fall in the same sample and bit an even number of times
	 * @param numErrors up to 
	 * @param numSamples up to 2^32
	 * @param bitsPerSample up to 64
	 * @param seed
	 * @param randomSeed true if seed is to be ignored
	 */
	public ErrorInjector (int numErrors, int numSamples, int bitsPerSample, long seed, boolean randomSeed) {
		this.numErrors = numErrors;
		this.numSamples = numSamples;
		
		this.sampleError = new int[numSamples];
		this.bitError = new int[numSamples];
		
		Random r;
		if (!randomSeed)
			r = new Random(seed);
		else
			r = new Random();
	
		this.sampleError = r.ints(0, numSamples).distinct().limit(numErrors).sorted().toArray();
		this.bitError = r.ints(0, bitsPerSample).limit(numErrors).toArray();
		
		injectIndex = 0;
		errorIndex = 0;
	}
	
	/**
	 * All samples must be processed through this routine. It will leave most
	 * intact and will only trigger on the randomly selected bitflips,
	 * altering the sample
	 * @param sample
	 * @return
	 */
	public long inject(long sample) {
		if (this.injectIndex >= this.numSamples)
			throw new IllegalStateException("Too many calls to this function: " + injectIndex + " of " + this.numSamples);
		
		if (this.errorIndex < this.numErrors && this.sampleError[this.errorIndex] == this.injectIndex) {
			int bitFlipPos = this.bitError[this.errorIndex];
			this.errorIndex++;
			sample = sample ^ (0x1 << bitFlipPos);
			//Do not ever comment this out or errors will not be verbose
			System.out.println("Injecting error in bit: " + bitFlipPos + " of sample: " + this.injectIndex);
		}
		this.injectIndex++;
		return sample;
	}
	
	
	
	

}
