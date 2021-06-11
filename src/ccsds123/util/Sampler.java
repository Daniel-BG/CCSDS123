package ccsds123.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import ccsds123.core.Coordinate;

public class Sampler <T> {
	
	private static final boolean DISABLE_CHECKING = false;
	private static final boolean DISABLE_SAMPLING = false;
	
	private Deque<T> samplingDQ, checkingDQ;
	private String filename;
	
	public Sampler(String filename) {
		this.filename = filename;
		if (!DISABLE_SAMPLING)
			samplingDQ = new LinkedList<T>();
		if (!DISABLE_CHECKING)
			checkingDQ = new LinkedList<T>();
	}
	
	public T sample(T t) {
		if (!DISABLE_SAMPLING)
			samplingDQ.addLast(t);
		if (!DISABLE_CHECKING)
			checkingDQ.addLast(t);
			
		return t;
	}
	
	private int uSampleCnt = 0;
	private T doUnSample(T t, boolean reverse) {
		if (!DISABLE_CHECKING) {
			T s;
			if (!reverse)
				s = checkingDQ.removeFirst();
			else
				s = checkingDQ.removeLast();
			
			if (!s.equals(t)) {
				throw new IllegalStateException("Difference @" + uSampleCnt + "! " + t.toString() + " -> " + s.toString());
			}
			uSampleCnt++;
		}
		
		return t;
	}
	
	public T unSample(T t) {		
		return this.doUnSample(t, false);
	}
	
	public T reverseUnSample(T t) {		
		return this.doUnSample(t, true);
	}
	
	public void burnSample() {
		if (!DISABLE_CHECKING)
			checkingDQ.removeFirst();
	}
	
	public void reverseBurnSample() {
		if (!DISABLE_CHECKING)
			checkingDQ.removeLast();
	}

	public void export() throws IOException {
		if (DISABLE_SAMPLING)
			return;
		
		FileOutputStream fos = new FileOutputStream(Sampler.samplePath + this.filename + Sampler.extension, false);
		OutputStreamWriter osw = new OutputStreamWriter(fos);
		BufferedWriter bw = new BufferedWriter(osw);
		for (T s: samplingDQ) {
			bw.write(s.toString());
			bw.newLine();
		}	
		bw.close();
	}
	
	public void exportDiagonal(int bands, int lines, int samples) throws IOException {
		if (DISABLE_SAMPLING || samplingDQ.isEmpty())
			return;
		FileOutputStream fos = new FileOutputStream(Sampler.samplePath + this.filename + Sampler.extension + Sampler.diagonalMarker, false);
		OutputStreamWriter osw = new OutputStreamWriter(fos);
		BufferedWriter bw = new BufferedWriter(osw);
		
		@SuppressWarnings("unchecked")
		T[][][] arr = (T[][][]) new Object[bands][lines][samples];
		Iterator<T> tIter = samplingDQ.iterator();
		
		for (int j = 0; j < lines; j++) {
			for (int k = 0; k < samples; k++) {
				for (int i = 0; i < bands; i++) {
					arr[i][j][k] = tIter.next();
				}
			}
		}
		Iterator<Coordinate> coordIter = Coordinate.getDiagonalIterator(bands, lines, samples);
		while (coordIter.hasNext()) {
			Coordinate coord = coordIter.next();
			bw.write(arr[coord.band][coord.line][coord.sample].toString());
			bw.newLine();
		}	
		bw.close();
	}
	
	private static String samplePath;
	private static String extension;
	private static String diagonalMarker = ".diag";
	
	public static void setSamplePath(String samplePath) {
		Sampler.samplePath = samplePath;
	}
	
	public static void setSampleExt(String ext) {
		Sampler.extension = ext;
	}
	
}

