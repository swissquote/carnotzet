package com.github.swissquote.carnotzet.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Start a carnotzet (in background)<br>
 * if -Dservice=... is passed, ony the chosen service will be started
 * a comma-separated list of regexp is also supported
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class StartMojo extends AbstractZetMojo {

	@Override
	public void executeInternal() throws MojoExecutionException, MojoFailureException {
		wrapWithLogFollowIfNeeded(command).run();
	}

	private Runnable command = () -> {
		if (getService() == null) {
			getRuntime().start();
		} else {
			getRuntime().start(getService());
		}
	};
}
