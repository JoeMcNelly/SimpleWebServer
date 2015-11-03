/*
 * ThrottledOutputStream.java
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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 
 * @author Chandan R. Rupakheti (rupakhcr@clarkson.edu)
 */
public class ThrottledOutputStream extends FilterOutputStream{

	private OutputStream innerStream;
	private long maxBps;
	private long numBytes;
	private long start;
	
	
	public ThrottledOutputStream(OutputStream out) {
		super(out);
		innerStream = out;
		maxBps = 10;
		numBytes = 0;
		start = System.currentTimeMillis();
	}
	@Override
	public void write(byte[] bytes) throws IOException{
		write(bytes, 0, bytes.length);
	}
	
	@Override
	public void write(int b) throws IOException {
		byte[] oneByteArray = new byte[1];
		oneByteArray[0] = (byte) b;
		write(oneByteArray, 0, 1);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException{
		numBytes += len;
		long elapsed = Math.max(System.currentTimeMillis() - start, 1);

		long bps = numBytes * 1000L / elapsed;
		if (bps > maxBps) {
			//sending too fast, sleep.
			long wakeElapsed = numBytes * 1000L / maxBps;
			try {
				Thread.sleep(wakeElapsed - elapsed);
			} catch (InterruptedException ignore) {
			}
		}

		// Write the bytes.
		out.write(b, off, len);
	}

}
