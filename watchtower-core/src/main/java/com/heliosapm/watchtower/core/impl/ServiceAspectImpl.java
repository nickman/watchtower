/**
 * 
 */
package com.heliosapm.watchtower.core.impl;

import groovy.lang.GroovyObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.helios.jmx.util.helpers.JMXHelper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.naming.SelfNaming;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import com.heliosapm.watchtower.collector.CollectorState;
import com.heliosapm.watchtower.component.APMLogLevel;
import com.heliosapm.watchtower.deployer.DeploymentBranch;

/**
 * <p>Title: ServiceAspectImpl</p>
 * <p>Description: The base script wrapper and support class</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.watchtower.core.impl.ServiceAspectImpl</code></b>
 */
@ManagedResource
public class ServiceAspectImpl implements SelfNaming, BeanNameGenerator {
	/** The logger context */
	protected final LoggerContext logCtx = (LoggerContext)LoggerFactory.getILoggerFactory();
	/** Instance logger */
	protected Logger log = logCtx.getLogger(getClass());
	
	/** The compiled deployment script */
	protected GroovyObject groovyObject = null;
	/** The deployment/script name */
	protected String beanName;
	/** The started state of this component */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	/** The state of this component */
	protected final AtomicReference<CollectorState> state = new AtomicReference<CollectorState>(CollectorState.INIT); 
	
	protected DeploymentBranch parent;
	/**
	 * Returns the 
	 * @return the parent
	 */
	public DeploymentBranch getParent() {
		return parent;
	}


	/**
	 * Sets the 
	 * @param parent the parent to set
	 */
	public void setParent(DeploymentBranch parent) {
		this.parent = parent;
	}

	protected File sourceFile;
	
	/**
	 * Returns the 
	 * @return the sourceFile
	 */
	public File getSourceFile() {
		return sourceFile;
	}


	/**
	 * Sets the 
	 * @param sourceFile the sourceFile to set
	 */
	public void setSourceFile(File sourceFile) {
		this.sourceFile = sourceFile;
	}


	/**
	 * Creates a new ServiceAspectImpl
	 */
	
//	public ServiceAspectImpl(DeploymentBranch parent, File source) {
//		this.beanName = getClass().getName();
//		this.parent = parent;
//		sourceFile = source;
//	}
	
	public ServiceAspectImpl() {
		
	}
		
	
	
	/**
	 * Called when bean is started.
	 * @throws Exception thrown on any errors starting the bean
	 */	
	@ManagedOperation
	public final void start() throws Exception {
		try {
			
			if(isStarted()) throw new IllegalStateException("Cannot start component once it is started", new Throwable());
			info(banner("Starting [", this.beanName, "]"));
			doStart();
			started.set(true);
			info(banner("Started [", this.beanName, "]"));
		} catch (Exception e) {
			error("Failed to start [", this.beanName, "]", e);
			throw e;
		}
	}
	
	/**
	 * Called when bean is stopped
	 */	
	@ManagedOperation
	public final void stop() {
		if(!isStarted()) throw new IllegalStateException("Cannot stop component once it is stopped", new Throwable());
		try {
			info(banner("Stopping [", this.beanName, "]"));
			doStop();
			info(banner("Stopped [", this.beanName, "]"));
		} catch (Exception ex) {
			warn("Problem stopping bean", ex);
			
		} finally {
			started.set(false);
		}		
	}
	
	/**
	 * To be implemented by concrete classes that have a specific start operation
	 * @throws Exception thrown if startup fails
	 */
	protected void doStart() throws Exception {/* No Op */}
	
	/**
	 * To be implemented by concrete classes that have a specific stop operation
	 */
	protected void doStop(){/* No Op */}
	
	/**
	 * Indicates if this component is started
	 * @return true if this component is started, false otherwise
	 */
	@ManagedAttribute
	public boolean isStarted() {
		return started.get();
	}
	
	// ==========================================================================================
	//		ServiceAspectImpl Methods
	// ==========================================================================================
	/**
	 * Executes a collection
	 * @return the result of the collection. Null implies {@link CollectionResult#COMPLETE}
	 */
	public CollectionResult collect() { return null; }
	
	/**
	 * Schedules the task for repeating execution on the defined period after the defined initial period 
	 * @param period The fixed delay of the executions
	 * @param initial The initial delay when first scheduled
	 * @param unit The unit of the period and initial
	 */
	public void schedule(long period, long initial, TimeUnit unit) {}
	
	/**
	 * Stops the scheduled exection
	 */
	public void cancelSchedule() {}
	
	/**
	 * Returns the scheduled period
	 * @return the scheduled period
	 */
	public long getPeriod() { return -1L; }
	/**
	 * Sets the schedule fixed delay period
	 * @param period the schedule fixed delay period 
	 */	
	public void setPeriod(long period) {}
	/**
	 * Returns the scheduled initial delay
	 * @return the scheduled initial delay
	 */
	public long getInitial() { return -1L; }
	
	/**
	 * Sets the schedule initial delay
	 * @param initial the schedule initial delay 
	 */	
	public void setInitial(long initial) { }
	/**
	 * Returns the schedule initial and period unit
	 * @return the schedule initial and period unit
	 */
	@ManagedAttribute(description="The schedule initial and period unit")
	public TimeUnit getUnit() { return null; }
	/**
	 * Sets the schedule initial and period unit
	 * @param unit the unit
	 */
	@ManagedAttribute
	public void setUnit(TimeUnit unit) {}
	
	
	// ==========================================================================================
	
	/**
	 * Returns the compiled deployment script
	 * @return the groovyObject
	 */
	public GroovyObject getGroovyObject() {
		return this.groovyObject;
	}

	/**
	 * Sets the compiled deployment script
	 * @param groovyObject the groovyObject to set
	 */
	public void setGroovyObject(GroovyObject groovyObject) {
		this.groovyObject = groovyObject;
	}
	
	/**
	 * Returns the state
	 * @return the state
	 */
	public CollectorState getState() {
		return state.get();
	}
	
	/**
	 * Returns the state name
	 * @return the state name
	 */
	@ManagedAttribute(description="The current state of this bean")
	public String getStateName() {
		return state.get().name();
	}
	

	/**
	 * Returns the name of the current logging level
	 * @return the name of the current logging level
	 */
	@ManagedAttribute(description="The logging level of this component")
	public String getLevel() {
		Level level = this.log.getLevel();
		if(level==null) return null;
		return level.toString();
	}
	
	/**
	 * Returns the name of the effective current logging level
	 * @return the name of the effective current logging level
	 */
	@ManagedAttribute(description="The effective logging level of this component")
	public String getEffectiveLevel() {
		return this.log.getEffectiveLevel().toString();
	}
	
	
	/**
	 * Sets the logging level for this instance
	 * @param levelName the name of the logging level for this instance
	 */
	@ManagedAttribute(description="The logging level of this component")
	public void setLevel(String levelName) {
		this.log.setLevel(Level.toLevel(levelName, Level.INFO));
		info("Set Logger to level [", this.log.getLevel().toString(), "]");
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
	
	
	
	
	
	
	/**
	 * Forwards the logging directive if the current level is enabled for the passed level
	 * @param l The requested level
	 * @param msgs The logged messages
	 */
	protected void logAtLevel(APMLogLevel l, Object...msgs) {
		if(this.log.isEnabledFor(l.getLevel())) {
			this.log.log(null, null, l.pCode(), format(msgs), new String[]{}, null);
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
	
	/** EOL bytes */
	private static final byte[] EOL = "\n".getBytes();
	
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
			/* No Op */
		}
		return baos.toString();
	}

	@Override
	public ObjectName getObjectName() throws MalformedObjectNameException {
		return JMXHelper.objectName(parent.getObjectName().toString() + ",bean=" + getClass().getSimpleName());
	}

	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		return getClass().getName();
	}
		

}
