package com.github.swissquote.carnotzet.maven.plugin;

import java.io.File;
import java.nio.file.Paths;

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
import com.github.swissquote.carnotzet.core.CarnotzetConfig;
import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;
import com.github.swissquote.carnotzet.core.maven.CarnotzetModuleCoordinates;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntime;
import com.github.swissquote.carnotzet.core.runtime.log.LogListener;
import com.github.swissquote.carnotzet.core.runtime.log.StdOutLogPrinter;
import com.github.swissquote.carnotzet.maven.plugin.impl.Utils;
import com.github.swissquote.carnotzet.runtime.docker.compose.DockerComposeRuntime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;

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

	@Parameter(property = "instanceId", readonly = true)
	@Getter
	private String instanceId;

	@Parameter(property = "service", readonly = true)
	@Getter
	private String service;

	@Parameter(property = "follow")
	@Getter
	private boolean follow;

	@Getter
	private Carnotzet carnotzet;

	@Getter
	private ContainerOrchestrationRuntime runtime;

	@Component
	private ProjectBuilder projectBuilder;

	@Override
	public final void execute() throws MojoFailureException, MojoExecutionException {
		MavenProject project = getCarnotzetProject();
		CarnotzetConfig config = CarnotzetConfig.builder()
				.topLevelModuleId(new CarnotzetModuleCoordinates(project.getGroupId(), project.getArtifactId(), project.getVersion()))
				.resourcesPath(Paths.get(project.getBuild().getDirectory(), "carnotzet"))
				.topLevelModuleResourcesPath(project.getBasedir().toPath().resolve("src/main/resources"))
				.build();
		carnotzet = new Carnotzet(config);
		runtime = new DockerComposeRuntime(carnotzet);
		executeInternal();
	}

	public abstract void executeInternal() throws MojoExecutionException, MojoFailureException;

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

	public Runnable wrapWithLogFollowIfNeeded(Runnable block) {
		if (follow) {
			return () -> {
				LogListener printer = new StdOutLogPrinter(Utils.getServiceNames(carnotzet), 0, true);
				runtime.registerLogListener(printer);
				block.run();
				Utils.waitForUserInterrupt();
			};
		}
		return block;
	}

}
