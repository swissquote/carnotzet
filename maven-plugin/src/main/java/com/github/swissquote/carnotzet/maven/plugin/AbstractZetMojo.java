package com.github.swissquote.carnotzet.maven.plugin;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetConfig;
import com.github.swissquote.carnotzet.core.CarnotzetExtension;
import com.github.swissquote.carnotzet.core.CarnotzetExtensionsFactory;
import com.github.swissquote.carnotzet.core.maven.CarnotzetModuleCoordinates;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntime;
import com.github.swissquote.carnotzet.core.runtime.log.LogListener;
import com.github.swissquote.carnotzet.core.runtime.log.StdOutLogPrinter;
import com.github.swissquote.carnotzet.maven.plugin.impl.Utils;
import com.github.swissquote.carnotzet.runtime.docker.compose.DockerComposeRuntime;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.settings.Settings;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.nio.file.Paths;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Maven fails to inject params when using a constructor")
public abstract class AbstractZetMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	@Getter
	private MavenProject project;

	@Parameter(defaultValue = "${settings}", readonly = true)
	@Getter
	private Settings settings;

	@Parameter(defaultValue = "${session}", readonly = true)
	@Getter
	private MavenSession session;

	@Parameter(property = "instance", readonly = true)
	@Getter
	private String instanceId;

	@Parameter(property = "service", readonly = true)
	@Getter
	private String service;

	@Parameter(property = "follow")
	@Getter
	private boolean follow;

	@Getter
	@Setter
	private Carnotzet carnotzet;

	@Parameter(property = "extensions", readonly = true)
	@Getter
	private List<ExtensionConfiguration> extensions;

	@Getter
	@Setter
	private ContainerOrchestrationRuntime runtime;

	@Component
	private ProjectBuilder projectBuilder;

	@Override
	public void execute() throws MojoFailureException, MojoExecutionException {
		SLF4JBridgeHandler.install();

		List<CarnotzetExtensionsFactory> factories = new ArrayList<>(0);
		ServiceLoader.load(CarnotzetExtensionsFactory.class).iterator().forEachRemaining(factories::add);

		CarnotzetConfig config = CarnotzetConfig.builder()
				.topLevelModuleId(new CarnotzetModuleCoordinates(project.getGroupId(), project.getArtifactId(), project.getVersion()))
				.resourcesPath(Paths.get(project.getBuild().getDirectory(), "carnotzet"))
				.topLevelModuleResourcesPath(project.getBasedir().toPath().resolve("src/main/resources"))
				.extensions(findRuntimeExtensions(factories))
				.build();
		carnotzet = new Carnotzet(config);
		runtime = new DockerComposeRuntime(carnotzet, instanceId);

		executeInternal();

		SLF4JBridgeHandler.uninstall();
	}

	private List<CarnotzetExtension> findRuntimeExtensions(List<CarnotzetExtensionsFactory> factories) {
		return factories.stream()
				.map(factory -> factory.create(findExtensionFactoryProperties(factory)))
				.collect(Collectors.toList());
	}

	private Properties findExtensionFactoryProperties(CarnotzetExtensionsFactory factory) {
		return getExtensionFactoryConfig(factory).map(ExtensionConfiguration::getProperties).orElseGet(() -> {
			getLog().info("No properties found for " + factory.getClass().getName());
			return new Properties();
		});
	}

	private Optional<ExtensionConfiguration> getExtensionFactoryConfig(CarnotzetExtensionsFactory factory) {
		return extensions.stream().filter(extConfig -> extConfig.isFor(factory.getClass())).findFirst();
	}

	public abstract void executeInternal() throws MojoExecutionException, MojoFailureException;

	public Runnable wrapWithLogFollowIfNeeded(Runnable block) {
		if (follow) {
			return () -> {
				LogListener printer = new StdOutLogPrinter(Utils.getServiceNames(getCarnotzet()), 0, true);
				getRuntime().registerLogListener(printer);
				block.run();
				Utils.waitForUserInterrupt();
			};
		}
		return block;
	}

}
