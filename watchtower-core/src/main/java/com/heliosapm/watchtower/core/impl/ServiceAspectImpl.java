/**
 * 
 */
package com.heliosapm.watchtower.core.impl;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.ObjectName;

import org.helios.jmx.util.helpers.StringHelper;
import org.helios.jmx.util.helpers.SystemClock;
import org.helios.jmx.util.helpers.SystemClock.ElapsedTime;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.naming.SelfNaming;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import com.heliosapm.watchtower.collector.CollectorState;
import com.heliosapm.watchtower.core.ServiceAspect;
import com.heliosapm.watchtower.deployer.DeploymentBranch;

/**
 * <p>Title: ServiceAspectImpl</p>
 * <p>Description: The base script wrapper and support class</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.watchtower.core.impl.ServiceAspectImpl</code></b>
 */
@ManagedResource
public class ServiceAspectImpl implements SelfNaming, BeanNameGenerator, InitializingBean, DisposableBean, ApplicationContextAware {
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
	/** The application context this bean is deployed in */
	protected ApplicationContext applicationContext = null;
	/** The parent deployment branch */
	protected DeploymentBranch parent;
	/** The service source file */
	protected File sourceFile;
	/** The service aspect bit mask */
	protected final int aspectBitMask;
	/** A map of closures keyed by the corresponding ServiceAspect */
	protected final Map<ServiceAspect, Closure<?>> closures = new EnumMap<ServiceAspect, Closure<?>>(ServiceAspect.class); 
	/** A map of annotations found on closures keyed by the corresponding ServiceAspect */
	protected final Map<ServiceAspect, Annotation> closureAnnotations = new EnumMap<ServiceAspect, Annotation>(ServiceAspect.class); 
	
	/** This bean's ObjectName */
	protected ObjectName objectName = null;

	/**
	 * Creates a new ServiceAspectImpl
	 */
	public ServiceAspectImpl() {
		aspectBitMask = ServiceAspect.computeBitMask(getClass());		
	}
	
	/**
	 * Loads the closure map
	 */
	protected void loadClosures() {
		ElapsedTime et = SystemClock.startClock();
		final Map<Field, Object> fieldValues = getFieldsOfType(Object.class);
		for(ServiceAspect sa: ServiceAspect.values()) {
			if(sa.isEnabled(aspectBitMask)) {
				for(Field f: fieldValues.keySet()) {
					Annotation ann = f.getAnnotation(sa.getAnnotationType());
					if(ann!=null) {						
						closures.put(sa, (Closure<?>)fieldValues.get(f));
						closureAnnotations.put(sa, ann);
					}
				}
			}
		}
		log.info("Loaded {} closures in {} ms.", closures.size(), et.elapsedMs());
	}
	
	/**
	 * Finds all the declared fields in this class of the passed type
	 * @param clazz The type of the fields to get
	 * @return a map of the field values keyed by the field
	 */
	protected <T> Map<Field, T> getFieldsOfType(Class<T> clazz) {
		Field[] dfields = getClass().getDeclaredFields();		
		Map<Field, T> fields = new HashMap<Field, T>(ServiceAspect.ASPECT_COUNT);
		try {
			for(Field f: dfields) {
				if(clazz.isAssignableFrom(f.getType())) {
					f.setAccessible(true);
					fields.put(f, clazz.cast(f.get(this)));					
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to read field values", ex);
		}
		return fields;
	}
		
	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() {
		groovyObject = (GroovyObject)this;
		loadClosures();
		if(ServiceAspect.STARTER.isEnabled(aspectBitMask)) {
			try {
				start();
			} catch (Exception ex) {
				throw new RuntimeException("Start failed", ex);
			}
		}
	}
	
	/**
	 * Binds the default objects
	 */
	protected void bindDefaultBindings() {
		groovyObject.setProperty("log", log);
		groovyObject.setProperty("deploymentBranch", parent);
		groovyObject.setProperty("appCtx", applicationContext);
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() {
		if(ServiceAspect.STOPPER.isEnabled(aspectBitMask)) {
			stop();
		}		
	}
	
	/**
	 * Called when bean is started.
	 * @throws Exception thrown on any errors starting the bean
	 */	
//	@ManagedOperation
	public void start() throws Exception {
		try {
			
			if(isStarted()) throw new IllegalStateException("Cannot start component once it is started", new Throwable());
			log.info(StringHelper.banner("Starting [%s]", this.beanName));
			doStart();
			started.set(true);
			log.info(StringHelper.banner("Started [%s]", this.beanName));
		} catch (Exception e) {
			log.error("Failed to start [{}]", this.beanName, e);
			throw e;
		}
	}
	
	/**
	 * Called when bean is stopped
	 */	
//	@ManagedOperation
	public void stop() {
		if(!isStarted()) throw new IllegalStateException("Cannot stop component once it is stopped", new Throwable());
		try {
			log.info(StringHelper.banner("Stopping [%s]", this.beanName));
			doStop();
			log.info(StringHelper.banner("Stopped [%s]", this.beanName));
		} catch (Exception ex) {
			log.warn("Problem stopping bean", ex);
			
		} finally {
			started.set(false);
		}		
	}
	
	/**
	 * To be implemented by concrete classes that have a specific start operation
	 * @throws Exception thrown if startup fails
	 */
	protected void doStart() throws Exception {
		Closure<?> closure = closures.get(ServiceAspect.STARTER);
		if(closure==null) {
			log.warn("No closure found for ServiceAspect.STARTER");
			return;
		}
		closure.call();
	}
	
	/**
	 * To be implemented by concrete classes that have a specific stop operation
	 */
	protected void doStop(){
		Closure<?> closure = closures.get(ServiceAspect.STOPPER);
		if(closure==null) {
			log.warn("No closure found for ServiceAspect.STOPPER");
			return;
		}
		closure.call();		
	}
	
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
	public TimeUnit getUnit() { return null; }
	/**
	 * Sets the schedule initial and period unit
	 * @param unit the unit
	 */
	public void setUnit(TimeUnit unit) {}
	
	public String getScriptName() {
		return beanName;
	}
	public void onEvent(Notification notification) {
		
	}
	public void setDependency(String name, Object value) {
		
	}
	// ==========================================================================================
	
	/**
	 * Returns the deployment branch parent
	 * @return the parent
	 */
	public DeploymentBranch getParent() {
		return parent;
	}


	/**
	 * Sets the deployment branch parent 
	 * @param parent the parent to set
	 */
	public void setParent(DeploymentBranch parent) {
		this.parent = parent;
	}
	
	/**
	 * Returns the source file for this service 
	 * @return the sourceFile
	 */
	public File getSourceFile() {
		return sourceFile;
	}


	/**
	 * Sets the source file for this service 
	 * @param sourceFile the sourceFile to set
	 */
	public void setSourceFile(File sourceFile) {
		this.sourceFile = sourceFile;
	}

	
	
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
		log.info("Set Logger to level [{}]", this.log.getLevel().toString());
	}
	
	

	@Override
	public ObjectName getObjectName() throws MalformedObjectNameException {
		return objectName;
	}

	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		return getClass().getName();
	}

	/**
	 * Sets the 
	 * @param objectName the objectName to set
	 */
	public void setObjectName(ObjectName objectName) {
		this.objectName = objectName;
	}

	/**
	 * Returns the application context this bean is deployed in
	 * @return the applicationContext
	 */
	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	/**
	 * Sets application context this bean is deployed in 
	 * @param applicationContext the applicationContext to set
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
		

}
