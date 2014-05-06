/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.watchtower.deployer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

/**
 * <p>Title: DeploymentWatchService</p>
 * <p>Description: A centralized watch service singleton to process file change events in the deployment directory</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.deployer.DeploymentWatchService</code></p>
 */

public class DeploymentWatchService {
	/** A map of registered watch services */
	private static final NonBlockingHashMap<Path, WatchService> watchServices = new NonBlockingHashMap<Path, WatchService>();
	
	/**
	 * Acquires a watch service for the passed path
	 * @param path The path to get a watch service for
	 * @return the watch service for the passed path, or its parent (FIXME:)
	 */
	@SuppressWarnings("resource")
	public static WatchService getWatchService(final Path path) {
		if(path==null) throw new IllegalArgumentException("The passed Path was null");
		WatchService ws = watchServices.get(path);
		if(ws == null) {
			synchronized(watchServices) {
				ws = watchServices.get(path);
				if(ws == null) {
					try {
						final WatchService _ws = path.getFileSystem().newWatchService();
						// A delegate wrapping watch service that pulls the service from
						// the watch service map when it closes.
						ws = new WatchService() {
							private final WatchService delegate = _ws;
							private final Path wspath = path;
							
							public void close() throws IOException {
								delegate.close();
								watchServices.remove(wspath);
							}
							public WatchKey poll() {
								return delegate.poll();
							}
							public WatchKey poll(long timeout, TimeUnit unit)
									throws InterruptedException {
								return delegate.poll(timeout, unit);
							}
							public WatchKey take() throws InterruptedException {
								return delegate.take();
							}							
						};						
						watchServices.put(path, ws);
						return ws;
					} catch (IOException e) {
						throw new RuntimeException(e);						
					}
				}
			}
		}		
		return ws;
	}
	
	/**
	 * Creates a new DeploymentWatchService
	 */
	private DeploymentWatchService() {
		
	}

}
