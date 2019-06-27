package ccsds123.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Sampler <T> {
	
	private Deque<T> samples;
	private String filename;
	
	public Sampler(String filename) {
		this.filename = filename;
		samples = new LinkedList<T>();
	}
	
	public T sample(T t) {
		samples.addLast(t);
		return t;
	}
	
	private int uSampleCnt = 0;
	public T unSample(T t) {
		T s = samples.removeFirst();
		if (!s.equals(t)) {
			throw new IllegalStateException("Difference @" + uSampleCnt + "! " + t.toString() + " -> " + s.toString());
		}
		uSampleCnt++;
		return t;
	}

	public void export() throws IOException {
		FileOutputStream fos = new FileOutputStream(Sampler.samplePath + this.filename + Sampler.extension, false);
		OutputStreamWriter osw = new OutputStreamWriter(fos);
		BufferedWriter bw = new BufferedWriter(osw);
		for (T s: samples) {
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

