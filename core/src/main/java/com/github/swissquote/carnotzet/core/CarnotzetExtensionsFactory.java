package com.github.swissquote.carnotzet.core;

import java.util.Properties;

/**
 * The interface for creating and initializing {@link CarnotzetExtension} particular instance
 */
public interface CarnotzetExtensionsFactory {
	CarnotzetExtension create(Properties configuration);
}
