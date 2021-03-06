/**
 * 
 */
package com.heliosapm.watchtower.groovy;

import groovy.transform.InheritConstructors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.stmt.Statement;
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
	
	
	/** Instance logger */	
	protected final LoggerContext logCtx = (LoggerContext)LoggerFactory.getILoggerFactory();
	/** Instance logger */
	protected final Logger log = logCtx.getLogger(getClass());
	/** A cache of interface implementing source keyed by method */
	protected final ConcurrentHashMap<Method, String> sourceCache = new ConcurrentHashMap<Method, String>();

	/** The inherrit constructors class node */
	protected final ClassNode inherritCtorsNode = ClassHelper.make(InheritConstructors.class);
	/** The inherrit constructors annotation node */
	protected final AnnotationNode inherritCtorsAnnNode = new AnnotationNode(inherritCtorsNode);

	
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
		if(classNode.getAnnotations(inherritCtorsNode).isEmpty()) {
			classNode.addAnnotation(inherritCtorsAnnNode);
		}
		Set<ServiceAspect> appliedAspects = EnumSet.noneOf(ServiceAspect.class);
		StringBuilder b = new StringBuilder("\n\tAdded Interfaces for class [").append(classNode).append("]");
		for(AnnotationNode anode : classNode.getAnnotations()) {
			Class<? extends Annotation> annClass = anode.getClassNode().getTypeClass();
			if(annClass!=null) {
				ServiceAspect sa = ServiceAspect.getAspectForAnnotation(annClass);
				if(sa!=null) {
					classNode.addInterface(sa.getIfaceNode());
					b.append("\n\t").append(sa.getBoundInterface().getSimpleName());
				}
			}
		}

		for(FieldNode fieldNode: classNode.getFields()) {
			for(AnnotationNode anode: fieldNode.getAnnotations()) {
				Class<? extends Annotation> annClass = anode.getClassNode().getTypeClass();
				if(annClass!=null) {
					ServiceAspect sa = ServiceAspect.getAspectForAnnotation(annClass);
					
					if(sa!=null && !appliedAspects.contains(sa)) {
						appliedAspects.add(sa);
						try {
							classNode.addInterface(sa.getIfaceNode());
							for(MethodNode mn: buildMethodNodes(sa.getBoundInterface())) {
								classNode.addMethod(mn);
							}
							b.append("\n\t").append(sa.getBoundInterface().getSimpleName());
						} catch (Exception ex) {
							String msg = String.format("Failed to process interface overlay on [%s] for [%s]", classNode, sa.getIfaceNode());
							log.error(msg, ex);
							throw new RuntimeException(msg, ex);
						}
					}
				}
			}
		}
		log.info(b.toString());
	}
	
	
	/**
	 * Builds an array of method nodes for the passed class's declared methods
	 * @param clazz The class to build methods nodes for
	 * @return an array of method nodes
	 */
	protected MethodNode[] buildMethodNodes(Class<?> clazz) {
		ClassNode classNode = new ClassNode(clazz);
		Method[] methods = clazz.getDeclaredMethods();
		List<MethodNode> methodNodes = new ArrayList<MethodNode>(methods.length);
		for(Method m: methods) {
			Class<?>[] paramTypes = m.getParameterTypes();
			Parameter[] paramNodes = new Parameter[paramTypes.length];
			for(int i = 0; i < paramTypes.length; i++) {
				paramNodes[i] = new Parameter(ClassHelper.make(paramTypes[i]), "p" + i);
			}
			MethodNode methodNode = classNode.getMethod(m.getName(), paramNodes);
			methodNodes.add(buildMethodNode(m, methodNode));
		}
		return methodNodes.toArray(new MethodNode[methods.length]);
	}
	
/*
 * org.codehaus.groovy.control.MultipleCompilationErrorsException: startup failed:
	/home/nwhitehead/.watchtower/deploy/foo-bar2/LocalStatsCollector.groovy: -1: 
	A transform used a generics containing ClassNode 
	com.heliosapm.watchtower.core.impl.ServiceAspectImpl$ScheduledClosure <T extends java.lang.Object -> java.lang.Object> 
	for the method public [Lcom.heliosapm.watchtower.core.impl.ServiceAspectImpl$ScheduledClosure; 
	getScheduledTasks()  { ... } directly. 
	You are not supposed to do this. 
	Please create a new ClassNode referring to the old ClassNode and use the new ClassNode instead of the old one. 
	Otherwise the compiler will create wrong descriptors and a potential NullPointerException in TypeResolver in the OpenJDK. If this is not your own doing, please report this bug to the writer of the transform.
 @ line -1, column -1.
1 error	
 */
	
	/**
	 * Builds a method node that implements the passed method
	 * @param m The method to build the method node for
	 * @param methodNode The groovy ast method node
	 * @return the method node
	 */
	protected MethodNode buildMethodNode(Method m, MethodNode methodNode) {
		Class<?>[] pTypes = m.getParameterTypes();
		Class<?>[] xTypes = m.getExceptionTypes();

		String source = sourceCache.get(m);
		
		if(source==null) {
			synchronized(sourceCache) {
				source = sourceCache.get(m);
				if(source==null) {
					StringBuilder astCode = new StringBuilder();
					final boolean isReturn = !void.class.equals(m.getReturnType());
					if(isReturn) {
						astCode.append("return super.").append(m.getName()).append("(");
					} else {
						astCode.append("super.").append(m.getName()).append("(");
					}
					if(pTypes.length > 0) {
						for(int i = 0; i < pTypes.length; i++) {
							astCode.append(" p").append(i).append(",");
						}
						astCode.deleteCharAt(astCode.length()-1);
					}
					astCode.append(");");
					if(!isReturn) {
						astCode.append("return;");
					}						
					source = astCode.toString();
					log.info("\n\tCaching Groovy Source for Service Aspect [{}]\n[{}]", m.toGenericString(), source);
					sourceCache.put(m, source);
				}
			}
		}
		Parameter[] params = new Parameter[pTypes.length];
		ClassNode[] exes = new ClassNode[xTypes.length];		
		for(int i = 0; i < pTypes.length; i++) {
			params[i] = new Parameter(ClassHelper.make(pTypes[i]), "p"+i); 			
		}		
		for(int i = 0; i < xTypes.length; i++) {
			exes[i] = new ClassNode(xTypes[i]);
		}								
		AstBuilder builder = new AstBuilder();
		List<ASTNode> nodes = builder.buildFromString(CompilePhase.CLASS_GENERATION, source);
		Statement statement = (Statement)nodes.get(0);
		return new MethodNode(m.getName(), m.getModifiers() & ~Modifier.ABSTRACT, ClassHelper.make(m.getReturnType()), params, exes, statement);		
	}
	
	
	
}

