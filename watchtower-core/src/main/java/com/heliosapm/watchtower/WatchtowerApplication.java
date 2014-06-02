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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;

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
	protected ConfigurableApplicationContext createApplicationContext() {
		ConfigurableApplicationContext appCtx = super.createApplicationContext();
		if(parent!=null) {
			appCtx.setEnvironment((ConfigurableEnvironment) parent.getEnvironment());
			appCtx.setId("WatchTowerCore");			
			ParentContextApplicationContextInitializer pinit = new ParentContextApplicationContextInitializer(parent);
			this.addInitializers(pinit);
		}
		return appCtx;
	}

	/**
	 * Sets the 
	 * @param parent the parent to set
	 */
	public void setParent(ApplicationContext parent) {
		this.parent = parent;
	}	
	

}
