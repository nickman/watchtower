/**
 * 
 */
package com.heliosapm.watchtower.core.impl;

import org.springframework.jmx.export.annotation.ManagedOperation;

import com.heliosapm.watchtower.core.IServiceAspect;

/**
 * <p>Title: ILifecycle</p>
 * <p>Description: Defines the lifecycle service aspect</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.watchtower.core.impl.ILifecycle</code></b>
 */

public interface ILifecycle extends IServiceAspect {
	/**
	 * Starts the lifecycle controlled object
	 * @throws Exception thrown on any error starting
	 */
	@ManagedOperation(description="Starts this service")
	public void start() throws Exception;
	/**
	 * Stops the lifecycle controlled object
	 */
	@ManagedOperation(description="Stops this service")
	public void stop();
	
}
