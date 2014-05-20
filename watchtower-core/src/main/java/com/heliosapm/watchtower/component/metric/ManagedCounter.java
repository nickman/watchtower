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
package com.heliosapm.watchtower.component.metric;

import org.helios.jmx.annotation.ManagedMetricImpl;
import org.helios.jmx.metrics.ewma.Counter;

/**
 * <p>Title: ManagedCounter</p>
 * <p>Description: An extension of {@link Counter} with added meta-data for management</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.component.metric.ManagedCounter</code></p>
 */

public class ManagedCounter extends Counter implements ManagedCounterMBean {
	/**  */
	private static final long serialVersionUID = -8579579810329749161L;
	/** The managed metric impl */
	protected final ManagedMetricImpl managedMetricImpl;

	/**
	 * Creates a new ManagedCounter
	 * @param managedMetricImpl The managed metric meta
	 */
	public ManagedCounter(ManagedMetricImpl managedMetricImpl) {
		super(managedMetricImpl.getInitialValue());
		this.managedMetricImpl = managedMetricImpl;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.metric.ManagedMetricMBean#getSubkeys()
	 */
	@Override
	public String[] getSubkeys() {
		return managedMetricImpl.getSubkeys();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.metric.ManagedMetricMBean#getDescription()
	 */
	@Override
	public String getDescription() {
		return managedMetricImpl.getDescription();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.metric.ManagedMetricMBean#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return managedMetricImpl.getDisplayName();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.metric.ManagedMetricMBean#getMetricType()
	 */
	@Override
	public String getMetricType() {
		return managedMetricImpl.getMetricType().name();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.metric.ManagedMetricMBean#getUnit()
	 */
	@Override
	public String getUnit() {
		return managedMetricImpl.getUnit();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.metric.ManagedMetricMBean#getCategory()
	 */
	@Override
	public String getCategory() {
		return managedMetricImpl.getCategory();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.metric.ManagedCounterMBean#getInitialValue()
	 */
	@Override
	public long getInitialValue() {
		return managedMetricImpl.getInitialValue();
	}


}
