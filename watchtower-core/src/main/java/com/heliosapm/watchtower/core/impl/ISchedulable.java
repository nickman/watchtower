/**
 * 
 */
package com.heliosapm.watchtower.core.impl;

import java.util.concurrent.TimeUnit;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;

import com.heliosapm.watchtower.core.IServiceAspect;

/**
 * <p>Title: ISchedulable</p>
 * <p>Description: Defines the scheduling collection service aspect</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.watchtower.core.impl.ISchedulable</code></b>
 */

public interface ISchedulable extends IServiceAspect {
	/**
	 * Schedules the task for repeating execution on the defined period after the defined initial period 
	 * @param period The fixed delay of the executions
	 * @param initial The initial delay when first scheduled
	 * @param unit The unit of the period and initial
	 */
	@ManagedOperation(description="Schedules the task for repeating execution on the defined period after the defined initial period")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="period", description="The fixed delay of the executions"),
		@ManagedOperationParameter(name="initial", description="The initial delay when first scheduled"),
		@ManagedOperationParameter(name="unit", description="The unit of the period and initial")
	})
	public void schedule(long period, long initial, TimeUnit unit);
	
	/**
	 * Stops the scheduled exection
	 */
	@ManagedOperation(description="Stops the scheduled exection")
	public void cancelSchedule();
	
	/**
	 * Returns the scheduled period
	 * @return the scheduled period
	 */
	@ManagedAttribute(description="The scheduled period")
	public long getPeriod();
	/**
	 * Sets the schedule fixed delay period
	 * @param period the schedule fixed delay period 
	 */
	@ManagedAttribute
	public void setPeriod(long period);	
	/**
	 * Returns the scheduled initial delay
	 * @return the scheduled initial delay
	 */
	@ManagedAttribute(description="The scheduled initial delay")
	public long getInitial();
	/**
	 * Sets the schedule initial delay
	 * @param initial the schedule initial delay 
	 */
	@ManagedAttribute
	public void setInitial(long initial);
	/**
	 * Returns the schedule initial and period unit
	 * @return the schedule initial and period unit
	 */
	@ManagedAttribute(description="The schedule initial and period unit")
	public TimeUnit getUnit();
	/**
	 * Sets the schedule initial and period unit
	 * @param unit the unit
	 */
	@ManagedAttribute
	public void setUnit(TimeUnit unit);
}
