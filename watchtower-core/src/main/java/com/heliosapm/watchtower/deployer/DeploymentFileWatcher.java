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

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.helios.jmx.util.helpers.ConfigurationHelper;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;

import com.heliosapm.watchtower.component.ServerComponentBean;


/**
 * <p>Title: DeploymentFileWatcher</p>
 * <p>Description: A file watcher to trigger deployments, undeployments and re-deployments.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.deployer.DeploymentFileWatcher</code></p>
 */

/*
 * 
 */

public class DeploymentFileWatcher extends ServerComponentBean {
	/** Instance logger */
	protected final Logger log = LogManager.getLogger(getClass());
	/** The watch keys registered for each directory keyed by the path of the watched directory */
	protected Map<Path, WatchKey> hotDirs = new ConcurrentHashMap<Path, WatchKey>();
	/** A map of file paths to Spring contexts to associate the ctx to the file it was booted from */
	protected final Map<String, GenericApplicationContext> deployedContexts = new ConcurrentHashMap<String, GenericApplicationContext>();
	/** The watch event handling thread */
	protected Thread watchThread = null;
	/** The processingQueue handling thread */
	protected Thread processingThread = null;
	/** The watch service */
	protected WatchService watcher = null;
	/** The keep running flag */
	protected final AtomicBoolean keepRunning = new AtomicBoolean(false);
	/** The processing delay queue that ensures the same file is not processed concurrently for two different events */
	protected final DelayQueue<FileEvent> processingQueue = new DelayQueue<FileEvent>();
	/** A set of file events that are in process */
	protected Set<FileEvent> inProcess = new CopyOnWriteArraySet<FileEvent>();
	/** Configuration added hot dir names */
	protected final Set<String> hotDirNames = new HashSet<String>();

	/** Thread serial number factory */
	protected static final AtomicLong serial = new AtomicLong(0L);
	
	/** The name of the default hot deploy directory */
	public static final String DEFAULT_HOT_DIR = System.getProperty("user.home") + File.separator + ".watchtower" + File.separator + "deploy";
	/** The system prop that specifies the hot deploy directories */
	public static final String HOT_DIR_PROP = "org.helios.watchtower.deploydir";
	
	/**
	 * Creates a new DeploymentFileWatcher
	 */
	public DeploymentFileWatcher() {
		
	}
	
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		initDefaultHotDir();
		initEnvHotDirs();
		validateInitialHotDirs();
		if(hotDirNames.isEmpty()) {
			warn("No hot deploy directories were defined or found. New directories can be added through the JMX interface.");
		} else {
			StringBuilder b = new StringBuilder("\n\t====================\n\tHot Deploy Directories\n\t====================");
			for(String s: hotDirNames) {
				b.append("\n\t").append(s);
			}
			b.append("\n");
			info(b);
		}		
	}
	
	/**
	 * Callback when the current app context refreshes
	 * @param cse The context refreshed event
	 */
	public void onApplicationContextRefresh(ContextRefreshedEvent cse) {
		info("Root AppCtx Started [", new Date(cse.getTimestamp()), "]:[", cse.getApplicationContext().getDisplayName(), "]");
		keepRunning.set(true);
		startFileEventListener();
	}
	

	/**
	 * Initializes the default hot dir unless it has been disabled. 
	 */
	protected void initDefaultHotDir() {	
		String defaultHotDirName = System.getProperty(HOT_DIR_PROP, DEFAULT_HOT_DIR);
		File defaultHotDir = new File(defaultHotDirName);
		if(defaultHotDir.exists()) {
			if(!defaultHotDir.isDirectory()) {
				warn("\n\t###########################################\n\tProblem: The default hot deploy directory [", defaultHotDir, "] exists but it is a file\n\t###########################################\n");
			} else {
				hotDirNames.add(defaultHotDir.getAbsolutePath());
				scanForApplications(defaultHotDir.toPath(), true);
			}
		} else {
			if(!defaultHotDir.mkdirs()) {
				warn("\n\t###########################################\n\tProblem: Failed to create hot deploy directory [", defaultHotDir, "]\n\t###########################################\n");
			} else {
				hotDirNames.add(defaultHotDir.getAbsolutePath());
				scanForApplications(defaultHotDir.toPath(), true);
			}
		}
	}
	
	/**
	 * Validates the initial hot directories, removing any that are invalid
	 */
	protected void validateInitialHotDirs() {
		for(Iterator<String> iter = hotDirNames.iterator(); iter.hasNext();) {
			File f = new File(iter.next().trim());
			if(!f.exists() || !f.isDirectory()) {
				warn("Configured hot dir path was invalid [", f, "]");
				iter.remove();
			}			
		}
	}
	
	/**
	 * Configures watches for system prop or environment defined hot directories
	 */
	protected void initEnvHotDirs() {
		String[] hds = ConfigurationHelper.getSystemThenEnvProperty(HOT_DIR_PROP, DEFAULT_HOT_DIR).split(",");
		for(String hd: hds) {
			if(hd.trim().isEmpty()) continue;
			hd = hd.trim();
			Path hdPath = Paths.get(hd);
			if(!Files.exists(hdPath) || Files.isDirectory(hdPath)) continue;
			hotDirNames.add(hd);
			scanForApplications(hdPath, true);
		}
	}
	
	/**
	 * Scans the passed path for application directories and adds them to the watched set, unless apps have been disabled
	 * @param hotDir The path to scan
	 * @param add If true, the located application directories will be added, if false, just returns the names, taking no further action 
	 * @return A set of the application directory names that were added
	 */
	protected Set<String> scanForApplications(Path hotDir, boolean add) {
		Set<String> added = new HashSet<String>();
		for(File f: hotDir.toFile().listFiles()) {
			if(f.isDirectory()) {
				hotDirNames.add(f.getAbsolutePath());
				added.add(f.getAbsolutePath());
			}
		}
		return added;
	}
	
	/**
	 * Adds a hot deploy directory
	 * @param dirName the name of the hot deploy directory to add
	 * @return a string message summarizing the results of the operation
	 */
	@ManagedOperation(description="Adds a hot deploy directory")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="dirName", description="The name of the hot deploy directory to add")
	})
	public String addHotDir(String dirName) {
		if(dirName==null || dirName.trim().isEmpty()) {
			return "Null or empty directory name";
		}		
		File f = new File(dirName.trim());
		StringBuilder b = new StringBuilder("Adding hot directory [").append(dirName).append("]");
		if(f.exists() && f.isDirectory()) {
			hotDirNames.add(f.getAbsolutePath());
			b.append("\n\tAdded [").append(f.getAbsolutePath()).append("]");
			Set<String> apps = scanForApplications(f.toPath(), true);			
			if(!apps.isEmpty()) {
				for(String appDir: apps) {
					b.append("\n\tAdded [").append(appDir).append("]");
				}
			}
		} else {
			b.append("\n\tDirectory did not exist");
		}
		b.append("\n");
		try { updateWatchers(); } catch (IOException ioe) {
			error("Failure during updateWatchers", ioe);
		}
		return b.toString();
	}

	/**
	 * Removes a hot deploy directory
	 * @param dirName the name of the hot deploy directory to remove
	 * @return a string message summarizing the results of the operation
	 */
	@ManagedOperation(description="Removes a hot deploy directory")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="dirName", description="The name of the hot deploy directory to remove")
	})
	public String removeHotDir(String dirName) {
		if(dirName==null || dirName.trim().isEmpty()) {
			return "Null or empty directory name";
		}		
		File f = new File(dirName.trim());
		StringBuilder b = new StringBuilder("Removing hot directory [").append(dirName).append("]");
		WatchKey watchKey = null;
		if(f.exists() && f.isDirectory()) {
			hotDirNames.remove(f.getAbsolutePath());
			watchKey = hotDirs.remove(f.toPath());
			if(watchKey!=null) watchKey.cancel();
			b.append("\n\tRemoved [").append(f.getAbsolutePath()).append("]");
			Set<String> apps = scanForApplications(f.toPath(), false);			
			if(!apps.isEmpty()) {
				for(String appDir: apps) {
					f = new File(appDir);
					hotDirNames.remove(appDir);
					watchKey = hotDirs.remove(f.toPath());
					if(watchKey!=null) watchKey.cancel();					
					b.append("\n\tRemoved [").append(appDir).append("]");
				}
			}
		} else {
			b.append("\n\tDirectory did not exist");
		}
		b.append("\n");
		return b.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {
		for(WatchKey wk: hotDirs.values()) {
			wk.cancel();
		}
		hotDirs.clear();
		keepRunning.set(false);
		watchThread.interrupt();
		processingThread.interrupt();
		processingQueue.clear();
		deployedContexts.clear();
		super.doStop();
	}

	/**
	 * Scans the hot diretory names and registers a watcher for any unwatched names,
	 * then removes any registered watchers that are no longer in the hot diretory names set 
	 * @throws IOException thrown on IO exceptions related to paths
	 */
	protected synchronized void updateWatchers() throws IOException {
		Map<Path, WatchKey> hotDirSnapshot = new HashMap<Path, WatchKey>(hotDirs);
		for(String fn: hotDirNames) {
			Path path = Paths.get(fn);
			if(hotDirs.containsKey(path)) {
				hotDirSnapshot.remove(path);
			} else {
				WatchKey watchKey = path.register(watcher, ENTRY_DELETE, ENTRY_MODIFY);
				hotDirs.put(path, watchKey);
				info("Added watched deployer directory [", path, "]");
			}
		}
		for(Map.Entry<Path, WatchKey> remove: hotDirSnapshot.entrySet()) {
			remove.getValue().cancel();
			info("Cancelled watch on deployer directory [", remove.getKey(), "]");
		}
		hotDirSnapshot.clear();
	}

	/**
	 * Starts the file change listener
	 */
	public void startFileEventListener() {
		startProcessingThread();
		try {
			watcher = FileSystems.getDefault().newWatchService();
			scanHotDirsAtStart();
			updateWatchers();
			
			
			
			watchThread = new Thread("SpringHotDeployerWatchThread"){
				WatchKey watchKey = null;
				public void run() {
					info("Started HotDeployer File Watcher Thread");
					while(keepRunning.get()) {
						try {
							watchKey = watcher.take();
							debug("Got watch key for [" + watchKey.watchable() + "]");
							debug("File Event Queue:", processingQueue.size());
					    } catch (InterruptedException ie) {
					        interrupted();
					        // check state
					        continue;
					    }
						if(watchKey!=null) {
							for (WatchEvent<?> event: watchKey.pollEvents()) {
								WatchEvent.Kind<?> kind = event.kind();
								if (kind == OVERFLOW) {
									warn("OVERFLOW OCCURED");
									if(!watchKey.reset()) {
										info("Hot Dir for watch key [", watchKey, "] is no longer valid");
										watchKey.cancel();
										Path dir = (Path)watchKey.watchable();
										hotDirNames.remove(dir.toFile().getAbsolutePath());
										hotDirs.remove(dir);
									}
						            continue;
								}								
								WatchEvent<Path> ev = (WatchEvent<Path>)event;
								Path dir = (Path)watchKey.watchable();
								
							    Path fileName = Paths.get(dir.toString(), ev.context().toString());
							    if(fileName.toFile().isDirectory()) {
							    	addHotDir(fileName.toFile().getAbsolutePath());
							    } else {
							    	enqueueFileEvent(500, new FileEvent(fileName.toFile().getAbsolutePath(), ev.kind()));
							    }
							}
						}
						boolean valid = watchKey.reset();
						// FIXME:  This stops polling completely.
					    if (!valid) {
					    	warn("Watch Key for [" , watchKey , "] is no longer valid. Polling will stop");
					        break;
					    }
					}
				}
			};
			watchThread.setDaemon(true);
			keepRunning.set(true);
			watchThread.start();
			info("HotDeploy watcher started on [" + hotDirs.keySet() + "]");			
		} catch (Exception ex) {
			error("Failed to start hot deployer", ex);
		}
	}
	
	/**
	 * Scans the hot dirs looking for files to deploy at startup. 
	 * Since there's no file change events, we need to go and look for them.
	 */
	protected void scanHotDirsAtStart() {
		for(String hotDirPathName: hotDirNames) {
			File hotDirPath = new File(hotDirPathName);
			for(File f: hotDirPath.listFiles()) {
				if(f.isDirectory() || !f.canRead()) continue;
				enqueueFileEvent(500, new FileEvent(f.getAbsolutePath(),  ENTRY_MODIFY));
			}
		}
	}
	
	/**
	 * Enqueues a file event, removing any older instances that this instance will replace
	 * @param delay The delay to add to the passed file event to give the queue a chance to conflate obsolete events already queued
	 * @param fe The file event to enqueue
	 */
	protected void enqueueFileEvent(long delay, FileEvent fe) {
		int removes = 0;
		while(processingQueue.remove(fe)) {removes++;}
		fe.addDelay(delay);
		processingQueue.add(fe);
		debug("Queued File Event for [", fe.getFileName(), "] and dropped [" , removes , "] older versions");
	}
	
	/**
	 * Starts the processing queue processor thread
	 */
	void startProcessingThread() {
		processingThread = new Thread("WatchTowerHotDeployerProcessingThread") {
			@Override
			public void run() {
				info("Started HotDeployer Queue Processor Thread");
				while(keepRunning.get()) {
					try {
						final FileEvent fe = processingQueue.take();						
						if(fe!=null) {
							debug("Processing File Event [" , fe.getFileName(), "]" );
							if(inProcess.contains(fe)) {								
								enqueueFileEvent(2000, fe);
							} else {
								Thread t = new Thread("WatchtowerHotDeployer#" + serial.incrementAndGet()) {
									public void run() {
										if(fe.getEventType()==ENTRY_DELETE) {
//											killAppCtx(fe);
										} else if(fe.getEventType()==ENTRY_MODIFY) {
//											redeployAppCtx(fe);
										}
									}
								};
								t.setDaemon(true);
								t.start();
							}
						}
					} catch (Exception e) {
						if(interrupted()) interrupted();
					}
				}
			}
		};
		processingThread.setDaemon(true);
		processingThread.start();
	}
		
	/**
	 * Returns the hot directory names
	 * @return the hot directory names
	 */
	@ManagedAttribute(description="The registered hot deploy directories")
	public Set<String> getHotDirNames() {
		return Collections.unmodifiableSet(hotDirNames);
	}
	
	/**
	 * Sets the hot directory names
	 * @param hotDirNames the hot directory names
	 */
	public void setHotDirNames(Set<String> hotDirNames) {
		if(hotDirNames!=null) {
			this.hotDirNames.addAll(hotDirNames);
		}
	}

	
}
