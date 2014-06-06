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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;

/**
 * <p>Title: Watchtower</p>
 * <p>Description: The main watchtower bootstrap class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.Watchtower</code></p>
 */
public class Watchtower {
	/** The cl option to disable web */
	public static final String NO_WEB_CL = "--noweb";
	/** Static class logger */	
	protected static final Logger LOG = LoggerFactory.getLogger(Watchtower.class);
	
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
				" http://www.springframework.org/schema/context/spring-context.xsd\"><context:annotation-config/>" + 
				"</beans>";
	
	/** The byte array resource to boot in the core service */
	private static final ByteArrayResource fileWatcherXml = new ByteArrayResource(BEANS_HEADER.getBytes()) {
		@Override
		public String getFilename() {				
			return "WatchtowerBoot.xml";
		}
	};

	/**
	 * The watchtower main boot
	 * @param args The command line arguments
	 */
	public static void main(String[] args) {
		WatchtowerApplication wapp = new WatchtowerApplication(
				WatchtowerCore.class, 
				AnnotationMBeanExporter.class,
				fileWatcherXml
		);
		wapp.setHeadless(false);
		boolean webEnabled = true;
		if(args!=null && args.length>0) {
			if(Arrays.binarySearch(args, NO_WEB_CL) >= 0) {
				webEnabled = false;
			}
		}		
		wapp.setWebEnvironment(webEnabled);
		ApplicationContext ctx = wapp.run(args);
		StringBuilder b = new StringBuilder("\n\t$$$$$$$$$$\n\tBootstrap Beans\n\t$$$$$$$$$$");
		for(String s: ctx.getBeanDefinitionNames()) {
			b.append("\n\t[").append(s).append("]  :  [").append(ctx.getBean(s).getClass().getName()).append("]");
		}
		LOG.info(b.toString());

	}


	
//	/**
//	 * {@inheritDoc}
//	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
//	 */
//	@Override
//	public void onApplicationEvent(ContextRefreshedEvent event) {
//		if(event.getApplicationContext()!=appCtx) return;
//		ByteArrayResource fileWatcherXml = new ByteArrayResource((BEANS_HEADER + "<bean id=\"DeploymentWatchService\" class=\"com.heliosapm.watchtower.deployer.DeploymentWatchService\" factory-method=\"getWatchService\"/></beans>"
//					+ "<bean id=\"Watchtower\" class=\"com.heliosapm.watchtower.Watchtower\" factory-method=\"getInstance\"/></beans>"
//				).getBytes()) {
//			/**
//			 * {@inheritDoc}
//			 * @see org.springframework.core.io.AbstractResource#getFilename()
//			 */
//			@Override
//			public String getFilename() {				
//				return "DeploymentWatchService.xml";
//			}
//		};
//		
//		WatchtowerApplication children = new WatchtowerApplication(AnnotationMBeanExporter.class, JMXMPServer.class, CollectionScheduler.class, CollectionExecutor.class, EventExecutor.class, fileWatcherXml);
//		
//		children.setShowBanner(false);
//		children.setWebEnvironment(false);
//		children.setParent(appCtx);
//		
//		ApplicationContext ctx = children.run();
//		if(LOG.isDebugEnabled()) {
//			StringBuilder b = new StringBuilder();
//			for(String s: ctx.getBeanDefinitionNames()) {
//				b.append("\n\t\t").append(s);
//			}
//			LOG.debug(StringHelper.banner("Watchtower Core Services Started\n\tBeans: {}"), b.toString());
//		} else {
//			LOG.info("Watchtower Core Services Started");
//		}
//		// LOG.info(StringHelper.banner("Started SubContext: [{}]\n\tBean Names:{}"), appCtx.getId(), Arrays.toString(appCtx.getBeanDefinitionNames()));
//	}
//	

}
