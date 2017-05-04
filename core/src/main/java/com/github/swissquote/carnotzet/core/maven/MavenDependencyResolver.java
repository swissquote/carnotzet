package com.github.swissquote.carnotzet.core.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import com.github.swissquote.carnotzet.core.CarnotzetModule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MavenDependencyResolver {

	private final Function<MavenCoordinate, String> moduleNameProvider;
	private final String defaultDockerRegistry;
	private final List<String> propFileNames;

	public List<CarnotzetModule> resolve(MavenCoordinate topLevelModuleId) {
		List<CarnotzetModule> result = new ArrayList<>();

		String topLevelModuleName = moduleNameProvider.apply(topLevelModuleId);

		//We trust that shrinkwrap resolver returns the order we expect
		List<MavenResolvedArtifact> resolvedDependencies = Arrays.stream(Maven.configureResolver()//.workOffline()
				.resolve(topLevelModuleId.getGroupId() + ":" + topLevelModuleId.getArtifactId() + ":" + topLevelModuleId.getVersion())
				.withTransitivity().asResolvedArtifact()).filter((artifact) -> moduleNameProvider.apply(artifact.getCoordinate()) != null)
				.collect(Collectors.toList());

		log.debug("Resolved dependencies using shrinkwrap : " + resolvedDependencies);

		for (MavenResolvedArtifact artifact : resolvedDependencies) {
			String moduleName = moduleNameProvider.apply(artifact.getCoordinate());
			Map<String, String> artifactProperties = readProperties(artifact.getCoordinate());

			// Default convention
			String imageName = defaultDockerRegistry + "/" + moduleName + ":" + artifact.getCoordinate().getVersion();

			// Allow custom image through configuration
			if (artifactProperties.containsKey("docker.image")) {
				imageName = artifactProperties.get("docker.image");
			}

			// Allow configuration based disabling of docker container (config only module)
			if ("none".equals(imageName)) {
				imageName = null;
			}

			CarnotzetModule module = CarnotzetModule.builder()
					.id(artifact.getCoordinate())
					.name(moduleName)
					.imageName(imageName)
					.topLevelModuleName(topLevelModuleName)
					.properties(artifactProperties)
					.build();

			result.add(0, module);
		}
		return result;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private Map<String, String> readProperties(MavenCoordinate coordinate) {
		File[] files = Maven.configureResolver().workOffline().resolve(coordinate.toCanonicalForm()).withoutTransitivity().asFile();
		try {
			URL[] urls = new URL[files.length];
			for (int i = 0; i < files.length; i++) {
				urls[i] = files[i].toURI().toURL();
			}
			@SuppressWarnings("resource")
			ClassLoader classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader().getParent());
			HashMap<String, String> result = new HashMap<>();
			for (String propFileName : propFileNames) {
				try (InputStream is = classLoader.getResourceAsStream(propFileName)) {
					if (is != null) {
						Properties props = new Properties();
						props.load(is);
						result.putAll((Map) props);
					}
				}
			}
			return result;

		}
		catch (IOException ex) {
			throw new UncheckedIOException("Exception when reading module properties file", ex);
		}
	}

}

