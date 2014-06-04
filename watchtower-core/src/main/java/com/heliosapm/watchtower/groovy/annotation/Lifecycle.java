/**
 * 
 */
package com.heliosapm.watchtower.groovy.annotation;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Title: Lifecycle</p>
 * <p>Description: Annotation to define a script as supporting the basic lifecycle coventions defined in {@link com.heliosapm.watchtower.core.impl.ILifecycle}</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.watchtower.groovy.annotation.Lifecycle</code></b>
 */
@Target(value={TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lifecycle {
	/* No Op */
}
