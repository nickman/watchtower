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
package com.heliosapm.watchtower.deployer;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helios.jmx.util.helpers.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;

import com.heliosapm.watchtower.Watchtower;
import com.heliosapm.watchtower.WatchtowerApplication;

/**
 * <p>Title: SubContextBoot</p>
 * <p>Description: A spring app bootstrap</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.deployer.SubContextBoot</code></p>
 */

public class SubContextBoot {
	/** Static class logger */
	protected static final Logger LOG = LoggerFactory.getLogger(Watchtower.class);

	/**
	 * Creates a new SubContextBoot
	 */
	private SubContextBoot() {}
	
	public static final String BEANS_HEADER = "<beans xmlns=\"http://www.springframework.org/schema/beans\"" +
			  " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + 
				" xmlns:aop=\"http://www.springframework.org/schema/aop\"" + 
				" xmlns:context=\"http://www.springframework.org/schema/context\"" + 
				" xsi:schemaLocation=\"http://www.springframework.org/schema/beans" + 
				" http://www.springframework.org/schema/beans/spring-beans.xsd" + 
				" http://www.springframework.org/schema/aop" + 
				" http://www.springframework.org/schema/aop/spring-aop.xsd" + 
				" http://www.springframework.org/schema/context" + 
				" http://www.springframework.org/schema/context/spring-context.xsd\"><context:annotation-config/><context:mbean-export/></beans>";	

	public static final String DEPLOYMENT_BRANCH = "<beans xmlns=\"http://www.springframework.org/schema/beans\"" +
			  " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + 
				" xmlns:aop=\"http://www.springframework.org/schema/aop\"" + 
				" xmlns:context=\"http://www.springframework.org/schema/context\"" + 
				" xsi:schemaLocation=\"http://www.springframework.org/schema/beans" + 
				" http://www.springframework.org/schema/beans/spring-beans.xsd" + 
				" http://www.springframework.org/schema/aop" + 
				" http://www.springframework.org/schema/aop/spring-aop.xsd" + 
				" http://www.springframework.org/schema/context" + 
				" http://www.springframework.org/schema/context/spring-context.xsd\">" + 
				
				"<bean class=\"\"com.heliosapm.watchtower.deployer.DeploymentBranch\">" + 
				
				"</beans>";
	
	//<context:annotation-config/><context:mbean-export/>
	
	
	public static void main(File subDeploy, ConfigurableApplicationContext parent) {
		ClassLoader branchClassLoader = loadBranchLibs(subDeploy);
		Map<String, Object> env = new HashMap<String, Object>();
		env.put("branch-file", subDeploy);
		env.put("branch-cl", branchClassLoader);
		ConfigurableApplicationContext branchCtx = null;
		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(branchClassLoader);	
			WatchtowerApplication wapp = new WatchtowerApplication(Class.forName("com.heliosapm.watchtower.deployer.DeploymentBranch", true, branchClassLoader));
			ParentContextApplicationContextInitializer parentSetter = new ParentContextApplicationContextInitializer(parent);		
			wapp.addInitializers(parentSetter);
	
			wapp.setDefaultProperties(env);
			wapp.setShowBanner(false);
			wapp.setWebEnvironment(false);
			branchCtx = wapp.run();
			branchCtx.setId("SubDeploy-[" + subDeploy + "]");
		} catch (Exception ex) {
			LOG.error("Failed to deploy Branch for [" + subDeploy + "]", ex);
			throw new RuntimeException(ex);
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
		if(LOG.isDebugEnabled()) {
			StringBuilder b = new StringBuilder();
			for(String s: branchCtx.getBeanDefinitionNames()) {
				b.append("\n\t\t").append(s);
			}
			LOG.debug(StringHelper.banner("Started SubContext: [{}]\n\tBean Names:{}"), branchCtx.getId(), b.toString());
		} else {
			LOG.info("Started SubContext: [{}]", branchCtx.getId());
		}
		
	}
	
	
	/**
	 * The watchtower main boot
	 * @param subDeploy The sub deployment directory
	 * @param parent The parent app context
	 */
	@SuppressWarnings("resource")
	public static void mainx(File subDeploy, ConfigurableApplicationContext parent) {
		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		AnnotationConfigApplicationContext appCtx = null;
		try {
			ClassLoader branchClassLoader = loadBranchLibs(subDeploy);
			Thread.currentThread().setContextClassLoader(branchClassLoader);			
			appCtx = new AnnotationConfigApplicationContext();
			appCtx.setParent(parent);
			appCtx.setId("SubDeploy-[" + subDeploy + "]");
			ByteArrayResource fileWatcherXml = new ByteArrayResource(BEANS_HEADER.getBytes()) {
				/**
				 * {@inheritDoc}
				 * @see org.springframework.core.io.AbstractResource#getFilename()
				 */
				@Override
				public String getFilename() {				
					return "DeploymentWatchService.xml";
				}
			};
			
			XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(appCtx);
			xmlReader.loadBeanDefinitions(fileWatcherXml);
			
			GenericBeanDefinition beanDef = new GenericBeanDefinition();
			beanDef.setBeanClassName("com.heliosapm.watchtower.deployer.DeploymentBranch");
			beanDef.setDescription("DeploymentBranch [" + subDeploy + "]");
			ConstructorArgumentValues ctorArgs = new ConstructorArgumentValues();
			ctorArgs.addGenericArgumentValue(subDeploy);
			ctorArgs.addGenericArgumentValue(branchClassLoader);
			beanDef.setConstructorArgumentValues(ctorArgs);
			appCtx.registerBeanDefinition(subDeploy.getName(), beanDef);
//			beanDef = new GenericBeanDefinition();
//			beanDef.setBeanClassName(AnnotationMBeanExporter.class.getName());
//			MutablePropertyValues mpv = new MutablePropertyValues();
//			mpv.add("server", JMXHelper.getHeliosMBeanServer());
//			beanDef.setPropertyValues(mpv);
//			appCtx.registerBeanDefinition(AnnotationMBeanExporter.class.getSimpleName(), beanDef);
			appCtx.refresh();
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
		
		if(LOG.isDebugEnabled()) {
			StringBuilder b = new StringBuilder();
			for(String s: appCtx.getBeanDefinitionNames()) {
				b.append("\n\t\t").append(s);
			}
			LOG.debug(StringHelper.banner("Started SubContext: [{}]\n\tBean Names:{}"), appCtx.getId(), b.toString());
		} else {
			LOG.info("Started SubContext: [{}]", appCtx.getId());
		}
	}	
	
	
	/**
	 * Returns a ClassLoader for the specified lib directory, or the context class loader if the dir does not exist or has no jars
	 * @param subDeploy The directory to search in
	 * @return a classloader
	 */
	protected static ClassLoader loadBranchLibs(File subDeploy) {
		File libDir = new File(subDeploy, "lib");
		if(libDir.exists() && libDir.isDirectory()) {
			Path pDir = libDir.toPath();
			List<URL> jarUrls = listSourceFiles(pDir);
			if(!jarUrls.isEmpty()) {
				LOG.info("---LIBS: {}", jarUrls.toString());
				return new URLClassLoader(jarUrls.toArray(new URL[jarUrls.size()]), Thread.currentThread().getContextClassLoader());
			}
		} 
		return Thread.currentThread().getContextClassLoader();
	}
	
	/**
	 * Finds all jar files in the passed directory
	 * @param dir The directory to search in
	 * @return a [possibly empty] list of URLs of jar files
	 */
	protected static List<URL> listSourceFiles(Path dir)  {
	       List<URL> result = new ArrayList<URL>();
	       try {
		       try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{jar}")) {
		           for (Path entry: stream) {
		               result.add(entry.toAbsolutePath().toUri().toURL());
		           }
		       } catch (DirectoryIteratorException ex) {
		           // I/O error encounted during the iteration, the cause is an IOException
		           throw ex.getCause();
		       }
	       } catch (Exception ex) {/* No Op */}
	       return result;
	   }
}
