package com.github.swissquote.carnotzet.core;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.walk;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;

import com.github.swissquote.carnotzet.core.maven.CarnotzetModuleCoordinates;
import com.github.swissquote.carnotzet.core.maven.MavenDependencyResolver;
import com.github.swissquote.carnotzet.core.maven.ResourcesManager;
import com.google.common.base.Strings;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents an environment definition as a set of executable applications with their configuration.
 */
@Slf4j
public class Carnotzet {

	@Getter
	private final CarnotzetConfig config;

	@Getter
	private final String topLevelModuleName;

	@Getter
	private final Pattern moduleFilterPattern;

	@Getter
	private final Pattern classifierIncludePattern;

	private List<CarnotzetModule> modules;

	private final MavenDependencyResolver resolver;

	private final ResourcesManager resourceManager;

	private final String defaultContainerRegistry;

	private final List<String> propFileNames;

	private final Boolean failOnDependencyCycle;

	public Carnotzet(CarnotzetConfig config) {
		log.debug("Creating new carnotzet with config [{}]", config);
		this.config = config;

		String filterPattern = CarnotzetConfig.DEFAULT_MODULE_FILTER_PATTERN;
		if (config.getModuleFilterPattern() != null) {
			filterPattern = config.getModuleFilterPattern();
		}
		moduleFilterPattern = Pattern.compile(filterPattern);
		if (moduleFilterPattern.matcher("").groupCount() != 1) {
			throw new CarnotzetDefinitionException("moduleFilterPattern must have exactly 1 capture group");
		}

		if (config.getClassifierIncludePattern() == null) {
			this.classifierIncludePattern = Pattern.compile(CarnotzetConfig.DEFAULT_CLASSIFIER_INCLUDE_PATTERN);
		} else {
			this.classifierIncludePattern = Pattern.compile(config.getClassifierIncludePattern());
		}

		this.topLevelModuleName = getModuleName(config.getTopLevelModuleId());

		Path resourcesPath = config.getResourcesPath();
		if (resourcesPath == null) {
			resourcesPath = Paths.get("/tmp/carnotzet_" + System.nanoTime());
		}
		this.resourceManager = new ResourcesManager(resourcesPath, config.getTopLevelModuleResourcesPath());

		if (config.getDefaultDockerRegistry() != null) {
			this.defaultContainerRegistry = config.getDefaultDockerRegistry();
		} else {
			this.defaultContainerRegistry = CarnotzetConfig.DEFAULT_DOCKER_REGISTRY;
		}

		if (config.getPropFileNames() != null) {
			this.propFileNames = config.getPropFileNames();
		} else {
			this.propFileNames = CarnotzetConfig.DEFAULT_PROP_FILE_NAMES;
		}

		if (config.getFailOnDependencyCycle() != null) {
			this.failOnDependencyCycle = config.getFailOnDependencyCycle();
		} else {
			this.failOnDependencyCycle = true;
		}

		resolver = new MavenDependencyResolver(this::getModuleName, resourcesPath.resolve("maven"));

	}

	public List<CarnotzetModule> getModules() {
		if (modules == null) {
			modules = resolver.resolve(config.getTopLevelModuleId(), failOnDependencyCycle);
			if (SystemUtils.IS_OS_LINUX || !getResourcesFolder().resolve("expanded-jars").toFile().exists()) {
				resourceManager.extractResources(modules);
				resourceManager.resolveResources(modules);
			}
			log.debug("configuring modules");
			modules = configureModules(modules);

			if (config.getExtensions() != null) {
				for (CarnotzetExtension feature : config.getExtensions()) {
					log.debug("Extension [{}] enabled", feature.getClass().getSimpleName());
					modules = feature.apply(this);
				}
			}
			assertNoDuplicateArtifactId(modules);
		}
		return modules;
	}

	private void assertNoDuplicateArtifactId(List<CarnotzetModule> modules) {
		Map<String, CarnotzetModule> seen = new HashMap<>();
		modules.forEach(m -> {
			String artifactId = m.getId().getArtifactId();
			if (seen.containsKey(artifactId)) {
				throw new CarnotzetDefinitionException("Duplicate artifact ID [" + artifactId + "] with groupIds "
						+ "[" + m.getId().getGroupId() + "] and [" + seen.get(artifactId).getId().getGroupId() + "]");
			}
			seen.put(artifactId, m);
		});
	}

	public Optional<CarnotzetModule> getModule(@NonNull String moduleName) {
		return getModules().stream().filter(module -> moduleName.equals(module.getName())).findFirst();
	}

	private List<CarnotzetModule> configureModules(List<CarnotzetModule> modules) {
		return modules.stream().map(this::configureModule).collect(toList());
	}

	private CarnotzetModule configureModule(CarnotzetModule module) {
		CarnotzetModule.CarnotzetModuleBuilder result = module.toBuilder();
		Map<String, String> properties = readPropertiesFiles(module);
		result.properties(properties);

		// Default convention
		String imageName = defaultContainerRegistry + "/" + module.getName() + ":" + module.getId().getVersion();

		// Allow custom image through configuration
		if (properties.containsKey("docker.image")) {
			imageName = properties.get("docker.image");
			// imageName might contain <module>.version which requires substitution
			Pattern pattern = Pattern.compile("(\\$\\{(\\S+)\\.version\\})");
			Matcher matcher = pattern.matcher(imageName);
			if (matcher.find()) {
				// we have identified myModule
				String myModule = matcher.group(2);
				// let's try to find myModule in modules and get its version
				String myModuleVersion = modules.stream()
						.filter(m -> m.getName().equals(myModule))
						.map(m -> m.getId().getVersion())
						.findFirst()
						.orElse("");
				if (Strings.isNullOrEmpty(myModuleVersion)) {
					// complain nicely with a list of modules
					String modulesList = modules.stream()
							.map(m -> m.getName())
							.collect(Collectors.joining(", "));
					throw new CarnotzetDefinitionException("Module " + myModule + " wasn't found in modules: " + modulesList);
				}
				imageName = matcher.replaceFirst(myModuleVersion);
			}
		}

		// Allow configuration based disabling of docker container (config only module)
		if ("none".equals(imageName)) {
			imageName = null;
		}
		result.imageName(imageName);
		if (properties.containsKey("docker.entrypoint")) {
			result.dockerEntrypoint(properties.get("docker.entrypoint"));
		}
		if (properties.containsKey("docker.cmd")) {
			result.dockerCmd(properties.get("docker.cmd"));
		}
		result.dockerVolumes(getFileVolumes(module));
		result.dockerEnvFiles(getEnvFiles(module));

		return result.build();
	}

	private Map<String, String> readPropertiesFiles(CarnotzetModule module) {
		Map<String, String> result = new HashMap<>();
		for (String fileName : propFileNames) {
			Path filePath = getModuleResourcesPath(module).resolve(fileName);
			if (filePath.toFile().exists()) {
				Properties props = new Properties();
				try (InputStream in = Files.newInputStream(filePath)) {
					props.load(in);
					result.putAll((Map) props);
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}
		return result;
	}

	public Path getModuleResourcesPath(CarnotzetModule module) {
		return resourceManager.getModuleResourcesPath(module);
	}

	private Set<String> getEnvFiles(CarnotzetModule module) {
		Set<String> envFiles = new HashSet<>();
		Path envFilesRoot = getModuleResourcesPath(module).resolve("env");
		if (!exists(envFilesRoot)) {
			return Collections.emptySet();
		}
		try {
			envFiles.addAll(walk(envFilesRoot).filter(p -> p.toFile().isFile()).map(Path::toString).collect(toList()));
		}
		catch (IOException e) {
			log.error(String.format("Error while reading env files for module: %s", module.getName()), e);
		}

		return envFiles.isEmpty() ? null : envFiles;
	}

	private Set<String> getFileVolumes(CarnotzetModule module) {
		Map<String, String> result = new HashMap<>();
		Path toMount = getModuleResourcesPath(module).resolve("files");
		if (!Files.exists(toMount)) {
			return Collections.emptySet();
		}
		try {
			Files.walk(toMount).forEach((p) -> {
				if (p.toFile().isFile()) {
					result.put(p.toString(), "/" + toMount.relativize(p).toString().replaceAll("\\\\", "/"));
				}
			});
		}
		catch (IOException e) {
			log.error(String.format("Error while reading files to mount for module:%s", module.getName()), e);
		}

		return result.isEmpty() ? Collections.emptySet() : result.entrySet().stream().map(
				entry -> String.format("%s:%s", entry.getKey(), entry.getValue()))
				.collect(Collectors.toSet());
	}

	public Path getResourcesFolder() {
		return resourceManager.getResourcesRoot();
	}

	public String getModuleName(CarnotzetModuleCoordinates module) {
		return getModuleName(module, moduleFilterPattern, classifierIncludePattern);
	}

	public static String getModuleName(CarnotzetModuleCoordinates module, Pattern moduleFilterPattern, Pattern classifierIncludePattern) {
		Matcher m = moduleFilterPattern.matcher(module.getArtifactId());
		if (m.find()) {
			return m.group(1);
		}

		// artifactId doesn't match. Can we try with classifier instead?
		if (module.getClassifier() == null || classifierIncludePattern == null) {
			return null;
		}

		// classifier exists and include pattern is specified, let's try
		m = classifierIncludePattern.matcher(module.getClassifier());
		if (m.matches()) {
			return module.getArtifactId();
		}

		// nothing matches. Nothing to do
		return null;
	}

}
