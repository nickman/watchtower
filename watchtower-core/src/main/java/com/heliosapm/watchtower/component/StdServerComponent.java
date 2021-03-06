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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.helios.jmx.annotation.ManagedAttribute;
import org.helios.jmx.annotation.ManagedResource;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

/**
 * <p>Title: StdServerComponent</p>
 * <p>Description: Base component for JMX exposed services</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.component.StdServerComponent</code></p>
 */
@ManagedResource
public class StdServerComponent implements StdServerComponentMBean {

	/** The logger context */
	protected final LoggerContext logCtx = (LoggerContext)LoggerFactory.getILoggerFactory();
	/** Instance logger */
	protected Logger log = logCtx.getLogger(getClass());


	/** EOL bytes */
	private static final byte[] EOL = "\n".getBytes();

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.StdServerComponentMBean#getLevel()
	 */
	@ManagedAttribute(description="The logging level of this component")
	public String getLevel() {
		Level level = log.getLevel();
		if(level==null) return null;
		return level.toString();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.StdServerComponentMBean#getEffectiveLevel()
	 */
	@ManagedAttribute(description="The effective logging level of this component")
	public String getEffectiveLevel() {
		return log.getEffectiveLevel().toString();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.StdServerComponentMBean#setLevel(java.lang.String)
	 */
	@ManagedAttribute(description="The logging level of this component")
	public void setLevel(String levelName) {
		log.setLevel(Level.toLevel(levelName, Level.INFO));
		info("Set Logger to level [", log.getLevel().toString(), "]");
	}
	
	
	/**
	 * Issues a trace level logging request
	 * @param msgs The objects to format into a log message
	 */
	protected void trace(Object...msgs) {
		logAtLevel(APMLogLevel.TRACE, msgs);
	}

	/**
	 * Issues a debug level logging request
	 * @param msgs The objects to format into a log message
	 */
	protected void debug(Object...msgs) {
		logAtLevel(APMLogLevel.DEBUG, msgs);
	}
	
	/**
	 * Issues an info level logging request
	 * @param msgs The objects to format into a log message
	 */
	protected void info(Object...msgs) {
		logAtLevel(APMLogLevel.INFO, msgs);
	}
	
	/**
	 * Issues a warn level logging request
	 * @param msgs The objects to format into a log message
	 */
	protected void warn(Object...msgs) {
		logAtLevel(APMLogLevel.WARN, msgs);
	}
	
	/**
	 * Issues a error level logging request
	 * @param msgs The objects to format into a log message
	 */
	protected void error(Object...msgs) {
		logAtLevel(APMLogLevel.ERROR, msgs);
	}
	
	/**
	 * Issues a fatal level logging request
	 * @param msgs The objects to format into a log message
	 */
	protected void fatal(Object...msgs) {
		logAtLevel(APMLogLevel.ERROR, msgs);
	}
	
	private static final String[] EMPTY_STR_ARR = {}; 
	/**
	 * Forwards the logging directive if the current level is enabled for the passed level
	 * @param l The requested level
	 * @param msgs The logged messages
	 */
	protected void logAtLevel(APMLogLevel l, Object...msgs) {
		if(log.isEnabledFor(l.getLevel())) {
			log.log(null, null, l.pCode(), format(msgs), EMPTY_STR_ARR, null);
		}				
	}
	
	/**
	 * Wraps the passed object in a formatted banner
	 * @param objs The objects to print inside the banner
	 * @return a formated banner
	 */
	public static String banner(Object...objs) {
		if(objs==null || objs.length<1) return "";
		StringBuilder b = new StringBuilder("\n\t================================\n\t");
		for(Object obj: objs) {
			b.append(obj);
		}
		b.append("\n\t================================");
		return b.toString();
	}	
	
	/**
	 * Formats the passed objects into a loggable string. 
	 * If the last object is a {@link Throwable}, it will be formatted into a stack trace.
	 * @param msgs The objects to log
	 * @return the loggable string
	 */
	public static String format(Object...msgs) {
		if(msgs==null||msgs.length<1) return "";
		StringBuilder b = new StringBuilder();
		int c = msgs.length-1;
		for(int i = 0; i <= c; i++) {
			if(i==c && msgs[i] instanceof Throwable) {
				b.append(formatStackTrace((Throwable)msgs[i]));
			} else {
				b.append(msgs[i]);
			}
		}
		return b.toString();
	}
	
	
	/**
	 * Formats a throwable's stack trace
	 * @param t The throwable to format
	 * @return the formatted stack trace
	 */
	public static String formatStackTrace(Throwable t) {
		if(t==null) return "";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write(EOL);
			t.printStackTrace(new PrintStream(baos, true));		
			baos.flush();
		} catch (IOException e) {
		}
		return baos.toString();
	}
	
//	/**
//	 * Returns the metric names implemented by this component
//	 * @return the metric names implemented by this component
//	 */
//	public Map<MetricType, Set<String>> getSupportedMetricNames() {
//		Map<MetricType, Set<String>> map = new EnumMap<MetricType, Set<String>>(MetricType.class);
//		for(MetricType mt: MetricType.values()) {
//			map.put(mt, new HashSet<String>());
//		}
//		
//		try {
//			for(Method method: this.getClass().getMethods()) {
//				ManagedMetric mm = method.getAnnotation(ManagedMetric.class);
//				if(mm!=null) {
//					String name = mm.category();
//					if(name!=null && !name.trim().isEmpty()) {
//						metrics.add(name.trim());
//					}
//				}
//			}
//			
//			for(Method method: this.getClass().getDeclaredMethods()) {
//				ManagedMetric mm = method.getAnnotation(ManagedMetric.class);
//				if(mm!=null) {
//					String name = mm.category();
//					if(name!=null && !name.trim().isEmpty()) {
//						metrics.add(name.trim());
//					}
//				}
//			}
//		} catch (Exception ex) {
//			ex.printStackTrace(System.err);
//		}
//		return metrics;
//		
//	}
	

	
//	/**
//	 * Initializes the metric counters for this component
//	 */
//	protected void initCounters() {
//		for(String name: getSupportedMetricNames()) {
//			name = name.trim();
//			if(!metrics.containsKey(name)) {
//				metrics.put(name, new Counter());
//			}
//		}
//	}
//	
//	protected Counter mget(String name) {
//		Counter ctr = metrics.get(name);
//		if(ctr==null) {
//			ctr = new Counter();
//			metrics.put(name, ctr);
//		}		
//		return ctr;
//	}
	
//	/**
//	 * Increments the named metric by the passed value
//	 * @param name The name of the metric
//	 * @param delta The amount to increment by
//	 */
//	protected void incr(String name, long delta) {
//		if(name==null) return;
//		mget(name).add(delta);
//	}
//	
//	/**
//	 * Decrements the named metric by the passed value
//	 * @param name The name of the metric
//	 * @param delta The amount to decrement by
//	 */
//	protected void decr(String name, long delta) {
//		if(name==null) return;
//		mget(name).add(-delta);
//	}
//	
//	/**
//	 * Decrements the named metric by 1
//	 * @param name The name of the metric
//	 */
//	protected void decr(String name) {
//		if(name==null) return;
//		mget(name).decrement();
//	}	
	
	
//	/**
//	 * Sets the named metric to the passed value
//	 * @param name The name of the metric
//	 * @param value The value to set to
//	 */
//	protected void set(String name, long value) {
//		if(name==null) return;
//		mget(name).set(value);
//	}
//	
//	
//	/**
//	 * Increments the named metric by 1
//	 * @param name The name of the metric
//	 */
//	protected void incr(String name) {
//		incr(name, 1);
//	}
	
//	/**
//	 * Returns the value of the named metric
//	 * @param name The name of the metric
//	 * @return the value of the named metric
//	 */
//	protected long getMetricValue(String name) {
//		return mget(name).get();
//	}
	
	/**
	 * Shortcut to get a logger
	 * @param name The name of the logger
	 * @return the requested logger
	 */
	public static Logger getLogger(String name) {
		return ((LoggerContext)LoggerFactory.getILoggerFactory()).getLogger(name);
	}
	
	/**
	 * Shortcut to get a logger
	 * @param clazz The class to get a logger for
	 * @return the requested logger
	 */
	public static Logger getLogger(Class<?> clazz) {
		return ((LoggerContext)LoggerFactory.getILoggerFactory()).getLogger(clazz);
	}	
	
	
	
//	/**
//	 * Resets all the metrics
//	 */
//	@ManagedOperation
//	public void resetMetrics() {
//		for(Counter ctr: metrics.values()) {
//			ctr.set(0);
//		}
//	}
//	
//	/**
//	 * Returns the names of the metrics supported by this component
//	 * @return the names of the metrics supported by this component
//	 */
//	@ManagedAttribute
//	public String[] getMetricNames() {
//		return metrics.keySet().toArray(new String[metrics.size()]);
//	}
	
//	/**
//	 * Returns the UTC long timestamp of the last time the metrics were reset
//	 * @return a UTC long timestamp 
//	 */
//	@ManagedAttribute
//	public long getLastMetricResetTime() {
//		return lastMetricResetTime.get();
//	}
//
//	/**
//	 * Returns the java date timestamp of the last time the metrics were reset
//	 * @return a java date
//	 */
//	@ManagedAttribute
//	public Date getLastMetricResetDate() {
//		return new Date(lastMetricResetTime.get());
//	}
	
	
	

}
