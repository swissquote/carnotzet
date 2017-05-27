package com.github.swissquote.carnotzet.extention;

import java.util.Properties;

import com.github.swissquote.carnotzet.core.CarnotzetExtension;
import com.github.swissquote.carnotzet.maven.plugin.spi.CarnotzetExtensionsFactory;

public class HelloLabelExtensionFactory implements CarnotzetExtensionsFactory {

	@Override
	public CarnotzetExtension create(Properties configuration) {
		return new HelloLabelExtension(configuration.getProperty("message.text", "Property not found"));
	}
}
