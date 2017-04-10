package com.github.swissquote.carnotzet.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.swissquote.carnotzet.core.runtime.log.LogListener;
import com.github.swissquote.carnotzet.core.runtime.log.StdOutLogPrinter;

/**
 * Start a carnotzet (in background)
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class StartMojo extends AbstractZetMojo {

	@Override
	public void executeGoal() throws MojoExecutionException, MojoFailureException {
		wrapWithLogFollowIfNeeded(command).run();
	}

	private Runnable command = () -> {
		if (service == null) {
			getRuntime().start();
		} else {
			getRuntime().start(service);
		}
	};
}
