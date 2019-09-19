package com.github.swissquote.carnotzet.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.swissquote.carnotzet.core.runtime.log.LogListener;
import com.github.swissquote.carnotzet.core.runtime.log.StdOutLogPrinter;
import com.github.swissquote.carnotzet.maven.plugin.impl.Utils;

/**
 * Stop all containers<br>
 * if -Dservice=... is passed, ony the chosen service will be stopped
 * a comma-separated list of regexp is also supported
 */
@Mojo(name = "stop", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class StopMojo extends AbstractZetMojo {

	@Override
	public void executeInternal() throws MojoExecutionException, MojoFailureException {

		if (isFollow()) {
			LogListener printer = new StdOutLogPrinter(Utils.getServiceNames(getCarnotzet()), 0, true);
			getRuntime().registerLogListener(printer);
		}

		if (getService() == null) {
			getRuntime().stop();
		} else {
			getRuntime().stop(getService());
		}

	}

}
