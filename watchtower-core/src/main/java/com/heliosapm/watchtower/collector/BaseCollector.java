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

import java.util.concurrent.atomic.AtomicReference;

import com.heliosapm.watchtower.component.StdServerComponent;

/**
 * <p>Title: BaseCollector</p>
 * <p>Description: The base collector operations, events and lifecycle</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.collector.BaseCollector</code></p>
 */

public abstract class BaseCollector extends StdServerComponent implements BaseCollectorMBean {
	/** The collector bean's state */
	protected final AtomicReference<CollectorState> state = new AtomicReference<CollectorState>(CollectorState.INIT);
	
	/**
	 * Creates a new BaseCollector
	 */
	public BaseCollector() {
		debug("Created Collector Bean [", getClass().getName(), "]");
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.collector.BaseCollectorMBean#start()
	 */
	@Override
	public void start() {
		info("Starting");
		preStart();
		
		
		postStart();
	}
	
	/**
	 * Called before the main {@link #start()} method body.
	 * To be overriden by concrete classes where applicable.
	 */
	protected void preStart() {
		/* No Op */
	}
	
	/**
	 * Called after the main {@link #start()} method body.
	 * To be overriden by concrete classes where applicable.
	 */
	protected void postStart() {
		/* No Op */
	}
	

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.collector.BaseCollectorMBean#stop()
	 */
	@Override
	public void stop() {
		info("Stopping");
		preStop();
		
		
		postStop();

	}
	
	/**
	 * Called before the main {@link #stop()} method body.
	 * To be overriden by concrete classes where applicable.
	 */
	protected void preStop() {
		/* No Op */
	}
	
	/**
	 * Called after the main {@link #stop()} method body.
	 * To be overriden by concrete classes where applicable.
	 */
	protected void postStop() {
		/* No Op */
	}
		

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.collector.BaseCollectorMBean#pause()
	 */
	@Override
	public void pause() {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.collector.BaseCollectorMBean#init()
	 */
	@Override
	public void init() {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.collector.BaseCollectorMBean#collect(java.lang.Object[])
	 */
	@Override
	public void collect(Object... collectArgs) {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.collector.BaseCollectorMBean#getState()
	 */
	@Override
	public String getState() {
		return state.get().name();
	}

}
