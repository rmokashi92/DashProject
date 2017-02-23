// Copyright (c) 2016 Triveni Digital, Inc. All rights reserved.

/*
 * DashProvider : Crux of the matter!
 * 
 * 
 */

package com.triveni.dashlive;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MediaBox;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentHeaderBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentBaseMediaDecodeTimeBox;
import com.googlecode.mp4parser.util.Path;

import DASHLibraries.ConfigProcessor;
import DASHLibraries.DASHConstants;

public class DashProvider {

	long baseMediaDecodeTime;
	long new_baseMediaDecodeTime; // bmdt after adding tfdt offset
	private long SECS_IN_DAY = 24 * 3600;
	private String DEFAULT_MINIMUM_UPDATE_PERIOD = "P100Y";
	private int DEFAULT_PUBLISH_ADVANCE_IN_S = 7200;
	private int EXTRA_TIME_AFTER_END_IN_S = 60;

	private String UTC_HEAD_PATH = "dash/time.txt";

	String videoBuffer = new String();
	String audioBuffer = new String();
	String Buffer_temp1 = new String();
	String Buffer_temp2 = new String();
	StringBuilder MPDBuffer = new StringBuilder();

	int start_mpd;
	int end_mpd;

	IsoFile iso;
	// Video MPD parameters
	String v_representation, v_mimiType, v_codecs, v_startNumber, v_timescale, v_duration, v_media, v_init;

	// Audio MPD parameters
	String a_representation, a_mimiType, a_codecs, a_startNumber, a_timescale, a_duration, a_media, a_init;

	public DashProvider(String hostname, String[] url_parts, String vodPath, long now) {
		// TODO Auto-generated constructor stub

		DASHConstants.protocol = "http";
		DASHConstants.content_path = vodPath;
		// base_url composition
		DASHConstants.base_URL = DASHConstants.protocol + "://" + hostname + ":" + DASHConstants.port + "/"
				+ DASHConstants.content_path + url_parts[1] + "/";
		DASHConstants.URL_parts = getURL(url_parts);

		DASHConstants.time_now = now;
		DASHConstants.time_now_float = now;
		DASHConstants.new_tfdt_value = 0;

	}

	// Extracts the path for manifest file from the URL into an array
	private String[] getURL(String[] url_parts2) {

		int length = url_parts2.length;
		String[] url_divs = new String[length - 2];
		int j = 0;
		for (int i = 2; i < length; i++) {
			url_divs[j] = url_parts2[i];
			j++;
		}
		return url_divs;
	}

	public String handle_req() {
		// TODO Auto-generated method stub
		// Forwarding the request so as to parse the url contents
		return parse_URL();

	}

	private String parse_URL() {
		// TODO Auto-generated method stub
		String mpd_filename = new String();
		ConfigProcessor config = new ConfigProcessor(DASHConstants.base_URL);
		// Gets the ext for the requested files
		config.process(DASHConstants.URL_parts);

		if (config.getExt().equals(".mpd")) {
			// Manifest file entry
			mpd_filename = DASHConstants.content_path + config.getContentName() + "/"
					+ config.getFileName().split("\\.(?=[^\\.]+$)")[0];

			input_data_MPD(mpd_filename + config.getExt());

			read_MPD(mpd_filename + config.getExt());

			get_MPD_data();
			get_config_parameters();
			String response = generate_dynamic_mpd();

			generate_reps(config);

			// Saving state of config in global scope
			DASHConstants.config = config;

			DASHConstants.v_initialization = v_init;
			DASHConstants.a_initialization = a_init;
			DASHConstants.v_media = v_media;
			DASHConstants.a_media = a_media;

			return response;

		}

		else if (config.getExt().equals(".mp4")) {

			generate_reps(config);

			DASHConstants.config = config;

			String response = process_init_segments();
			return response;

		}

		else if (config.getExt().equals(".m4s")) {

			System.out.println(DASHConstants.AVAILABILITY_START_TIME);
			System.out.println(System.currentTimeMillis());

			generate_reps(config);

			DASHConstants.config = config;

			long first_seg_ast = 0;
			if (DASHConstants.config.getAvailability_time_offset_in_s() == -1) {
				first_seg_ast = DASHConstants.AVAILABILITY_START_TIME;
			}
			if (DASHConstants.time_now < DASHConstants.AVAILABILITY_START_TIME) {
				DASHConstants.success = false;
				return "Request is too early";
			} else {
				String response = process_media_seg();
				return response;
			}
		} else {
			System.out.println(config.getExt());
			return null;
		}

	}

	private String process_media_seg() {
		// TODO Auto-generated method stub
		String seg_content = "";
		String rel_path;
		long seg_dur = (long) DASHConstants.v_seg_duration * 1000;
		String seg_name = DASHConstants.config.getFileName();
		String[] seg_parts;
		seg_parts = seg_name.split("\\.");
		String seg_base = seg_parts[0];
		String seg_ext = seg_parts[1];
		long timescale;
		seg_base = seg_base.replaceAll("[^0-9]+", "").trim();

		if (DASHConstants.URL_parts[DASHConstants.URL_parts.length - 2].equals(DASHConstants.v_dir_name)) {
			timescale = DASHConstants.v_timescale;
			rel_path = DASHConstants.v_dir_name;
		} else {
			timescale = DASHConstants.a_timescale;
			rel_path = DASHConstants.a_dir_name;
		}
		int seg_nr = Integer.parseInt(seg_base);
		int seg_start_nr = DASHConstants.config.getStart_nr();
		long seg_time = (seg_nr - seg_start_nr) * seg_dur + DASHConstants.AVAILABILITY_START_TIME;
		long seg_ast = seg_time + seg_dur;

		long seg_time_ast = seg_time - DASHConstants.AVAILABILITY_START_TIME;
		long loop_dur = seg_dur * DASHConstants.nr_v_seg_in_loop;
		long num_of_loops_done = seg_time_ast / loop_dur;
		long time_in_loop = seg_time_ast % loop_dur;
		long offset_at_loop_start = (num_of_loops_done * loop_dur) / 1000;
		long seg_nr_in_loop = time_in_loop / seg_dur;
		long vod_nr = seg_nr_in_loop + DASHConstants.first_v_seg_in_loop;
		int nr_reps = DASHConstants.config.get_reps().size();

		if (DASHConstants.seq_no == 0) {
			DASHConstants.seq_no = vod_nr;
		}

		if (nr_reps == 1) {
			seg_content = filterMediaSegment(seg_nr, seg_nr_in_loop, vod_nr, offset_at_loop_start, timescale, rel_path,
					seg_ext);
		}

		return seg_content;
	}

	private String filterMediaSegment(int seg_nr, long seg_nr_in_loop, long vod_nr, long offset_at_loop_start,
			long timescale, String rel_path, String seg_ext) {
		// TODO Auto-generated method stub
		DASHConstants.tfdt_offset = offset_at_loop_start * timescale;
		DASHConstants.media_file = DASHConstants.content_path + DASHConstants.URL_parts[0] + "/" + rel_path + "/"
				+ vod_nr + "." + seg_ext;

		DASHConstants.iso = null;
		try {
			DASHConstants.iso = new IsoFile(DASHConstants.media_file);

			MovieFragmentHeaderBox box = Path.getPath(DASHConstants.iso, "/moof[0]/mfhd[0]");
			box.setSequenceNumber(seg_nr);

			TrackFragmentBaseMediaDecodeTimeBox tfdtBox = Path.getPath(DASHConstants.iso, "/moof[0]/traf[0]/tfdt[0]");
			System.out.println(tfdtBox.getBaseMediaDecodeTime());
			baseMediaDecodeTime = tfdtBox.getBaseMediaDecodeTime();
			new_baseMediaDecodeTime = baseMediaDecodeTime + DASHConstants.tfdt_offset;
			tfdtBox.setBaseMediaDecodeTime(new_baseMediaDecodeTime);
			System.out.println("bmdt: = " + baseMediaDecodeTime);
			System.out.println("new_bmdt: = " + new_baseMediaDecodeTime);
			System.out.println("vod_nr = " + vod_nr);
			System.out.println("seq_nr = " + box.getSequenceNumber());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return DASHConstants.iso.toString();

	}

	private void generate_reps(ConfigProcessor config) {
		// TODO Auto-generated method stub
		// Generation of reps for media files
		config.get_reps().clear();

		if (DASHConstants.URL_parts.length > 2) { /// More than just a manifest
													/// file
			DashMedia Media = new DashMedia();
			String content = (DASHConstants.URL_parts[DASHConstants.URL_parts.length - 2]);
			if (content.equalsIgnoreCase("video")) {
				Media.setTimescale(DASHConstants.v_timescale);
				Media.setContent_media(DASHConstants.v_representation);
				Media.setRepresentation(DASHConstants.v_representation);
			} else {
				Media.setTimescale(DASHConstants.a_timescale);
				Media.setContent_media(DASHConstants.a_representation);
				Media.setRepresentation(DASHConstants.a_representation);
			}

			config.set_reps(Media);
		}

	}

	private String process_init_segments() {
		// TODO Auto-generated method stub
		String init_file = new String();
		int nr_reps = DASHConstants.config.get_reps().size();
		if (nr_reps == 1) {

			if (DASHConstants.URL_parts[DASHConstants.URL_parts.length - 2].equals(DASHConstants.v_dir_name)) {
				String[] v_init_parts;
				v_init_parts = DASHConstants.v_initialization.split("/");
				if (v_init_parts[0].equals("$RepresentationID$")) {
					DASHConstants.v_initialization = DASHConstants.v_dir_name + "/" + v_init_parts[1];
				}
				init_file = DASHConstants.content_path + DASHConstants.URL_parts[0] + "/"
						+ DASHConstants.v_initialization;
			}

			if (DASHConstants.URL_parts[DASHConstants.URL_parts.length - 2].equals(DASHConstants.a_dir_name)) {
				String[] a_init_parts;
				a_init_parts = DASHConstants.a_initialization.split("/");
				if (a_init_parts[0].equals("$RepresentationID$")) {
					DASHConstants.a_initialization = DASHConstants.a_dir_name + "/" + a_init_parts[1];
				}
				init_file = DASHConstants.content_path + DASHConstants.URL_parts[0] + "/"
						+ DASHConstants.a_initialization;
			}

			return liveFilterForMedia(init_file);
		}

		return null;
	}

	private String liveFilterForMedia(String init_file) {
		// TODO Auto-generated method stub
		try {

			iso = new IsoFile(init_file);

			init_MP4data();

			// Reads the data in binary format in python
			// MovieHeaderBox box = Path.getPath(iso, "/moov[0]/mvhd[0]");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return DASHConstants.mp4_data;
	}

	private void init_MP4data() {
		// TODO Auto-generated method stub
		try {
			MovieBox box = Path.getPath(iso, "/moov[0]");

			List<Box> boxes = iso.getBoxes();
			MediaBox mbox = Path.getPath(iso, "/moov[0]/trak[0]/mdia[0]");

			DASHConstants.top_level_boxes = new ArrayList<>();
			DASHConstants.top_level_boxes.add("moov");
			DASHConstants.composite_boxes = new ArrayList<>();
			DASHConstants.composite_boxes.add("moov");
			DASHConstants.composite_boxes.add("trak");
			DASHConstants.composite_boxes.add("mdia");
			DASHConstants.movie_timescale = -1;

			DASHConstants.mp4_data = iso.getByteBuffer(0, iso.getSize()).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void input_data_MPD(String fileName) {
		// TODO Auto-generated method stub
		// Creates a StringBuffer MPDBuffer for manipulating manifest and
		// modifying it
		try {
			File media_file = new File(fileName);
			Scanner input = new Scanner(media_file);
			while (input.hasNext()) {
				String nextToken = input.nextLine();
				MPDBuffer = MPDBuffer.append(nextToken);
				MPDBuffer.append("\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private String generate_dynamic_mpd() {
		// TODO Auto-generated method stub
		// Generates dynamic mpd for sending response

		if (!MPDBuffer.toString().contains("maxSegmentDuration")) {
			find_end_mpdtag();
			MPDBuffer.insert(end_mpd, " maxSegmentDuration = \"PT" + DASHConstants.v_seg_duration + "S\"");
		}

		if (!MPDBuffer.toString().contains("availabilityStartTime")) {
			find_end_mpdtag();
			MPDBuffer.insert(end_mpd,
					" availabilityStartTime=\""
							+ convert_to_xml_format(
									new Timestamp(new java.util.Date(DASHConstants.AVAILABILITY_START_TIME).getTime()))
							+ "\"");
		} else {
			int avail_ind = MPDBuffer.indexOf("availabilityStartTime") + "availabilityStartTime".length() + 2;
			int end_ind = MPDBuffer.indexOf("\"", avail_ind);
			MPDBuffer.replace(avail_ind, end_ind, convert_to_xml_format(
					new Timestamp(new java.util.Date(DASHConstants.AVAILABILITY_START_TIME).getTime())));
		}

		int per_index = MPDBuffer.indexOf("<Period") - 1;
		int baseurl_index = MPDBuffer.lastIndexOf(">", per_index) + 1;
		// int baseurl_index = MPDBuffer.indexOf(">", prog_info_index) + 1;
		MPDBuffer.insert(baseurl_index, "\n");
		MPDBuffer.insert(baseurl_index + 1,
				"<BaseURL>" + DASHConstants.base_URL + DASHConstants.URL_parts[0] + "/</BaseURL>");

		int type_index = MPDBuffer.indexOf("static");
		MPDBuffer.replace(type_index, type_index + 6, "dynamic");

		if (!MPDBuffer.toString().contains("publishTime")) {
			find_end_mpdtag();
			MPDBuffer.insert(end_mpd,
					" publishTime=\""
							+ convert_to_xml_format(
									new Timestamp(new java.util.Date(DASHConstants.AVAILABILITY_START_TIME).getTime()))
							+ "\"");
		} else {
			int avail_ind = MPDBuffer.indexOf("publishTime") + "publishTime".length() + 2;
			int end_ind = MPDBuffer.indexOf("\"", avail_ind);
			MPDBuffer.replace(avail_ind, end_ind, convert_to_xml_format(
					new Timestamp(new java.util.Date(DASHConstants.AVAILABILITY_START_TIME).getTime())));
		}

		/*
		 * 
		 * 
		 * Generate period data implementation is pending
		 * 
		 * 
		 * 
		 */

		return MPDBuffer.toString();
	}

	private String convert_to_xml_format(Timestamp timestamp) {
		StringBuilder time = new StringBuilder(timestamp.toString());
		time.replace(10, 11, "T");
		time.replace(19, 24, "Z");
		return time.toString();
	}

	private void find_end_mpdtag() {
		// TODO Auto-generated method stub
		start_mpd = MPDBuffer.indexOf("<MPD");
		end_mpd = MPDBuffer.indexOf(">", start_mpd);
	}

	private void get_config_parameters() {
		// Retrieves all the required parameters that were taken from the config
		// file
		// TODO Auto-generated method stub
		if (v_representation != null) {
			DASHConstants.v_representation = v_representation;
		}
		if (v_timescale != null) {
			DASHConstants.v_timescale = Long.parseLong(v_timescale);
		}

		if (a_representation != null) {
			DASHConstants.a_representation = a_representation;
		}
		if (a_timescale != null) {
			DASHConstants.a_timescale = Long.parseLong(a_timescale);
		}
		if (a_duration != null) {
			DASHConstants.a_seg_duration = Long.parseLong(a_duration) / DASHConstants.a_timescale;
		}
		if (v_duration != null) {
			DASHConstants.v_seg_duration = Long.parseLong(v_duration) / DASHConstants.v_timescale;
		}

		if (a_startNumber != null) {
			DASHConstants.first_a_seg_in_loop = Integer.parseInt(a_startNumber);
		} else {
			DASHConstants.first_a_seg_in_loop = 1;
		}
		if (v_startNumber != null) {
			DASHConstants.first_v_seg_in_loop = Integer.parseInt(v_startNumber);
		} else {
			DASHConstants.first_v_seg_in_loop = 1;
		}

		try {
			// new File(<directory path>).listFiles().length
			int no_of_v_files = new File(
					DASHConstants.content_path + DASHConstants.URL_parts[0] + "/" + DASHConstants.v_dir_name)
							.listFiles().length;
			DASHConstants.nr_v_seg_in_loop = no_of_v_files - 1;

			if (a_representation != null) {
				int no_of_a_files = new File(
						DASHConstants.content_path + DASHConstants.URL_parts[0] + "/" + DASHConstants.a_dir_name)
								.listFiles().length;
				DASHConstants.nr_a_seg_in_loop = no_of_a_files - 1;
			}
		} catch (Exception e) {
			System.out.println("Check if directory name is same as representation in MPD");
		}

	}

	private void get_MPD_data() {
		// TODO Auto-generated method stub
		// Getting the audio and video metadata
		// Using mimetype to identify type of media
		String mimetype1 = new String();
		String mimetype2 = new String();
		if (!Buffer_temp1.isEmpty()) {
			mimetype1 = getComponent("mimeType", Buffer_temp1);
		}
		if (!Buffer_temp2.isEmpty()) {
			mimetype2 = getComponent("mimeType", Buffer_temp2);
		}

		if (mimetype1 != null && mimetype1.contains("video")) {
			DASHConstants.video_present = true;
			videoBuffer = Buffer_temp1;
		}
		if (mimetype2 != null && mimetype2.contains("video")) {
			DASHConstants.video_present = true;
			videoBuffer = Buffer_temp2;
		}
		if (mimetype1 != null && mimetype1.contains("audio")) {
			DASHConstants.audio_present = true;
			audioBuffer = Buffer_temp1;
		}
		if (mimetype2 != null && mimetype2.contains("audio")) {
			DASHConstants.audio_present = true;
			audioBuffer = Buffer_temp2;
		}

		if (!videoBuffer.equals("")) {
			get_video_data();
		}
		if (!audioBuffer.equals("")) {
			get_audio_data();
		}

		if (v_media != null && v_media.contains("/")) {
			DASHConstants.v_is_in_pool = false;
		} else {
			DASHConstants.v_is_in_pool = true;
		}
		if (a_media != null && a_media.contains("/")) {
			DASHConstants.a_is_in_pool = false;
		} else {
			DASHConstants.a_is_in_pool = true;
		}

		get_directory_details();

	}

	private void get_directory_details() {
		// TODO Auto-generated method stub
		// Identifying the directory name for media files from manifest
		String[] v_parts, a_parts;
		if (!DASHConstants.v_is_in_pool) {
			v_parts = v_media.split("/");
			if (v_parts[0].equals("$RepresentationID$")) {
				DASHConstants.v_dir_name = v_representation;
			} else {
				DASHConstants.v_dir_name = v_parts[0];
			}

		} else {
			DASHConstants.v_dir_name = DASHConstants.URL_parts[DASHConstants.URL_parts.length - 2];
		}
		if (!DASHConstants.a_is_in_pool) {
			a_parts = a_media.split("/");
			if (a_parts[0].equals("$RepresentationID$")) {
				DASHConstants.a_dir_name = a_representation;
			} else {
				DASHConstants.a_dir_name = a_parts[0];
			}

		} else {
			DASHConstants.a_dir_name = DASHConstants.URL_parts[DASHConstants.URL_parts.length - 2];
		}
	}

	private void get_audio_data() {
		// TODO Auto-generated method stub
		if (audioBuffer.contains("<Representation")) {
			a_representation = getComponent("<Representation", audioBuffer);
		}
		if (audioBuffer.contains("mimeType")) {
			a_mimiType = getComponent("mimeType", audioBuffer);
		}
		if (audioBuffer.contains("codecs")) {
			a_codecs = getComponent("codecs", audioBuffer);
		}
		if (audioBuffer.contains("startNumber")) {
			a_startNumber = getComponent("startNumber", audioBuffer);
		}
		if (audioBuffer.contains("timescale")) {
			a_timescale = getComponent("timescale", audioBuffer);
		}
		if (audioBuffer.contains("duration")) {
			a_duration = getComponent("duration", audioBuffer);
		}
		if (audioBuffer.contains("media")) {
			a_media = getComponent("media", audioBuffer);
		}
		if (audioBuffer.contains("initialization")) {

			a_init = getComponent("initialization", audioBuffer);
		}
	}

	private void get_video_data() {
		// TODO Auto-generated method stub
		if (videoBuffer.contains("<Representation")) {
			v_representation = getComponent("<Representation", videoBuffer);
		}
		if (videoBuffer.contains("mimeType")) {
			v_mimiType = getComponent("mimeType", videoBuffer);
		}
		if (videoBuffer.contains("codecs")) {
			v_codecs = getComponent("codecs", videoBuffer);
		}
		if (videoBuffer.contains("startNumber")) {
			v_startNumber = getComponent("startNumber", videoBuffer);
		}
		if (videoBuffer.contains("timescale")) {
			v_timescale = getComponent("timescale", videoBuffer);
		}
		if (videoBuffer.contains("duration")) {
			v_duration = getComponent("duration", videoBuffer);
		}
		if (videoBuffer.contains("media")) {
			v_media = getComponent("media", videoBuffer);
		}
		if (videoBuffer.contains("initialization")) {
			v_init = getComponent("initialization", videoBuffer);
		}

	}

	// finds value of the attributes in MPD
	private String getComponent(String parameter, String Buffer) {
		// TODO Auto-generated method stub
		int v_repr_id = Buffer.indexOf(parameter);
		int first_occur = Buffer.indexOf("\"", v_repr_id);
		int second_occur = Buffer.indexOf("\"", first_occur + 1);
		return Buffer.substring(first_occur + 1, second_occur);
	}

	private void read_MPD(String fileName) {
		// TODO Auto-generated method stub
		// Parsing the manifest and retrieving the metadata for media
		int count = 0;

		try {
			File media_file = new File(fileName);
			Scanner input = new Scanner(media_file);

			while (input.hasNext()) {
				String nextToken = input.nextLine();
				String token = nextToken.trim();
				// System.out.println(nextToken);

				if (token.startsWith("<AdaptationSet") && Buffer_temp1.isEmpty()) {
					while (!token.startsWith("</AdaptationSet>")) {
						Buffer_temp1 = Buffer_temp1 + token;
						nextToken = input.nextLine();
						token = nextToken.trim();

					}
				}

				if (token.startsWith("<AdaptationSet")) {
					while (!token.startsWith("</AdaptationSet>")) {
						Buffer_temp2 = Buffer_temp2 + token;
						nextToken = input.nextLine();
						token = nextToken.trim();

					}
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
