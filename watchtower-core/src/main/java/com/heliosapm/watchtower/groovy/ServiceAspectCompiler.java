/**
 * 
 */
package com.heliosapm.watchtower.groovy;

import java.lang.annotation.Annotation;
import java.util.EnumMap;
import java.util.Map;

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

import com.heliosapm.watchtower.core.ServiceAspect;

/**
 * <p>Title: ServiceAspectCompiler</p>
 * <p>Description: Applies the interface overlays on compiled groovy classes in accordance with the discovered annotations</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.watchtower.groovy.ServiceAspectCompiler</code></b>
 */

public class ServiceAspectCompiler extends CompilationCustomizer {
	
	private static final Map<ServiceAspect, AnnotationNode> annotationNodes = new EnumMap<ServiceAspect, AnnotationNode>(ServiceAspect.class);
	private static final Map<ServiceAspect, ClassNode> annotationClassNodes = new EnumMap<ServiceAspect, ClassNode>(ServiceAspect.class);
	private static final Map<ServiceAspect, ClassNode> ifaceClassNodes = new EnumMap<ServiceAspect, ClassNode>(ServiceAspect.class);
	
	static {
		for(ServiceAspect sa: ServiceAspect.values()) {
			ClassNode cn = new ClassNode(sa.getAnnotationType());
			annotationNodes.put(sa, new AnnotationNode(cn));
			annotationClassNodes.put(sa, cn);
			ifaceClassNodes.put(sa, new ClassNode(sa.getBoundInterface()));
		}
	}
	
	
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
			Class<? extends Annotation> annClass = anode.getClassNode().getTypeClass();
			ServiceAspect sa = ServiceAspect.getAspectForAnnotation(annClass);
			if(sa!=null) {
				classNode.addInterface(ifaceClassNodes.get(sa));
			}
		}
	}

}
