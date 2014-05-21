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
package com.heliosapm.watchtower.collector;

/**
 * <p>Title: CollectorState</p>
 * <p>Description: Enumerates the possible states of a collector bean</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.collector.CollectorState</code></p>
 */

public enum CollectorState {
	/** The bean is initialized but not started yet */
	INIT,
	/** The bean is started and stable */
	STARTED,
	/** The bean is paused */
	PAUSED,
	/** The bean is stopped */
	STOPPED,
	/** The bean is collecting */
	COLLECTING,
	/** The bean is broken meaning it could not be initialized or started, typically a config problem */
	BROKEN,
	/** The script underlying the bean has a compile error */
	BUST,
	/** The bean has been isolated on account of serial collection or connection errors */
	ISOLATED,
	/** The bean is in a blackout window */
	BLACKOUT;
}
