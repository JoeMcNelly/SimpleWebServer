/*
 * DefaultPlugin.java
 * Oct 26, 2015
 *
 * Simple Web Server (SWS) for EE407/507 and CS455/555
 * 
 * Copyright (C) 2011 Chandan Raj Rupakheti, Clarkson University
 * 
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either 
 * version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl.html>.
 * 
 * Contact Us:
 * Chandan Raj Rupakheti (rupakhcr@clarkson.edu)
 * Department of Electrical and Computer Engineering
 * Clarkson University
 * Potsdam
 * NY 13699-5722
 * http://clarkson.edu/~rupakhcr
 */
 
package Plugin;

import java.util.HashMap;
import java.util.Map;

import protocol.HttpRequest;
import protocol.HttpResponse;
import Servlet.DefaultServlet;
import Servlet.IServlet;

/**
 * 
 * @author Chandan R. Rupakheti (rupakhcr@clarkson.edu)
 */
public class DefaultPlugin implements IPlugin{

	private Map<String, IServlet> servlets;
	
	public DefaultPlugin() {
		servlets = new HashMap<String, IServlet>();
		servlets.put("MyServlet", new DefaultServlet());
	}

	/* (non-Javadoc)
	 * @see Plugin.IPlugin#handle(protocol.HttpRequest)
	 */
	@Override
	public HttpResponse handle(HttpRequest request, String rootDir) {
		String uri = request.getUri();
		String[] splitURI = uri.split("/");
		return servlets.get(splitURI[2]).handle(request,rootDir);
	}
	

}
