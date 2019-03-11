package com.github.swissquote.carnotzet.core;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
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
	private final String serviceId;
	private final String topLevelModuleName;
	@Builder.Default
	private final Map<String, String> properties = new HashMap<>();
	@Builder.Default
	private final Map<String, String> labels = new HashMap<>();
	private final String imageName;
	@Builder.Default
	private final Set<String> dockerVolumes = new HashSet<>();
	// supports shell and exec formats (same as Dockerfile's ENTRYPOINT)
	private final String dockerEntrypoint;
	// supports shell and exec formats (same as Dockerfile's CMD)
	private final String dockerCmd;
	private final String dockerShmSize;
	@Builder.Default
	private final Map<String, String> env = new HashMap<>();
	@Builder.Default
	private final Set<String> dockerEnvFiles = new HashSet<>();
	private final Path jarPath;
	@Builder.Default
	private final Integer replicas = 1;

	public String getShortImageName() {
		String withoutHost = imageName.replaceFirst(".*/", "");
		return withoutHost.replaceFirst(":.*", "");
	}

}
