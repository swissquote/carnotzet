package com.github.swissquote.carnotzet.maven.plugin.impl;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntime;
import com.github.swissquote.carnotzet.core.runtime.log.LogListener;
import com.github.swissquote.carnotzet.core.runtime.log.StdOutLogPrinter;

public final class Run {

	private Run() {
		// static function holder
	}

	public static void execute(ContainerOrchestrationRuntime runtime, Carnotzet carnotzet, String service) throws MojoExecutionException,
			MojoFailureException {
		if (service == null) {
			runtime.start();
		} else {
			runtime.start(service);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (service == null) {
				if (runtime.isRunning()) {
					runtime.stop();
				}
			} else {
				if (runtime.getContainer(service).isRunning()) {
					runtime.stop(service);
				}
			}
		}));
		LogListener printer = new StdOutLogPrinter(Utils.getServiceNames(carnotzet), null, true);
		if (service != null) {
			printer.setEventFilter(event -> event.getService().equals(service));
		}
		runtime.registerLogListener(printer);

		Utils.waitForUserInterrupt();
	}

}
