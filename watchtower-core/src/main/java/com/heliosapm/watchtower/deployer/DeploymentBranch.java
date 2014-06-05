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

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;
import javax.management.ObjectName;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.helios.jmx.concurrency.JMXManagedThreadPool;
import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.jmx.util.helpers.SystemClock;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedNotification;
import org.springframework.jmx.export.annotation.ManagedNotifications;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.jmx.export.notification.NotificationPublisherAware;

import com.heliosapm.watchtower.component.ServerComponentBean;

/**
 * <p>Title: DeploymentBranch</p>
 * <p>Description: Represents a directory in the deployment tree</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.deployer.DeploymentBranch</code></p>
 */

@ManagedNotifications({
	@ManagedNotification(notificationTypes={"com.heliosapm.watchtower.deployer.DeploymentBranch.start"}, name="javax.management.Notification", description="Notifies when a new DeploymentBranch has started")
})
@ManagedResource
public class DeploymentBranch extends ServerComponentBean implements /* DeploymentBranchMBean, */ PathWatchEventListener, EnvironmentAware, EnvironmentCapable, NotificationPublisherAware {
	/** The deployment directory represented by this deployment branch */
	protected File deploymentDir;

	/** The directory name key */
	protected String dirKey;
	/** The directory name value */
	protected String dirValue;
	/** This branch's parent branch */
	protected DeploymentBranch parentBranch;
	/** The watch key for this branch */
	protected WatchKey watchKey;
	/** Indicates if this deployment branch is a root branch */
	protected boolean root;
	
	/** The spring app supplied environment */
	protected Environment environment = null;
	
	/** Thread pool for async deployment tasks */
	protected JMXManagedThreadPool deploymentThreadPool = null;
	
	
	
	/** Groovy file filter */
	protected static final FileFilter GROOVY_FILE_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			if(pathname!=null && pathname.isFile() && pathname.toString().toLowerCase().endsWith(".groovy")) return true;
			return false;
		}
	};
	
	private static URL toURL(File f) {
		try {
			return f.toURI().toURL();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to convert file [" + f + "] to URL", ex);
		}
	}
	
	/**
	 * Returns the environment
	 * @return the environment
	 */
	public Environment getEnvironment() {
		return environment;
	}
	
	

	/**
	 * Sets the bean's environment
	 * @param environment the environment to set
	 */
	public void setEnvironment(Environment environment) {
		this.environment = environment;
		onEnvironmentSet();
	}

	/** A map of deployed objects keyed by the relative name of the file they were loaded from */
	protected final NonBlockingHashMap<String, Object> deployments = new NonBlockingHashMap<String, Object>();

	
	/** The class loader designated for this branch */
	protected ClassLoader classLoader = null;
	
	/** The deployment branch object name base */
	public static final String OBJECT_NAME_BASE = "com.heliosapm.watchtower.deployment:type=branch";
	
	/** The file watcher events we'll subscribe to */
	@SuppressWarnings("unchecked")
	private static final WatchEvent.Kind<Path>[] WATCH_EVENT_TYPES = new WatchEvent.Kind[] {
		ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY
	}; 
	
	/** Non lib sub dir file filter */
	protected static final FileFilter SUBDIR_FILE_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			if(pathname!=null && pathname.isDirectory()) {
				if(!pathname.toString().equalsIgnoreCase("lib") && !pathname.toString().equalsIgnoreCase("xlib")) return true;
			}
			return false;
		}
	};

	
	/**
	 * Creates a new DeploymentBranch
//	 * @param deploymentDir the deployment directory represented by this deployment branch
//	 * @param classLoader The class loader designated for this branch
	 */
	public DeploymentBranch() {  // File deploymentDir, ClassLoader classLoader
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.ServerComponentBean#doStart()
	 */
	protected void doStart() throws Exception {
		deploymentThreadPool = JMXHelper.getAttribute("com.heliosapm.watchtower.core.threadpools:service=ThreadPool,name=DeploymentService", "Instance");
//		if(applicationContext!=null) {
//			ApplicationContext parent = applicationContext.getParent();
//			if(parent!=null) {
//				if(JMXHelper.isValidObjectName(parent.getDisplayName())) {
//					ObjectName parentObjectName = JMXHelper.objectName(parent.getDisplayName());					
//					ObjectName defaultAssigned = this.objectName;
//					if(!defaultAssigned.getKeyPropertyList().containsKey("root")) {
//						StringBuilder b = new StringBuilder(parentObjectName.toString());
//						for(Map.Entry<String, String> entry: parentObjectName.getKeyPropertyList().entrySet()) {
//							if("root".equals(entry.getKey())) continue;
//							if(!defaultAssigned.getKeyPropertyList().containsKey(entry.getKey())) {
//								b.append(",").append(entry.getKey()).append("=").append(entry.getValue());						
//							}
//						}
//						this.objectName = JMXHelper.objectName(b);
//					}
//				}
//			}
//		}
		super.doStart();
		if(objectName!=null && applicationContext!=null) {
			applicationContext.setDisplayName(objectName.toString());
		}
		for(final File subDir: deploymentDir.listFiles(SUBDIR_FILE_FILTER)) {
			final DeploymentBranch self = this;
			deploymentThreadPool.execute(new Runnable(){
				public void run() {
					SubContextBoot.main(subDir, applicationContext, self, objectName);
				}
			});
			
		}		
		watchKey =  DeploymentWatchService.getWatchService().getWatchKey(deploymentDir.toPath(), this, WATCH_EVENT_TYPES);
		//fireSubContextStarted();			
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.ServerComponentBean#setNotificationPublisher(org.springframework.jmx.export.notification.NotificationPublisher)
	 */
	public void setNotificationPublisher(NotificationPublisher notificationPublisher) {
		this.notificationPublisher = notificationPublisher;
		fireSubContextStarted();
	}
	
	
//	protected void bootDeployment(final File subDeploy) {
//		ClassLoader branchClassLoader = SubContextBoot.loadBranchLibs(subDeploy);
//		Map<String, Object> env = new HashMap<String, Object>();
//		env.put("branch-file", subDeploy);
//		env.put("branch-cl", branchClassLoader);
//		
//		ConfigurableApplicationContext branchCtx = new AnnotationConfigApplicationContext();
//		final Set<Object> resources = new LinkedHashSet<Object>();
//		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
//		try {
//			Thread.currentThread().setContextClassLoader(branchClassLoader);	
//			ByteArrayResource subContextXml = new ByteArrayResource(DEPLOYMENT_BRANCH.getBytes()) {
//				/**
//				 * {@inheritDoc}
//				 * @see org.springframework.core.io.AbstractResource#getFilename()
//				 */
//				@Override
//				public String getFilename() {				
//					return "DeploymentBranch-" + subDeploy + ".xml"; 
//				}
//			};
//			resources.add(subContextXml);
//			for(File groovyFile: subDeploy.listFiles(GROOVY_FILE_FILTER)) {
//				resources.add(new FileSystemResource(groovyFile));
//			}
////			WatchtowerApplication wapp = new WatchtowerApplication(Class.forName("com.heliosapm.watchtower.deployer.DeploymentBranch", true, branchClassLoader));
//			WatchtowerApplication wapp = new WatchtowerApplication(resources.toArray());
//			ParentContextApplicationContextInitializer parentSetter = new ParentContextApplicationContextInitializer(parent);		
//			wapp.addInitializers(parentSetter);
//	
//			wapp.setDefaultProperties(env);
//			wapp.setShowBanner(false);
//			wapp.setWebEnvironment(false);
//			branchCtx = wapp.run();
//			branchCtx.setId("SubDeploy-[" + subDeploy + "]");
//		} catch (Exception ex) {
//			LOG.error("Failed to deploy Branch for [" + subDeploy + "]", ex);
//			throw new RuntimeException(ex);
//		} finally {
//			Thread.currentThread().setContextClassLoader(cl);
//		}
//		if(LOG.isDebugEnabled()) {
//			StringBuilder b = new StringBuilder();
//			for(String s: branchCtx.getBeanDefinitionNames()) {
//				b.append("\n\t\t").append(s);
//			}
//			LOG.debug(StringHelper.banner("Started SubContext: [{}]\n\tBean Names:{}"), branchCtx.getId(), b.toString());
//		} else {
//			LOG.info("Started SubContext: [{}]", branchCtx.getId());
//		}
//		
//	}
	
	/**
	 * Returns the split directory pair for a deployment directory
	 * @param deploymentDirectory The deployment directory
	 * @return the key-value pair
	 */
	public static String[] getDirectoryPair(File deploymentDirectory) {
		String[] dirPair = dirPair(deploymentDirectory);
		String dirKey = null;
		String dirValue = null;
		if(dirPair.length==1) {
			dirPair = new String[]{"root", deploymentDirectory.getName()};
		}

		dirKey = dirPair[0];
		if("branch".equals(dirKey)) {
			throw new IllegalArgumentException("The passed deployment directory [" + deploymentDirectory + "] has an illegal key: [" + dirPair[0] + "]");
		}
		dirValue = dirPair[1];
		if("lib".equals(dirValue) || "xlib".equals(dirValue)) {
			dirKey = "ext";
			dirPair[0] = "ext";
		}
		return new String[]{dirKey, dirValue};
	}
	
	
	protected void onEnvironmentSet() {
		deploymentDir = environment.getProperty("branch-file", File.class);
		classLoader = environment.getProperty("branch-cl", ClassLoader.class);
		ObjectName parentObjectName = environment.getProperty("parentObjectName", ObjectName.class);
		if(deploymentDir==null || !deploymentDir.isDirectory()) throw new IllegalArgumentException("The passed deployment directory was invalid [" + deploymentDir + "]");
		String[] dirPair = dirPair(deploymentDir);
		if(dirPair.length==1) {
			dirPair = new String[]{"root", deploymentDir.getName()};
			//throw new IllegalArgumentException("The passed deployment directory [" + deploymentDir + "] is invalid: [" + dirPair[0] + "]");
		}
		Path path = Paths.get(this.deploymentDir.getPath());		
		dirKey = dirPair[0];
		if("branch".equals(dirKey)) {
			throw new IllegalArgumentException("The passed deployment directory [" + deploymentDir + "] has an illegal key: [" + dirPair[0] + "]");
		}
		dirValue = dirPair[1];
		if("lib".equals(dirValue) || "xlib".equals(dirValue)) {
			dirKey = "ext";
			dirPair[0] = "ext";
		}
//		ObjectName parentObjectName = findParent(deploymentDir.getParentFile());
		if(parentObjectName!=null) {
			//env.put("branch-parent", parentBranch);
			parentBranch = environment.getProperty("branch-parent", DeploymentBranch.class);
			objectName = JMXHelper.objectName(new StringBuilder(parentObjectName.toString()).append(",").append(dirKey).append("=").append(dirValue));
			if(parentBranch==null && JMXHelper.isRegistered(parentObjectName)) {
				parentBranch = (DeploymentBranch)JMXHelper.getAttribute(parentObjectName, "ParentBranch");
			}		
			root = false;
		} else {
			objectName = JMXHelper.objectName(new StringBuilder(OBJECT_NAME_BASE).append(",").append(dirKey).append("=").append(dirValue));
			parentBranch = null;
			root = true;
		}
	}
	
//	protected void doStart() {
//		watchKey =  DeploymentWatchService.getWatchService().getWatchKey(deploymentDir.toPath(), this, WATCH_EVENT_TYPES);
//		fireSubContextStarted();			
//	}
//	
//	
	
	/** JMX notification serial */
	protected final AtomicLong notificationSerial = new AtomicLong(0L);
	
	/**
	 * Sends a SubContext started notification
	 */
	protected void fireSubContextStarted() {
		notificationPublisher.sendNotification(new Notification("com.heliosapm.watchtower.deployer.DeploymentBranch.start", objectName, notificationSerial.incrementAndGet(), SystemClock.time(), "Started SubContext [" + deploymentDir + "]"));
	}
	
	/**
	 * @return
	 */
	@ManagedAttribute
	public String getDirectoryName() {
		return deploymentDir.getAbsolutePath();
	}
	
	/**
	 * @return
	 */
	@ManagedAttribute
	public boolean isWatchKeyValid() {
		if(watchKey==null) return false;
		return watchKey.isValid();
	}
	

	/**
	 * @return
	 */
	@ManagedAttribute	
	public ClassLoader getClassLoader() {
		return classLoader;
	}
	
	/**
	 * Finds the ObjectName of the theoretical parent
	 * @param file the parent file of the branch we're looking for 
	 * @return the ObjectName or null if one was not found
	 * Root Parent:  com.heliosapm.watchtower.deployment:type=branch,root=deploy
	 * Sample Child: com.heliosapm.watchtower.deployment:type=branch,foo=bar
	 */
	protected static ObjectName findParent(File file) {
		if(file==null || !file.isDirectory()) return null;
		int cnt = 1;
		TreeMap<Integer, String[]> parents = new TreeMap<Integer, String[]>();
		String[] dirPair = dirPair(file);
		if(dirPair.length!=2) {
			ObjectName parent = JMXHelper.objectName(new StringBuilder("com.heliosapm.watchtower.deployment:type=branch,root=").append(file.getName()));
			if(JMXHelper.isRegistered(parent)) return parent;
			return null;
		}
		parents.put(0, dirPair);
		while(true) {
			file = file.getParentFile();
			if(file==null) break;
			dirPair = dirPair(file);
			if(dirPair.length!=2) break;
			parents.put(cnt, dirPair);
			cnt++;
		}
		StringBuilder b = new StringBuilder(OBJECT_NAME_BASE);
		for(String[] pair: parents.values()) {
			b.append(",").append(pair[0]).append("=").append(pair[1]);
		}
		try {
			return JMXHelper.objectName(b);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			return null;
		}
	}
	
	/**
	 * Parses the dir key of the passed file
	 * @param file The file to get the dir key for
	 * @return the dir key pair or a one item array with the fail reason if file is null, not a dir or is not dashed
	 */
	protected static String[] dirPair(File file) {
		if(file==null) return new String[] {"The file was null"};
		if(!file.isDirectory()) return new String[] {"The file is not a directory"};
		String dirName = file.getName();
		int index = dirName.indexOf('-');
		if(index==-1) return new String[] {"The file name is not dashed"};
		return new String[] {dirName.substring(0, index), dirName.substring(index+1)};		
	}

	/**
	 * Returns the deployment directory represented by this deployment branch
	 * @return the deployment directory represented by this deployment branch
	 */
	public File getDeploymentDir() {
		return deploymentDir;
	}
	
	/**
	 * Sets the deployment directory
	 * @param deploymentDir the deploymentDir to set
	 */
	public void setDeploymentDir(File deploymentDir) {
		this.deploymentDir = deploymentDir;
	}

	/**
	 * Sets the classloader
	 * @param classLoader the classLoader to set
	 */
	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.component.ServerComponentBean#getObjectName()
	 */
	@Override
	public ObjectName getObjectName() {
		return objectName;		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.DeploymentBranchMBean#getDirKey()
	 */
//	@Override
	@ManagedAttribute
	public String getDirKey() {
		return dirKey;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.DeploymentBranchMBean#getDirValue()
	 */
//	@Override
	@ManagedAttribute
	public String getDirValue() {
		return dirValue;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.DeploymentBranchMBean#getParentBranch()
	 */
//	@Override
	@ManagedAttribute
	public DeploymentBranch getParentBranch() {
		return parentBranch;
	}



	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onCancel(java.nio.file.WatchKey)
	 */
	@Override
	public void onCancel(WatchKey canceledWatchKey) {
		// TODO Auto-generated method stub
		
	}



	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onOverflow(java.nio.file.WatchEvent)
	 */
	@Override
	public void onOverflow(WatchEvent<Path> overflow) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onDirectoryDeleted(java.io.File)
	 */
	@Override
	public void onDirectoryDeleted(File dir) {
		log.info("----> Directory Deleted [{}]", dir);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onDirectoryModified(java.io.File)
	 */
	@Override
	public void onDirectoryModified(File dir) {
		log.info("----> Directory Modified [{}]", dir);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onDirectoryCreated(java.io.File)
	 */
	@Override
	public void onDirectoryCreated(File dir) {
		log.info("----> Directory Created [{}]", dir);
	}
	

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onFileCreated(java.io.File)
	 */
	@Override
	public void onFileCreated(File file) {
		log.info("----> File Created [{}]", file);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onFileDeleted(java.io.File)
	 */
	@Override
	public void onFileDeleted(File file) {
		log.info("----> File Deleted [{}]", file);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.PathWatchEventListener#onFileModified(java.io.File)
	 */
	@Override
	public void onFileModified(File file) {
		log.info("----> File Modified [{}]", file);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.DeploymentBranchMBean#isRoot()
	 */
	@ManagedAttribute
	public boolean isRoot() {
		return root;
	}

	/**
	 * @return
	 * @see org.springframework.context.ApplicationContext#getId()
	 */
	@ManagedAttribute
	public String getAppCtxId() {
		return applicationContext.getId();
	}

	/**
	 * @return
	 * @see org.springframework.context.ApplicationContext#getApplicationName()
	 */
	@ManagedAttribute
	public String getApplicationName() {
		return applicationContext.getApplicationName();
	}

	/**
	 * @return
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionCount()
	 */
	@ManagedAttribute
	public int getBeanDefinitionCount() {
		return applicationContext.getBeanDefinitionCount();
	}

	/**
	 * @return
	 * @see org.springframework.context.ApplicationContext#getDisplayName()
	 */
	@ManagedAttribute
	public String getAppCtxDisplayName() {
		return applicationContext.getDisplayName();
	}

	/**
	 * @return
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionNames()
	 */
	@ManagedAttribute
	public String[] getBeanDefinitionNames() {
		return applicationContext.getBeanDefinitionNames();
	}
	
	/**
	 * @return
	 */
	@ManagedAttribute
	public ApplicationContext getContext() {
		return applicationContext;
	}
	
	@ManagedAttribute
	public Map<String, String> getBeanDefinitionTypes() {
		Map<String, String> map = new HashMap<String, String>(getBeanDefinitionCount());
		for(String s: getBeanDefinitionNames()) {
			map.put(s,  applicationContext.getBean(s).getClass().getName());
		}
		return map;
	}


	

}
