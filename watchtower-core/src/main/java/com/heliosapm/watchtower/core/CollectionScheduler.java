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
import java.util.concurrent.atomic.AtomicBoolean;
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
 * FIXME: Push cron expression out to different class and conditionally load if quartz is available
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
	
	/**
	 * Schedules the passed task for execution in accordance with the passed cron expression
	 * @param command The task to schedule
	 * @param cron The cron expression. (See {@link CronExpression})
	 * @return a handle to the schedule
	 */
	public <T> ScheduledFuture<T> scheduleWithCron(final Callable<T> command, String cron) {
		try {
			return new CronScheduledFuture<T>(command, new CronExpression(cron));
		} catch (Exception ex) {
			throw new RuntimeException("Failed to schedule task [" + command + "] with cron expression [" + cron + "]", ex);
		}
	}
	
	/**
	 * <p>Title: CronScheduledFuture</p>
	 * <p>Description: A scheduled future implementation for a task scheduled by a cron expression</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.watchtower.core.CronScheduledFuture</code></p>
	 * @param <T> The assumed type of the callable and scheduled future return type
	 */
	class CronScheduledFuture<T> implements Callable<T>, ScheduledFuture<T> {
		/** The scheduled task */
		protected final Callable<T> command;
		/** The cron expression managing the execution */
		protected final CronExpression cex;
		/** The scheduled thingy for the next scheduled execution */
		protected final AtomicReference<ScheduledFuture<T>> nextExecutionFuture = new AtomicReference<ScheduledFuture<T>>(null);
		/** Indicates if the schedule has been canceled */
		protected final AtomicBoolean cancelled = new AtomicBoolean(false);
		/**
		 * Creates a new CronScheduleFuture
		 * @param command The repeating command
		 * @param cex The cron expression
		 */
		public CronScheduledFuture(Callable<T> command, CronExpression cex) {
			this.command = command;
			this.cex = cex;
			Long nextExec = nextExecutionTime();
			if(nextExec==null) {
				cancel(true);
				return;
			}
			nextExecutionFuture.set(schedule(this, nextExec, TimeUnit.MILLISECONDS));
		}
		
		/**
		 * Returns the next valid execution time in ms.UTC after now
		 * @return the next valid execution time or null if there isn't one
		 */
		public Long nextExecutionTime() {
			Date nextExec = cex.getNextValidTimeAfter(new Date());
			if(nextExec==null) return null;
			return nextExec.getTime();
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public T call() throws Exception {
			T result = command.call();
			if(!cancelled.get()) {
				synchronized(cancelled) {
					if(!cancelled.get()) {
						Long nextExec = nextExecutionTime();
						if(nextExec==null) {
							cancel(true);							
						} else {
							nextExecutionFuture.set(schedule(this, nextExec, TimeUnit.MILLISECONDS));
						}												
					}
				}
			}
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
			ScheduledFuture<T> prior = this.nextExecutionFuture.getAndSet(nextExecutionFuture);
			if(prior!=null) {
				prior.cancel(true);
			}
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
		 */
		@Override
		public long getDelay(TimeUnit unit) {
			ScheduledFuture<T> sf = nextExecutionFuture.get();
			if(sf==null) throw new RuntimeException("No scheduled task in state", new Throwable());						
			return sf.getDelay(unit);
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(Delayed delayed) {
			ScheduledFuture<T> sf = nextExecutionFuture.get();
			if(sf==null) throw new RuntimeException("No scheduled task in state", new Throwable());			
			return sf.compareTo(delayed);
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Future#cancel(boolean)
		 */
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			if(cancelled.compareAndSet(false, true)) {
				ScheduledFuture<T> sf = nextExecutionFuture.get();
				if(sf!=null) {
					sf.cancel(mayInterruptIfRunning);
				}				
			}
			return true;
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Future#isCancelled()
		 */
		@Override
		public boolean isCancelled() {
			return cancelled.get();
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Future#isDone()
		 */
		@Override
		public boolean isDone() {
			ScheduledFuture<T> sf = nextExecutionFuture.get(); 
			return sf == null || sf.isDone();
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Future#get()
		 */
		@Override
		public T get() throws InterruptedException, ExecutionException {
			ScheduledFuture<T> sf = nextExecutionFuture.get();
			if(sf==null) throw new ExecutionException("No scheduled task in state", new Throwable());
			return sf.get();
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
		 */
		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			ScheduledFuture<T> sf = nextExecutionFuture.get();
			if(sf==null) throw new ExecutionException("No scheduled task in state", new Throwable());			
			return sf.get(timeout, unit);
		}
	}

}
