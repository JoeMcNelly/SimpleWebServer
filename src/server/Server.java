/*
 * Server.java
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

import gui.WebServer;
import jarloader.DirectoryWatcher;
import jarloader.JarLoader;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import protocol.DeleteRequestHandler;
import protocol.GetRequestHandler;
import protocol.IRequestHandler;
import protocol.PostRequestHandler;
import protocol.Protocol;
import protocol.PutRequestHandler;
import Plugin.IPlugin;

/**
 * This represents a welcoming server for the incoming TCP request from a HTTP
 * client such as a web browser.
 * 
 * @author Chandan R. Rupakheti (rupakhet@rose-hulman.edu)
 */
public class Server implements Runnable {
	private String rootDirectory;
	private int port;
	private boolean stop;
	private ServerSocket welcomeSocket;
	private ConnectionIdler idler;
	private long connections;
	private long serviceTime;
	private DirectoryWatcher watcher;
	private WebServer window;
	private int runningConnections;
	public static final int MAX_RUNNING_CONNECTIONS = 30;
	public static final int BLACKLISTING_LIMIT = 100;
	public static final int THROTTLING_LIMIT = 25;
	private Map<String, IPlugin> plugins;
	private Map<String, IRequestHandler> requestMap;
	private ArrayList<String> blackListedIPs;
	private ArrayList<String> throttledIPs;
	private HashMap<String, Integer> ipOccurrences;
	public List<Socket> waitingConnections;

	/**
	 * @param rootDirectory
	 * @param port
	 */
	public Server(String rootDirectory, int port, WebServer window) {
		this.rootDirectory = rootDirectory;
		this.port = port;
		this.stop = false;
		this.connections = 0;
		this.serviceTime = 0;
		this.window = window;
		this.plugins = new HashMap<String, IPlugin>();
		this.requestMap = new HashMap<String, IRequestHandler>();
		this.requestMap.put(Protocol.GET, new GetRequestHandler());
		this.requestMap.put(Protocol.POST, new PostRequestHandler());
		this.requestMap.put(Protocol.PUT, new PutRequestHandler());
		this.requestMap.put(Protocol.DELETE, new DeleteRequestHandler());
		this.blackListedIPs = new ArrayList<String>();
		this.throttledIPs = new ArrayList<String>();
		this.ipOccurrences = new HashMap<String, Integer>();
		this.watcher = new DirectoryWatcher(this);
		this.waitingConnections = Collections
				.synchronizedList(new LinkedList<Socket>());
		this.runningConnections = 0;
		Thread t = new Thread(this.watcher);
		t.start();
		initPlugins();
	}

	public IPlugin getPlugin(String URI) {
		try {
			return plugins.get(URI);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public IRequestHandler getRequestHandler(String method) {
		try {
			return requestMap.get(method);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Gets the root directory for this web server.
	 * 
	 * @return the rootDirectory
	 */
	public String getRootDirectory() {
		return rootDirectory;
	}

	/**
	 * Gets the port number for this web server.
	 * 
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Returns connections serviced per second. Synchronized to be used in
	 * threaded environment.
	 * 
	 * @return
	 */
	public synchronized double getServiceRate() {
		if (this.serviceTime == 0)
			return Long.MIN_VALUE;
		double rate = this.connections / (double) this.serviceTime;
		rate = rate * 1000;
		return rate;
	}

	/**
	 * Increments number of connection by the supplied value. Synchronized to be
	 * used in threaded environment.
	 * 
	 * @param value
	 */
	public synchronized void incrementConnections(long value) {
		this.connections += value;
	}

	/**
	 * Increments the service time by the supplied value. Synchronized to be
	 * used in threaded environment.
	 * 
	 * @param value
	 */
	public synchronized void incrementServiceTime(long value) {
		this.serviceTime += value;
	}

	/**
	 * The entry method for the main server thread that accepts incoming TCP
	 * connection request and creates a {@link ConnectionHandler} for the
	 * request.
	 */
	public void run() {
		
		this.idler = new ConnectionIdler(this);
		Thread t = new Thread(idler);
		t.start();

		try {
			this.welcomeSocket = new ServerSocket(port);

			// Now keep welcoming new connections until stop flag is set to true
			while (true) {
				// Listen for incoming socket connection
				// This method block until somebody makes a request
				Socket connectionSocket = this.welcomeSocket.accept();

				// Come out of the loop if the stop flag is set
				if (this.stop)
					break;
				// Create a handler for this incoming connection and start the
				// handler in a new thread
				String ipAddress = connectionSocket.getInetAddress().toString();
				addIPAddress(ipAddress);

				if (!isBlackListed(ipAddress)) {
					if (!isThrottled(ipAddress)) {
						this.waitingConnections.add(connectionSocket);
						System.out.println("Connection added to queue");
					}else {
						this.waitingConnections.add(connectionSocket);
						System.out.println("throttled connection added");
						connectionSocket.setPerformancePreferences(0, 0, 10);
						this.idler.throttleConnections(ipAddress);
					}
				} else {
					System.out.println("blackIp");
				}
			}
			this.welcomeSocket.close();
		} catch (Exception e) {
			window.showSocketException(e);
		}
	}


	/**
	 * Stops the server from listening further.
	 */
	public synchronized void stop() {
		if (this.stop)
			return;

		// Set the stop flag to be true
		this.stop = true;
		try {
			// This will force welcomeSocket to come out of the blocked accept()
			// method
			// in the main loop of the start() method
			Socket socket = new Socket(InetAddress.getLocalHost(), port);

			// We do not have any other job for this socket so just close it
			socket.close();
		} catch (Exception e) {
		}
	}

	/**
	 * Checks if the server is stopeed or not.
	 * 
	 * @return
	 */
	public boolean isStoped() {
		if (this.welcomeSocket != null)
			return this.welcomeSocket.isClosed();
		return true;
	}

	public boolean addPlugin(String s) {
		JarLoader loader = new JarLoader("./plugins/" + s);
		String newString = s.substring(0, s.length() - 4);

		Class clazz;
		try {
			clazz = loader.loadClass(newString, true);
			Object o = clazz.newInstance();
			if (o instanceof IPlugin) {
				IPlugin pluginClass = (IPlugin) o;
				this.plugins.put(s.replace(".jar", ""), pluginClass);
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public void removePlugin(String filename) {
		// TODO: do something???
	}

	private void initPlugins() {
		File pluginDir = new File("./plugins/");
		File[] plugins = pluginDir.listFiles();
		for (File plugin : plugins) {
			System.out.println("Trying to add " + plugin.getName());
			boolean success = addPlugin(plugin.getName());
			System.out.println(success ? "Added " + plugin.getName() + "!"
					: plugin.getName() + " was not loaded, JAR was misformed.");
		}
	}

	public void addBlackListedIP(String ip) {
		this.blackListedIPs.add(ip);
	}

	public boolean isBlackListed(String ip) {
		return this.blackListedIPs.contains(ip);
	}

	/**
	 * @param ipAddress
	 */
	private void addIPAddress(String ipAddress) {
		System.out.println("add ipAddress: " + ipAddress);
		if (this.ipOccurrences.containsKey(ipAddress)) {
			if (ipOccurrences.get(ipAddress) == BLACKLISTING_LIMIT) {
				addBlackListedIP(ipAddress);
			} else if (ipOccurrences.get(ipAddress) == THROTTLING_LIMIT) {
				addThrottledIP(ipAddress);
				this.ipOccurrences.put(ipAddress, this.ipOccurrences.get(ipAddress) + 1);
			} else {
				this.ipOccurrences.put(ipAddress,
						this.ipOccurrences.get(ipAddress) + 1);
			}
		} else {
			this.ipOccurrences.put(ipAddress, 1);
		}
	}

	/**
	 * @param string
	 */
	public void decreasingIPOccurrences(String ipAddress) {
		this.ipOccurrences
				.put(ipAddress, this.ipOccurrences.get(ipAddress) - 1);

	}

	public void increaseRunningConnections() {
		this.runningConnections++;
	}

	public void decreaseRunningConnections() {
		this.runningConnections--;
	}

	public int getRunningConnections() {
		return this.runningConnections;
	}

	public Socket getWaitingConnection() {
		return this.waitingConnections.remove(0);
	}

	public boolean hasWaitingConnections() {
		return !this.waitingConnections.isEmpty();
	}

	/**
	 * @param ipAddress
	 */
	private void addThrottledIP(String ipAddress) {
		this.throttledIPs.add(ipAddress);

	}
	
	/**
	 * @param ipAddress
	 * @return
	 */
	private boolean isThrottled(String ipAddress) {
		
		return this.throttledIPs.contains(ipAddress);
	}

	public void handleFinishedConnection(ConnectionHandler handle) {
		String ipAddress = handle.getSocket().getInetAddress().toString();
		decreasingIPOccurrences(ipAddress);
		decreaseRunningConnections();
		this.idler.connectionDone(handle);
	}

}
