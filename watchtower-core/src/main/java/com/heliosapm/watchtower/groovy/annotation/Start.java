
package com.heliosapm.watchtower.groovy.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * <p>Title: Start</p>
 * <p>Description: Annotates a method in a groovy script that should be invoked when the class is loaded.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.groovy.annotation.Start</code></p>
 */
@Target({METHOD, FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Start {

}
