package com.github.swissquote.carnotzet.core.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;
import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.runtime.CommandRunner;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

@Slf4j
public class MavenDependencyResolver {

	public List<CarnotzetModule> resolve(MavenCoordinate topLevelModuleId) {
		List<CarnotzetModule> result = new ArrayList<>();

		String topLevelModuleName = getModuleName(topLevelModuleId);

		//We trust that shrinkwrap resolver returns the order we expect
		List<MavenResolvedArtifact> resolvedDependencies = Arrays.stream(Maven.configureResolver()//.workOffline()
				.resolve(topLevelModuleId.getGroupId() + ":" + topLevelModuleId.getArtifactId() + ":" + topLevelModuleId.getVersion())
				.withTransitivity().asResolvedArtifact()).filter((artifact) -> artifact.getCoordinate().getArtifactId().endsWith("-carnotzet"))
				.collect(Collectors.toList());

		log.debug("Resolved dependencies using shrinkwrap : " + resolvedDependencies);

		for (MavenResolvedArtifact artifact : resolvedDependencies) {
			String moduleName = getModuleName(artifact.getCoordinate());
			Map<String, String> artifactProperties = readProperties(artifact.getCoordinate());

			// Default convention
			String imageName = "docker.bank.swissquote.ch/" + moduleName + ":" + artifact.getCoordinate().getVersion();

			// Allow custom image through configuration
			if (artifactProperties.containsKey("docker.image")) {
				imageName = artifactProperties.get("docker.image");
			}

			// Allow configuration based disabling of docker container (config only module)
			if ("none".equals(imageName)) {
				imageName = null;
			}

			if (imageName != null) {
				imageName = useDataModuleIfPresent(imageName, artifactProperties, moduleName, topLevelModuleName);
				if (!doesImageExistInLocalDockerHost(imageName)) {
					if (!pullImage(imageName)) {
						throw new CarnotzetDefinitionException("Image " + imageName + " not found locally or in remote docker registry");
					}
				}
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

	private String useDataModuleIfPresent(String imageName, Map<String, String> artifactProperties, String moduleName,
			String topLevelModuleName) {

		if (artifactProperties == null || !"true".equals(artifactProperties.get("data"))) {
			return imageName;
		}

		DockerImageName name = new DockerImageName(imageName);
		if (name.getGroup() != null) {
			throw new CarnotzetDefinitionException("Cannot use a data-image with a base name that has a group");
		}

		name.setGroup(topLevelModuleName);
		name.setVersion(null);
		String dataModuleImageName = name.toString();

		if (!doesImageExistInLocalDockerHost(dataModuleImageName)) {
			if (!pullImage(dataModuleImageName)) {
				log.warn("Service [" + moduleName + "] has no data for ["
						+ topLevelModuleName
						+ "-carnotzet]. To use pre-provisioned data, push an image named " + dataModuleImageName + "");
			}
		}

		if (doesImageExistInLocalDockerHost(dataModuleImageName)) {
			return dataModuleImageName;
		}

		return imageName;
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
			try (InputStream is = classLoader.getResourceAsStream("carnotzet.properties")) {
				if (is != null) {
					Properties props = new Properties();
					props.load(is);
					return (Map) props;
				}
			}
			return new HashMap<>();

		}
		catch (IOException ex) {
			throw new UncheckedIOException("Exception when reading carnotzet.properties", ex);
		}
	}

	private boolean doesImageExistInLocalDockerHost(String imageName) {
		return 0 == CommandRunner.runCommand(false, "docker", "inspect", imageName);
	}

	private boolean pullImage(String imageName) {
		return 0 == CommandRunner.runCommand(false, "docker", "pull", imageName);
	}

	private ZipFile getJarFile(MavenCoordinate id) throws DependencyResolutionRequiredException, ZipException {
		File jarFile = Maven.configureResolver().workOffline()
				.resolve(id.getGroupId() + ":" + id.getArtifactId() + ":" + id.getVersion())
				.withoutTransitivity().asSingleFile();
		return new ZipFile(jarFile);
	}

	public void copyModuleResources(MavenCoordinate moduleId, Path moduleResourcesPath) {
		try {
			ZipFile f = this.getJarFile(moduleId);
			f.extractAll(moduleResourcesPath.toAbsolutePath().toString());
		}
		catch (DependencyResolutionRequiredException | ZipException e) {
			throw new CarnotzetDefinitionException(e);
		}

	}

	public String getModuleName(MavenCoordinate moduleId) {
		return moduleId.getArtifactId().replaceAll("-carnotzet", "");
	}

	@Data
	private static class DockerImageName {
		String registry;
		String group;
		String image;
		String version;

		DockerImageName(String s) {
			String[] parts = s.split("/");
			String imageAndVersion = "";

			if (parts.length > 3) {
				throw new CarnotzetDefinitionException("Docker image name is invalid");
			}

			if (parts.length == 3) {
				registry = parts[0];
				group = parts[1];
				imageAndVersion = parts[2];
			}

			if (parts.length == 2) {
				if (parts[0].contains(".")) {
					registry = parts[0];
				} else {
					group = parts[0];
				}
				imageAndVersion = parts[1];
			}

			if (parts.length == 1) {
				imageAndVersion = parts[0];
			}

			parts = imageAndVersion.split(":");

			if (parts.length > 2) {
				throw new CarnotzetDefinitionException("Docker image name is invalid");
			}

			if (parts.length == 2) {
				image = parts[0];
				version = parts[1];
			}

			if (parts.length == 1) {
				image = parts[0];
			}

		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (registry != null) {
				sb.append(registry).append("/");
			}
			if (group != null) {
				sb.append(group).append("/");
			}
			sb.append(image);
			if (version != null) {
				sb.append(":").append(version);
			}
			return sb.toString();
		}
	}

}

