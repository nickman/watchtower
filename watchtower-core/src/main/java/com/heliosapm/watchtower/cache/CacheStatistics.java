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
package com.heliosapm.watchtower.cache;

import javax.management.ObjectName;

import org.helios.jmx.util.helpers.JMXHelper;

import com.google.common.cache.Cache;

/**
 * <p>Title: CacheStatistics</p>
 * <p>Description: A cache stats wrapper for a guava cache</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.cache.CacheStatistics</code></p>
 */
public class CacheStatistics implements CacheStatisticsMBean {
	/** The wrapped cache instance */
	protected final Cache<?, ?> cache;	
	/** The cache name */
	protected final String cacheName;
	/** The cache stats JMX object name */
	protected final ObjectName objectName;


	/**
	 * Creates a new CacheStatistics
	 * @param cache The guava cache instance to wrap
	 * @param cacheName The assigned name for this cache
	 */
	public CacheStatistics(Cache<?, ?> cache, String cacheName) {
		this.cache = cache;
		this.cacheName = cacheName;
		objectName = JMXHelper.objectName(JMXHelper.objectName(getClass()).toString() + ",name=" + cacheName);
	}

	/**
	 * Registers the JMX interface for this cache stats, if not already registered
	 */
	public void register() {
		if(!JMXHelper.getHeliosMBeanServer().isRegistered(objectName)) {
			try {
				JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#invalidateAll()
	 */
	@Override
	public void invalidateAll() {
		cache.invalidateAll();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#cleanup()
	 */
	@Override
	public void cleanup() {
		cache.cleanUp();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#getSize()
	 */
	@Override
	public long getSize() {
		return cache.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#getRequestCount()
	 */
	@Override
	public long getRequestCount() {
		return cache.stats().requestCount();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#getHitCount()
	 */
	@Override
	public long getHitCount() {
		return cache.stats().hitCount();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#getHitRate()
	 */
	@Override
	public double getHitRate() {
		return cache.stats().hitRate();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#getMissCount()
	 */
	@Override
	public long getMissCount() {
		return cache.stats().missCount();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#getMissRate()
	 */
	@Override
	public double getMissRate() {
		return cache.stats().missRate();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#getLoadCount()
	 */
	@Override
	public long getLoadCount() {
		return cache.stats().loadCount();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#getLoadSuccessCount()
	 */
	@Override
	public long getLoadSuccessCount() {
		return cache.stats().loadSuccessCount();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#getLoadExceptionCount()
	 */
	@Override
	public long getLoadExceptionCount() {
		return cache.stats().loadExceptionCount();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#getLoadExceptionRate()
	 */
	@Override
	public double getLoadExceptionRate() {
		return cache.stats().loadExceptionRate();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#getTotalLoadTime()
	 */
	@Override
	public long getTotalLoadTime() {
		return cache.stats().totalLoadTime();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#getAverageLoadPenalty()
	 */
	@Override
	public double getAverageLoadPenalty() {
		return cache.stats().averageLoadPenalty();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.cache.CacheStatisticsMBean#getEvictionCount()
	 */
	@Override
	public long getEvictionCount() {
		return cache.stats().evictionCount();
	}

}
