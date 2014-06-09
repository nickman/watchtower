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
package com.heliosapm.watchtower.core;

import org.helios.jmx.concurrency.JMXManagedThreadPool;
import org.helios.jmx.util.helpers.JMXHelper;
import org.springframework.beans.factory.annotation.Qualifier;

import com.heliosapm.watchtower.core.annotation.Propagate;

/**
 * <p>Title: EventExecutor</p>
 * <p>Description: The spring event executor</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.core.EventExecutor</code></p>
 */
@Qualifier("SpringEvent")
@Propagate
public class EventExecutor extends JMXManagedThreadPool implements EventExecutorMBean {
	/** The event executor singleton instance */
	private static volatile EventExecutor instance = null;
	/** The event executor singleton instance */
	private static final Object lock = new Object();
	
	/**
	 * Acquires and returns the EventExecutor singleton instance
	 * @return the EventExecutor singleton instance
	 */
	public static EventExecutor getEventExecutor() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new EventExecutor();
				}
			}
		}
		return instance;
	}	

	/**
	 * Creates a new EventExecutor
	 */
	private EventExecutor() {
		super(JMXHelper.objectName("com.heliosapm.watchtower.core.threadpools:service=ThreadPool,name=" + EventExecutor.class.getSimpleName()), EventExecutor.class.getSimpleName());
	}

}
