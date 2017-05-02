package com.github.swissquote.carnotzet.core;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.walk;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import com.github.swissquote.carnotzet.core.maven.MavenDependencyResolver;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * This is the main class of this library <br>
 * It represents an environment definition as a set of executable applications with their configuration.
 */
@Slf4j
public class Carnotzet {

	@Getter
	private final CarnotzetConfig config;

	@Getter
	private final String topLevelModuleName;

	@Getter
	private final Pattern moduleFilterPattern;

	private List<CarnotzetModule> modules;

	private final MavenDependencyResolver resolver;

	private final ResourcesManager resourceManager;

	public Carnotzet(CarnotzetConfig config) {
		log.debug("Creating new carnotzet with config [{}]", config);
		this.config = config;

		String filterPattern = "(.*)-carnotzet";
		if (config.getModuleFilterPattern() != null) {
			filterPattern = config.getModuleFilterPattern();
		}
		moduleFilterPattern = Pattern.compile(filterPattern);
		if (moduleFilterPattern.matcher("").groupCount() != 1) {
			throw new CarnotzetDefinitionException("moduleFilterPattern must have exactly 1 capture group");
		}

		this.topLevelModuleName = getModuleName(config.getTopLevelModuleId());

		Path resourcesPath = config.getResourcesPath();
		if (resourcesPath == null) {
			resourcesPath = Paths.get("/tmp/carnotzet_" + System.nanoTime());
		}
		resourcesPath = resourcesPath.resolve(topLevelModuleName);
		this.resourceManager = new ResourcesManager(resourcesPath, config.getTopLevelModuleResourcesPath());

		String defaultContainerRegistry = "docker.io";
		if (config.getDefaultDockerRegistry() != null) {
			defaultContainerRegistry = config.getDefaultDockerRegistry();
		}

		List<String> propFileNames = Arrays.asList("carnotzet.properties");
		if (config.getPropFileNames() != null) {
			propFileNames = config.getPropFileNames();
		}
		resolver = new MavenDependencyResolver(this::getModuleName, defaultContainerRegistry, propFileNames);

	}

	public List<CarnotzetModule> getModules() {
		if (modules == null) {
			log.debug("resolving module dependencies");
			modules = resolver.resolve(config.getTopLevelModuleId());
			log.debug("resolving module resources");
			resourceManager.resolveResources(modules, resolver::copyModuleResources);
			log.debug("Configuring individual file volumes");
			modules = configureFilesVolumes(modules);
			log.debug("Configuring env_file volumes");
			modules = configureEnvFilesVolumes(modules);

			if (config.getExtensions() != null) {
				for (CarnotzetExtension feature : config.getExtensions()) {
					log.debug("Extension [{}] enabled", feature.getClass().getSimpleName());
					modules = feature.apply(this);
				}
			}
		}
		return modules;
	}

	public Path getModuleResourcesPath(CarnotzetModule module) {
		return resourceManager.getModuleResourcesPath(module);
	}

	private List<CarnotzetModule> configureEnvFilesVolumes(List<CarnotzetModule> modules) {
		List<CarnotzetModule> result = new ArrayList<>();
		for (CarnotzetModule module : modules) {
			CarnotzetModule.CarnotzetModuleBuilder clone = module.toBuilder();
			clone.dockerEnvFiles(getEnvFiles(module));
			result.add(clone.build());
		}
		return result;
	}

	private Set<String> getEnvFiles(CarnotzetModule module) {
		Set<String> envFiles = new HashSet<>();
		for (CarnotzetModule otherModule : modules) {
			Path otherModuleEnvFolder = getModuleResourcesPath(otherModule).resolve(module.getName()).resolve("env");
			if (!exists(otherModuleEnvFolder)) {
				continue;
			}
			try {
				envFiles.addAll(walk(otherModuleEnvFolder).filter(p -> p.toFile().isFile()).map(Path::toString).collect(toList()));
			}
			catch (IOException e) {
				log.error(String.format("Error while reading env files for module: %s", module.getName()), e);
			}
		}
		return envFiles.isEmpty() ? null : envFiles;
	}

	private List<CarnotzetModule> configureFilesVolumes(List<CarnotzetModule> modules) {
		List<CarnotzetModule> result = new ArrayList<>();
		for (CarnotzetModule module : modules) {
			CarnotzetModule.CarnotzetModuleBuilder clone = module.toBuilder();
			clone.dockerVolumes(getFileVolumes(module));
			result.add(clone.build());
		}
		return result;
	}

	private Set<String> getFileVolumes(CarnotzetModule module) {
		Map<String, String> result = new HashMap<>();
		//look for files proposed by other modules that will need to be linked to the given module
		for (CarnotzetModule otherModule : getModules()) {
			Path toMount = getModuleResourcesPath(otherModule).resolve(module.getName()).resolve("files");
			if (!Files.exists(toMount)) {
				continue;
			}
			try {
				Files.walk(toMount).forEach((p) -> {
					if (p.toFile().isFile()) {
						result.put(p.toString(),
								new File(p.toString().substring(p.toString().indexOf("/files/") + "files/".length())).getAbsolutePath());
					}
				});
			}
			catch (IOException e) {
				log.error(String.format("Error while reading env files for module:%s", module.getName()), e);
			}
		}
		return result.isEmpty() ? Collections.emptySet() : result.entrySet().stream().map(
				entry -> String.format("%s:%s", entry.getKey(), entry.getValue()))
				.collect(Collectors.toSet());
	}

	public Path getResourcesFolder() {
		return resourceManager.getResourcesRoot();
	}

	public String getModuleName(MavenCoordinate module) {
		Matcher m = moduleFilterPattern.matcher(module.getArtifactId());
		if (m.find()) {
			return m.group(1);
		}
		return null;
	}

}
