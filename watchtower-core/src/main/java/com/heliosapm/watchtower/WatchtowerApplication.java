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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;

import com.heliosapm.watchtower.deployer.BeanDefinitionResource;

/**
 * <p>Title: WatchtowerApplication</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.WatchtowerApplication</code></p>
 */

public class WatchtowerApplication extends SpringApplication {
	protected ApplicationContext parent = null;
	/**
	 * Creates a new WatchtowerApplication
	 * @param sources
	 */
	public WatchtowerApplication(Object... sources) {
		super(sources);
		setHeadless(false);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new WatchtowerApplication
	 * @param resourceLoader
	 * @param sources
	 */
	public WatchtowerApplication(ResourceLoader resourceLoader, Object... sources) {
		super(resourceLoader, sources);
		setHeadless(false);
		// TODO Auto-generated constructor stub
	}
	
	public void addSources(Object...source) {
		Set<Object> set = new HashSet<Object>(source.length);
		Collections.addAll(set, source);
		setSources(set);
		
	}

	/**
	 * Load beans into the application context.
	 * @param context the context to load beans into
	 * @param sources the sources to load
	 */
	protected void load(ApplicationContext context, Object[] sources) {
//		BeanDefinitionLoader loader = createBeanDefinitionLoader(
//				getBeanDefinitionRegistry(context), sources);
//		if (this.beanNameGenerator != null) {
//			loader.setBeanNameGenerator(this.beanNameGenerator);
//		}
//		if (this.resourceLoader != null) {
//			loader.setResourceLoader(this.resourceLoader);
//		}
//		if (this.environment != null) {
//			loader.setEnvironment(this.environment);
//		}
//		loader.load();
		BeanDefinitionRegistry registry = getBeanDefinitionRegistry(context);
		List<Object> nonSupported = new ArrayList<Object>();
		for(Object o: sources) {
			if(o==null) continue;
			if(o instanceof BeanDefinitionResource) {
				BeanDefinitionResource bdr = (BeanDefinitionResource)o;
				BeanDefinition beanDefinition = bdr.getBeanDefinition(); 
				registry.registerBeanDefinition(bdr.getBeanName(), beanDefinition);
			} else {
				nonSupported.add(o);
			}
			
		}
		if(!nonSupported.isEmpty()) {
			super.load(context, nonSupported.toArray(new Object[0]));
		}
	}
	
	/**
	 * @param context the application context
	 * @return the BeanDefinitionRegistry if it can be determined
	 */
	private BeanDefinitionRegistry getBeanDefinitionRegistry(ApplicationContext context) {
		if (context instanceof BeanDefinitionRegistry) {
			return (BeanDefinitionRegistry) context;
		}
		if (context instanceof AbstractApplicationContext) {
			return (BeanDefinitionRegistry) ((AbstractApplicationContext) context)
					.getBeanFactory();
		}
		throw new IllegalStateException("Could not locate BeanDefinitionRegistry");
	}
	

	
	/**
	 * Print a simple banner message to the console. Subclasses can override this method
	 * to provide additional or alternative banners.
	 * @see #setShowBanner(boolean)
	 */
	protected void printBanner() {
		Banner.write(System.out);
	}	
	
	/**
	 * Strategy method used to create the {@link ApplicationContext}. By default this
	 * method will respect any explicitly set application context or application context
	 * class before falling back to a suitable default.
	 * @return the application context (not yet refreshed)
	 * @see #setApplicationContextClass(Class)
	 */
	public ConfigurableApplicationContext createApplicationContext() {
		ConfigurableApplicationContext appCtx = super.createApplicationContext();
		if(parent!=null) {
			appCtx.setEnvironment((ConfigurableEnvironment) parent.getEnvironment());
			appCtx.setId("WatchTowerCore");			
			ParentContextApplicationContextInitializer pinit = new ParentContextApplicationContextInitializer(parent);
			this.addInitializers(pinit);
		}
		return appCtx;
	}
	
	@Override
	protected void refresh(ApplicationContext applicationContext) {
		// TODO Auto-generated method stub
		super.refresh(applicationContext);
	}

	/**
	 * Sets the 
	 * @param parent the parent to set
	 */
	public void setParent(ApplicationContext parent) {
		this.parent = parent;
	}	
	

}
