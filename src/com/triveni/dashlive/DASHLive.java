// Copyright (c) 2016 Triveni Digital, Inc. All rights reserved.

package com.triveni.dashlive;

import java.io.File;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import DASHLibraries.DASHConstants;

public class DASHLive {
	public static final String VOD_PATH = "vod/";

	public final Tomcat embeddedTomcat;

	public DASHLive() throws Exception {
		embeddedTomcat = new Tomcat();
		embeddedTomcat.setBaseDir("tomcat/");
		embeddedTomcat.getConnector().setPort(DASHConstants.port);

		final StandardContext ctx = (StandardContext) embeddedTomcat.addWebapp("",
				new File("tomcat/").getAbsolutePath());
		final File additionWebInfClasses = new File("bin/com/triveni/dashlive/servlets/");
		final WebResourceRoot resources = new StandardRoot(ctx);
		resources.addPreResources(
				new DirResourceSet(resources, "/WEB-INF/classes", additionWebInfClasses.getAbsolutePath(), "/"));
		ctx.setResources(resources);

		embeddedTomcat.start();
		System.out.println("Waiting for requests at" + embeddedTomcat.getHost().toString() + ":"
				+ embeddedTomcat.getConnector().getPort());
		final String host = embeddedTomcat.getHost().toString();
		DASHLibraries.DASHConstants.hostname = host.substring(36, host.length() - 1);
		embeddedTomcat.getServer().await();
	}

	public static void main(final String args[]) throws Exception {
		new DASHLive();
	}
}
