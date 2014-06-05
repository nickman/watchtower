/**
 * 
 */
package com.heliosapm.watchtower.groovy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.management.Notification;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
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
import com.heliosapm.watchtower.core.impl.CollectionResult;
import com.heliosapm.watchtower.core.impl.IAllIServiceAspects;
import com.heliosapm.watchtower.core.impl.ServiceAspectImpl;

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
//						Method m = sa.getBoundInterface().getDeclaredMethods()[0];
//						
//						Class<?>[] pTypes = m.getParameterTypes();
//						Parameter[] params = new Parameter[pTypes.length];
//						for(int i = 0; i < pTypes.length; i++) {
//							params[i] = new Parameter(new ClassNode(pTypes[i]), "p"+i); 										
//						}
//						MethodNode methodNode = TEMPLATE.getDeclaredMethod(m.getName(), params);
//						String statementText = methodNode.getFirstStatement().getText();
//						//methodNode.setDeclaringClass(classNode);
//						
//						
//						classNode.addMethod(methodNode.getName(), methodNode.getModifiers(), methodNode.getReturnType(), methodNode.getParameters(), methodNode.getExceptions(), methodNode.getFirstStatement());
						Method m = sa.getBoundInterface().getDeclaredMethods()[0];
						StringBuilder astCode = new StringBuilder();
						final boolean isReturn = !void.class.equals(m.getReturnType());
						if(isReturn) {
							astCode.append("return super.").append(m.getName()).append("(");
						} else {
							astCode.append("super.").append(m.getName()).append("(");
						}
						
								
						Class<?>[] pTypes = m.getParameterTypes();
						Class<?>[] xTypes = m.getExceptionTypes();
						
						Parameter[] params = new Parameter[pTypes.length];
						ClassNode[] exes = new ClassNode[xTypes.length];
						
						if(pTypes.length > 0) {
							for(int i = 0; i < pTypes.length; i++) {
								params[i] = new Parameter(ClassHelper.make(pTypes[i]), "p"+i); 										
								astCode.append(pTypes[i].getName()).append(" p").append(i).append(",");
							}
							astCode.deleteCharAt(astCode.length()-1);
						}
						astCode.append(");");
						if(!isReturn) {
							astCode.append("return;");
						}						
						for(int i = 0; i < xTypes.length; i++) {
							exes[i] = new ClassNode(xTypes[i]);
						}
												
						AstBuilder builder = new AstBuilder();
						List<ASTNode> nodes = builder.buildFromString(CompilePhase.CLASS_GENERATION, astCode.toString());
						Statement statement = (Statement)nodes.get(0);
						classNode.addMethod(m.getName(), m.getModifiers() & ~Modifier.ABSTRACT, ClassHelper.make(m.getReturnType()), params, exes, statement);
						
						b.append("\n\t").append(sa.getBoundInterface().getSimpleName());
					}
				}
			}
		}
		log.info(b.toString());
	}
	
	private static class EmptyImpl extends ServiceAspectImpl implements IAllIServiceAspects {

		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.watchtower.core.impl.IDependency#setDependency(java.lang.String, java.lang.Object)
		 */
		@Override
		public void setDependency(String name, Object value) {
			super.setDependency(name, value);
			
		}

		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.watchtower.core.impl.IEventListener#onEvent(javax.management.Notification)
		 */
		@Override
		public void onEvent(Notification notification) {
			super.onEvent(notification);
			
		}

		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.watchtower.core.impl.INamed#getScriptName()
		 */
		@Override
		public String getScriptName() {
			return super.getScriptName();
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.watchtower.core.impl.ServiceAspectImpl#cancelSchedule()
		 */
		@Override
		public void cancelSchedule() {
			super.cancelSchedule();
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.watchtower.core.impl.ServiceAspectImpl#collect()
		 */
		@Override
		public CollectionResult collect() {
			return super.collect();
		}
		
/**
		 * {@inheritDoc}
		 * @see com.heliosapm.watchtower.core.impl.ServiceAspectImpl#getInitial()
		 */
		@Override
		public long getInitial() {
			return super.getInitial();
		}
		
/**
		 * {@inheritDoc}
		 * @see com.heliosapm.watchtower.core.impl.ServiceAspectImpl#getPeriod()
		 */
		@Override
		public long getPeriod() {
			return super.getPeriod();
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.watchtower.core.impl.ServiceAspectImpl#getUnit()
		 */
		@Override
		public TimeUnit getUnit() {
			return super.getUnit();
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.watchtower.core.impl.ServiceAspectImpl#schedule(long, long, java.util.concurrent.TimeUnit)
		 */
		@Override
		public void schedule(long period, long initial, TimeUnit unit) {
			super.schedule(period, initial, unit);
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.watchtower.core.impl.ServiceAspectImpl#setInitial(long)
		 */
		@Override
		public void setInitial(long initial) {
			super.setInitial(initial);
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.watchtower.core.impl.ServiceAspectImpl#setPeriod(long)
		 */
		@Override
		public void setPeriod(long period) {
			super.setPeriod(period);
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.watchtower.core.impl.ServiceAspectImpl#setUnit(java.util.concurrent.TimeUnit)
		 */
		@Override
		public void setUnit(TimeUnit unit) {
			super.setUnit(unit);
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.watchtower.core.impl.ServiceAspectImpl#start()
		 */
		@Override
		public void start() throws Exception {			
			super.start();
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.watchtower.core.impl.ServiceAspectImpl#stop()
		 */
		@Override
		public void stop() {
			super.stop();
		}
	}
	
	private static final ClassNode TEMPLATE = new ClassNode(EmptyImpl.class);

}

/*
						Method m = clazz.getDeclaredMethods()[0];
						StringBuilder astCode = new StringBuilder("return super.").append(m.getName()).append("(");
								
						Class<?>[] pTypes = m.getParameterTypes();
						Class<?>[] xTypes = m.getExceptionTypes();
						
						Parameter[] params = new Parameter[pTypes.length];
						ClassNode[] exes = new ClassNode[xTypes.length];
						
						if(pTypes.length > 0) {
							for(int i = 0; i < pTypes.length; i++) {
								params[i] = new Parameter(new ClassNode(pTypes[i]), "p"+i); 										
								astCode.append(pTypes[i].getName()).append(" p").append(i).append(",");
							}
							astCode.deleteCharAt(astCode.length()-1);
						}
						astCode.append(");");
						for(int i = 0; i < xTypes.length; i++) {
							exes[i] = new ClassNode(xTypes[i]);
						}
												
						classNode.addMethod(m.getName(), m.getModifiers() & ~Modifier.ABSTRACT, new ClassNode(m.getReturnType()), params, exes, (Statement)new AstBuilder().buildFromString(astCode.toString()).get(0));

 */ 
