package com.github.swissquote.carnotzet.maven.plugin;


import com.github.swissquote.carnotzet.core.CarnotzetExtensionsFactory;
import lombok.Data;

import java.util.Properties;

/**
 * Maven configuration for initializing {@link com.github.swissquote.carnotzet.core.CarnotzetExtension}
 */
@Data
public class ExtensionConfiguration {
	private String factoryClass;
	private Properties properties;

	public boolean isFor(Class<? extends CarnotzetExtensionsFactory> extFactoryClass) {
		return extFactoryClass.getName().equalsIgnoreCase(factoryClass);
	}
}
