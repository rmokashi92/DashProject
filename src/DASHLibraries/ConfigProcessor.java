// Copyright (c) 2016 Triveni Digital, Inc. All rights reserved.

/*
 * ConfigProcessor is responsible for getting the metadata that was originally retrieved from config file in the DASH project.
 * Here only a manifest file is used as a basis for retrieving all the required data.
 * 
 * 
 */

package DASHLibraries;

import java.util.ArrayList;

import com.triveni.dashlive.DashMedia;

public class ConfigProcessor {

	long availability_start_time_in_s;

	long availability_time_offset_in_s = -1;

	long availability_end_time = 0;
	long media_presentation_duration = 0;
	long timeshift_buffer_depth_in_s = 0;
	long minimum_update_period_in_s = 0;
	long modulo_period = 0;
	int[] last_segment_numbers = null; // The last segment number in every
										// period.
	long init_seg_avail_offset = 0; // The number of secs before AST that
									// one can fetch the init segments
	boolean tfdt32_flag = false; // Restart every 3 hours make tfdt fit into
									// 32 bits.
	boolean cont = false; // Continuous update of MPD AST and seg_nr.
	float periods_per_hour = -1; // If > 0, generates that many periods
									// per hour. If 0, only one offset
									// period.

	float period_offset = -1; // Make one period with an offset compared
								// to ast

	String[] utc_timing_methods = new String[3];
	int start_nr = 0;

	String content_name;
	String base_url;
	String rel_path;
	String filename;
	ArrayList<DashMedia> reps = new ArrayList<>(); // An array
													// of
													// representations
													// with id,

	String ext; // File extension
	float seg_duration;
	int vod_first_segment_in_loop = 0;
	int vod_nr_segments_in_loop = 0;
	int vod_default_tsbd_secs = 0;
	long publish_time = 0;

	public ConfigProcessor(String b_url) {

		availability_start_time_in_s = DASHConstants.AVAILABILITY_START_TIME;
		base_url = b_url;

	}

	/* This method -->process() extracts the extention out of the file */
	public void process(String[] URL_parts) {
		// TODO Auto-generated method stub
		content_name = URL_parts[0];
		base_url = base_url + URL_parts[0] + "/";
		filename = URL_parts[URL_parts.length - 1];
		String[] tokens = filename.split("\\.(?=[^\\.]+$)");
		ext = "." + tokens[tokens.length - 1];
	}

	public String getExt() {
		// TODO Auto-generated method stub
		return ext;
	}

	public String getContentName() {
		return content_name;
	}

	public String getFileName() {
		return filename;
	}

	public void set_reps(DashMedia media) {
		// TODO Auto-generated method stub
		reps.add(media);
	}

	public ArrayList<DashMedia> get_reps() {
		// TODO Auto-generated method stub
		return reps;
	}

	public long getAvailability_start_time_in_s() {
		return availability_start_time_in_s;
	}

	public void setAvailability_start_time_in_s(long availability_start_time_in_s) {
		this.availability_start_time_in_s = availability_start_time_in_s;
	}

	public long getAvailability_time_offset_in_s() {
		return availability_time_offset_in_s;
	}

	public void setAvailability_time_offset_in_s(long availability_time_offset_in_s) {
		this.availability_time_offset_in_s = availability_time_offset_in_s;
	}

	public int getStart_nr() {
		return start_nr;
	}
}
