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
package org.springframework.jmx.export.annotation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.modelmbean.DescriptorSupport;

/**
 * <p>Title: MetricMBeanAttributeInfo</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.springframework.jmx.export.annotation.MetricMBeanAttributeInfo</code></p>
 */

public class MetricMBeanAttributeInfo extends MBeanAttributeInfo {

	/**
	 * Creates a new MetricMBeanAttributeInfo
	 * @param method The method supplying the metric
	 * @param mm The managed metric annotation on the method
	 */
	public MetricMBeanAttributeInfo(Method method, ManagedMetric mm)  {
		super(attributeName(method, mm), method.getReturnType().getName(), mm.description(), true, false, false, descriptor(method, mm));
		// TODO Auto-generated constructor stub
	}
	
	public static String attributeName(Method method, ManagedMetric mm) {
		String attrName = mm.displayName();
		if(attrName.trim().isEmpty()) {
			String mname = method.getName();
			if(mname.startsWith("get")) {
				mname = mname.replace("get", "");
			}
			attrName = mname;
		}
		return attrName;
	}
	
	public static Descriptor descriptor(Method method, ManagedMetric mm) {
		
		Map<String, Object> map = new HashMap<String, Object>();
		//map.put("category", value)   /// -----> get from class ? package ?
		return new ImmutableDescriptor(map);
	}
	
//	String category() default "";
//	int currencyTimeLimit() default -1;
//	String description() default "";
//	String displayName() default "";
//	MetricType metricType() default MetricType.GAUGE;
//	int persistPeriod() default -1;
//	String persistPolicy() default "";
//	String unit() default "";
//	
	


	/**
	 * Creates a new MetricMBeanAttributeInfo
	 * @param name
	 * @param type
	 * @param description
	 * @param isReadable
	 * @param isWritable
	 * @param isIs
	 * @param descriptor
	 */
	public MetricMBeanAttributeInfo(String name, String type,
			String description, boolean isReadable, boolean isWritable,
			boolean isIs, Descriptor descriptor) {
		super(name, type, description, isReadable, isWritable, isIs, descriptor);
		// TODO Auto-generated constructor stub
	}

}
