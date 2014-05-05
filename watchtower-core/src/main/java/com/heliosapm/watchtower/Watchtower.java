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

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.helios.jmx.util.helpers.StringHelper;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.WebSocketAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.heliosapm.watchtower.core.CollectionExecutor;
import com.heliosapm.watchtower.core.CollectionScheduler;

/**
 * <p>Title: Watchtower</p>
 * <p>Description: The main watchtower bootstrap class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.Watchtower</code></p>
 */
@RestController
@EnableAutoConfiguration
public class Watchtower extends TextWebSocketHandler implements ApplicationContextAware,  ApplicationListener<ContextRefreshedEvent> {
	/** The watchtower root application context */
	protected ConfigurableApplicationContext appCtx;
	/** The watchtower core services application context */
	protected ConfigurableApplicationContext coreCtx;
	
//	/** The command line arguments */
//	protected final String[] commanLineArgs;
	/** The static watchtower instance */
	protected static Watchtower watchtower = null;
	
	/** Static class logger */
	protected static final Logger LOG = LogManager.getLogger(Watchtower.class);
	
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
	
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
		session.sendMessage(new TextMessage("This is Watchtower, come in please."));
	}

	/**
	 * The watchtower main boot
	 * @param args The command line arguments
	 */
	public static void main(String[] args) {
		WatchtowerApplication wapp = new WatchtowerApplication(Watchtower.class, WebSocketAutoConfiguration.class);
		wapp.setWebEnvironment(true);
		wapp.run(args);
		LOG.info(StringHelper.banner("Watchtower Started"));
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		appCtx = (ConfigurableApplicationContext) applicationContext;
		appCtx.addApplicationListener(this);
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if(event.getApplicationContext()!=appCtx) return;
		WatchtowerApplication children = new WatchtowerApplication(CollectionScheduler.class, CollectionExecutor.class, WebSocketConfig.class);
		children.setShowBanner(false);
		children.setWebEnvironment(false);
		children.setParent(appCtx);
		coreCtx = children.run();
		LOG.info(StringHelper.banner("Watchtower Core Services Started"));
	}

}