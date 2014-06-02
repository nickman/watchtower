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

import groovy.lang.GroovyClassLoader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.ObjectName;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.helios.jmx.opentypes.otenabled.OpenTypeEnabledURLClassLoader;
import org.helios.jmx.util.helpers.StringHelper;
import org.helios.jmx.util.helpers.URLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ByteArrayResource;

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
				" xmlns:lang=\"http://www.springframework.org/schema/lang\"" + 
				" xmlns:context=\"http://www.springframework.org/schema/context\"" + 
				" xsi:schemaLocation=\"http://www.springframework.org/schema/beans" + 
				" http://www.springframework.org/schema/beans/spring-beans.xsd" + 
				" http://www.springframework.org/schema/aop" + 
				" http://www.springframework.org/schema/aop/spring-aop.xsd" +
				" http://www.springframework.org/schema/lang http://www.springframework.org/schema/lang/spring-lang.xsd" +
				" http://www.springframework.org/schema/context" + 
				" http://www.springframework.org/schema/context/spring-context.xsd\">" + 
				" <context:annotation-config/><context:mbean-export/> " + 
				"<bean name=\"%s\" class=\"com.heliosapm.watchtower.deployer.DeploymentBranch\"/>" + 		
				"</beans>";
	
	/** Groovy file filter */
	protected static final FilenameFilter GROOVY_FILENAME_FILTER = new FilenameFilter() {
		/**
		 * {@inheritDoc}
		 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
		 */
		@Override
		public boolean accept(File dir, String name) {
			if(name.toLowerCase().endsWith(".groovy")) return true;
			return false;
		}
//		@Override
//		public boolean accept(File pathname) {
//			if(pathname!=null && pathname.isFile() && pathname.toString().toLowerCase().endsWith(".groovy")) return true;
//			return false;
//		}
	};

	
	public static void main(final File subDeploy, ConfigurableApplicationContext parent, DeploymentBranch parentBranch) {
		main(subDeploy, parent, parentBranch, null);
	}
	
	public static void main(final File subDeploy, ConfigurableApplicationContext parent, DeploymentBranch parentBranch, ObjectName objectName) {
		ClassLoader branchClassLoader = loadBranchLibs(subDeploy);
		Map<String, Object> env = new HashMap<String, Object>();
		String[] args = subDeploy.list(GROOVY_FILENAME_FILTER);
		for(int i = 0; i < args.length; i++) {
			args[i] = subDeploy.getAbsolutePath() + File.separator + args[i];
		}
		CompilerConfiguration cc = new CompilerConfiguration(CompilerConfiguration.DEFAULT);
		File compilerConfig = new File(subDeploy, "groovy.properties");
		if(compilerConfig.canRead()) {
			Properties configuration = new Properties();
			try {
				configuration.load(new ByteArrayInputStream(URLHelper.getBytesFromURL(URLHelper.toURL(compilerConfig))));
				LOG.info("Loaded Groovy Compiler Options from [{}]", compilerConfig.getAbsolutePath());
			}  catch (Exception ex) {
				ex.printStackTrace(System.err);
			}			
		}		
		GroovyClassLoader gcl = new GroovyClassLoader(branchClassLoader, cc);
		env.put("branch-file", subDeploy);
		env.put("branch-cl", branchClassLoader);
		env.put("branch-parent", parentBranch);
		env.put("groovy-classloader", gcl);
		if(objectName!=null) {
			env.put("parentObjectName", objectName);
		}
		ConfigurableApplicationContext branchCtx = null;
		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(branchClassLoader);	
			ByteArrayResource subContextXml = new ByteArrayResource(String.format(DEPLOYMENT_BRANCH, subDeploy.getAbsolutePath()).getBytes()) {
				/**
				 * {@inheritDoc}
				 * @see org.springframework.core.io.AbstractResource#getFilename()
				 */
				@Override
				public String getFilename() {				
					return "DeploymentBranch-" + subDeploy + ".xml"; 
				}
			};
			Object[] apps = new Object[args.length + 1];
			apps[0] = subContextXml;
			for(int i = 0; i < args.length; i++) {
				long start = System.currentTimeMillis();
				File gFile = new File(args[i]);
				apps[i+1] = gcl.parseClass(gFile);
				LOG.info("Compiled Groovy Script [{}] in [{}] ms", gFile.getName(), System.currentTimeMillis()-start);
			}
			WatchtowerApplication wapp = new WatchtowerApplication(apps);
			ParentContextApplicationContextInitializer parentSetter = new ParentContextApplicationContextInitializer(parent);		
			wapp.addInitializers(parentSetter);
	
			wapp.setDefaultProperties(env);
			wapp.setShowBanner(false);
			wapp.setWebEnvironment(false);
			branchCtx = wapp.run();
//			DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) branchCtx.getBeanFactory();
//			beanFactory.registerBeanDefinition(subDeploy + "ObjectName", BeanDefinitionBuilder.genericBeanDefinition(ObjectName.class).addConstructorArgValue(
//					branchCtx.getBeansOfType(DeploymentBranch.class).values().iterator().next().getObjectName().toString()
//			).getBeanDefinition());
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
				return new OpenTypeEnabledURLClassLoader(jarUrls.toArray(new URL[jarUrls.size()]), Thread.currentThread().getContextClassLoader());
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
