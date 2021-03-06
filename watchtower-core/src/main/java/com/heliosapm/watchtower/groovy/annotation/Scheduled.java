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
package com.heliosapm.watchtower.groovy.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: Scheduled</p>
 * <p>Description: Marks a groovy script or groovy class as schedulable.
 * The annotation can specify a scheduling period (and unit) but this can be overriden
 * using the scheduler editor meta methods that will be applied to all annotated groovy objects.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.groovy.annotation.Scheduled</code></p>
 */
@Target(value={FIELD, TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scheduled {
	/**
	 * The scheduling period
	 */
	long period() default 15000;
	
	/**
	 * The initial delay
	 */
	long initialDelay() default 1000;
	
	/**
	 * The default scheduling period unit
	 */
	TimeUnit unit() default TimeUnit.MILLISECONDS;
	
	/**
	 * Overrides the {@link #period()}/{@link #unit()} with a cron expression.
	 */
	String cron() default "";
	
	/**
	 * Indicates if the scheduled task can be interrupted if it is running when canceled
	 */
	boolean interruptible() default true; 
}
