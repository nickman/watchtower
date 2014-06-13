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
	INIT(true, false),
	/** The bean is started and stable */
	STARTING(false, false),	
	/** The bean is started and stable */
	STARTED(true, false),
	/** The bean is paused */
	PAUSED(true, false),
	/** The bean is stopping */
	STOPPING(false, false),
	
	/** The bean is stopped */
	STOPPED(true, false),
	
	/** The bean is collecting */
	COLLECTING(false, false),
	/** The bean is broken meaning it could not be initialized or started, typically a config problem */
	BROKEN(true, true),
	/** The script underlying the bean has a compile error */
	BUST(true, true),
	/** The bean has been isolated on account of serial collection or connection errors */
	ISOLATED(true, false),
	/** The bean cannot connect to an unreliable resource (like a connection) */
	DOWN(true, false),
	/** The bean is in a blackout window */
	BLACKOUT(true, false);
	
	private CollectorState(boolean stable, boolean terminal) {
		this.stable = stable;
		this.terminal = terminal;
	}
	
	/** Indicates the state is stable, suggesting state changes can be applied */
	public final boolean stable;
	/** Indicates the state is terminal and will not change */
	public final boolean terminal;
	
	/**
	 * Indicates the state is stable, suggesting state changes can be applied
	 * @return true if stable, false otherwise
	 */
	public boolean isStable() {
		return stable;
	}
	
	/**
	 * Indicates the state is terminal and will not change
	 * @return true if terminal, false otherwise
	 */
	public boolean isTerminal() {
		return terminal;
	}
}
