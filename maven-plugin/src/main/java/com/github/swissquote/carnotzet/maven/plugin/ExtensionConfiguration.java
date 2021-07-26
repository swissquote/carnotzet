package com.github.swissquote.carnotzet.maven.plugin;

import java.util.Properties;

import com.github.swissquote.carnotzet.maven.plugin.spi.ExtensionFactory;

import lombok.Data;

/**
 * Maven configuration for initializing {@link ExtensionFactory}
 */
@Data
public class ExtensionConfiguration {
	/**
	 * The class name of the {@link ExtensionFactory} instance to be configured using provided properties
	 */
	private String factoryClass;
	/**
	 * Properties to be used for {@link ExtensionFactory} configuration
	 */
	private Properties properties;

	public <F> boolean isFor(Class<F> extFactoryClass) {
		return extFactoryClass.getName().equalsIgnoreCase(factoryClass);
	}
}
