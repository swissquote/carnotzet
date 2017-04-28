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

}
