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

import javax.management.ObjectName;

import org.helios.jmx.concurrency.JMXManagedThreadPool;
import org.helios.jmx.util.helpers.JMXHelper;

import com.heliosapm.watchtower.core.annotation.Propagate;

/**
 * <p>Title: CollectionExecutor</p>
 * <p>Description: The thread pool for collection asynch tasks</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.core.CollectionExecutor</code></p>
 * FIXME: expose config with spring annotations
 */
@Propagate
public class CollectionExecutor extends JMXManagedThreadPool implements CollectionExecutorMBean {
	
	
	/**
	 * Creates a new CollectionExecutor
	 */
	public CollectionExecutor() {		
		super(JMXHelper.objectName("com.heliosapm.watchtower.core.threadpools:service=ThreadPool,name=" + CollectionExecutor.class.getSimpleName()), CollectionExecutor.class.getSimpleName());		
	}

	/**
	 * Creates a new CollectionExecutor
	 * @param objectName The JMX ObjectName of the executor
	 * @param poolName The executor pool name
	 * @param publishJMX If true, publishes the JMX interface
	 */
	public CollectionExecutor(ObjectName objectName, String poolName, boolean publishJMX) {
		super(objectName, poolName, publishJMX);
	}


}
