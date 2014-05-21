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

import org.springframework.jmx.export.annotation.ManagedAttribute;

import com.heliosapm.watchtower.component.ServerComponentBeanMXBean;

/**
 * <p>Title: DeploymentBranchMBean</p>
 * <p>Description: JMX MBean interface for {@link DeploymentBranch} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.deployer.DeploymentBranchMBean</code></p>
 */

public interface DeploymentBranchMBean extends ServerComponentBeanMXBean {
	/**
	 * Returns this branch's parent, or null if it does not have one
	 * @return this branch's parent, or null
	 */
	public DeploymentBranchMBean getParentBranch();
	
	/**
	 * Returns the directory key
	 * @return the directory key
	 */
	public String getDirKey();

	/**
	 * Returns the directory key value
	 * @return the directory key value
	 */
	public String getDirValue();
	
	/**
	 * Returns the absolute directory name for this branch
	 * @return the absolute directory name for this branch
	 */
	public String getDirectoryName();
	
	/**
	 * Returns the classloader for this branch
	 * @return the classloader for this branch
	 */
	public ClassLoader getClassLoader();
	
	/**
	 * Indicates if the watch key is valid
	 * @return true if the watch key is valid, false otherwise
	 */
	public boolean isWatchKeyValid();
	
	/**
	 * Indicates if this deployment branch is a root branch
	 * @return true if this deployment branch is a root branch, false otherwise
	 */
	@ManagedAttribute(description="Indicates if this deployment branch is a root branch")
	public boolean isRoot();

}
