package com.github.swissquote.carnotzet.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Pulls all images in the carnotzet from the docker image registry
 */
@Mojo(name = "pull", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class PullMojo extends AbstractZetMojo {
	@Override
	public void executeInternal() throws MojoExecutionException, MojoFailureException {
		getRuntime().pull();
	}
}
