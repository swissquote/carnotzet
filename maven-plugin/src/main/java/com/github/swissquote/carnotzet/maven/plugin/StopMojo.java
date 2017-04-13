package com.github.swissquote.carnotzet.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.swissquote.carnotzet.core.runtime.log.LogListener;
import com.github.swissquote.carnotzet.core.runtime.log.StdOutLogPrinter;

/**
 * this goal builds the image and starts a container with some volumes and ports bound.
 *
 * @author acraciun
 */
@Mojo(name = "stop", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class StopMojo extends AbstractZetMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (follow) {
			LogListener printer = new StdOutLogPrinter(getServiceNames(), 0, true);
			getRuntime().registerLogListener(printer);
		}

		if (service == null) {
			getRuntime().stop();
		} else {
			getRuntime().stop(service);
		}

	}

}
