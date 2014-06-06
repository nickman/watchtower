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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.helios.jmx.concurrency.JMXManagedThreadPool;
import org.helios.jmx.util.helpers.ConfigurationHelper;
import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.jmx.util.helpers.StringHelper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.jmx.export.annotation.ManagedNotification;
import org.springframework.jmx.export.annotation.ManagedNotifications;

import ch.qos.logback.classic.Logger;

import com.heliosapm.watchtower.component.ServerComponentBean;
import com.heliosapm.watchtower.core.EventExecutorMBean;

/**
 * <p>Title: DeploymentWatchService</p>
 * <p>Description: A centralized watch service singleton to process file change events in the deployment directory</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.deployer.DeploymentWatchService</code></p>
 */

@EnableAutoConfiguration
@ManagedNotifications({
	@ManagedNotification(notificationTypes={"com.heliosapm.watchtower.deployer.DeploymentBranch.start"}, name="javax.management.Notification", description="Notifies when a new DeploymentBranch has started")
})

public class DeploymentWatchService extends ServerComponentBean implements Runnable, PathWatchEventListener {
	/** The singleton instance */
	private static volatile DeploymentWatchService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** Indicates if the service is started */
	private final AtomicBoolean started = new AtomicBoolean(false);
	
	/** The deployment root paths */
	private final Set<Path> deploymentRoots = new CopyOnWriteArraySet<Path>();
	
	/** Static class logger */
	private static final Logger log = getLogger(DeploymentWatchService.class);
	
	/** A map of registered watch services keyed by the Path root */
	private final NonBlockingHashMap<Path, WatchService> watchServices = new NonBlockingHashMap<Path, WatchService>();
	
	/** A map of event polling threads keyed by the path root of the file system they are polling */
	private final NonBlockingHashMap<Path, Thread> fileSystemEventPollers = new NonBlockingHashMap<Path, Thread>(); 
	
	/** A map of wrapped watch keys keyed by the watch key  */
	private final NonBlockingHashMap<WatchKey, WrappedWatchKey> watchKeys = new NonBlockingHashMap<WatchKey, WrappedWatchKey>(); 
	
	/** The delay queue processor */
	private Thread delayQueueProcessor = null;
	
	/** Thread pool which executes all the actual deployments */
	private final JMXManagedThreadPool deploymentThreadPool;
	
	/** Flag indicating if the pollers should keep running */
	protected final AtomicBoolean keepRunning  = new AtomicBoolean(true);
	/** The thread group that the watch service pollers run in */
	protected final ThreadGroup threadGroup = new ThreadGroup(getClass().getSimpleName() + "PollingThreads");
	/** The processing delay queue that ensures the same file is not processed concurrently for two different events */
	protected final DelayQueue<FileEvent> processingQueue = new DelayQueue<FileEvent>();
	
	
	
	/** The system property name for configuring the default deployment root directories */
	public static final String DEPLOY_ROOT_DIRS_PROP = "com.heliosapm.watchtower.deploydirs";
	/** The default deployment root directories */
	public static final String DEFAULT_DEPLOY_ROOT_DIRS = System.getProperty("user.home") + File.separator + ".watchtower" + File.separator + "deploy";
	
	/**
	 * Creates a new DeploymentWatchService
	 */
	private DeploymentWatchService() {
		String cfg = ConfigurationHelper.getSystemThenEnvProperty(DEPLOY_ROOT_DIRS_PROP, DEFAULT_DEPLOY_ROOT_DIRS);
		String[] rootDirs = cfg.split(",");
		for(String rootDir: rootDirs) {
			String dirName = rootDir.trim();
			File dirFile = new File(dirName);
			if(dirFile.exists() && dirFile.isDirectory()) {
				Path path = dirFile.toPath();
				deploymentRoots.add(path);
			}
		}
		delayQueueProcessor = new Thread(threadGroup, this, "DelayQueueProcessor");
		delayQueueProcessor.setDaemon(true);		
		log.info("Added {} root deployment directories", deploymentRoots.size());	
		deploymentThreadPool = new JMXManagedThreadPool(JMXHelper.objectName(String.format(EventExecutorMBean.OBJECT_NAME_TEMPLATE, "DeploymentService")), "DeploymentService", true);
	}
	
	/**
	 * Starts the watch service
	 */
	protected void doStart() {
		if(started.get()) return;
		log.info(StringHelper.banner("Starting DeploymentWatchService....."));
		// Start the delay queue processor
		delayQueueProcessor.start();
		// Create a thread for each registered deploy dir
		for(Path path: deploymentRoots) {
			startDeploymentRootWatcher(path);
		}
		this.applicationContext.getAutowireCapableBeanFactory().autowireBean(this);
		for(Path path: deploymentRoots) {
			//enqueueFileEvent(0, new FileEvent(path.toAbsolutePath().toFile().getAbsolutePath(), ENTRY_CREATE, this));
			SubContextBoot.main(path.toAbsolutePath().toFile(), applicationContext, null, null);
		}
		log.info(StringHelper.banner("DeploymentWatchService Started"));
		started.set(true);
	}

	/**
	 * Stops the watch service
	 */
	protected void doStop() {
		log.info(StringHelper.banner("Stopping DeploymentWatchService....."));
		keepRunning.set(false);
		threadGroup.interrupt();
		
		// MORE TO DO HERE.
		log.info(StringHelper.banner("DeploymentWatchService Stopped"));
	}
	
	/**
	 * Acquires the DeploymentWatchService singleton
	 * @return the DeploymentWatchService singleton
	 */
	public static DeploymentWatchService getWatchService() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new DeploymentWatchService();
				}
			}
		}
		return instance;
	}
	
	/**
	 * <p>The delay queue processor</p>
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		log.info(StringHelper.banner("Starting DeploymentWatchService DelayQueue Processor"));
		while(true) {
			try {
				final FileEvent fe = processingQueue.take();
					deploymentThreadPool.submit(new Runnable(){
						public void run() {
							PathWatchEventListener listener = fe.getListener();
							if(listener!=null) {
								File file = new File(fe.getFileName()); 							
								if(fe.getEvent().kind()==ENTRY_CREATE) {
									if(file.isDirectory()) listener.onDirectoryCreated(file);
									else listener.onFileCreated(file);
								} else if(fe.getEvent().kind()==ENTRY_DELETE) {
									if(file.isDirectory()) listener.onDirectoryDeleted(file);
									else listener.onFileDeleted(file);						
								} else if(fe.getEvent().kind()==ENTRY_MODIFY) {
									if(file.isDirectory()) listener.onDirectoryModified(file);
									else listener.onFileModified(file);												
								}
							}
						}
					});				
			} catch (Exception ex) {
				if(keepRunning.get()) {
					if(Thread.interrupted()) Thread.interrupted();
				} else {
					break;
				}
			}
		}
	}
	
	protected class WrappedWatchKey implements WatchKey {
		/** The delegate watch key */
		private final WatchKey delegate;
		/** The path being watched */
		private final Path path;
		/** The event listener */
		private final PathWatchEventListener listener;
		
		/**
		 * Creates a new WrappedWatchKey
		 * @param delegate The delegate watch key
		 * @param listener The optional event listener
		 * @param path The path being watched
		 */
		public WrappedWatchKey(WatchKey delegate, PathWatchEventListener listener, Path path) {
			this.delegate = delegate;
			this.listener = listener;
			this.path = path;
		}
		
		

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((delegate == null) ? 0 : delegate.hashCode());
			result = prime * result
					+ ((listener == null) ? 0 : listener.hashCode());
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			return result;
		}



		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof WrappedWatchKey))
				return false;
			WrappedWatchKey other = (WrappedWatchKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (delegate == null) {
				if (other.delegate != null)
					return false;
			} else if (!delegate.equals(other.delegate))
				return false;
			if (listener == null) {
				if (other.listener != null)
					return false;
			} else if (!listener.equals(other.listener))
				return false;
			if (path == null) {
				if (other.path != null)
					return false;
			} else if (!path.equals(other.path))
				return false;
			return true;
		}



		/**
		 * {@inheritDoc}
		 * @see java.nio.file.WatchKey#isValid()
		 */
		public boolean isValid() {
			boolean valid = delegate.isValid();
			if(!valid) {
				watchKeys.remove(delegate);
				if(listener!=null) listener.onCancel(this);
			}
			return valid;
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.nio.file.WatchKey#reset()
		 */
		public boolean reset() {
			boolean reset = delegate.reset();
			if(!reset) {
				watchKeys.remove(delegate);
				if(listener!=null) listener.onCancel(this);
			}
			return reset;
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.nio.file.WatchKey#cancel()
		 */
		public void cancel() {
			delegate.cancel();
			watchKeys.remove(delegate);
			if(listener!=null) listener.onCancel(this);
		}
		

		/**
		 * {@inheritDoc}
		 * @see java.nio.file.WatchKey#pollEvents()
		 */
		public List<WatchEvent<?>> pollEvents() {
			return delegate.pollEvents();
		}



		/**
		 * {@inheritDoc}
		 * @see java.nio.file.WatchKey#watchable()
		 */
		public Watchable watchable() {
			return delegate.watchable();
		}



		private DeploymentWatchService getOuterType() {
			return DeploymentWatchService.this;
		}
		
		
	}
	
	/**
	 * Returns a watch key for the passed path
	 * @param path The path to generate a watch key for
	 * @param listener An optional listener to be notified of path events on the generated watch key
	 * @param events the watch events to subscribe to
	 * @return the generated watch key
	 */
	public WatchKey getWatchKey(final Path path,  PathWatchEventListener listener, WatchEvent.Kind<?>... events) {
		final Path root = path.getRoot();
		WatchService ws = getWatchService(root);
		try {
			final WatchKey wk = path.register(ws, events);
			final WrappedWatchKey wwk = new WrappedWatchKey(wk, listener, path); 
			watchKeys.put(wk, wwk);
			return wwk;
		} catch (IOException e) {
			log.error("Failed to get watch key for path [{}]", path, e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Starts a watch service poller for a deployment root
	 * @param deploymentPathRoot The root deployment path
	 */
	protected void startDeploymentRootWatcher(Path deploymentPathRoot) {
		if(deploymentPathRoot==null) throw new IllegalArgumentException("Passed deployment path root was null");
		if(!fileSystemEventPollers.containsKey(deploymentPathRoot)) {
			synchronized(fileSystemEventPollers) {
				if(!fileSystemEventPollers.containsKey(deploymentPathRoot)) {
					WatchService ws = getWatchService(deploymentPathRoot.getRoot());
					WatchKey wk = null;
					try {
						wk = deploymentPathRoot.register(ws, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
					} catch (IOException iex) {
						log.error("Failed to register watch key for deployment root [{}]", deploymentPathRoot, iex);
						throw new RuntimeException(iex);
					}
					WrappedWatchKey wwk = new WrappedWatchKey(wk, this, deploymentPathRoot);					
					watchKeys.put(wk, wwk);
					Thread pollerThread = newWatcherPollerThread(ws, deploymentPathRoot);
					fileSystemEventPollers.put(deploymentPathRoot, pollerThread);
					pollerThread.start();
				}
			}
		}
	}
	
	/**
	 * Acquires a watch service for the passed path
	 * @param path The path to get a watch service for
	 * @return the watch service for the passed path, or its parent
	 */	
	protected WatchService getWatchService(final Path path) {
		if(path==null) throw new IllegalArgumentException("The passed Path was null");
		WatchService ws = watchServices.get(path);
		if(ws == null) {
			synchronized(watchServices) {
				ws = watchServices.get(path);
				if(ws == null) {
					try {
						ws = path.getFileSystem().newWatchService();
						watchServices.put(path, ws);
						return ws;
					} catch (IOException e) {
						throw new RuntimeException(e);						
					}
				}
			}
		}		
		return ws;
	}
	
	
	/**
	 * Attempts to find an already registered watch service for a parent path of the passed path
	 * @param path The path to find a watch service for 
	 * @return the located watch service or null if one is not found
	 */
	protected WatchService parentFor(Path path) {
		for(Map.Entry<Path, WatchService> entry: watchServices.entrySet()) {
			if(path.startsWith(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}
	
	/**
	 * Enqueues a file event, removing any older instances that this instance will replace
	 * @param fe The file event to enqueue
	 */
	protected void enqueueFileEvent(FileEvent fe) {
		enqueueFileEvent(-1L, fe);
	}

	
	/**
	 * Enqueues a file event, removing any older instances that this instance will replace
	 * @param delay The delay to add to the passed file event to give the queue a chance to conflate obsolete events already queued
	 * @param fe The file event to enqueue
	 */
	protected void enqueueFileEvent(long delay, FileEvent fe) {
		int removes = 0;
		Kind<Path> eventType =  null;
		while(processingQueue.remove(fe)) {removes++;}
		if(delay==-1L) {
			eventType = fe.getEventType();
			if(eventType==ENTRY_DELETE) {
				fe.addDelay(0);
			} else if(eventType==ENTRY_MODIFY) {
				fe.addDelay(500);
			} else if(eventType==ENTRY_CREATE) {
				fe.addDelay(1000);
			}
		}
		processingQueue.add(fe);
		log.info("Queued File [{}] Event for [{}] and dropped [{}] older versions", eventType.name(), fe.getFileName(), removes);
	}
	
	/**
	 * Creates a new watch service polling runnable
	 * @param watchService The watch service to poll
	 * @param path The root of the path that the watch service is watching
	 * @return a new watch service polling runnable
	 */
	protected Runnable newWatcherPoller(final WatchService watchService, final Path path) {
		return new Runnable() {
			public void run() {
				log.info(StringHelper.banner("Starting WatchService Poller for Deployment Root [{}]"), path);
				WatchKey watchKey = null;
				WrappedWatchKey wwk = null;
				for(;;) {
					try {
						watchKey = watchService.take();
						wwk = watchKeys.get(watchKey);
						List<WatchEvent<?>> polledEvents = watchKey.pollEvents();
						if(wwk!=null && wwk.listener!=null) {							
							for(WatchEvent<?> event: polledEvents) {
								@SuppressWarnings("unchecked")
								WatchEvent<Path> pathEvent = (WatchEvent<Path>)event;  
								Path parent = (Path)watchKey.watchable();
								FileEvent fileEvent = new FileEvent(new File(parent.toFile(), pathEvent.context().getFileName().toFile().getName()).getAbsolutePath(), pathEvent, wwk.listener);
								if(event.kind()==OVERFLOW) {
									wwk.listener.onOverflow(pathEvent);
								} else {
									enqueueFileEvent(fileEvent);
								}
							}
						}
					} catch (InterruptedException iex) {
						if(keepRunning.get()) {
							if(Thread.interrupted()) Thread.interrupted();
						} else {							
							break;
						}
					} finally {
						if(wwk!=null) {
							wwk.reset();
						}
						wwk = null;
						watchKey = null;
					}
				}
				fileSystemEventPollers.remove(path);
				log.info("WatchService poller terminated for [{}]", path);
			}
		};
	}
	
	public static class DeploymentWatchServiceFactory {
		
		
		public DeploymentWatchService get() {
			return DeploymentWatchService.getWatchService();
		}
	}


	/**
	 * Creates a new watch service polling thread
	 * @param watchService The watch service to poll
	 * @param path The root of the path that the watch service is watching
	 * @return a new watch service polling thread
	 */
	public Thread newWatcherPollerThread(WatchService watchService, Path path) {
		Thread thread = new Thread(threadGroup, newWatcherPoller(watchService, path), "FileWatcherThread[" + path + "]");
		thread.setDaemon(true);
		return thread;
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onCancel(java.nio.file.WatchKey)
	 */
	@Override
	public void onCancel(WatchKey canceledWatchKey) {
		log.info("WatchKey cancelled: [{}]", canceledWatchKey.watchable().toString());
		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onOverflow(java.nio.file.WatchEvent)
	 */
	@Override
	public void onOverflow(WatchEvent<Path> overflow) {
		log.warn("---------> [{}] {}", overflow.kind(), overflow.context().getFileName());		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onDirectoryCreated(java.io.File)
	 */
	@Override
	public void onDirectoryCreated(File dir) {
		if(!started.get()) return;
		log.info("----> Directory Created [{}]", dir);
		SubContextBoot.main(dir, this.applicationContext, null, null);
		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onDirectoryDeleted(java.io.File)
	 */
	@Override
	public void onDirectoryDeleted(File dir) {
		if(!started.get()) return;
		log.info("----> Directory Deleted [{}]", dir);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onDirectoryModified(java.io.File)
	 */
	@Override
	public void onDirectoryModified(File dir) {
		if(!started.get()) return;
		log.info("----> Directory Modified [{}]", dir);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onFileCreated(java.io.File)
	 */
	@Override
	public void onFileCreated(File file) {
		if(!started.get()) return;
		log.info("----> File Created [{}]", file);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onFileDeleted(java.io.File)
	 */
	@Override
	public void onFileDeleted(File file) {
		if(!started.get()) return;
		log.info("----> File Deleted [{}]", file);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onFileModified(java.io.File)
	 */
	@Override
	public void onFileModified(File file) {
		if(!started.get()) return;
		log.info("----> File Modified [{}]", file);
	}


//	@Override
//	public void onCreatePathEvent(WatchEvent<Path> event) {
//		log.info("---------> [CREATED] {}", event.kind(), event.context().getFileName());
//		
//	}


}
