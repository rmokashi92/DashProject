// Copyright (c) 2016 Triveni Digital, Inc. All rights reserved.

package DASHLibraries;

import com.triveni.dashlive.DashProvider;

public class DASHProxy {

	public static String handleRequest(String hostname, String[] url_parts, String vodPath, long now) {
		// TODO Auto-generated method stub
		DashProvider dash_provider = new DashProvider(hostname, url_parts, vodPath, now);
		return dash_provider.handle_req();
	}

}
