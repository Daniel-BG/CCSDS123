package ccsds123.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Deque;
import java.util.LinkedList;

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
	public T unSample(T t) {		
		if (!DISABLE_CHECKING) {
			T s = checkingDQ.removeFirst();
			if (!s.equals(t)) {
				throw new IllegalStateException("Difference @" + uSampleCnt + "! " + t.toString() + " -> " + s.toString());
			}
			uSampleCnt++;
		}
		
		return t;
	}
	
	public T burnSample() {
		return checkingDQ.removeFirst();
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
	
	private static String samplePath;
	private static String extension;
	
	public static void setSamplePath(String samplePath) {
		Sampler.samplePath = samplePath;
	}
	
	public static void setSampleExt(String ext) {
		Sampler.extension = ext;
	}
	
}

