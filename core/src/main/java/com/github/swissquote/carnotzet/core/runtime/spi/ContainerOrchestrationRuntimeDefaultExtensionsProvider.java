package com.github.swissquote.carnotzet.core.runtime.spi;

import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntimeExtension;

public interface ContainerOrchestrationRuntimeDefaultExtensionsProvider {

	/**
	 * Allows to provide runtime extensions that should be enabled by default in all runtimes if the list of extensions is not explicit.
	 */
	ContainerOrchestrationRuntimeExtension getDefaultExtension();

}
