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

import org.helios.jmx.util.helpers.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;

import com.heliosapm.watchtower.Watchtower;

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
	
	/**
	 * The watchtower main boot
	 * @param subDeploy The sub deployment directory
	 * @param parent The parent app context
	 */
	public static void main(File subDeploy, ConfigurableApplicationContext parent) {
		AnnotationConfigApplicationContext appCtx = new AnnotationConfigApplicationContext();
		appCtx.setParent(parent);
		appCtx.setId("SubDeploy-[" + subDeploy + "]");
		GenericBeanDefinition beanDef = new GenericBeanDefinition();
		beanDef.setBeanClassName(DeploymentBranch.class.getName());
		beanDef.setDescription("DeploymentBranch [" + subDeploy + "]");
		ConstructorArgumentValues ctorArgs = new ConstructorArgumentValues();
		ctorArgs.addGenericArgumentValue(subDeploy);
		beanDef.setConstructorArgumentValues(ctorArgs);
		appCtx.registerBeanDefinition(subDeploy.getName(), beanDef);
		beanDef = new GenericBeanDefinition();
		beanDef.setBeanClassName(AnnotationMBeanExporter.class.getName());
		appCtx.registerBeanDefinition(AnnotationMBeanExporter.class.getSimpleName(), beanDef);
		appCtx.refresh();
		StringBuilder b = new StringBuilder();
		for(String s: appCtx.getBeanDefinitionNames()) {
			b.append("\n\t\t").append(s);
		}
		LOG.info(StringHelper.banner("Started SubContext: [{}]\n\tBean Names:{}"), appCtx.getId(), b.toString());
	}	
	
}
