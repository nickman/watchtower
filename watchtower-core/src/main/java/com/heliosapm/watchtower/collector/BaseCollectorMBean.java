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

import com.heliosapm.watchtower.component.StdServerComponentMBean;

/**
 * <p>Title: BaseCollectorMBean</p>
 * <p>Description: Defines the base collector operations, events and lifecycle</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.collector.BaseCollectorMBean</code></p>
 */

public interface BaseCollectorMBean extends StdServerComponentMBean {
	/**
	 * Starts the collector bean
	 */
	public void start();
	/**
	 * Stops the collector bean
	 */
	public void stop();
	/**
	 * Pauses the collector bean
	 */
	public void pause();
	/**
	 * Initializes the collector bean
	 */
	public void init();
	
	/**
	 * Triggers a collect and trace on the collector bean
	 * @param collectArgs The collection arguments
	 */
	public void collect(Object...collectArgs);
	
	/**
	 * Returns the current state name of the bean
	 * @return the current state name of the bean
	 */
	public String getState();
	
	
	
//	public void connect(Object...connectArgs);
//	public long getFrequency();
//	public long getRetryFrequency();
//	public int getMaxRetries();
//	public long getAverageCollectTime();
//	public long getLastCollectTime();
//	
//	public long getCollectErrors();
//	public long getConnectErrors();
	
	/**
	 * Base:
	 * =====
	 * start
	 * stop
	 * pause
	 * init
	 * state
	 * collect stats
	 * error stats
	 * 
	 * Connecting:
	 * ==========
	 * connect
	 * disconnect
	 * connect stats
	 * connect error stats
	 * 
	 * FScheduled:
	 * ===========
	 * getFrequency
	 * 
	 * CScheduled:
	 * ==========
	 * getCron
	 * --- recovery ---
	 * 
	 * Evented:
	 * =======
	 * getHotEvents
	 * 
	 * Retrying:
	 * ========
	 * getRetryFrequency
	 * getMaxRetries
	 * 
	 * Error Types:
	 * ===========
	 * connect
	 * collect
	 * trace
	 * compile
	 * 
	 * States:
	 * ======
	 * INIT
	 * OK
	 * PAUSED
	 * STOPPED
	 * COLLECTING
	 * BROKEN (bad config)
	 * BUST (bad script/compile error)
	 * 
	 * 
	 */
	
	
}
