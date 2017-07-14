package com.github.swissquote.carnotzet.core.maven;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

			CarnotzetModule module = CarnotzetModule.builder()
					.id(artifact.getCoordinate())
					.name(moduleName)
					.topLevelModuleName(topLevelModuleName)
					.build();

			result.add(0, module);
		}
		return result;
	}

}

