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

import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.jmxmp.JMXMPConnectorServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.jmx.util.helpers.StringHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * <p>Title: JMXMPServer</p>
 * <p>Description: The watchtower JMXMP server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.jmx.server.JMXMPServer</code></p>
 */
@Configuration
@EnableAutoConfiguration
public class JMXMPServer implements ApplicationListener<ContextRefreshedEvent> {
	@Value("${jmxmp.iface:0.0.0.0}")
	protected String bindInterface;
	@Value("${jmxmp.port:8006}")
	protected int port;
	
	protected JMXConnectorServer server = null;
	protected JMXServiceURL serviceURL = null;
	protected ObjectName objectName = JMXHelper.objectName(JMXMPConnectorServer.class);
	protected final Logger log = LogManager.getLogger(getClass());
	/**
	 * Creates a new JMXMPServer
	 */
	public JMXMPServer() {

	}

	public void start() {
		try {
			log.info(StringHelper.banner("Starting JMXMPServer...."));
			serviceURL = new JMXServiceURL(String.format("service:jmx:jmxmp://%s:%s", bindInterface, port));
			server = JMXConnectorServerFactory.newJMXConnectorServer(serviceURL, null, JMXHelper.getHeliosMBeanServer());
			//JMXHelper.registerMBean(server, objectName);
			server.start();
			log.info(StringHelper.banner("Started JMXMPServer on [{}:{}]"), bindInterface, port);
		} catch (Exception ex) {
			log.error("Failed to start JMXMPServer on [{}:{}]", bindInterface, port, ex);
		}
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		start();		
	}
}
