/**
 * 
 */
package com.heliosapm.watchtower.groovy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
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
					if(sa!=null) {
						 
						classNode.addInterface(sa.getIfaceNode());
						for(MethodNode mn: buildMethodNodes(sa.getBoundInterface())) {
							classNode.addMethod(mn);
						}
						b.append("\n\t").append(sa.getBoundInterface().getSimpleName());
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
		Method[] methods = clazz.getDeclaredMethods();
		List<MethodNode> methodNodes = new ArrayList<MethodNode>(methods.length);
		for(Method m: methods) {
			methodNodes.add(buildMethodNode(m));
		}
		return methodNodes.toArray(new MethodNode[methods.length]);
	}
	
	/**
	 * Builds a method node that implements the passed method
	 * @param m The method to build the method node for
	 * @return the method node
	 */
	protected MethodNode buildMethodNode(Method m) {
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
							astCode.append(pTypes[i].getName()).append(" p").append(i).append(",");
						}
						astCode.deleteCharAt(astCode.length()-1);
					}
					astCode.append(");");
					if(!isReturn) {
						astCode.append("return;");
					}						
					source = astCode.toString();
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

