package com.github.swissquote.carnotzet.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.swissquote.carnotzet.core.runtime.log.LogListener;
import com.github.swissquote.carnotzet.core.runtime.log.StdOutLogPrinter;

@Mojo(name = "run", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class RunMojo extends AbstractZetMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (getService() == null) {
			getRuntime().start();
		} else {
			getRuntime().start(getService());
		}
		Runtime.getRuntime().addShutdownHook(stopOnShutdownHook);
		LogListener printer = new StdOutLogPrinter(getServiceNames(), null, true);
		if (getService() != null) {
			printer.setEventFilter(event -> event.getService().equals(getService()));
		}
		getRuntime().registerLogListener(printer);
		waitForUserInterrupt();
	}

	private final Thread stopOnShutdownHook = new Thread(() -> getRuntime().stop());

}
