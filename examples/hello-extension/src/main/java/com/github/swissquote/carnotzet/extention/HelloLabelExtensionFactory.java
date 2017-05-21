package com.github.swissquote.carnotzet.extention;

import com.github.swissquote.carnotzet.core.CarnotzetExtension;
import com.github.swissquote.carnotzet.core.CarnotzetExtensionsFactory;

import java.util.Properties;

public class HelloLabelExtensionFactory implements CarnotzetExtensionsFactory {
	@Override
	public CarnotzetExtension create(Properties configuration) {
		return new HelloLabelExtension(configuration.getProperty("message.text", "Property not found"));
	}
}
