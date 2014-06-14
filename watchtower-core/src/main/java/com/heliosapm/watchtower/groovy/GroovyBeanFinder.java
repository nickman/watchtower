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
package com.heliosapm.watchtower.groovy;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;
import javax.management.StringValueExp;

import org.helios.jmx.util.helpers.JMXHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.heliosapm.watchtower.cache.CacheStatistics;
import com.heliosapm.watchtower.cache.CacheStatisticsMBean;
import com.heliosapm.watchtower.collector.groovy.GroovyCollector;
import com.heliosapm.watchtower.deployer.DeploymentBranch;

/**
 * <p>Title: GroovyBeanFinder</p>
 * <p>Description: A bean finder for indexing and retrieving registered bean instances.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.groovy.GroovyBeanFinder</code></p>
 */

public class GroovyBeanFinder implements GroovyBeanFinderMXBean, NotificationListener, NotificationFilter {
	/** The singleton instance */
	private static volatile GroovyBeanFinder instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object(); 
	
	/** The bean index cache */
	private final Cache<String, Object> beanCache = CacheBuilder.newBuilder()
			.concurrencyLevel(8)
			.initialCapacity(128)
			.weakValues()
			.recordStats()
			.build();
	/** The cache stats */
	private final CacheStatisticsMBean cacheStats = new CacheStatistics(beanCache, "BeanCache");
	
	/** The instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());


	/**
	 * Acquires the singleton GroovyBeanFinder instance
	 * @return the singleton GroovyBeanFinder instance
	 */
	public static GroovyBeanFinder getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new GroovyBeanFinder();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Returns the named application context
	 * @param on The object name of the target application context
	 * @return the named application context
	 */
	public ApplicationContext context(final CharSequence on) {
		return context(JMXHelper.objectName(on));
	}
	
	/** A JMX Query that narrows down matching mbeans to those that are instances of {@link DeploymentBranch} */
	public static final QueryExp CTX_QUERY = Query.isInstanceOf(new StringValueExp(DeploymentBranch.class.getName()));
	
	/**
	 * Returns the named application context
	 * @param on The object name of the target application context
	 * @return the named application context
	 */
	public ApplicationContext context(final ObjectName on) {
		if(on==null) throw new IllegalArgumentException("The passed object name was null");
		ObjectName[] matches = JMXHelper.query(on, CTX_QUERY);
		if(matches.length>1) {
			throw new RuntimeException("Expression [" + on + "] too general. Expected 1 result, got " + matches.length);
		} else if(matches.length==0) {
			throw new RuntimeException("No matches for expression [" + on + "]");
		}
		Object obj = JMXHelper.getAttribute(on, "Context");
		if(obj==null) throw new RuntimeException("Matched expression [" + on + "] but no Context was found");
		if(!ApplicationContext.class.isInstance(obj)) throw new RuntimeException("Matched expression [" + on + "] but object was not an ApplicationContext. Was [" + obj.getClass().getName() + "]");
		return (ApplicationContext)obj;
		
	}
	
	

	/**
	 * Returns the named bean from the app context registered at the passed object name
	 * @param on The object name of the target application context
	 * @param beanName The name of the bean to retrieve
	 * @return the located bean
	 */
	public Object bean(final CharSequence on, final CharSequence beanName) {
		if(on==null) throw new IllegalArgumentException("The passed object name was null");
		if(beanName==null) throw new IllegalArgumentException("The passed bean name was null");
		final String key = key(on, beanName);
		try {
			return beanCache.get(key, new Callable<Object>(){
				@Override
				public Object call() throws Exception {
					return context(on).getBean(beanName.toString());
				}
			});
		} catch (ExecutionException ex) {
			throw new RuntimeException("Failed to find bean name [" + key + "]", ex);
		}			
	}
	
	/**
	 * Returns the named bean from the app context registered at the passed object name
	 * @param on The object name of the target application context
	 * @param beanName The name of the bean to retrieve
	 * @param type The required type of the retrieved bean
	 * @return the located bean
	 */
	public <T> T bean(final CharSequence on, final CharSequence beanName, final Class<T> type) {
		if(on==null) throw new IllegalArgumentException("The passed object name was null");
		if(beanName==null) throw new IllegalArgumentException("The passed bean name was null");
		if(type==null) throw new IllegalArgumentException("The passed bean type was null");
		final String key = key(on, beanName, type.getName());
		try {
			return (T)beanCache.get(key, new Callable<Object>(){
				@Override
				public Object call() throws Exception {
					return context(on).getBean(beanName.toString(), type);
				}
			});
		} catch (ExecutionException ex) {
			throw new RuntimeException("Failed to find bean name [" + key + "]", ex);
		}					
	}

	/**
	 * Returns the named bean from the app context registered at the passed object name
	 * @param on The object name of the target application context
	 * @param beanName The name of the bean to retrieve
	 * @param type The required type of the retrieved bean
	 * @return the located bean
	 */
	public <T> T bean(final ObjectName on, final CharSequence beanName, final Class<T> type) {
		if(on==null) throw new IllegalArgumentException("The passed object name was null");
		if(beanName==null) throw new IllegalArgumentException("The passed bean name was null");
		if(type==null) throw new IllegalArgumentException("The passed bean type was null");
		return bean(on.toString(), beanName, type);
	}
	
	/**
	 * Returns the named bean from the app context registered at the passed object name
	 * @param on The object name of the target application context
	 * @param beanName The name of the bean to retrieve
	 * @return the located bean
	 */
	public Object bean(final ObjectName on, final CharSequence beanName) {
		if(on==null) throw new IllegalArgumentException("The passed object name was null");
		if(beanName==null) throw new IllegalArgumentException("The passed bean name was null");		
		return bean(on.toString(), beanName);
	}
	
	/**
	 * Returns a map of beans of the specified type keyed by the bean name
	 * @param on The object name of the target application context
	 * @param type The type of the beans to retrieve
	 * @return the [possibly empty] map of beans
	 */
	public <T> Map<String, T> beans(final CharSequence on, final Class<T> type) {
		if(on==null) throw new IllegalArgumentException("The passed object name was null");
		if(type==null) throw new IllegalArgumentException("The passed type was null");				
		return context(on).getBeansOfType(type);
	}
	
	/**
	 * Returns a map of beans of the specified type keyed by the bean name
	 * @param on The object name of the target application context
	 * @param type The type of the beans to retrieve
	 * @return the [possibly empty] map of beans
	 */
	public <T> Map<String, T> beans(final ObjectName on, final Class<T> type) {
		if(on==null) throw new IllegalArgumentException("The passed object name was null");
		if(type==null) throw new IllegalArgumentException("The passed type was null");						
		return beans(on, type);
	}
	
	/**
	 * Returns a map of beans annotated with the specified annotation type keyed by the bean name
	 * @param on The object name of the target application context
	 * @param annotation The annotation the beans should be annotated with
	 * @return the [possibly empty] map of beans
	 */
	public Map<String, Object> annotated(final CharSequence on, final Class<? extends Annotation> annotation) {
		if(on==null) throw new IllegalArgumentException("The passed object name was null");
		if(annotation==null) throw new IllegalArgumentException("The passed annotation type was null");
		return context(on).getBeansWithAnnotation(annotation);
	}
	
	/**
	 * Returns a map of beans annotated with the specified annotation type keyed by the bean name
	 * @param on The object name of the target application context
	 * @param annotation The annotation the beans should be annotated with
	 * @return the [possibly empty] map of beans
	 */
	public Map<String, Object> annotated(final ObjectName on, final Class<? extends Annotation> annotation) {
		if(on==null) throw new IllegalArgumentException("The passed object name was null");
		if(annotation==null) throw new IllegalArgumentException("The passed annotation type was null");
		return annotated(on.toString(), annotation);

	}
	/**
	 * Returns the named environment entry from the app context registered at the passed object name
	 * @param on The object name of the target application context
	 * @param envKey The key of the environment entry to retrieve
	 * @return the located environment entry
	 */
	public Object env(final CharSequence on, final CharSequence envKey) {
		if(on==null) throw new IllegalArgumentException("The passed object name was null");
		if(envKey==null) throw new IllegalArgumentException("The passed environment key was null");
		final String key = key(on, envKey);
		try {
			return beanCache.get(key, new Callable<Object>(){
				@Override
				public Object call() throws Exception {
					return context(on).getEnvironment().getProperty(envKey.toString());
				}
			});
		} catch (ExecutionException ex) {
			throw new RuntimeException("Failed to find environment entry [" + key + "]", ex);
		}			
	}
	
	/**
	 * Returns the named environment entry from the app context registered at the passed object name
	 * @param on The object name of the target application context
	 * @param envKey The key of the environment entry to retrieve
	 * @return the located environment entry
	 */
	public Object env(final ObjectName on, final CharSequence envKey) {
		if(on==null) throw new IllegalArgumentException("The passed object name was null");
		if(envKey==null) throw new IllegalArgumentException("The passed environment key was null");		
		return env(on.toString(), envKey);
	}	
	
	
	/**
	 * Builds a cache key
	 * @param args The key components
	 * @return the built cache key
	 */
	private String key(Object ...args) {
		StringBuilder b = new StringBuilder();
		if(args==null || args.length==0) return "";
		for(Object o: args) {
			b.append(o==null ? "" : o.toString()).append("/");
		}
		return b.deleteCharAt(b.length()-1).toString();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.groovy.GroovyBeanFinderMXBean#getCacheStats()
	 */
	public CacheStatisticsMBean getCacheStats() {
		return cacheStats;
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.groovy.GroovyBeanFinderMXBean#invalidateAll()
	 */
	public void invalidateAll() {
		cacheStats.invalidateAll();
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.groovy.GroovyBeanFinderMXBean#cleanup()
	 */
	public void cleanup() {
		cacheStats.cleanup();
	}

	/**
	 * Creates a new GroovyBeanFinder
	 */
	private GroovyBeanFinder() {
		JMXHelper.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this, this, null);
		JMXHelper.registerMBean(this, JMXHelper.objectName(getClass()));
	}

	@Override
	public boolean isNotificationEnabled(Notification notification) {
		if(notification instanceof MBeanServerNotification) {
			MBeanServerNotification msn = (MBeanServerNotification)notification;
			return JMXHelper.isInstanceOf(msn.getMBeanName(), DeploymentBranch.class.getName());
		}
		return false;
	}

	@Override
	public void handleNotification(Notification notification, Object handback) {
		MBeanServerNotification msn = (MBeanServerNotification)notification;
		String keyPrefix = msn.getMBeanName().toString();
		if(MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(msn.getType())) {
			ApplicationContext ctx = context(keyPrefix);
			log.info("Cached DeploymentBranch for [{}]", keyPrefix);
			Map<String, GroovyCollector> colls = ctx.getBeansOfType(GroovyCollector.class);
			for(Map.Entry<String, GroovyCollector> entry: colls.entrySet()) {
				String beanKey = key(keyPrefix, entry.getKey());
				beanCache.put(beanKey, entry.getValue());
				log.info("Cached Bean for [{}]", beanKey);
			}
			beanCache.putAll(ctx.getBeansOfType(GroovyCollector.class));
		} else if(MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(msn.getType())) {			
			beanCache.invalidate(keyPrefix);
			log.info("Invalidated DeploymentBranch for [{}]", keyPrefix);
			for(String key: beanCache.asMap().keySet()) {
				if(key.startsWith(keyPrefix)) {
					log.info("Invalidated bean for [{}]", key);
					beanCache.invalidate(key);
				}
			}
		}		
	}
	
}
