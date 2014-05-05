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

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Title: Watchtower</p>
 * <p>Description: The main watchtower bootstrap class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.Watchtower</code></p>
 */
@RestController
@EnableAutoConfiguration
public class Watchtower {
//	/** The watchtower root application context */
//	protected final ConfigurableApplicationContext appCtx;
//	/** The command line arguments */
//	protected final String[] commanLineArgs;
	/** The static watchtower instance */
	protected static Watchtower watchtower = null;
	
//	/**
//	 * Creates a new Watchtower
//	 * @param appCtx The watchtower root application context
//	 * @param args The command line arguments
//	 */
//	public Watchtower(ConfigurableApplicationContext appCtx, String[] args) {
//		this.appCtx = appCtx;
//		this.commanLineArgs = args;
//	}
	
	public Watchtower() {
		
	}
	
	@RequestMapping("/")
    String home() {
        return "This is Watchtower, come in please.";
    }

	/**
	 * The watchtower main boot
	 * @param args The command line arguments
	 */
	public static void main(String[] args) {
		@SuppressWarnings("resource")
		ConfigurableApplicationContext appCtx = new WatchtowerApplication(new Object[]{Watchtower.class}).run(args);
//		watchtower = new Watchtower(appCtx, args);
	}

}
