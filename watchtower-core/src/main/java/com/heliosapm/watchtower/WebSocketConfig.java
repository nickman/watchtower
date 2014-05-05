/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2014, Helios Development Group and individual contributors
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

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * <p>Title: WebSocketConfig</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.WebSocketConfig</code></p>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
	
	protected WebSocketHandlerRegistry registry;
	protected HandshakeHandler handshakeHandler;
	
	protected Watchtower watchtower = null;
	
	protected final Logger log = LogManager.getLogger(getClass());
	/**
	 * Creates a new WebSocketConfig
	 */
	public WebSocketConfig() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.web.socket.config.annotation.WebSocketConfigurer#registerWebSocketHandlers(org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry)
	 */
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		this.registry = registry;		
		if(watchtower!=null) {
			registry.addHandler(watchtower, "/watchtower");//  . addInterceptors(new HttpSessionHandshakeInterceptor());
			log.info("Watchtower websock handler registered");
		}
		log.info("Acquired WebSocketHandlerRegistry");
	}


	/**
	 * Returns the 
	 * @return the watchtower
	 */
	public Watchtower getWatchtower() {
		return watchtower;
	}

	/**
	 * Sets the watchtower instance
	 * @param watchtower the watchtower to set
	 */
	@Autowired
	public void setWatchtower(Watchtower watchtower) {
		this.watchtower = watchtower;
		if(registry!=null) {
			registry.addHandler(watchtower, "/"). addInterceptors(new HttpSessionHandshakeInterceptor());
			log.info("Watchtower websock handler registered");
		}
		
	}


}
