package com.github.swissquote.carnotzet.maven.plugin;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.Settings;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;
import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.maven.CarnotzetModuleCoordinates;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntime;
import com.github.swissquote.carnotzet.core.runtime.log.LogListener;
import com.github.swissquote.carnotzet.core.runtime.log.StdOutLogPrinter;
import com.github.swissquote.carnotzet.runtime.docker.compose.DockerComposeRuntime;

import lombok.Getter;
import lombok.NonNull;

public abstract class AbstractZetMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true)
	protected MavenProject project;

	@Parameter(defaultValue = "${settings}", readonly = true)
	protected Settings settings;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession session;

	@Parameter(property = "runtime", defaultValue = "compose", readonly = true)
	private String runtime;

	@Parameter(property = "service")
	protected String service;

	@Parameter(property = "follow")
	protected boolean follow;

	@Parameter(property = "instanceId", readonly = true)
	private String k8sInstanceId;

	@Getter
	protected final Carnotzet carnotzet;

	private ContainerOrchestrationRuntime chosenRuntime;

	@Component
	private ProjectBuilder projectBuilder;

	public AbstractZetMojo() {
		MavenProject project = getCarnotzetProject();
		CarnotzetModuleCoordinates topLevelArtifact =
				new CarnotzetModuleCoordinates(project.getGroupId(), project.getArtifactId(), project.getVersion());
		Path resourcesRoot = Paths.get(project.getBuild().getDirectory(), "carnotzet");
		carnotzet = new Carnotzet(topLevelArtifact, resourcesRoot, Collections.emptyList(),
				project.getBasedir().toPath().resolve("src/main/resources"));

	}

	public ContainerOrchestrationRuntime getRuntime() {
		if (chosenRuntime != null) {
			return chosenRuntime;
		}

		switch (runtime.toLowerCase()) {
			case "compose":
				getLog().info("Using docker-compose runtime");
				chosenRuntime = new DockerComposeRuntime(carnotzet);
				break;
			default:
				throw new RuntimeException("Unknown container runtime [" + runtime + "]");
		}

		return chosenRuntime;
	}

	/**
	 * @return the currently built project if it's a carnotzet, or a child/sibling otherwise
	 */
	private MavenProject getCarnotzetProject() {
		if (!project.getArtifactId().endsWith("-carnotzet")) {
			getLog().info("Current project is not a valid carnotzet project. ArtifactId must end with '-carnotzet'");

			//try sibling
			File tryOther = new File(project.getBasedir().getParentFile(), "carnotzet");
			if (!tryOther.exists()) {
				//try child
				tryOther = new File(project.getBasedir(), "carnotzet");
			}

			if (tryOther.exists()) {
				getLog().info("Found a potential carnotzet in " + tryOther);
				try {
					session.getProjectBuildingRequest().setResolveDependencies(true);
					MavenProject newProject =
							projectBuilder.build(new File(tryOther, "pom.xml"), session.getProjectBuildingRequest()).getProject();
					getLog().info("New Project:" + newProject);
					return newProject;
				}
				catch (ProjectBuildingException e) {
					throw new CarnotzetDefinitionException(e);
				}
			}
		}
		return project;
	}

	protected List<String> getServiceNames() {
		return getCarnotzet().getModules().stream().map(CarnotzetModule::getName).sorted().collect(toList());
	}

	protected void waitForUserInterrupt() {
		try {
			Thread.sleep(Long.MAX_VALUE);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	protected Runnable wrapWithLogFollowIfNeeded(Runnable block) {
		if (follow){
			return () -> {
				LogListener printer = new StdOutLogPrinter(getServiceNames(), 0, true);
				getRuntime().registerLogListener(printer);
				block.run();
				waitForUserInterrupt();
			};
		}
		return block;
	}

}
