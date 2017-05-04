package com.github.swissquote.carnotzet.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * restart all services for this carnotzet
 * if -Dservice=... is passed, ony the chose service will be restarted
 */
@Mojo(name = "restart", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class RestartMojo extends AbstractZetMojo {

	@Override
	public void executeInternal() throws MojoExecutionException, MojoFailureException {
		wrapWithLogFollowIfNeeded(command).run();
	}

	private Runnable command = () -> {
		if (getService() == null) {
			getRuntime().stop();
			getRuntime().start();
		} else {
			getRuntime().stop(getService());
			getRuntime().start(getService());
		}
	};

}
