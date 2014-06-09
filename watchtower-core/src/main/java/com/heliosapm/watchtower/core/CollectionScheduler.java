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
package com.heliosapm.watchtower.core;

import org.helios.jmx.concurrency.JMXManagedScheduler;
import org.helios.jmx.util.helpers.JMXHelper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import com.heliosapm.watchtower.core.annotation.Propagate;

/**
 * <p>Title: CollectionScheduler</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.core.CollectionScheduler</code></p>
 * FIXME: expose config with spring annotations
 */
@EnableAutoConfiguration
@Propagate
public class CollectionScheduler extends JMXManagedScheduler implements CollectionSchedulerMBean {
	/** The collection scheduler singleton instance */
	private static volatile CollectionScheduler instance = null;
	/** The collection scheduler singleton instance */
	private static final Object lock = new Object();
	
	/**
	 * Acquires and returns the CollectionScheduler singleton instance
	 * @return the CollectionScheduler singleton instance
	 */
	public static CollectionScheduler getCollectionScheduler() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new CollectionScheduler();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new CollectionScheduler
	 */
	private CollectionScheduler() {		
		super(JMXHelper.objectName("com.heliosapm.watchtower.core.threadpools:service=ThreadPool,name=" + CollectionScheduler.class.getSimpleName()), CollectionScheduler.class.getSimpleName());
	}

}
