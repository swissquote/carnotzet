package com.github.swissquote.carnotzet.maven.plugin.spi;

import java.util.Properties;

public interface ExtensionFactory<E> {
	E create(Properties configuration);
}
