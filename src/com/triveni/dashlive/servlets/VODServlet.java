// Copyright (c) 2016 Triveni Digital, Inc. All rights reserved.

package com.triveni.dashlive.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.triveni.dashlive.DASHLive;

import DASHLibraries.DASHConstants;
import DASHLibraries.DASHProxy;

@WebServlet(urlPatterns = { "/vod/*" }, initParams = { @WebInitParam(name = "VOD_PATH", value = DASHLive.VOD_PATH) })
public class VODServlet extends HttpServlet {
	private static final long serialVersionUID = -5068455595314685265L;
	private static final Logger log = Logger.getLogger(VODServlet.class.getName());
	private String hostname;
	private String url;
	private String[] url_parts;
	private String ext;
	private long now = System.currentTimeMillis();
	String response_dash;
	boolean is_audio_Initialized = false;
	int start_seg;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ServletOutputStream out = resp.getOutputStream();
		String path = req.getPathInfo();
		System.out.println(path);
		// Added for getting the constants required for application
		// initialization
		hostname = DASHLibraries.DASHConstants.hostname;
		// url = path.substring(12);
		url_parts = path.split("/");
		now = System.currentTimeMillis();
		int ext_detector = url_parts[(url_parts.length - 1)].indexOf(".");
		ext = url_parts[(url_parts.length - 1)].substring(ext_detector);
		DASHConstants.success = true;

		try {
			response_dash = DASHProxy.handleRequest(hostname, url_parts, DASHLive.VOD_PATH, now);

			if (ext.equals(".mpd")) {
				byte[] bytes = response_dash.getBytes();
				resp.setContentType("application/dash+xml");
				// resp.setHeader("Content-Length",
				// Integer.toString(response_dash.length()));
				out.write(bytes);
			}

			else {
				File realFile = null;

				String file_path = new String();
				if (ext.equals(".m4s")) {
					file_path = DASHConstants.media_file;
					realFile = new File(file_path);
					if (realFile.exists()) {
						if (url_parts[url_parts.length - 2].equals(DASHConstants.v_dir_name)) {
							resp.setContentType("video/iso.segment");
						}
					}

					DASHConstants.iso.writeContainer(Channels.newChannel(out));
				}

				if (ext.equals(".mp4")) {
					file_path = getInitParameter("VOD_PATH") + stringify_path();
					realFile = new File(file_path);
					if (realFile.exists()) {
						if (url_parts[url_parts.length - 2].equals(DASHConstants.v_dir_name)) {
							resp.setContentType("video/mp4");
						}

						FileInputStream in = null;
						try {
							in = new FileInputStream(realFile);

							IOUtils.copy(in, out);
						} catch (IOException ioe) {
							log.log(Level.WARNING, "IOException with " + realFile.getAbsolutePath(), ioe);
						} finally {
							IOUtils.closeQuietly(in);
						}

					}
				}

			}
			// Setting headers for response
			resp.setHeader("Pragma", "no-cache");
			resp.setHeader("Cache-Control", "no-cache");
			resp.setHeader("Expires", "-1");
			resp.setHeader("Access-Control-Allow-Headers", "origin,range,accept-encoding,referer");
			resp.setHeader("Access-Control-Allow-Methods", "GET,HEAD,OPTIONS");
			resp.setHeader("Access-Control-Allow-Origin", "*");
			resp.setStatus(HttpServletResponse.SC_OK);

			if (response_dash == null) {
				System.out.println("Invalid file extension");
			}

		} catch (Exception e) {
			DASHConstants.success = false;
			e.printStackTrace();
		}

		IOUtils.closeQuietly(out);
	}

	private String stringify_path() {
		String path = new String();
		for (int i = 0; i < DASHConstants.URL_parts.length; i++) {
			path = path + DASHConstants.URL_parts[i] + "/";
		}
		return path;
	}

}
