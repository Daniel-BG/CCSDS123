package ccsds123.core;

import java.io.IOException;

import ccsds123.util.Sampler;

public class SamplingUnit {
	//predictor samplers
	
	protected Sampler<Long> wrsmpl 			= new Sampler<Long>("c_wr");
	protected Sampler<Long> nrsmpl 			= new Sampler<Long>("c_nr");
	protected Sampler<Long> nwrsmpl 		= new Sampler<Long>("c_nwr");
	protected Sampler<Long> nersmpl 		= new Sampler<Long>("c_ner");

	protected Sampler<Integer> ssmpl 		= new Sampler<Integer>("c_s");
	protected Sampler<Long> drpsvsmpl 		= new Sampler<Long>("c_drpsv");
	protected Sampler<Long> psvsmpl	 		= new Sampler<Long>("c_psv");
	protected Sampler<Long> prsmpl	 		= new Sampler<Long>("c_pr");
	protected Sampler<Integer> wsmpl		= new Sampler<Integer>("c_w");
	protected Sampler<Long> wusesmpl		= new Sampler<Long>("c_wuse");
	protected Sampler<Long> drpesmpl		= new Sampler<Long>("c_drpe");
	protected Sampler<Long> drsrsmpl		= new Sampler<Long>("c_drsr");
	protected Sampler<Long> cqbcsmpl		= new Sampler<Long>("c_cqbc");
	protected Sampler<Long> mevsmpl			= new Sampler<Long>("c_mev");
	protected Sampler<Long> hrpsvsmpl		= new Sampler<Long>("c_hrpsv");
	protected Sampler<Long> pcdsmpl			= new Sampler<Long>("c_pcd");
	protected Sampler<Long> cldsmpl			= new Sampler<Long>("c_cld");
	protected Sampler<Long> nwdsmpl			= new Sampler<Long>("c_nwd");
	protected Sampler<Long> wdsmpl			= new Sampler<Long>("c_wd");
	protected Sampler<Long> ndsmpl			= new Sampler<Long>("c_nd");
	protected Sampler<Long> lssmpl			= new Sampler<Long>("c_ls");
	protected Sampler<Long> qismpl			= new Sampler<Long>("c_qi");
	protected Sampler<Long> srsmpl			= new Sampler<Long>("c_sr");
	protected Sampler<Long> tsmpl			= new Sampler<Long>("c_ts");
	protected Sampler<Long> mqismpl			= new Sampler<Long>("c_mqi");
	//encoder samplers
	protected Sampler<Integer> uismpl		= new Sampler<Integer>("c_ui");
	protected Sampler<Integer> uicismpl		= new Sampler<Integer>("c_uici");
	protected Sampler<Long> accsmpl			= new Sampler<Long>("c_acc");
	protected Sampler<Long> cntsmpl 		= new Sampler<Long>("c_cnt");
	
	
	public void export(boolean diagonal, int bands, int lines, int samples) throws IOException {
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
		wrsmpl.export();
		nrsmpl.export();
		nwrsmpl.export();
		nersmpl.export();
		
		if (diagonal) {
			ssmpl.exportDiagonal(bands, lines, samples);
			drpsvsmpl.exportDiagonal(bands, lines, samples);
			psvsmpl.exportDiagonal(bands, lines, samples);
			prsmpl.exportDiagonal(bands, lines, samples);
			wsmpl.exportDiagonal(bands, lines, samples);
			wusesmpl.exportDiagonal(bands, lines, samples);
			drpesmpl.exportDiagonal(bands, lines, samples);
			drsrsmpl.exportDiagonal(bands, lines, samples);
			cqbcsmpl.exportDiagonal(bands, lines, samples);
			mevsmpl.exportDiagonal(bands, lines, samples);
			hrpsvsmpl.exportDiagonal(bands, lines, samples);
			pcdsmpl.exportDiagonal(bands, lines, samples);
			cldsmpl.exportDiagonal(bands, lines, samples);
			nwdsmpl.exportDiagonal(bands, lines, samples);
			wdsmpl.exportDiagonal(bands, lines, samples);
			ndsmpl.exportDiagonal(bands, lines, samples);
			lssmpl.exportDiagonal(bands, lines, samples);
			qismpl.exportDiagonal(bands, lines, samples);
			srsmpl.exportDiagonal(bands, lines, samples);
			tsmpl.exportDiagonal(bands, lines, samples);
			mqismpl.exportDiagonal(bands, lines, samples);
			wrsmpl.exportDiagonal(bands, lines, samples);
			nrsmpl.exportDiagonal(bands, lines, samples);
			nwrsmpl.exportDiagonal(bands, lines, samples);
			nersmpl.exportDiagonal(bands, lines, samples);
		}
		
		
		accsmpl.export();
		uicismpl.export();
		uismpl.export();	
		cntsmpl.export();
	}

}
