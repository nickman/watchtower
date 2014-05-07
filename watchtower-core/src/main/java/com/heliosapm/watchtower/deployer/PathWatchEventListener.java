/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2014, Helios Development Group and individual contributors
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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;

/**
 * <p>Title: PathWatchEventListener</p>
 * <p>Description: Defines a path based watch event listener</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.deployer.PathWatchEventListener</code></p>
 */

public interface PathWatchEventListener {
	/**
	 * Fired when a created directory path event occurs that this listener is subscribed to
	 * @param createdDir The created directory
	 */
	public void onDirectoryCreated(File createdDir);
	
	/**
	 * Fired when a deleted directory path event occurs that this listener is subscribed to
	 * @param deletedDir The deleted directory
	 */
	public void onDirectoryDeleted(File deletedDir);
	
	/**
	 * Fired when a modified directory path event occurs that this listener is subscribed to
	 * @param modifiedDir The modified directory
	 */
	public void onDirectoryModified(File modifiedDir);
	
	/**
	 * Fired when a created file path event occurs that this listener is subscribed to
	 * @param createdFile The created file
	 */
	public void onFileCreated(File createdFile);
	
	/**
	 * Fired when a deleted file path event occurs that this listener is subscribed to
	 * @param deletedFile The deleted file
	 */
	public void onFileDeleted(File deletedFile);
	
	/**
	 * Fired when a modified file path event occurs that this listener is subscribed to
	 * @param modifiedFile The modified file
	 */
	public void onFileModified(File modifiedFile);
	
	
	/**
	 * Called when the watched key is cancelled
	 * @param canceledWatchKey the cancelled watch key
	 */
	public void onCancel(WatchKey canceledWatchKey);
	
	/**
	 * Called on an overflow on processing events for the watch key this listener is listening on
	 * @param overflow the overflow event
	 */
	public void onOverflow(WatchEvent<Path> overflow);
}
