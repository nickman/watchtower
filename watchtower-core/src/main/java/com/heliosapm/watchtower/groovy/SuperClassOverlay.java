/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.watchtower.groovy;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.DelegatingCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: SuperClassOverlay</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.groovy.SuperClassOverlay</code></p>
 */

public class SuperClassOverlay extends CompilationCustomizer {
	/** The instance logger */
	protected static final Logger log = LoggerFactory.getLogger(SuperClassOverlay.class);

	/** The super class to overlay */
	protected final Class<?> superClass;
	
	/**
	 * Creates a new conversion phase SuperClassOverlay
	 * @param superClass The super class to overlay
	 */
	public SuperClassOverlay(Class<?> superClass) {
		super(CompilePhase.CONVERSION);
		this.superClass = superClass;
	}

	@Override
	public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
		StringBuilder b = new StringBuilder("\n\t******************\n\tCompilation Customizer Callback\n\tClass:").append(classNode.getName()).append("\n\t******************\n");
		classNode.setSuperClass(new ClassNode(superClass));
		log.info(b.toString());
		
	}


}
