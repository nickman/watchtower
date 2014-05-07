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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;
import javax.management.ObjectName;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.helios.jmx.util.helpers.JMXHelper;
import org.helios.jmx.util.helpers.SystemClock;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedNotification;
import org.springframework.jmx.export.annotation.ManagedNotifications;

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

public class DeploymentBranch extends ServerComponentBean implements /* DeploymentBranchMBean, */ PathWatchEventListener, EnvironmentAware {
	/** The deployment directory represented by this deployment branch */
	protected File deploymentDir;

	/** The directory name key */
	protected String dirKey;
	/** The directory name value */
	protected String dirValue;
	/** This branch's parent branch */
	protected DeploymentBranchMBean parentBranch;
	/** The watch key for this branch */
	protected WatchKey watchKey;
	/** Indicates if this deployment branch is a root branch */
	protected boolean root;
	
	/** The spring app supplied environment */
	protected Environment environment = null;
	
	/**
	 * Returns the 
	 * @return the environment
	 */
	public Environment getEnvironment() {
		return environment;
	}

	/**
	 * Sets the 
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
	
	/**
	 * Creates a new DeploymentBranch
	 * @param deploymentDir the deployment directory represented by this deployment branch
	 * @param classLoader The class loader designated for this branch
	 */
	public DeploymentBranch() {  // File deploymentDir, ClassLoader classLoader
	}
	
	protected void onEnvironmentSet() {
		deploymentDir = environment.getProperty("branch-file", File.class);
		classLoader = environment.getProperty("branch-cl", ClassLoader.class);
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
		
		ObjectName parentObjectName = findParent(deploymentDir.getParentFile());
		if(parentObjectName!=null) {
			objectName = JMXHelper.objectName(new StringBuilder(parentObjectName.toString()).append(",").append(dirKey).append("=").append(dirValue));
			if(JMXHelper.isRegistered(parentObjectName)) {
				parentBranch = (DeploymentBranchMBean)JMXHelper.getAttribute(parentObjectName, "ParentBranch");
			} else {
				parentBranch = null;
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
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.DeploymentBranchMBean#getDirectoryName()
	 */
//	@Override
	@ManagedAttribute
	public String getDirectoryName() {
		return deploymentDir.getAbsolutePath();
	}
	
	public boolean isWatchKeyValid() {
		if(watchKey==null) return false;
		return watchKey.isValid();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.watchtower.deployer.DeploymentBranchMBean#getClassLoader()
	 */
//	@Override
	@ManagedAttribute	
	public String getClassLoader() {
		return classLoader.toString();
	}
	
	/**
	 * Finds the ObjectName of the theoretical parent
	 * @param file the parent file of the branch we're looking for 
	 * @return the ObjectName or null if one was not found
	 */
	protected static ObjectName findParent(File file) {
		if(file==null || !file.isDirectory()) return null;
		int cnt = 1;
		TreeMap<Integer, String[]> parents = new TreeMap<Integer, String[]>();
		String[] dirPair = dirPair(file);
		if(dirPair.length!=2) {
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
	public DeploymentBranchMBean getParentBranch() {
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


	

}
