
package com.heliosapm.watchtower.groovy.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Title: ScriptName</p>
 * <p>Description:  Type level annotation to specify the name of the script.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.groovy.annotation.ScriptName</code></p>
 */
@Target(value={CONSTRUCTOR,FIELD,LOCAL_VARIABLE,METHOD,PARAMETER,TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScriptName {
	/**
	 * The name of this script
	 */
	public String value();

}
