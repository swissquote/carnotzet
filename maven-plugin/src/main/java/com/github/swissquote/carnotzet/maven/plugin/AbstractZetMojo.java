package com.github.swissquote.carnotzet.maven.plugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;
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

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetConfig;
import com.github.swissquote.carnotzet.core.CarnotzetExtension;
import com.github.swissquote.carnotzet.core.maven.CarnotzetModuleCoordinates;
import com.github.swissquote.carnotzet.core.runtime.DefaultCommandRunner;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntime;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntimeExtension;
import com.github.swissquote.carnotzet.core.runtime.log.LogListener;
import com.github.swissquote.carnotzet.core.runtime.log.StdOutLogPrinter;
import com.github.swissquote.carnotzet.maven.plugin.impl.Utils;
import com.github.swissquote.carnotzet.maven.plugin.spi.CarnotzetExtensionsFactory;
import com.github.swissquote.carnotzet.maven.plugin.spi.ContainerOrchestrationRuntimeExtensionsFactory;
import com.github.swissquote.carnotzet.maven.plugin.spi.ExtensionFactory;
import com.github.swissquote.carnotzet.runtime.docker.compose.DockerComposeRuntime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;

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

	@Parameter(property = "instance")
	@Getter
	private String instanceId;

	@Parameter(property = "service")
	@Getter
	private String service;

	@Parameter(property = "follow")
	@Getter
	private boolean follow;

	@Parameter(property = "failOnDependencyCycle")
	@Getter
	private Boolean failOnDependencyCycle;

	@Parameter(property = "attachToCarnotzetNetwork")
	@Getter
	private Boolean attachToCarnotzetNetwork;

	@Parameter(property = "supportLegacyDnsNames")
	@Getter
	private Boolean supportLegacyDnsNames;

	@Parameter(property = "bindLocalPorts")
	@Getter
	private Boolean bindLocalPorts;

	@Getter
	@Setter
	private Carnotzet carnotzet;

	/**
	 * The list of configuration objects for Carnotzet Maven extensions
	 */
	@Parameter(property = "extensions", readonly = true)
	@Getter
	private List<ExtensionConfiguration> extensions;

	/**
	 * The list of configuration objects for container runtime extensions
	 */
	@Parameter(property = "runtimeExtensions", readonly = true)
	@Getter
	private List<ExtensionConfiguration> runtimeExtensions;

	@Getter
	@Setter
	private ContainerOrchestrationRuntime runtime;

	@Component
	private ProjectBuilder projectBuilder;

	@Override
	public void execute() throws MojoFailureException, MojoExecutionException {
		SLF4JBridgeHandler.install();

		CarnotzetModuleCoordinates coordinates =
				new CarnotzetModuleCoordinates(project.getGroupId(), project.getArtifactId(), project.getVersion());

		if (instanceId == null) {
			instanceId = Carnotzet.getModuleName(coordinates, Pattern.compile(CarnotzetConfig.DEFAULT_MODULE_FILTER_PATTERN),
					Pattern.compile(CarnotzetConfig.DEFAULT_CLASSIFIER_INCLUDE_PATTERN));
		}

		Path resourcesPath = Paths.get(project.getBuild().getDirectory(), "carnotzet");
		if (SystemUtils.IS_OS_WINDOWS) {
			// we avoid using ${project.build.directory} because "mvn clean" when the sandbox is running would try to delete mounted files,
			// which is not supported on Windows.
			resourcesPath = Paths.get("/var/tmp/carnotzet_" + instanceId);
		}

		List<CarnotzetExtension> extensions = findDynamicExtensions(this.extensions, CarnotzetExtensionsFactory.class);

		List<ContainerOrchestrationRuntimeExtension> runtimeExtensions =
				findDynamicExtensions(this.runtimeExtensions, ContainerOrchestrationRuntimeExtensionsFactory.class);

		CarnotzetConfig config = CarnotzetConfig.builder()
				.topLevelModuleId(coordinates)
				.resourcesPath(resourcesPath)
				.topLevelModuleResourcesPath(project.getBasedir().toPath().resolve("src/main/resources"))
				.failOnDependencyCycle(failOnDependencyCycle)
				.attachToCarnotzetNetwork(attachToCarnotzetNetwork)
				.supportLegacyDnsNames(supportLegacyDnsNames)
				.extensions(extensions)
				.build();

		carnotzet = new Carnotzet(config);
		if (bindLocalPorts == null) {
			bindLocalPorts = !SystemUtils.IS_OS_LINUX;
		}

		runtime = new DockerComposeRuntime(carnotzet, instanceId, DefaultCommandRunner.INSTANCE, bindLocalPorts, runtimeExtensions);

		executeInternal();

		SLF4JBridgeHandler.uninstall();
	}

	protected <F extends ExtensionFactory<E>, E> List<E> findDynamicExtensions(List<ExtensionConfiguration> extensionsConfigurations,
			Class<F> factoryClass) {
		List<F> factories = new ArrayList<>(0);
		ServiceLoader.load(factoryClass).iterator().forEachRemaining(factories::add);

		return factories.stream()
				.map(factory -> factory.create(findExtensionFactoryProperties(extensionsConfigurations, factory)))
				.collect(Collectors.toList());
	}

	private <F extends ExtensionFactory<?>> Properties findExtensionFactoryProperties(List<ExtensionConfiguration> extensionConfigurations,
			F factory) {
		return getExtensionFactoryConfig(extensionConfigurations, factory).map(ExtensionConfiguration::getProperties).orElseGet(() -> {
			getLog().info("No properties found for " + factory.getClass().getName());
			return new Properties();
		});
	}

	private <F extends ExtensionFactory<?>> Optional<ExtensionConfiguration> getExtensionFactoryConfig(
			List<ExtensionConfiguration> extensionConfigurations, F factory) {
		return extensionConfigurations.stream().filter(extConfig -> extConfig.isFor(factory.getClass())).findFirst();
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
