package ccsds123.core;

import java.io.IOException;

import ccsds123.util.Sampler;

public class SamplingUnit {
	//predictor samplers
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
	
	
	public void export() throws IOException {
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
		
		accsmpl.export();
		uicismpl.export();
		uismpl.export();	
		cntsmpl.export();
	}

}
