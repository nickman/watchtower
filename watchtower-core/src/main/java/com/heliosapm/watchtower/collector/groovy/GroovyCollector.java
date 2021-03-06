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
package com.heliosapm.watchtower.collector.groovy;

import groovy.lang.GroovyObject;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import com.heliosapm.watchtower.collector.BaseCollector;

/**
 * <p>Title: GroovyCollector</p>
 * <p>Description:A base collector wrapper for deployed groovy scripts</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.collector.groovy.GroovyCollector</code></p>
 */

public class GroovyCollector extends BaseCollector {
	/** The wrapped groovy object */
	protected GroovyObject groovyObject = null;
	
	/**
	 * Creates a new GroovyCollector
	 */
	public GroovyCollector() {
		
	}
	
	/**
	 * Sets the delegate groovy object
	 * @param groovyObject The wrapped groovy object
	 * @return this groovy collector
	 */
	public GroovyCollector setGroovyObject(GroovyObject groovyObject) {
		this.groovyObject = groovyObject;
		return this;
	}
	
	/**
	 * Groovy coerces the wrapped groovy object to the passed type
	 * @param type the type to coerce to
	 * @return the groovy object coerced to the given type
	 */
	public <T> T as(Class<T> type) {
		return DefaultGroovyMethods.asType(groovyObject, type);
	}

}
