package com.github.swissquote.carnotzet.maven.plugin.spi;

import java.util.Properties;

import com.github.swissquote.carnotzet.core.CarnotzetExtension;

/**
 * The interface for creating and initializing {@link CarnotzetExtension} particular instance
 */
public interface CarnotzetExtensionsFactory {
	CarnotzetExtension create(Properties configuration);
}
