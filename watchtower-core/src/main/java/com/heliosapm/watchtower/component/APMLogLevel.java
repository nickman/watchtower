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
package com.heliosapm.watchtower.component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;

/**
 * <p>Title: APMLogLevel</p>
 * <p>Description: Enumerates the logging levels used by Watchtower</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.APMLogLevel</code></p>
 */

public enum APMLogLevel {
	/** No filtering */
	ALL(2147483647, Level.ALL),
	/** Highest verbosity filtering */
	TRACE(600, Level.TRACE),
	/** High verbosity filtering */
	DEBUG(500, Level.DEBUG),
	/** Standard verbosity filtering */
	INFO(400, Level.INFO),
	/** Filters out all but warnings and more severe */
	WARN(300, Level.WARN),
	/** Filters out all but errors and more severe */
	ERROR(200, Level.ERROR),
	/** Filters out all but fatal */
	FATAL(100, Level.FATAL),
	/** Turns off logging */
	OFF(0, Level.OFF);
	
	/** Map of Log Levels keyed by the ordinal */
	private static final Map<Integer, APMLogLevel> ORD2ENUM;
	/** Map of Log Levels keyed by the pcode */
	private static final Map<Integer, APMLogLevel> PCODE2ENUM;
	
	static {
		Map<Integer, APMLogLevel> tmp = new HashMap<Integer, APMLogLevel>(APMLogLevel.values().length);
		for(APMLogLevel ll: APMLogLevel.values()) {
			tmp.put(ll.ordinal(), ll);
		}
		ORD2ENUM = Collections.unmodifiableMap(tmp);
		tmp = new HashMap<Integer, APMLogLevel>(APMLogLevel.values().length);
		for(APMLogLevel ll: APMLogLevel.values()) {
			tmp.put(ll.pCode(), ll);
		}
		PCODE2ENUM = Collections.unmodifiableMap(tmp);		
	}
	
	/**
	 * Detemrines if this log level is enabled for the passed level
	 * @param level The level to compare
	 * @return true if enabled, false otherwise
	 */
	public boolean isEnabledFor(APMLogLevel level) {
		return level.ordinal() >= ordinal();
	}
	
	/**
	 * Decodes the passed ordinal to a APMLogLevel.
	 * Throws a runtime exception if the ordinal is invalud
	 * @param ordinal The ordinal to decode
	 * @return the decoded APMLogLevel
	 */
	public static APMLogLevel valueOf(int ordinal) {
		APMLogLevel mt = ORD2ENUM.get(ordinal);
		if(mt==null) throw new IllegalArgumentException("The passed ordinal [" + ordinal + "] is not a valid APMLogLevel ordinal", new Throwable());
		return mt;
	}	
	
	/**
	 * Decodes the passed pCode to a APMLogLevel.
	 * Throws a runtime exception if the ordinal is invalud
	 * @param pCode The pCode to decode
	 * @return the decoded APMLogLevel
	 */
	public static APMLogLevel pCode(int pCode) {
		APMLogLevel ll = PCODE2ENUM.get(pCode);
		if(ll==null) throw new IllegalArgumentException("The passed pCode [" + pCode + "] is not a valid APMLogLevel pCode", new Throwable());
		return ll;
	}		
	
	
	public static void main(String[] args) {
		try {
			for(Field f: Level.class.getDeclaredFields()) {
				if(f.getType().equals(Level.class)) {
					if(Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers())) {
						Level level = (Level)f.get(null);
						System.out.println(String.format("Name: %s, Int: %s", level.name(), level.intLevel()));
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	/**
	 * Decodes the passed name to a APMLogLevel.
	 * Throws a runtime exception if the ordinal is invalud
	 * @param name The metricId type name to decode. Trimmed and uppercased.
	 * @return the decoded APMLogLevel
	 */
	public static APMLogLevel valueOfName(CharSequence name) {
		if(name==null) throw new IllegalArgumentException("APMLogLevel Name was null");
		String n = name.toString().trim().toUpperCase();
		try {
			return APMLogLevel.valueOf(n);
		} catch (Exception e) {
			throw new IllegalArgumentException("The passed name [" + name + "] is not a valid APMLogLevel name", new Throwable());
		}
	}	
	
	
	private APMLogLevel(int pCode, Level level) {
		this.pCode = pCode;
		this.level = level;
	}
	
	/** The native level */
	private final Level level;
	/** The logging package native code */
	private final int pCode;

	/**
	 * Returns the native level code
	 * @return the native level code
	 */
	public int pCode() {
		return pCode;
	}

	/**
	 * Returns the native logging level
	 * @return the native logging level
	 */
	public Level getLevel() {
		return level;
	}

}
