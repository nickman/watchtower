/**
 * 
 */
package com.heliosapm.watchtower.core.impl;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;

import java.io.File;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.ObjectName;

import org.helios.jmx.concurrency.JMXManagedScheduler;
import org.helios.jmx.concurrency.JMXManagedThreadPool;
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
import com.heliosapm.watchtower.core.CollectionExecutor;
import com.heliosapm.watchtower.core.CollectionScheduler;
import com.heliosapm.watchtower.core.EventExecutor;
import com.heliosapm.watchtower.core.ServiceAspect;
import com.heliosapm.watchtower.deployer.DeploymentBranch;
import com.heliosapm.watchtower.groovy.annotation.Scheduled;
import com.heliosapm.watchtower.groovy.annotation.ScriptName;

/**
 * <p>Title: ServiceAspectImpl</p>
 * <p>Description: The base script wrapper and support class</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.watchtower.core.impl.ServiceAspectImpl</code></b>
 */
@ManagedResource
public class ServiceAspectImpl implements SelfNaming, BeanNameGenerator, InitializingBean, DisposableBean, ApplicationContextAware {
	/** The static logger context */
	protected static final LoggerContext logCtx = (LoggerContext)LoggerFactory.getILoggerFactory();
	/** Instance logger */
	protected final Logger log = logCtx.getLogger(getClass());
	/** Thread pool for collection execution */
	protected final JMXManagedThreadPool collectionThreadPool = CollectionExecutor.getCollectionExecutor();
	/** Thread pool for notification broadcast */
	protected final JMXManagedThreadPool notificationThreadPool = EventExecutor.getEventExecutor();
	/** Scheduler for collection scheduling */
	protected final JMXManagedScheduler collectionScheduler = CollectionScheduler.getCollectionScheduler();
	
	/** The compiled deployment script */
	protected GroovyObject groovyObject = null;
	/** The deployment/script name */
	protected String beanName;
	/** The started state of this component */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	/** The state of this component */
	protected final AtomicReference<CollectorState> collectorState = new AtomicReference<CollectorState>(CollectorState.INIT); 
	/** The application context this bean is deployed in */
	protected ApplicationContext applicationContext = null;
	/** The parent deployment branch */
	protected DeploymentBranch parent;
	/** The service source file */
	protected File sourceFile;
	// ======================  Scheduling  ====================== 
	/** A map of schedle handles keyed by the closure name that was annotated within a map keyed by the corresponding ServiceAspect */
	protected final Map<String, ScheduledClosure<?>> scheduleHandles = new ConcurrentHashMap<String, ScheduledClosure<?>>(); 
	
	
	// ======================  Aspect Management ======================
	/** The service aspect bit mask */
	protected final int aspectBitMask;
	/** A map of closures keyed by the corresponding ServiceAspect */
	protected final Map<ServiceAspect, Map<String, Closure<?>>> closures = new EnumMap<ServiceAspect, Map<String, Closure<?>>>(ServiceAspect.class); 
	/** A map of annotations found on closures keyed by the corresponding ServiceAspect */
	protected final Map<ServiceAspect, Map<String, Annotation>> closureAnnotations = new EnumMap<ServiceAspect, Map<String, Annotation>>(ServiceAspect.class); 
	
	/** This bean's ObjectName */
	protected ObjectName objectName = null;

	/**
	 * Creates a new ServiceAspectImpl
	 */
	public ServiceAspectImpl() {
		aspectBitMask = ServiceAspect.computeBitMask(getClass());		
		if(ServiceAspect.NAMED.isEnabled(aspectBitMask)) {
			beanName = getClass().getAnnotation(ScriptName.class).value();
		} else {
			beanName = getClass().getSimpleName();
		}
	}
	
	/**
	 * Transitions the state of this bean
	 * @param state The new state
	 */
	protected void transitionState(CollectorState state) {
		if(state==null) throw new IllegalArgumentException("The passed state was null");
		CollectorState oldState = collectorState.getAndSet(state);
		if(state!=oldState) {
			log.debug("Transitioned State from [{}]--->[{}]", oldState, state);
			// FIXME:  notif
		}
		
		
	}
	
	/**
	 * <p>Title: ScheduledClosure</p>
	 * <p>Description: Wraps a scheduled closure</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.watchtower.core.impl.ServiceAspectImpl.ScheduledClosure</code></p>
	 * @param <T> The return type of the scheduled closure
	 */
	class ScheduledClosure<T> implements Serializable, Runnable, Callable<T> {
		/**  */
		private static final long serialVersionUID = 3311828463751427076L;
		/** Thread pool for collection execution */
		protected final JMXManagedThreadPool collectionThreadPool = CollectionExecutor.getCollectionExecutor();
		/** Thread pool for notification broadcast */
		protected final JMXManagedThreadPool notificationThreadPool = EventExecutor.getEventExecutor();
		/** Scheduler for collection scheduling */
		protected final CollectionScheduler collectionScheduler = CollectionScheduler.getCollectionScheduler();
		/** static class logger */
		protected final Logger log = logCtx.getLogger(ScheduledClosure.class);

		/** The closure to be invoked on a schedule */
		Closure<T> closure;
		/** The schedule handle */
		ScheduledFuture<?> scheduleHandle = null;
		/** The scheduling period */
		long schedulePeriod = -1;
		/** The initial delay */
		long initialDelay = -1;
		/** The scheduling period unit */
		TimeUnit schedulePeriodUnit = null;
		/** The scheduling period cron */
		String schedulePeriodCron = null;
		/** The field name of the closure */
		String closureName;
		/** The most recent scheduled execution return value */
		final AtomicReference<T> lastResult = new AtomicReference<T>(null);
		/**
		 * Creates a new ScheduledClosure
		 * @param closureName The closure name (the name of the field the closure was declared in)
		 * @param closure The closure instance to schedule
		 * @param schedulePeriod The scheduling period
		 * @param initialDelay The initial delay before the first execution
		 * @param schedulePeriodUnit The scheduling period unit
		 */
		ScheduledClosure(String closureName, Closure<T> closure, long schedulePeriod, long initialDelay, TimeUnit schedulePeriodUnit) {
			this.closureName = closureName;
			this.closure = closure;
			this.schedulePeriod = schedulePeriod;
			this.schedulePeriodUnit = schedulePeriodUnit;
			this.initialDelay = initialDelay;
			scheduleHandle = collectionScheduler.scheduleWithFixedDelay(this, initialDelay, schedulePeriod, schedulePeriodUnit);
		}
		
		/**
		 * Creates a new ScheduledClosure
		 * @param closureName The closure name (the name of the field the closure was declared in)
		 * @param closure The closure instance to schedule
		 * @param scheduledAnnotation The @Scheduled annotation providing the scheduling details
		 */
		ScheduledClosure(String closureName, Closure<T> closure, Scheduled scheduledAnnotation) {
			this.closureName = closureName;
			this.closure = closure;
			if(scheduledAnnotation.cron().trim().isEmpty()) {
				this.schedulePeriod = scheduledAnnotation.period();
				this.schedulePeriodUnit = scheduledAnnotation.unit();
				this.initialDelay = scheduledAnnotation.initialDelay();
				scheduleHandle = collectionScheduler.scheduleWithFixedDelay(this, initialDelay, schedulePeriod, schedulePeriodUnit);				
			} else {
				this.schedulePeriodCron = scheduledAnnotation.cron();
				scheduleHandle = collectionScheduler.scheduleWithCron(this, schedulePeriodCron);
			}			
		}
		
		/**
		 * Cancels the schedule
		 */
		void cancel() {
			if(scheduleHandle!=null) {
				scheduleHandle.cancel(true); // FIXME:  Configurable
			}
			scheduleHandle=null;
			scheduleHandles.remove(closureName);
		}
		
		/**
		 * Creates a new ScheduledClosure
		 * @param closureName The closure name (the name of the field the closure was declared in)
		 * @param closure The closure instance to schedule
		 * @param schedulePeriodCron The scheduling period cron expression
		 */
		ScheduledClosure(String closureName, Closure<T> closure, String schedulePeriodCron) {
			this.closureName = closureName;
			this.closure = closure;
			this.schedulePeriodCron = schedulePeriodCron;	
			scheduleHandle = collectionScheduler.scheduleWithCron(this, schedulePeriodCron);
		}
		
		
		/**
		 * Replaces this object with a descriptive string when serialized
		 * @return a descriptive string
		 * @throws ObjectStreamException thrown on errors writing to the output stream
		 */
		Object writeReplace() throws ObjectStreamException {
			return closureName + ":[" + (schedulePeriodCron!=null ? schedulePeriodCron : (schedulePeriod + "/" + schedulePeriodUnit) + "]");
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public T call() throws Exception {
			if(log.isDebugEnabled()) log.debug("Scheduled Execution of [{}]", this);
			try {
				T result = closure.call();
				lastResult.set(result);
				return result;
			} catch (Throwable t) {
				log.error("Scheduled Execution of task [{}] failed", t);
				throw new Exception("Scheduled Execution of task [{}] failed", t);
			}
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			if(log.isDebugEnabled()) log.debug("Scheduled Execution of [{}]", this);
			try {
				lastResult.set(closure.call());
			} catch (Throwable t) {
				log.error("Scheduled Execution of task [{}] failed", t);
			}
		}
		
		
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
						Map<String, Closure<?>> cmap = closures.get(sa);
						if(cmap==null) {
							cmap = new ConcurrentHashMap<String, Closure<?>>();
							closures.put(sa, cmap);
						}
						cmap.put(f.getName(), (Closure<?>)fieldValues.get(f));
						Map<String, Annotation> amap = closureAnnotations.get(sa);
						if(amap==null) {
							amap = new ConcurrentHashMap<String, Annotation>();
							closureAnnotations.put(sa, amap);
						}
						amap.put(f.getName(), ann);
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
		transitionState(CollectorState.STARTING);
		groovyObject = (GroovyObject)this;
		loadClosures();
		if(ServiceAspect.SCHEDULED.isEnabled(aspectBitMask)) {
			try {
				Map<String, Closure<?>> cmap = closures.get(ServiceAspect.SCHEDULED);
				Map<String, Annotation> amap = closureAnnotations.get(ServiceAspect.SCHEDULED);
				for(Map.Entry<String, Closure<?>> entry: cmap.entrySet()) {
					Scheduled scheduledAnnotation = (Scheduled)amap.get(entry.getKey());
					ScheduledClosure<?> sc = new ScheduledClosure<>(entry.getKey(), entry.getValue(), scheduledAnnotation);
					scheduleHandles.put(entry.getKey(), sc);					
				}
			} catch (Exception ex) {
				throw new RuntimeException("Start failed", ex);
			}
		}				
		if(ServiceAspect.STARTER.isEnabled(aspectBitMask)) {
			try {
				start();
				transitionState(CollectorState.STARTED);
			} catch (Exception ex) {
				throw new RuntimeException("Start failed", ex);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() {
		//started.set(false);
		transitionState(CollectorState.STOPPING);
		for(ScheduledClosure<?> handle: scheduleHandles.values()) {
			log.info("Stopping Schedule for [{}]", handle.closureName);
			handle.cancel();
		}
		if(ServiceAspect.STOPPER.isEnabled(aspectBitMask)) {
			stop();
			transitionState(CollectorState.STOPPED);
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
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return objectName.toString();
	}
	 	
	/**
	 * To be implemented by concrete classes that have a specific start operation
	 * @throws Exception thrown if startup fails
	 */
	protected void doStart() throws Exception {
		// FIXME:  closures is empty after start. Will need to keep it for restart.
		for(Map.Entry<String, Closure<?>> entry: closures.get(ServiceAspect.STARTER).entrySet()) {
			try {
				entry.getValue().call();
			} catch (Exception ex) {
				log.error("Failed to execute @Start aspect named [" + entry.getKey() + "]", ex);
			}
		}
	}
	
	/**
	 * To be implemented by concrete classes that have a specific stop operation
	 */
	protected void doStop(){
		for(Map.Entry<String, Closure<?>> entry: closures.get(ServiceAspect.STOPPER).entrySet()) {
			try {
				entry.getValue().call();
			} catch (Exception ex) {
				log.error("Failed to execute @Stop aspect named [" + entry.getKey() + "]", ex);
			}
		}		
		closureAnnotations.clear();
		closures.clear();
		groovyObject = null;
		scheduleHandles.clear();
		
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
	//		ServiceAspectImpl COLLECT Methods
	// ==========================================================================================
	/**
	 * Executes a collection
	 * @return the result of the collection. Null implies {@link CollectionResult#COMPLETE}
	 */
	public CollectionResult collect() { return null; }
	
	// ==========================================================================================
	//		ServiceAspectImpl SCHEDULE Methods
	// ==========================================================================================
	/**
	 * Schedules the task for repeating execution on the defined period after the defined initial period 
	 * @param name The name of the closure to schedule
	 * @param period The fixed delay of the executions
	 * @param initial The initial delay when first scheduled
	 * @param unit The unit of the period and initial
	 */
	@SuppressWarnings("unchecked")
	public void schedule(String name, long period, long initial, TimeUnit unit) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty");
		if(unit==null) throw new IllegalArgumentException("The passed unit was null or empty");
		Closure<?> closure  = closures.get(ServiceAspect.SCHEDULED).get(name);
		if(closure==null) throw new IllegalArgumentException("No closure mapped to name [" + name + "]");
		ScheduledClosure<?> sc = scheduleHandles.get(name);
		if(sc==null) {
			synchronized(scheduleHandles) {
				sc = scheduleHandles.get(name);
				if(sc==null) {
					sc = new ScheduledClosure(name, closure, period, initial, unit);
					scheduleHandles.put(name, sc);
				}
			}
		}
	}
	
	/**
	 * Schedules the task for repeating execution on the defined period after the defined initial period 
	 * @param name The name of the closure to schedule
	 * @param period The fixed delay of the executions
	 * @param initial The initial delay when first scheduled
	 * @param unit The name of the unit of the period and initial
	 */
	public void schedule(String name, long period, long initial, String unit) {		
		if(unit==null) throw new IllegalArgumentException("The passed unit was null or empty");
		schedule(name, period, initial, TimeUnit.valueOf(unit.trim().toUpperCase())); 
	}	

	/**
	 * Schedules the task for repeating execution based on the passed cron expression
	 * @param name The name of the closure to schedule
	 * @param cron The cron expression defining the execution schedule
	 */
	public void schedule(String name, String cron) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty");
		Closure<?> closure  = closures.get(ServiceAspect.SCHEDULED).get(name);
		if(closure==null) throw new IllegalArgumentException("No closure mapped to name [" + name + "]");
		ScheduledClosure<?> sc = scheduleHandles.get(name);
		if(sc==null) {
			synchronized(scheduleHandles) {
				sc = scheduleHandles.get(name);
				if(sc==null) {
					sc = new ScheduledClosure(name, closure, cron);
					scheduleHandles.put(name, sc);
				}
			}
		}
		
	}
	
	/**
	 * Returns the names of the closures annotated for scheduled execution
	 * @return an array of closure names
	 */
	public String[] getScheduledNames() {
		return scheduleHandles.keySet().toArray(new String[0]);
	}
	
	/**
	 * Stops the scheduled exection
	 * @param name The name of the scheduled task to cancel
	 */
	public void cancelSchedule(String name) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty");
		ScheduledClosure<?> sc = scheduleHandles.get(name);		
		if(sc!=null) {
			log.info("Cancelling Schedule [{}]", name);
			sc.cancel();
		} else {
			log.warn("Attempted to cancel non-existent Schedule [{}]", name);
		}
	}
	
	/**
	 * Returns the scheduled period
	 * @param name The name of the scheduled task to get the period for
	 * @return the scheduled period. -1 means WTF or that the task is a cron task
	 */
	public long getPeriod(String name) {
		ScheduledClosure<?> sc = scheduleHandles.get(name);
		if(sc!=null) return sc.schedulePeriod;
		return -1;
	}

	/**
	 * Returns the scheduled initial delay
	 * @param name The name of the scheduled task to get the initial delay for
	 * @return the scheduled initial delay. -1 means WTF or that the task is a cron task
	 */
	public long getInitial(String name) {
		ScheduledClosure<?> sc = scheduleHandles.get(name);
		if(sc!=null) return sc.initialDelay;
		return -1;
		
	}
	
	
	/**
	 * Returns the schedule initial and period unit
	 * @param name The name of the scheduled task to get the initial delay for
	 * @return the schedule initial and period unit. null means WTF or that the task is a cron task
	 */
	public TimeUnit getUnit(String name) {
		ScheduledClosure<?> sc = scheduleHandles.get(name);
		if(sc!=null) return sc.schedulePeriodUnit;
		return null;
	}
	
	/**
	 * Returns the schedule cron expression
	 * @param name The name of the scheduled task to get the cron expression for
	 * @return the scheduled task cron expression. null means WTF or that the task is a not cron task
	 */
	public String getCron(String name) {
		ScheduledClosure<?> sc = scheduleHandles.get(name);
		if(sc!=null) return sc.schedulePeriodCron;
		return null;		
	}
	

	/**
	 * Returns a sumary of the scehduled tasks
	 * @return an array of scheduled closure objects
	 */
	public ScheduledClosure<?>[] getScheduledTasks() {
		return scheduleHandles.values().toArray(new ScheduledClosure[0]);
	}
	

	// ==========================================================================================
	//		ServiceAspectImpl EVENT Methods
	// ==========================================================================================

	public void onEvent(Notification notification) {
		
	}
	
	// ==========================================================================================
	//		ServiceAspectImpl DEPENDENCY Methods
	// ==========================================================================================
	
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
		return collectorState.get();
	}
	
	/**
	 * Returns the state name
	 * @return the state name
	 */
	@ManagedAttribute(description="The current state of this bean")
	public String getStateName() {
		return collectorState.get().name();
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
		return beanName;
	}
	
	/**
	 * Returns the bean name which will be the simple class name defined in the script,
	 * or the value specified in the @ScriptName annotation
	 * @return the script name
	 */
	public String getScriptName() {
		return beanName;
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
