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

/**
 * <p>Title: ManagedMetricMBean</p>
 * <p>Description: Common base interface for managed metric implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.component.metric.ManagedMetricMBean</code></p>
 */

public interface ManagedMetricMBean {
	/**
	 * Returns the managed metric sub-keys
	 * @return the managed metric sub-keys
	 */
	public String[] getSubkeys();
	/**
	 * Returns the managed metric description
	 * @return the managed metric description
	 */
	public String getDescription();
	/**
	 * Returns the managed metric display name
	 * @return the managed metric display name
	 */
	public String getDisplayName();
	/**
	 * Returns the managed metric type name
	 * @return the managed metric type name
	 */
	public String getMetricType();
	/**
	 * Returns the managed metric unit
	 * @return the managed metric unit
	 */
	public String getUnit();
	/**
	 * Returns the managed metric category
	 * @return the managed metric category
	 */
	public String getCategory();
}
