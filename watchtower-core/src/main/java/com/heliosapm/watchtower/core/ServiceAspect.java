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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.helios.jmx.util.helpers.BitMaskedEnum;

import com.heliosapm.watchtower.core.impl.ICollector;
import com.heliosapm.watchtower.core.impl.IDependency;
import com.heliosapm.watchtower.core.impl.IEventListener;
import com.heliosapm.watchtower.core.impl.INamed;
import com.heliosapm.watchtower.core.impl.ISchedulable;
import com.heliosapm.watchtower.core.impl.IStart;
import com.heliosapm.watchtower.core.impl.IStop;
import com.heliosapm.watchtower.groovy.annotation.Collector;
import com.heliosapm.watchtower.groovy.annotation.Dependency;
import com.heliosapm.watchtower.groovy.annotation.EventListener;
import com.heliosapm.watchtower.groovy.annotation.Scheduled;
import com.heliosapm.watchtower.groovy.annotation.ScriptName;
import com.heliosapm.watchtower.groovy.annotation.Start;
import com.heliosapm.watchtower.groovy.annotation.Stop;

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
	EVENTLISTENER(EventListener.class, IEventListener.class),
	/** Has dependencies on other components in the watchtower bus */
	DEPENDENT(Dependency.class, IDependency.class),
//	/** Requires an unreliable resource connection  */
//	CONNECTOR(RequiresConnector.class, null),
	/** Specifies its own name */
	NAMED(ScriptName.class, INamed.class),
	/** Supports the watchtower service lifecycle start operation */
	STARTER(Start.class, IStart.class),
	/** Supports the watchtower service lifecycle start operation */
	STOPPER(Stop.class, IStop.class);
	
	/** A decoding map to decode the ServiceAspect code to a ServiceAspect */
	public static final Map<Integer, ServiceAspect> CODE2ENUM = BitMaskedEnum.Support.generateIntOrdinalMap(ServiceAspect.values());
	/** A decoding map to decode the ServiceAspect mask to a ServiceAspect */
	public static final Map<Integer, ServiceAspect> MASK2ENUM = BitMaskedEnum.Support.generateIntMaskMap(ServiceAspect.values());
	/** A decoding map to decode the service interface to the ServiceAspect */
	public static final Map<Class<? extends IServiceAspect>, ServiceAspect> IFACE2ENUM;
	/** A decoding map to decode the service annotation to the ServiceAspect */
	public static final Map<Class<? extends Annotation>, ServiceAspect> ANNOT2ENUM;

	/** The number of aspects */
	public static final int ASPECT_COUNT = values().length;
	
	static {
		Map<Class<? extends IServiceAspect>, ServiceAspect> tmp = new HashMap<Class<? extends IServiceAspect>, ServiceAspect>(ASPECT_COUNT);
		Map<Class<? extends Annotation>, ServiceAspect> tmp2 = new HashMap<Class<? extends Annotation>, ServiceAspect>(ASPECT_COUNT);
		for(ServiceAspect sa: values()) {
			tmp.put(sa.boundInterface, sa);
			tmp2.put(sa.annotationType, sa);
		}
		IFACE2ENUM = Collections.unmodifiableMap(tmp);
		ANNOT2ENUM = Collections.unmodifiableMap(tmp2);
	}
	
	private ServiceAspect(Class<? extends Annotation> annotationType, Class<? extends IServiceAspect> boundInterface) {
		this.annotationType = annotationType;
		this.boundInterface = boundInterface;
		mask = BitMaskedEnum.Support.getBitMask(0, this);
		ifaceNode = new ClassNode(this.boundInterface);
		annotationClassNode = new ClassNode(this.annotationType);
		annotationNode = new AnnotationNode(annotationClassNode);
	}
	
	
	/** The binary mask for this ServiceAspect */
	final int mask;
	/** The annotation that marks the script */
	final Class<? extends Annotation> annotationType; 
	/** The implied service interface extrapolated from the annotation */
	final Class<? extends IServiceAspect> boundInterface;
	/** The iface node */
	final ClassNode ifaceNode;
	/** The annotation class node */
	final ClassNode annotationClassNode;
	/** The annotation node */
	final AnnotationNode annotationNode;
	
	/**
	 * Computes the service aspect bit mask for the passed class
	 * @param clazz The class to get the aspect bit mask for
	 * @return the computed bit mask
	 */
	public static int computeBitMask(Class<?> clazz) {
		int m = 0;
		for(ServiceAspect sa: values()) {
			if(sa.isEnabled(clazz)) {
				m = sa.enable(m);
			}
		}
		return m;
	}
	
	/**
	 * Returns an array of the enabled aspects for the passed bit mask
	 * @param bitMask The bit mask to get the enabled service aspects for
	 * @return an array of service aspects
	 */
	public static ServiceAspect[] getEnabledServiceAspects(int bitMask) {
		Set<ServiceAspect> aspects = EnumSet.noneOf(ServiceAspect.class);
		for(ServiceAspect sa: values()) {
			if(sa.isEnabled(bitMask)) {
				aspects.add(sa);
			}
		}
		return aspects.toArray(new ServiceAspect[aspects.size()]);
	}
	
	/**
	 * Returns the service aspect for the passed service interface type
	 * @param serviceIface The service interface type to get the aspect for
	 * @return The service aspect or null if not found
	 */
	public static ServiceAspect getAspectForIface(Class<? extends IServiceAspect> serviceIface) {
		return IFACE2ENUM.get(serviceIface);
	}
	
	/**
	 * Returns the service aspect for the passed service annotation type
	 * @param annotationType The service annotation type to get the aspect for
	 * @return The service aspect or null if not found
	 */
	public static ServiceAspect getAspectForAnnotation(Class<? extends Annotation> annotationType) {
		return ANNOT2ENUM.get(annotationType);
	}
	
	/**
	 * Returns the service aspect for the passed service annotation
	 * @param annotation The service annotation to get the aspect for
	 * @return The service aspect or null if not found
	 */
	public static ServiceAspect getAspectForAnnotation(Annotation annotation) {
		return ANNOT2ENUM.get(annotation.annotationType());
	}
	
	
	/**
	 * Determines if the passed class implements this aspect's bound interface
	 * @param clazz The class to test
	 * @return true if the passed class implements this aspect's bound interface, false otherwise
	 */
	public boolean isEnabled(Class<?> clazz) {
		return boundInterface.isAssignableFrom(clazz);
	}

	/**
	 * Determines if this aspect is enabled in the passed mask
	 * @param bitMask The mask to evaluate
	 * @return true if enabled, false otherwise
	 */
	public boolean isEnabled(int bitMask) {
		return (bitMask | this.mask) == bitMask;
	}
	
	/**
	 * Returns the passed bit mask enabled for this aspect
	 * @param bitMask the bit mask to modify to enable this aspect
	 * @return the modified bit mask
	 */
	public int enable(int bitMask) {
		return (bitMask | this.mask);
	}
	
	/**
	 * Returns the bit mask for this aspect
	 * @return the bit mask for this aspect
	 */
	public int getMask() {
		return mask;
	}

	/**
	 * Returns the annotation indicating enablement of this aspect
	 * @return the annotation indicating enablement of this aspect
	 */
	public Class<? extends Annotation> getAnnotationType() {
		return annotationType;
	}

	/**
	 * Returns interface class woven into a class when this aspect is enabled
	 * @return the boundInterface the aspect interface 
	 */
	public Class<? extends IServiceAspect> getBoundInterface() {
		return boundInterface;
	}

	/**
	 * Returns the bound interface node 
	 * @return the ifaceNode
	 */
	public ClassNode getIfaceNode() {
		return ifaceNode;
	}

	/**
	 * Returns the annotation class node
	 * @return the annotationClassNode
	 */
	public ClassNode getAnnotationClassNode() {
		return annotationClassNode;
	}

	/**
	 * Returns the annotation node
	 * @return the annotationNode
	 */
	public AnnotationNode getAnnotationNode() {
		return annotationNode;
	}
}
