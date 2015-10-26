/*
 * DefaultServlet.java
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
 
package Servlet;

import java.util.HashMap;
import java.util.Map;

import protocol.DeleteRequestHandler;
import protocol.GetRequestHandler;
import protocol.HttpRequest;
import protocol.HttpResponse;
import protocol.HttpResponseFactory;
import protocol.IRequestHandler;
import protocol.PostRequestHandler;
import protocol.Protocol;
import protocol.PutRequestHandler;

/**
 * 
 * @author Chandan R. Rupakheti (rupakhcr@clarkson.edu)
 */
public class DefaultServlet implements IServlet{

	private Map<String, IRequestHandler> handlers;
	
	public DefaultServlet() {
		handlers = new HashMap<String, IRequestHandler>();
		handlers.put(Protocol.GET, new GetRequestHandler());
		handlers.put(Protocol.POST, new PostRequestHandler());
		handlers.put(Protocol.PUT, new PutRequestHandler());
		handlers.put(Protocol.DELETE, new DeleteRequestHandler());
	}

	/* (non-Javadoc)
	 * @see Servlet.IServlet#handle(protocol.HttpRequest, java.lang.String)
	 */
	@Override
	public HttpResponse handle(HttpRequest request, String rootDir) {
		try{
			return handlers.get(request.getMethod()).handleRequest(request, rootDir);
		}catch(Exception e){
			return HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
		}
	}

	
	

}