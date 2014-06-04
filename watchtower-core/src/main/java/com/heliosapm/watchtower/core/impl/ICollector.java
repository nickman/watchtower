/**
 * 
 */
package com.heliosapm.watchtower.core.impl;

import org.springframework.jmx.export.annotation.ManagedOperation;

import com.heliosapm.watchtower.core.IServiceAspect;

/**
 * <p>Title: ICollector</p>
 * <p>Description: Defines the collection service aspect</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.watchtower.core.impl.ICollector</code></b>
 */

public interface ICollector extends IServiceAspect {
	/**
	 * Executes a collection
	 * @return the result of the collection. Null implies {@link CollectionResult#COMPLETE}
	 */
	@ManagedOperation(description="Executes the configured collection")
	public CollectionResult collect();
}
