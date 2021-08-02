package com.github.swissquote.carnotzet.maven.plugin.spi;

import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntimeExtension;

/**
 * The interface for creating and initializing {@link com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntimeExtension}
 * instance
 */
public interface ContainerOrchestrationRuntimeExtensionsFactory extends ExtensionFactory<ContainerOrchestrationRuntimeExtension> {
}
