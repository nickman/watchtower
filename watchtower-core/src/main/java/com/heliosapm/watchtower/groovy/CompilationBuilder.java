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

import groovy.lang.GroovyCodeSource;

import java.io.File;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

/**
 * <p>Title: CompilationBuilder</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.watchtower.groovy.CompilationBuilder</code></p>
 */

public class CompilationBuilder {
	/** The compilation target */
	protected final GroovyCodeSource codeSource;
	/** The compiler configuration */
	protected final CompilerConfiguration compilerConfiguration; 
	
	/** Serial number generator for anonymous classes */
	protected static final AtomicLong syntheticNameSerial = new AtomicLong(0L);
	
	/**
	 * Creates a new CompilationBuilder
	 * @param source The source file 
	 * @param compilerConfiguration The optional supplied compiler configuration
	 */
	CompilationBuilder(File source, CompilerConfiguration compilerConfiguration) {
		if(source==null) throw new IllegalArgumentException("The passed file was null");
		if(source.canRead()) throw new RuntimeException("The file [" + source + "] cannot be read");
		this.compilerConfiguration = compilerConfiguration!=null ? compilerConfiguration : new CompilerConfiguration(CompilerConfiguration.DEFAULT);
		try {
			codeSource = new GroovyCodeSource(source);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create GroovyCodeSource from [" + source + "]", ex);
		}
	}

	/**
	 * Creates a new CompilationBuilder
	 * @param source The source URI 
	 * @param compilerConfiguration The optional supplied compiler configuration
	 */
	CompilationBuilder(URI source, CompilerConfiguration compilerConfiguration) {
		if(source==null) throw new IllegalArgumentException("The passed URI was null");
		this.compilerConfiguration = compilerConfiguration!=null ? compilerConfiguration : new CompilerConfiguration(CompilerConfiguration.DEFAULT);
		try {
			codeSource = new GroovyCodeSource(source);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create GroovyCodeSource from [" + source + "]", ex);
		}
	}
	
	/**
	 * Creates a new CompilationBuilder
	 * @param source The source URL 
	 * @param compilerConfiguration The optional supplied compiler configuration
	 */
	CompilationBuilder(URL source, CompilerConfiguration compilerConfiguration) {
		if(source==null) throw new IllegalArgumentException("The passed URL was null");
		this.compilerConfiguration = compilerConfiguration!=null ? compilerConfiguration : new CompilerConfiguration(CompilerConfiguration.DEFAULT);
		try {
			codeSource = new GroovyCodeSource(source);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create GroovyCodeSource from [" + source + "]", ex);
		}
	}
	
	/**
	 * Creates a new CompilationBuilder
	 * @param source The source text
	 * @param name The synthetic file name
	 * @param compilerConfiguration The optional supplied compiler configuration
	 */
	CompilationBuilder(CharSequence source, String name, CompilerConfiguration compilerConfiguration) {
		if(source==null) throw new IllegalArgumentException("The passed source text was null");
		if(name==null) name = "WatchTowerGroovy" + syntheticNameSerial.incrementAndGet();
		this.compilerConfiguration = compilerConfiguration!=null ? compilerConfiguration : new CompilerConfiguration(CompilerConfiguration.DEFAULT);		
		try {
			codeSource = new GroovyCodeSource(source.toString(), name, "watchtower");
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create GroovyCodeSource from [" + name + "/" + source + "]", ex);
		}
	}
	
	/**
	 * Creates a new CompilationBuilder
	 * @param source The source reader
	 * @param name The synthetic file name
	 * @param compilerConfiguration The optional supplied compiler configuration
	 */
	CompilationBuilder(Reader source, String name, CompilerConfiguration compilerConfiguration) {
		if(source==null) throw new IllegalArgumentException("The passed source text was null");
		if(name==null) name = "WatchTowerGroovy" + syntheticNameSerial.incrementAndGet();
		this.compilerConfiguration = compilerConfiguration!=null ? compilerConfiguration : new CompilerConfiguration(CompilerConfiguration.DEFAULT);
		try {
			codeSource = new GroovyCodeSource(source.toString(), name, "watchtower");
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create GroovyCodeSource from [" + name + "/" + source + "]", ex);
		}
	}
	
	/**
	 * Adds compilation customizers to the compile request
	 * @param customizers An array of compilation customizers 
	 * @return this builder
	 */
	public CompilationBuilder addCompilationCustomizers(CompilationCustomizer... customizers) {
		if(customizers!=null && customizers.length>0) {
			compilerConfiguration.addCompilationCustomizers(customizers);
		}
		return this;
	}

	/**
	 * Sets the compiler warning level
	 * @param level the compiler warning level
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setWarningLevel(int)
	 * @return this builder
	 */
	public CompilationBuilder setWarningLevel(int level) {
		compilerConfiguration.setWarningLevel(level);
		return this;
	}

	/**
	 * Adds classpath definitions to the compiler configuration
	 * @param parts a list of classpath entries
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setClasspathList(java.util.List)
	 * @return this builder
	 */
	public CompilationBuilder setClasspathList(List<String> parts) {
		compilerConfiguration.setClasspathList(parts);
		return this;
	}
	
	/**
	 * Adds classpath definitions to the compiler configuration
	 * @param parts an array of classpath entries
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setClasspathList(java.util.List)
	 * @return this builder
	 */
	public CompilationBuilder setClasspathList(String ...parts) {
		if(parts!=null && parts.length>0) {
			compilerConfiguration.setClasspathList(new ArrayList<String>(Arrays.asList(parts)));
		}
		return this;
	}
	

	/**
	 * Enables or diabled compiler verbosity
	 * @param verbose true to enable, false otherwise
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setVerbose(boolean)
	 * @return this builder
	 */
	public CompilationBuilder setVerbose(boolean verbose) {
		compilerConfiguration.setVerbose(verbose);
		return this;
	}

	/**
	 * Enables or disabled debug in the compilation
	 * @param debug true to enable debug, false otherwise
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setDebug(boolean)
	 * @return this builder
	 */
	public CompilationBuilder setDebug(boolean debug) {
		compilerConfiguration.setDebug(debug);
		return this;
	}

	/**
	 * Sets the script base class
	 * @param scriptBaseClass the script base class
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setScriptBaseClass(java.lang.String)
	 * @return this builder
	 */
	public CompilationBuilder setScriptBaseClass(String scriptBaseClass) {
		compilerConfiguration.setScriptBaseClass(scriptBaseClass);
		return this;
	}


	/**
	 * Sets the default script extension
	 * @param defaultScriptExtension the default script extension
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setDefaultScriptExtension(java.lang.String)
	 * @return this builder
	 */
	public CompilationBuilder setDefaultScriptExtension(String defaultScriptExtension) {
		compilerConfiguration.setDefaultScriptExtension(defaultScriptExtension);
		return this;
	}

	/**
	 * Indicates if the source should be elligible for recompilation
	 * @param recompile true if the source should be elligible for recompilation, false otherwise
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setRecompileGroovySource(boolean)
	 * @return this builder
	 */
	public CompilationBuilder setRecompileGroovySource(boolean recompile) {
		compilerConfiguration.setRecompileGroovySource(recompile);
		return this;
	}

	/**
	 * Sets the minimum recompilation interval
	 * @param time the minimum recompilation interval
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setMinimumRecompilationInterval(int)
	 * @return this builder
	 */
	public CompilationBuilder setMinimumRecompilationInterval(int time) {
		compilerConfiguration.setMinimumRecompilationInterval(time);
		return this;
	}

	/**
	 * Sets the compiler optimization options
	 * @param options the compiler optimization options
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setOptimizationOptions(java.util.Map)
	 * @return this builder
	 */
	public CompilationBuilder setOptimizationOptions(Map<String, Boolean> options) {
		compilerConfiguration.setOptimizationOptions(options);
		return this;
	}
	
	
}
