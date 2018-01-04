package com.github.swissquote.carnotzet.core;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.github.swissquote.carnotzet.core.maven.CarnotzetModuleCoordinates;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class CarnotzetConfig {

	public final static String DEFAULT_MODULE_FILTER_PATTERN = "(.*)-carnotzet";
	public final static String DEFAULT_CLASSIFIER_INCLUDE_PATTERN = ".*carnotzet";
	public final static String DEFAULT_DOCKER_REGISTRY = "docker.io";
	public final static List<String> DEFAULT_PROP_FILE_NAMES = Collections.singletonList("carnotzet.properties");

	@NonNull
	private final CarnotzetModuleCoordinates topLevelModuleId;

	/**
	 * defaults to a temporary folder
	 */
	private final Path resourcesPath;

	private final List<CarnotzetExtension> extensions;

	private final Path topLevelModuleResourcesPath;

	/**
	 * Allows to use custom suffix/prefix for carnotzet modules artifactId.<br>
	 * and filter out some dependencies.<br>
	 * Must have exactly one capture group.<br>
	 * The first capture group will be the name of the module.<br>
	 * Dependencies which do not match the pattern will be ignored, unless they use a
	 * classifier that matches classifierIncludePattern.<br>
	 * defaults to DEFAULT_MODULE_FILTER_PATTERN
	 */
	private final String moduleFilterPattern;

	/**
	 * If non-null, makes it possible for any artifact that doesn't match the <br>
	 * moduleFilterPattern but matches the classifier include pattern to be be picked <br>
	 * up in a Carnotzet.<br>
	 * If a dependency matches the moduleFilterPattern, then classifierIncludePattern is ignored.<br>
	 * The name of the module will be the artifactId of the dependency.<br>
	 * defaults to DEFAULT_CLASSIFIER_INCLUDE_PATTERN
	 */
	private final String classifierIncludePattern;

	/**
	 * Registry used when inferring docker image name from artifact id (convention).<br>
	 * This is not used when the image name is defined in carnotzet.properties<br>
	 * Defaults to DEFAULT_DOCKER_REGISTRY
	 */
	private final String defaultDockerRegistry;

	/**
	 * Names of the properties files to read in the classpath of the modules.<br>
	 * Those properties file can be used for module level config such as docker.image.<br>
	 * They are also the standard way to configure extension features inside carnotzet modules.<br>
	 * defaults to DEFAULT_PROP_FILE_NAMES<br>
	 * if a property is present in two different files, the last one in the list wins.
	 */
	private final List<String> propFileNames;

	/**
	 * Indicates if the dependency resolution should throw and exception if a dependency cycle is detected.<br>
	 * defaults to true.<br>
	 * When false, some configuration overrides may be ignored.
	 */
	private final Boolean failOnDependencyCycle;

}
