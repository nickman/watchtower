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
package com.heliosapm.watchtower;

import java.util.HashMap;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.helios.jmx.concurrency.JMXManagedThreadPool;
import org.helios.jmx.util.helpers.JMXHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.naming.SelfNaming;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.heliosapm.watchtower.core.CollectionExecutor;
import com.heliosapm.watchtower.core.CollectionScheduler;
import com.heliosapm.watchtower.core.EventExecutor;
import com.heliosapm.watchtower.deployer.DeploymentWatchService;
import com.heliosapm.watchtower.jmx.server.JMXMPServer;

/**
 * <p>Title: WatchtowerCore</p>
 * <p>Description: The core watchtower service</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.WatchtowerCore</code></p>
 */
//@Configuration
@RestController
//@EnableAutoConfiguration
@ManagedResource
public class WatchtowerCore implements ApplicationContextAware, SelfNaming, ApplicationListener<ContextRefreshedEvent>, NotificationListener, NotificationFilter {
	/** The watchtower core singleton instance */
	private static volatile WatchtowerCore instance = null;
	/** The watchtower core singleton instance ctor lock */
	private static final Object lock = new Object();
	/** The core watchtower object name */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("com.heliosapm.watchtower.core:service=Watchtower");
	
	/** The core watchtower service deployment beans xml template */
	public static final String BEANS_HEADER = "<beans xmlns=\"http://www.springframework.org/schema/beans\"" +
			  " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + 
				" xmlns:aop=\"http://www.springframework.org/schema/aop\"" + 
				" xmlns:context=\"http://www.springframework.org/schema/context\"" + 
				" xsi:schemaLocation=\"http://www.springframework.org/schema/beans" + 
				" http://www.springframework.org/schema/beans/spring-beans.xsd" + 
				" http://www.springframework.org/schema/aop" + 
				" http://www.springframework.org/schema/aop/spring-aop.xsd" + 
				" http://www.springframework.org/schema/context" + 
				" http://www.springframework.org/schema/context/spring-context.xsd\">" +  // <context:annotation-config/> 
				"<bean id=\"DeploymentWatchService\" class=\"com.heliosapm.watchtower.deployer.DeploymentWatchService\" factory-method=\"getWatchService\"/>" + 
				"</beans>";

	/** The watchtower root application context */
	private ApplicationContext appCtx;
	/** Instance logger */
	private final Logger log = LoggerFactory.getLogger(getClass());
	/** The deployment file change watch service */
	private final DeploymentWatchService watchService = DeploymentWatchService.getWatchService();
	/** The byte array resource to boot in the core service */
	ByteArrayResource fileWatcherXml = new ByteArrayResource(BEANS_HEADER.getBytes()) {
		@Override
		public String getFilename() {				
			return "WatchtowerCore.xml";
		}
	};
	/** Thread pool for async deployment tasks */
	protected JMXManagedThreadPool deploymentThreadPool = null;
	/** Thread pool for collection execution */
	protected JMXManagedThreadPool collectionThreadPool = null;
	/** Thread pool for notification broadcast */
	protected JMXManagedThreadPool notificationThreadPool = null;
	
	
	/**
	 * Acquires the watchtower singleton instance
	 * @return the watchtower singleton instance
	 */
	public static WatchtowerCore getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new WatchtowerCore();
				}
			}
		}
		return instance;
	}

	/**
	 * Creates a new WatchtowerCore
	 */
	private WatchtowerCore() {
		log.info("Created WatchtowerCore");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if(event.getApplicationContext()!=appCtx) return;
		WatchtowerApplication children = new WatchtowerApplication(JMXMPServer.class, CollectionScheduler.class, CollectionExecutor.class, EventExecutor.class, fileWatcherXml);
		children.setShowBanner(false);
		children.setWebEnvironment(false);
		children.setParent(appCtx);
		ApplicationContext ctx = children.run();
	}
	
	/**
	 * Web app access stub
	 * @return a place holder message
	 */
	@RequestMapping("/")	
    String home() {
        return "This is Watchtower, come in please.";
    }
	
	
	/**
	 * Returns the application id
	 * @return the application id
	 * @see org.springframework.context.ApplicationContext#getId()
	 */
	@ManagedAttribute
	public String getAppCtxId() {
		return appCtx.getId();
	}

	/**
	 * Returns the application name
	 * @return the application name
	 * @see org.springframework.context.ApplicationContext#getApplicationName()
	 */
	@ManagedAttribute
	public String getApplicationName() {
		return appCtx.getApplicationName();
	}

	/**
	 * Returns the number of bound beans
	 * @return the number of bound beans
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionCount()
	 */
	@ManagedAttribute
	public int getBeanDefinitionCount() {
		return appCtx.getBeanDefinitionCount();
	}

	/**
	 * Returns the application context display name
	 * @return the application context display name
	 * @see org.springframework.context.ApplicationContext#getDisplayName()
	 */
	@ManagedAttribute
	public String getAppCtxDisplayName() {
		return appCtx.getDisplayName();
	}

	/**
	 * Returns the names of the bound bean names
	 * @return the names of the bound bean names
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionNames()
	 */
	@ManagedAttribute
	public String[] getBeanDefinitionNames() {
		return appCtx.getBeanDefinitionNames();
	}
	
	/**
	 * Returns the application context
	 * @return the application context
	 */
	@ManagedAttribute
	public ApplicationContext getContext() {
		return appCtx;
	}
	
	/**
	 * Returns a map of the app context's bound beans keyed by the bean name
	 * @return a map of the app context's bound beans keyed by the bean name
	 */
	@ManagedAttribute
	public Map<String, String> getBeanDefinitionTypes() {
		Map<String, String> map = new HashMap<String, String>(getBeanDefinitionCount());
		for(String s: getBeanDefinitionNames()) {
			map.put(s,  appCtx.getBean(s).getClass().getName());
		}
		return map;
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.jmx.export.naming.SelfNaming#getObjectName()
	 */
	@Override
	public ObjectName getObjectName() throws MalformedObjectNameException {
		return OBJECT_NAME;
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		appCtx = applicationContext;		
	}

	/**
	 * Returns the file watch service
	 * @return the watchService
	 */
	public DeploymentWatchService getWatchService() {
		return watchService;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
	 */
	@Override
	public boolean isNotificationEnabled(Notification notification) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		// TODO Auto-generated method stub
		//com.heliosapm.watchtower.core.threadpools:service=ThreadPool,name=		
	}
	
	

}
