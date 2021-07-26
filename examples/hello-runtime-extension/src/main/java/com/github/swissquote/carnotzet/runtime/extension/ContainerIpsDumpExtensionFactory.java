package com.github.swissquote.carnotzet.runtime.extension;

import java.nio.file.Paths;
import java.util.Properties;

import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntimeExtension;
import com.github.swissquote.carnotzet.maven.plugin.spi.ContainerOrchestrationRuntimeExtensionsFactory;

public class ContainerIpsDumpExtensionFactory implements ContainerOrchestrationRuntimeExtensionsFactory {

	@Override
	public ContainerOrchestrationRuntimeExtension create(Properties configuration) {
		return new ContainerIpsDumpExtension(
				configuration.getProperty("message.text", "Property not found"),
				Paths.get(configuration.getProperty("dump.directory", "/tmp/carnotzet_ip_dump"))
		);
	}
}
