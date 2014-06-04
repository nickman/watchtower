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

import java.lang.annotation.Annotation;
import java.util.Map;

import org.helios.jmx.util.helpers.BitMaskedEnum;

import com.heliosapm.watchtower.core.impl.ICollector;
import com.heliosapm.watchtower.core.impl.ILifecycle;
import com.heliosapm.watchtower.core.impl.ISchedulable;
import com.heliosapm.watchtower.groovy.annotation.Collector;
import com.heliosapm.watchtower.groovy.annotation.Dependency;
import com.heliosapm.watchtower.groovy.annotation.EventListener;
import com.heliosapm.watchtower.groovy.annotation.Lifecycle;
import com.heliosapm.watchtower.groovy.annotation.RequiresConnector;
import com.heliosapm.watchtower.groovy.annotation.Scheduled;
import com.heliosapm.watchtower.groovy.annotation.ScriptName;

/**
 * <p>Title: ServiceAspect</p>
 * <p>Description: Functional enumeration of the dynamic aspects that can be applied to a deployed script</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.core.ServiceAspect</code></p>
 */

public enum ServiceAspect implements BitMaskedEnum {
	/** Executes collections */
	COLLECTOR(Collector.class, ICollector.class),
	/** Scheduled by the watchtower collection scheduler */
	SCHEDULED(Scheduled.class, ISchedulable.class),
	/** Listens on events from the watchtower bus */
	EVENTLISTENER(EventListener.class, null),
	/** Has dependencies on other components in the watchtower bus */
	DEPENDENT(Dependency.class, null),
	/** Requires an unreliable resource connection  */
	CONNECTOR(RequiresConnector.class, null),
	/** Specifies its own name */
	NAMED(ScriptName.class, null),
	/** Supports the watchtower service lifecycle */
	LIFECYCLE(Lifecycle.class, ILifecycle.class);
	
	/** A decoding map to decode the ThreadResource code to a ThreadResource */
	public static final Map<Integer, ServiceAspect> CODE2ENUM = BitMaskedEnum.Support.generateIntOrdinalMap(ServiceAspect.values());
	/** A decoding map to decode the ThreadResource mask to a ThreadResource */
	public static final Map<Integer, ServiceAspect> MASK2ENUM = BitMaskedEnum.Support.generateIntMaskMap(ServiceAspect.values());

	
	private ServiceAspect(Class<? extends Annotation> annotationType, Class<? extends IServiceAspect> boundInterface) {
		this.annotationType = annotationType;
		this.boundInterface = boundInterface;
		mask = BitMaskedEnum.Support.getBitMask(0, this);
	}
	
	
	/** The binary mask for this ServiceAspect */
	private final int mask;
	/** The annotation that marks the script */
	final Class<? extends Annotation> annotationType; 
	/** The implied service interface extrapolated from the annotation */
	final Class<? extends IServiceAspect> boundInterface;
}
