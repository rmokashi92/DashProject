// Copyright (c) 2016 Triveni Digital, Inc. All rights reserved.

/*
 * 
 * DashMedia saves the media related data for saving metadata from metadata
 * 
 */

package com.triveni.dashlive;

public class DashMedia {

	private String representation;
	private long timescale;
	private String content_media;

	public String getRepresentation() {
		return representation;
	}

	public void setRepresentation(String representation) {
		this.representation = representation;
	}

	public long getTimescale() {
		return timescale;
	}

	public void setTimescale(long timescale) {
		this.timescale = timescale;
	}

	public String getContent_media() {
		return content_media;
	}

	public void setContent_media(String content_media) {
		this.content_media = content_media;
	}

}
