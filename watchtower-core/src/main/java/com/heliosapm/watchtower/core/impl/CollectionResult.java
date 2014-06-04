/**
 * 
 */
package com.heliosapm.watchtower.core.impl;

/**
 * <p>Title: CollectionResult</p>
 * <p>Description: Enumerates the possible collection results</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.watchtower.core.impl.CollectionResult</code></b>
 */

public enum CollectionResult {
	/** Collection completed */
	COMPLETE,
	/** Collection failed due to a failed connection */
	NOCONN,
	/** Collection failed due to an unspecified collect time error */
	FAILED,
	/** Collection completed partially */
	PARTIAL;
}
