/*
 * ConnectionHandler.java
 * Oct 7, 2012
 *
 * Simple Web Server (SWS) for CSSE 477
 * 
 * Copyright (C) 2012 Chandan Raj Rupakheti
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
 */

package server;

import jarloader.JarLoader;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
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
import protocol.ProtocolException;
import protocol.PutRequestHandler;
import Plugin.IPlugin;

/**
 * This class is responsible for handling a incoming request by creating a
 * {@link HttpRequest} object and sending the appropriate response be creating a
 * {@link HttpResponse} object. It implements {@link Runnable} to be used in
 * multi-threaded environment.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class ConnectionHandler implements Runnable {
	private Server server;
	private Socket socket;
	private Map<String, IRequestHandler> requestMap = new HashMap<String, IRequestHandler>();

	public ConnectionHandler(Server server, Socket socket) {
		this.server = server;
		this.socket = socket;
		this.requestMap.put(Protocol.GET, new GetRequestHandler());
		this.requestMap.put(Protocol.POST, new PostRequestHandler());
		this.requestMap.put(Protocol.PUT, new PutRequestHandler());
		this.requestMap.put(Protocol.DELETE, new DeleteRequestHandler());
	}

	/**
	 * @return the socket
	 */
	public Socket getSocket() {
		return socket;
	}

	/**
	 * The entry point for connection handler. It first parses incoming request
	 * and creates a {@link HttpRequest} object, then it creates an appropriate
	 * {@link HttpResponse} object and sends the response back to the client
	 * (web browser).
	 */
	public void run() {
		// Get the start time
		long start = System.currentTimeMillis();

		InputStream inStream = null;
		OutputStream outStream = null;

		try {
			inStream = this.socket.getInputStream();
			outStream = this.socket.getOutputStream();
		} catch (Exception e) {
			// Cannot do anything if we have exception reading input or output
			// stream
			// May be have text to log this for further analysis?
			e.printStackTrace();

			// Increment number of connections by 1
			server.incrementConnections(1);
			// Get the end time
			long end = System.currentTimeMillis();
			this.server.incrementServiceTime(end - start);
			return;
		}

		// At this point we have the input and output stream of the socket
		// Now lets create a HttpRequest object
		HttpRequest request = null;
		HttpResponse response = null;
		try {
			request = HttpRequest.read(inStream);
			System.out.println(request);

			response = requestMap.get(request.getMethod()).handleRequest(
					request, server.getRootDirectory());

			if (response.getPhrase().equals(Protocol.NOT_FOUND_TEXT)) {
				try {
					// Fill in the code to create a response for version
					// mismatch.
					// You may want to use constants such as Protocol.VERSION,
					// Protocol.NOT_SUPPORTED_CODE, and more.
					// You can check if the version matches as follows
					if (!request.getVersion()
							.equalsIgnoreCase(Protocol.VERSION)) {
						// Here you checked that the "Protocol.VERSION" string
						// is not equal to the
						// "request.version" string ignoring the case of the
						// letters in both strings
						// TODO: Fill in the rest of the code here
					}
					String[] URIparts = request.getUri().split("/");
					String URI = URIparts[1];
					File file = new File(this.server.getRootDirectory() + URI);
					if (file.exists()) {
						JarLoader loader = new JarLoader("./web/" + URI
								+ ".jar");
						Class clazz;
						try {
							clazz = loader.loadClass(URI, true);
							Object o = clazz.newInstance();
							if (o instanceof IPlugin) {
								IPlugin pluginClass = (IPlugin) o;
								response = ((IPlugin) o).handle(request,
										this.server.getRootDirectory());

							}
						} catch (ClassNotFoundException
								| InstantiationException
								| IllegalAccessException e1) {
							e1.printStackTrace();
						}

					}else {
						String location = this.server.getRootDirectory() + System.getProperty("file.separator") + "404message.txt" + System.getProperty("file.separator");
						file = new File(location);
						response = HttpResponseFactory.create200OK(file, Protocol.CLOSE);
					}
				} catch (Exception e) {
					e.printStackTrace();
					response = HttpResponseFactory
							.create400BadRequest(Protocol.CLOSE);
				}

			}

		} catch (ProtocolException pe) {
			// We have some sort of protocol exception. Get its status code and
			// create response
			// We know only two kind of exception is possible inside
			// fromInputStream
			// Protocol.BAD_REQUEST_CODE and Protocol.NOT_SUPPORTED_CODE
			int status = pe.getStatus();
			if (status == Protocol.BAD_REQUEST_CODE) {
				response = HttpResponseFactory
						.create400BadRequest(Protocol.CLOSE);
			}
			// TODO: Handle version not supported code as well
		} catch (Exception e) {
			e.printStackTrace();
			// For any other error, we will create bad request response as well
			response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
		}

		if (response != null) {
			// Means there was an error, now write the response object to the
			// socket
			try {
				response.write(outStream);
				// System.out.println(response);
			} catch (Exception e) {
				// We will ignore this exception
				e.printStackTrace();
			}

			// Increment number of connections by 1
			server.incrementConnections(1);
			// Get the end time
			long end = System.currentTimeMillis();
			this.server.incrementServiceTime(end - start);
			return;
		}

		// We reached here means no error so far, so lets process further

		// TODO: So far response could be null for protocol version mismatch.
		// So this is a temporary patch for that problem and should be removed
		// after a response object is created for protocol version mismatch.
		if (response == null) {
			response = HttpResponseFactory.create400BadRequest(Protocol.CLOSE);
		}

		try {
			// Write response and we are all done so close the socket
			response.write(outStream);
			// System.out.println(response);
			socket.close();
		} catch (Exception e) {
			// We will ignore this exception
			e.printStackTrace();
		}

		// Increment number of connections by 1
		server.incrementConnections(1);
		// Get the end time
		long end = System.currentTimeMillis();
		this.server.incrementServiceTime(end - start);
	}
}
