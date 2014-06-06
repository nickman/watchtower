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
package com.heliosapm.watchtower.jmx.server;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.jmxmp.JMXMPConnectorServer;

import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.jmx.util.helpers.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.naming.SelfNaming;

/**
 * <p>Title: JMXMPServer</p>
 * <p>Description: The watchtower JMXMP server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.jmx.server.JMXMPServer</code></p>
 */
@Configuration
@EnableAutoConfiguration
@ManagedResource
public class JMXMPServer implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, SelfNaming {
	@Value("${jmxmp.iface:0.0.0.0}")
	protected String bindInterface;
	@Value("${jmxmp.port:8006}")
	protected int port;
	
	protected JMXConnectorServer server = null;
	protected JMXServiceURL serviceURL = null;
	protected ObjectName objectName = JMXHelper.objectName(JMXMPConnectorServer.class);
	protected ApplicationContext applicationContext = null;
	protected final AtomicBoolean started = new AtomicBoolean(false);
	protected final Logger log = LoggerFactory.getLogger(JMXMPServer.class);
	/**
	 * Creates a new JMXMPServer
	 */
	public JMXMPServer() {

	}

	public void start() {
		if(started.compareAndSet(false, true)) {
			try {				
				log.info(StringHelper.banner("Starting JMXMPServer...."));
				serviceURL = new JMXServiceURL(String.format("service:jmx:jmxmp://%s:%s", bindInterface, port));
				server = JMXConnectorServerFactory.newJMXConnectorServer(serviceURL, null, JMXHelper.getHeliosMBeanServer());
				//JMXHelper.registerMBean(server, objectName);
				server.start();
				JMXHelper.registerMBean(server, objectName);
				log.info(StringHelper.banner("Started JMXMPServer on [{}:{}]"), bindInterface, port);
			} catch (Exception ex) {
				log.error("Failed to start JMXMPServer on [{}:{}]", bindInterface, port, ex);
			}
		}
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if(applicationContext != event.getApplicationContext()) return;
		start();		
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.jmx.export.naming.SelfNaming#getObjectName()
	 */
	@Override
	public ObjectName getObjectName() throws MalformedObjectNameException {
		return objectName;
	}
}
