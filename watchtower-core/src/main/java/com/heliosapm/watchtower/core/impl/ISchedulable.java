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
import com.heliosapm.watchtower.core.impl.ServiceAspectImpl.ScheduledClosure;

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
	 * @param name The name of the closure to schedule
	 * @param period The fixed delay of the executions
	 * @param initial The initial delay when first scheduled
	 * @param unit The unit of the period and initial
	 */
	@ManagedOperation(description="Schedules the task for repeating execution on the defined period after the defined initial period")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="name", description="The name of the closure to schedule"),
		@ManagedOperationParameter(name="period", description="The fixed delay of the executions"),
		@ManagedOperationParameter(name="initial", description="The initial delay when first scheduled"),
		@ManagedOperationParameter(name="unit", description="The unit of the period and initial")
	})
	public void schedule(String name, long period, long initial, TimeUnit unit);

	/**
	 * Schedules the task for repeating execution on the defined period after the defined initial period 
	 * @param name The name of the closure to schedule
	 * @param period The fixed delay of the executions
	 * @param initial The initial delay when first scheduled
	 * @param unit The unit of the period and initial
	 */
	@ManagedOperation(description="Schedules the task for repeating execution on the defined period after the defined initial period")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="name", description="The name of the closure to schedule"),
		@ManagedOperationParameter(name="period", description="The fixed delay of the executions"),
		@ManagedOperationParameter(name="initial", description="The initial delay when first scheduled"),
		@ManagedOperationParameter(name="unit", description="The unit of the period and initial")
	})
	public void schedule(String name, long period, long initial, String unit);
	
	/**
	 * Schedules the task for repeating execution based on the passed cron expression
	 * @param name The name of the closure to schedule
	 * @param cron The cron expression defining the execution schedule
	 */
	@ManagedOperation(description="Schedules the task for repeating execution on the defined period after the defined initial period")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="name", description="The name of the closure to schedule"),
		@ManagedOperationParameter(name="cron", description="The cron expression defining the execution schedule")
	})
	public void schedule(String name, String cron);
	
	/**
	 * Returns the names of the closures annotated for scheduled execution
	 * @return an array of closure names
	 */
	@ManagedAttribute(description="The names of the closures annotated for scheduled execution")
	public String[] getScheduledNames();
	
	/**
	 * Stops the scheduled exection
	 * @param name The name of the scheduled task to cancel
	 */
	@ManagedOperation(description="Stops the named scheduled exection")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="name", description="The name of the scheduled closure to cancel")
	})	
	public void cancelSchedule(String name);
	
	/**
	 * Returns the scheduled period
	 * @param name The name of the scheduled task to get the period for
	 * @return the scheduled period
	 */
	@ManagedOperation(description="The scheduled period")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="name", description="The name of the scheduled closure to get the period for")
	})		
	public long getPeriod(String name);

	/**
	 * Returns the scheduled initial delay
	 * @param name The name of the scheduled task to get the initial delay for
	 * @return the scheduled initial delay
	 */
	@ManagedAttribute(description="The scheduled initial delay")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="name", description="The name of the scheduled closure to get the period for")
	})			
	public long getInitial(String name);
	
	
	/**
	 * Returns the schedule initial and period unit
	 * @param name The name of the scheduled task to get the initial delay for
	 * @return the schedule initial and period unit
	 */
	@ManagedOperation(description="The schedule initial and period unit")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="name", description="The name of the scheduled closure to get the unit for")
	})				
	public TimeUnit getUnit(String name);
	
	/**
	 * Returns the schedule cron expression
	 * @param name The name of the scheduled task to get the cron expression for
	 * @return the scheduled task cron expression
	 */
	@ManagedOperation(description="The schedule cron expression")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="name", description="The name of the scheduled closure to get the unit for")
	})				
	public String getCron(String name);
	

//	/**
//	 * Returns a sumary of the scehduled tasks
//	 * @return an array of scheduled closure objects
//	 */
//	@ManagedAttribute(description="An array of scheduled task objects")
//	public ScheduledClosure[] getScheduledTasks();
}
