// Copyright (c) 2016 Triveni Digital, Inc. All rights reserved.

/*
 * DASHConstants holds all the constants that are desired to have a global scope and can be 
 * easily used and accessed accross classes and packages. 
 * 
 */

package DASHLibraries;

import java.util.List;

import com.coremedia.iso.IsoFile;

public class DASHConstants {

	public static final long AVAILABILITY_START_TIME = (System.currentTimeMillis() / 1000) * 1000;
	public static int port = 80;
	public static String hostname;
	public static String protocol;
	public static String base_URL;
	public static String utc_head_URL;
	public static String[] URL_parts;
	public static String content_path;
	public static long time_now;
	public static long time_now_float;
	public static long new_tfdt_value;
	public static ConfigProcessor config;
	public static float v_seg_duration;
	public static float a_seg_duration;
	public static int nr_a_seg_in_loop;
	public static int nr_v_seg_in_loop;
	public static String v_representation;
	public static String a_representation;
	public static long v_timescale;
	public static long a_timescale;
	public static int first_a_seg_in_loop;
	public static int first_v_seg_in_loop;
	public static String v_initialization;
	public static String a_initialization;
	public static String v_media;
	public static String a_media;
	public static String mp4_data;
	public static List<String> top_level_boxes;
	public static List<String> composite_boxes;
	public static int movie_timescale;
	public static boolean success;
	public static String v_dir_name;
	public static String a_dir_name;
	public static boolean audio_present;
	public static boolean video_present;
	public static boolean v_is_in_pool;
	public static boolean a_is_in_pool;
	public static String media_file;
	public static long seq_no = 0;
	public static long tfdt_offset;
	public static IsoFile iso;
}
