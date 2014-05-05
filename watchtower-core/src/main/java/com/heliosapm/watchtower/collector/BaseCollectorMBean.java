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
package com.heliosapm.watchtower.collector;

/**
 * <p>Title: BaseCollectorMBean</p>
 * <p>Description: Defines the base collector operations, events and lifecycle</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.collector.BaseCollectorMBean</code></p>
 */

public interface BaseCollectorMBean {
	public void start();
	public void stop();
	public void pause();
	public void init();
	public void connect();
	public void collect();
	
	public long getFrequency();
	public long getRetryFrequency();
	public int getMaxRetries();
	public long getAverageCollectTime();
	public long getLastCollectTime();
	public String getState();
	public long getCollectErrors();
	public long getConnectErrors();
	
	
}
