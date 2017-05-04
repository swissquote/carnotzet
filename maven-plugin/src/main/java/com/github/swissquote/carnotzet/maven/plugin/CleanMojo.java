package com.github.swissquote.carnotzet.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Deletes stopped containers
 */
@Mojo(name = "clean", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class CleanMojo extends AbstractZetMojo {

	@Override
	public void executeInternal() throws MojoExecutionException, MojoFailureException {
		getRuntime().clean();
	}

}
