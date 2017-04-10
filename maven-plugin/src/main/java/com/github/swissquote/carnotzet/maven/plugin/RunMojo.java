package com.github.swissquote.carnotzet.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.swissquote.carnotzet.core.runtime.CommandRunner;
import com.github.swissquote.carnotzet.core.runtime.log.LogListener;
import com.github.swissquote.carnotzet.core.runtime.log.StdOutLogPrinter;

@Mojo(name = "run", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class RunMojo extends AbstractZetMojo {

	@Override
	public void executeGoal() throws MojoExecutionException, MojoFailureException {
		if (service == null) {
			getRuntime().start();
		} else {
			getRuntime().start(service);
		}
		Runtime.getRuntime().addShutdownHook(stopOnShutdownHook);
		LogListener printer = new StdOutLogPrinter(getServiceNames(), null, true);
		if (service != null) {
			printer.setEventFilter(event -> event.getService().equals(service));
		}
		getRuntime().registerLogListener(printer);
		waitForUserInterrupt();
	}

	private final Thread stopOnShutdownHook = new Thread(() -> getRuntime().stop());

}
