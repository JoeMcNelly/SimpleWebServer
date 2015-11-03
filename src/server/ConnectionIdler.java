/*
 * ConnectionIdler.java
 * Nov 2, 2015
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

package server;

import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Chandan R. Rupakheti (rupakhcr@clarkson.edu)
 */
public class ConnectionIdler implements Runnable {
	private Server server;
	private Map<String, List<ConnectionHandler>> ipThreads;

	public ConnectionIdler(Server server) {
		this.server = server;
		HashMap<String, List<ConnectionHandler>> map = new HashMap<String, List<ConnectionHandler>>();
		this.ipThreads = Collections.synchronizedMap(map);
	}

	@Override
	public void run() {
		while (true) {
			if (this.server.getRunningConnections() < this.server.MAX_RUNNING_CONNECTIONS
					&& this.server.hasWaitingConnections()) {
				Socket s = this.server.getWaitingConnection();
				this.server.increaseRunningConnections();
				System.out.println("Connection taken from queue");
				ConnectionHandler handler = new ConnectionHandler(server, s);
				String ipAddress = s.getInetAddress().toString();
				if (!ipThreads.containsKey(ipAddress)) {
					List<ConnectionHandler> list = Collections.synchronizedList(new LinkedList<ConnectionHandler>());
					ipThreads.put(ipAddress, list);
				}
				ipThreads.get(ipAddress).add(handler);
				new Thread(handler).start();

			}
		}

	}

	public void throttleConnections(String ipAddress) {
		if (ipThreads.containsKey(ipAddress)) {
			for (ConnectionHandler handle : ipThreads.get(ipAddress)) {
				handle.throttle();
			}
		}
	}

	public void connectionDone(ConnectionHandler handle) {
		String ipAddress = handle.getSocket().getInetAddress().toString();
		if(ipThreads.containsKey(ipAddress)) {
			ipThreads.get(ipAddress).remove(handle);
		}
	}

}
