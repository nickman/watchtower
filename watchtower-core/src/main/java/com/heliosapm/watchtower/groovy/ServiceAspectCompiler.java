/**
 * 
 */
package com.heliosapm.watchtower.groovy;

import java.util.List;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import com.heliosapm.watchtower.core.impl.ILifecycle;
import com.heliosapm.watchtower.groovy.annotation.Lifecycle;

/**
 * <p>Title: ServiceAspectCompiler</p>
 * <p>Description: Applies the interface overlays on compiled groovy classes in accordance with the discovered annotations</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.watchtower.groovy.ServiceAspectCompiler</code></b>
 */

public class ServiceAspectCompiler extends CompilationCustomizer {
	private static final AnnotationNode LIFECYCLE_ANNOTATION_NODE = new AnnotationNode(new ClassNode(Lifecycle.class));
	private static final ClassNode LIFECYCLE_ANNOTATION_CNODE = new ClassNode(Lifecycle.class);
	private static final ClassNode LIFECYCLE_IFACE_NODE = new ClassNode(ILifecycle.class);
	
	/** Instance logger */	
	protected final LoggerContext logCtx = (LoggerContext)LoggerFactory.getILoggerFactory();
	/** Instance logger */
	protected Logger log = logCtx.getLogger(getClass());


	
	/** A convenience sharable static ServiceAspectCompiler */
	public static final ServiceAspectCompiler Instance = new ServiceAspectCompiler();
	/**
	 * Creates a new ServiceAspectCompiler
	 */

	public ServiceAspectCompiler() {
		super(CompilePhase.CANONICALIZATION);
	}

	/**
	 * {@inheritDoc}
	 * @see org.codehaus.groovy.control.CompilationUnit.PrimaryClassNodeOperation#call(org.codehaus.groovy.control.SourceUnit, org.codehaus.groovy.classgen.GeneratorContext, org.codehaus.groovy.ast.ClassNode)
	 */
	@Override
	public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
		for(AnnotationNode anode : classNode.getAnnotations()) {
			if(anode.getClassNode().getTypeClass().equals(Lifecycle.class)) {
				classNode.addInterface(LIFECYCLE_IFACE_NODE);
				break;
			}
		}
	}

}
