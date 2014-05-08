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
package com.heliosapm.watchtower.component.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;

import org.helios.jmx.util.helpers.StringHelper;

/**
 * <p>Title: ManagedMetricImpl</p>
 * <p>Description: A concrete bean representing an extracted {@link ManagedMetric}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.component.annotation.ManagedMetricImpl</code></p>
 */

public class ManagedMetricImpl {
	/** A description of the metric  */
	protected final String description;
	/** The name of the metric as exposed in the MBeanAttributeInfo. */
	protected final String displayName;
	/** The type of the metric */
	protected final MetricType metricType;
	/** The optional unit of the metric */
	protected final String unit;
	/**
	 * The metric category describing the class or package that the metric is grouped into.
	 * The default blamk value indicates that the containing class's 
	 * {@link MetricGroup} annotation should be read for this value.
	 */
	protected final String category;
	/** An optional arbitrary content descriptor for this metric which could be JSON, XML or CSV etc. */
	protected final String descriptor;
	
	/** A const empty array */
	public static final ManagedMetricImpl[] EMPTY_ARR = {};
	
	/**
	 * Returns an array of ManagedMetricImpls extracted from the passed class
	 * @param clazz The class to extract from
	 * @return a [possibly zero length] array of ManagedMetricImpls 
	 */
	public static ManagedMetricImpl[] from(Class<?> clazz) {
		List<ManagedMetricImpl> impls = new ArrayList<ManagedMetricImpl>();
		Map<String, Method> methods = new HashMap<String, Method>();		
		for(Method m: clazz.getMethods()) {
			if(m.getAnnotation(ManagedMetric.class)!=null) {
				methods.put(StringHelper.getMethodDescriptor(m), m);
			}
		}
		for(Method m: clazz.getDeclaredMethods()) {
			if(m.getAnnotation(ManagedMetric.class)!=null) {
				methods.put(StringHelper.getMethodDescriptor(m), m);
			}
		}
		if(methods.isEmpty()) return EMPTY_ARR;
		if(!methods.isEmpty()) {
			for(Method m: methods.values()) {
				impls.add(from(m));
			}
		}
		return impls.toArray(new ManagedMetricImpl[methods.size()]);
	}
	
	/**
	 * Creates a new ManagedMetricImpl from the passed method if it is annotated with {@link ManagedMetric}.
	 * @param method The method to extract a ManagedMetricImpl from 
	 * @return the ManagedMetricImpl created, or null if the method was not annotated
	 */
	public static ManagedMetricImpl from(Method method) {
		ManagedMetric mm = nvl(method, "method").getAnnotation(ManagedMetric.class);
		if(mm==null) return null;
		String category = nws(mm.category());
		String displayName = nws(mm.displayName());
		if(displayName==null) {
			displayName = attr(method);
		}
		Class<?> clazz = method.getDeclaringClass();
		if(category==null) {
			MetricGroup mg = clazz.getAnnotation(MetricGroup.class);
			if(mg!=null) {
				category = nws(mg.category());
			}
			if(category==null) {
				mg = clazz.getPackage().getAnnotation(MetricGroup.class);
				if(mg!=null) {
					category = nws(mg.category());
				}
			}
		}
		if(category==null) {
			category = clazz.getSimpleName() + " Metric";
		}
		return new ManagedMetricImpl(mm.description(), mm.displayName(), mm.metricType(), category, mm.unit(), mm.descriptor());
	}
	
	/**
	 * Creates and returns a new {@link MBeanAttributeInfo} for the ManagedMetric annotation data on the passed method.
	 * @param method The method to extract a MBeanAttributeInfo from 
	 * @return the MBeanAttributeInfo created, or null if the method was not annotated
	 */
	public static MBeanAttributeInfo minfo(Method method) {
		ManagedMetricImpl mmi = from(method);
		if(mmi==null) return null;
		MBeanAttributeInfo minfo = new MBeanAttributeInfo(
				mmi.displayName,
				method.getReturnType().getName(),
				mmi.description,
				true,
				false, 
				false,
				null
		);
		return null;
	}
	
	public static Descriptor descriptor(Method method, ManagedMetric mm) {
		
		Map<String, Object> map = new HashMap<String, Object>();
		//map.put("category", value)   /// -----> get from class ? package ?
		return new ImmutableDescriptor(map);
	}
	
	/**
	 * Creates a new ManagedMetricImpl
	 * @param description A description of the metric
	 * @param displayName The name of the metric as exposed in the MBeanAttributeInfo
	 * @param metricType The type of the metric
	 * @param category The metric category describing the class or package that the metric is grouped into.
	 * @param unit The optional unit of the metric
	 * @param descriptor An optional arbitrary content descriptor for this metric which could be JSON, XML or CSV etc.
	 */
	protected ManagedMetricImpl(String description, String displayName, MetricType metricType, String category, String unit, String descriptor) {
		this.description = nvl(description, "description");
		this.displayName = nvl(displayName, "displayName");
		this.metricType = nvl(metricType, "metricType");
		this.category = nvl(category, "category");
		this.unit = unit;		
		this.descriptor = descriptor;
	}
	
	private static String attr(Method m) {
		String name = m.getName();
		if(name.startsWith("get")) {
			name = name.substring(0, 3);
		}
		return name;
	}
	
	private static String nws(CharSequence cs) {
		if(cs==null) return null;
		String s = cs.toString().trim();
		return s.isEmpty() ? null : s;
	}

	private static <T> T nvl(T t, String name) {
		if(t==null) {
			throw new IllegalArgumentException(String.format("The passed %s was null or empty", name));
		}
		if(t instanceof CharSequence) {
			if(((CharSequence)t).toString().trim().isEmpty()) {
				throw new IllegalArgumentException(String.format("The passed %s was null or empty", name));
			}
		}
		return t;
		
	}
	
	/**
	 * Creates a new ManagedMetricImpl
	 * @param managedMetric The managed metric instance to ingest
	 */
	public ManagedMetricImpl(ManagedMetric managedMetric) {
		if(managedMetric==null) throw new IllegalArgumentException("The passed managed metric was null");
		description = managedMetric.description();
		displayName = managedMetric.displayName();
		metricType = managedMetric.metricType();
		category = managedMetric.category();		
		descriptor = managedMetric.descriptor().isEmpty() ? null : managedMetric.descriptor();
		unit = managedMetric.unit().isEmpty() ? null : managedMetric.unit();
	}

	/**
	 * Returns the metric description 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the name of the metric as exposed in the MBeanAttributeInfo
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the metric type
	 * @return the metricType
	 */
	public MetricType getMetricType() {
		return metricType;
	}

	/**
	 * Returns the metric unit
	 * @return the unit or null if not defined
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * Returns the metric category
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * Returns the metric descriptor.
	 * An arbitrary content descriptor for this metric which could be JSON, XML or CSV etc.
	 * @return the descriptor or null if one was not defined
	 */
	public String getDescriptor() {
		return descriptor;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ManagedMetricImpl [description=");
		builder.append(description);
		builder.append(", displayName:");
		builder.append(displayName);
		builder.append(", metricType:");
		builder.append(metricType);
		builder.append(", category:");
		builder.append(category);
		if(unit!=null) {
			builder.append(", unit:");
			builder.append(unit);			
		}
		if(descriptor!=null) {
			builder.append(", descriptor:");
			builder.append(descriptor);
		}
		builder.append("]");
		return builder.toString();
	}

}
