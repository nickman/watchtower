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
import org.helios.jmx.annotation.ManagedNotificationImpl;
import org.helios.jmx.metrics.ewma.ConcurrentDirectEWMA;

/**
 * <p>Title: ManagedConcurrentDirectEWMA</p>
 * <p>Description: An extension of {@link ConcurrentDirectEWMA} with added meta-data for management</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.component.metric.ManagedConcurrentDirectEWMA</code></p>
 */

public class ManagedConcurrentDirectEWMA extends ConcurrentDirectEWMA implements ManagedConcurrentDirectEWMAMBean {
	/** The managed metric impl */
	protected final ManagedMetricImpl managedMetricImpl;
	/**
	 * Creates a new ManagedConcurrentDirectEWMA
	 * @param managedMetricImpl The managed metric impl
	 */
	public ManagedConcurrentDirectEWMA(ManagedMetricImpl managedMetricImpl) {
		super(managedMetricImpl.getWindowSize());
		this.managedMetricImpl = managedMetricImpl;
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.metric.ManagedConcurrentDirectEWMAMBean#getSubkeys()
	 */
	public String[] getSubkeys() {
		return managedMetricImpl.getSubkeys();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.metric.ManagedConcurrentDirectEWMAMBean#getDescription()
	 */
	public String getDescription() {
		return managedMetricImpl.getDescription();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.metric.ManagedConcurrentDirectEWMAMBean#getDisplayName()
	 */
	public String getDisplayName() {
		return managedMetricImpl.getDisplayName();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.metric.ManagedConcurrentDirectEWMAMBean#getMetricType()
	 */
	public String getMetricType() {
		return managedMetricImpl.getMetricType().name();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.metric.ManagedConcurrentDirectEWMAMBean#getUnit()
	 */
	public String getUnit() {
		return managedMetricImpl.getUnit();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.metric.ManagedConcurrentDirectEWMAMBean#getCategory()
	 */
	public String getCategory() {
		return managedMetricImpl.getCategory();
	}
	/**
	 * Returns the notifications emitted by this managed metric
	 * @return an array of notifications
	 */
	public ManagedNotificationImpl[] getNotifications() {
		return managedMetricImpl.getNotifications();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.metric.ManagedConcurrentDirectEWMAMBean#getWindowSize()
	 */
	public int getWindowSize() {
		return managedMetricImpl.getWindowSize();
	}


}
