package com.github.swissquote.carnotzet.core;

import java.nio.file.Path;
import java.util.List;

import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class CarnotzetConfig {

	@NonNull
	private final MavenCoordinate topLevelModuleId;

	/** defaults to a temporary folder */
	private final Path resourcesPath;

	private final List<CarnotzetExtension> extensions;

	private final Path topLevelModuleResourcesPath;

	/**
	 * Allows to use custom suffix/prefix for carnotzet modules artifactId.<br>
	 * and filter out some dependencies.<br>
	 * Must have exactly one capture group.<br>
	 * The first capture group will be the name of the module.<br>
	 * Dependencies which do not match the pattern will be ignored.<br>
	 * defaults to (.*)-cartnozet  */
	private final String moduleFilterPattern;

	/**
	 * Registry used when inferring docker image name from artifact id (convention).<br>
	 * This is not used when the image name is defined in carnotzet.properties<br>
	 * Defaults to docker.io
	 */
	private final String defaultDockerRegistry;

}
