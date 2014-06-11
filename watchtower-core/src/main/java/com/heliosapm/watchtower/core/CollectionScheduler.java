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
package com.heliosapm.watchtower.core;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.jmx.concurrency.JMXManagedScheduler;
import org.helios.jmx.util.helpers.JMXHelper;
import org.quartz.CronExpression;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import com.heliosapm.watchtower.core.annotation.Propagate;

/**
 * <p>Title: CollectionScheduler</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.core.CollectionScheduler</code></p>
 * FIXME: expose config with spring annotations
 */
@EnableAutoConfiguration
@Propagate
public class CollectionScheduler extends JMXManagedScheduler implements CollectionSchedulerMBean {
	/** The collection scheduler singleton instance */
	private static volatile CollectionScheduler instance = null;
	/** The collection scheduler singleton instance */
	private static final Object lock = new Object();
	
	/**
	 * Acquires and returns the CollectionScheduler singleton instance
	 * @return the CollectionScheduler singleton instance
	 */
	public static CollectionScheduler getCollectionScheduler() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new CollectionScheduler();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new CollectionScheduler
	 */
	private CollectionScheduler() {		
		super(JMXHelper.objectName("com.heliosapm.watchtower.core.threadpools:service=ThreadPool,name=" + CollectionScheduler.class.getSimpleName()), CollectionScheduler.class.getSimpleName());
	}
	
	public <T> ScheduledFuture<T> scheduleWithCron(final Callable<T> command, String cron) {
		try {
			return new CronScheduledFuture(command, new CronExpression(cron));
		} catch (Exception ex) {
			throw new RuntimeException("Failed to schedule task [" + command + "] with cron expression [" + cron + "]", ex);
		}
	}
	
	class CronScheduledFuture<T> implements Callable<T>, ScheduledFuture<T> {
		/** The scheduled task */
		protected final Callable<T> command;
		/** The cron expression managing the execution */
		protected final CronExpression cex;
		/** The scheduled thingy for the next scheduled execution */
		protected final AtomicReference<ScheduledFuture<T>> nextExecutionFuture = new AtomicReference<ScheduledFuture<T>>(null);
		
		/**
		 * Creates a new CronScheduleFuture
		 * @param command The repeating command
		 * @param cex The cron expression
		 */
		public CronScheduledFuture(Callable<T> command, CronExpression cex) {
			this.command = command;
			this.cex = cex;
			nextExecutionFuture.set(schedule(this, nextExecutionTime(), TimeUnit.MILLISECONDS));
		}
		
		/**
		 * Returns the next valid execution time after now
		 * @return the next valid execution time 
		 */
		public long nextExecutionTime() {
			return cex.getNextValidTimeAfter(new Date()).getTime();
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public T call() throws Exception {
			T result = command.call();
			return result;
		}

		/**
		 * Returns the next execution future
		 * @return the nextExecutionFuture
		 */
		public ScheduledFuture<T> getNextExecutionFuture() {
			return nextExecutionFuture.get();
		}

		/**
		 * Sets the next execution future
		 * @param nextExecutionFuture the nextExecutionFuture to set
		 */
		public void setNextExecutionFuture(ScheduledFuture<T> nextExecutionFuture) {
			this.nextExecutionFuture.set(nextExecutionFuture);
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
		 */
		@Override
		public long getDelay(TimeUnit unit) {
			return nextExecutionFuture.get().getDelay(unit);
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(Delayed delayed) {
			return nextExecutionFuture.get().compareTo(delayed);
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Future#cancel(boolean)
		 */
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			boolean cancelled = nextExecutionFuture.get().cancel(mayInterruptIfRunning);
			
			return cancelled;
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Future#isCancelled()
		 */
		@Override
		public boolean isCancelled() {
			return nextExecutionFuture.get().isCancelled();
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Future#isDone()
		 */
		@Override
		public boolean isDone() {
			return nextExecutionFuture.get().isDone();
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Future#get()
		 */
		@Override
		public T get() throws InterruptedException, ExecutionException {
			return nextExecutionFuture.get().get();
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
		 */
		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return nextExecutionFuture.get().get(timeout, unit);
		}
	}

}
