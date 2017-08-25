package com.github.swissquote.carnotzet.core;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import com.github.swissquote.carnotzet.core.maven.CarnotzetModuleCoordinates;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * Represents and application with it's configuration inside a Carnotzet environment
 */
@Value
@AllArgsConstructor
@Builder(toBuilder = true)
public class CarnotzetModule {

	private final CarnotzetModuleCoordinates id;
	private final String name;
	private final String topLevelModuleName;
	private final Map<String, String> properties;
	private final Map<String, String> labels;
	private final String imageName;
	private final Set<String> dockerVolumes;
	private final String dockerEntrypoint;
	private final String dockerCmd;
	private final Set<String> dockerEnvFiles;
	private final Path jarPath;

	public String getShortImageName() {
		String withoutHost = imageName.replaceFirst(".*/", "");
		return withoutHost.replaceFirst(":.*", "");
	}
}
