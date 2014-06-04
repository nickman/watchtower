/**
 * 
 */
package com.heliosapm.watchtower.deployer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.io.AbstractResource;
import org.springframework.util.Assert;

/**
 * <p>Title: BeanDefinitionResource</p>
 * <p>Description: Loadable Resource wrapper for a bean definition</p>
 * <p>Copied from {@link org.springframework.beans.factory.support.BeanDefinitionResource} for access.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.watchtower.deployer.BeanDefinitionResource</code></b>
 */

public class BeanDefinitionResource extends AbstractResource {

	private final BeanDefinition beanDefinition;
	private final File file;
	private final String beanName;


	/**
	 * Returns the 
	 * @return the beanName
	 */
	public String getBeanName() {
		return beanName;
	}

	/**
	 * Create a new BeanDefinitionResource.
	 * @param beanDefinition the BeanDefinition objectto wrap
	 * @param file The file the resource came from
	 */
	public BeanDefinitionResource(BeanDefinition beanDefinition, File file, String beanName) {
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");
		this.beanDefinition = beanDefinition;
		this.file = file;
		this.beanName = beanName;
	}
	
	@Override
	public String getFilename() {		
		return file.getName().replace("groovy", "gru");
	}
	
	@Override
	public File getFile() throws IOException {		
		return file;
	}

	/**
	 * Return the wrapped BeanDefinition object.
	 * @return the wrapped BeanDefinition object
	 */
	public final BeanDefinition getBeanDefinition() {
		return this.beanDefinition;
	}


	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public boolean isReadable() {
		return false;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		throw new FileNotFoundException(
				"Resource cannot be opened because it points to " + getDescription());
	}

	@Override
	public String getDescription() {
		return "BeanDefinition defined in " + this.beanDefinition.getResourceDescription();
	}


	/**
	 * This implementation compares the underlying BeanDefinition.
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj == this ||
			(obj instanceof BeanDefinitionResource &&
						((BeanDefinitionResource) obj).beanDefinition.equals(this.beanDefinition)));
	}

	/**
	 * This implementation returns the hash code of the underlying BeanDefinition.
	 */
	@Override
	public int hashCode() {
		return this.beanDefinition.hashCode();
	}

}
