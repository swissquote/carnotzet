package com.github.swissquote.carnotzet.maven.plugin;

import java.util.Properties;

import com.github.swissquote.carnotzet.maven.plugin.spi.CarnotzetExtensionsFactory;

import lombok.Data;

/**
 * Maven configuration for initializing {@link com.github.swissquote.carnotzet.core.CarnotzetExtension}
 */
@Data
public class ExtensionConfiguration {
	/**
	 * The class name of the {@link com.github.swissquote.carnotzet.maven.plugin.spi.CarnotzetExtensionsFactory} instance to be configured using
	 * provided properties
	 */
	private String factoryClass;
	/**
	 * Properties to be used for {@link CarnotzetExtensionsFactory} configuration
	 */
	private Properties properties;

	public boolean isFor(Class<? extends CarnotzetExtensionsFactory> extFactoryClass) {
		return extFactoryClass.getName().equalsIgnoreCase(factoryClass);
	}
}
