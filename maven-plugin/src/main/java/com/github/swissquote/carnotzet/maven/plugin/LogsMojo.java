package com.github.swissquote.carnotzet.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.swissquote.carnotzet.core.runtime.log.LogEvent;
import com.github.swissquote.carnotzet.core.runtime.log.StdOutLogPrinter;

/**
 * Output logs, by default, logs are followed with a tail of 200<br>
 * You can override this behavior using "follow" and "tail" system properties<br>
 * example to get the last 5 log events from each service and return : mvn zet:logs -Dfollow=false -Dtail=5
 */
@Mojo(name = "logs", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class LogsMojo extends AbstractZetMojo {

	private volatile long lastLogEventTime = System.currentTimeMillis();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// defaults
		boolean follow = true;
		Integer tail = 200;

		String followStr = System.getProperty("follow");
		if (followStr != null) {
			follow = Boolean.valueOf(followStr);
		}
		String tailStr = System.getProperty("tail");
		if (tailStr != null) {
			tail = Integer.valueOf(tailStr);
		}

		StdOutLogPrinter printer = new StdOutLogPrinter(getServiceNames(), tail, follow) {
			@Override
			public void acceptInternal(LogEvent event) {
				super.acceptInternal(event);
				lastLogEventTime = System.currentTimeMillis();
			}
		};

		if (getService() != null) {
			printer.setEventFilter((event) -> getService().equals(event.getService()));
		}

		getRuntime().registerLogListener(printer);
		if (follow) {
			waitForUserInterrupt();
		} else {
			// small hack to avoid complex synchronization: only exit main thread
			// if there were no log events printed in the last 400ms
			do {
				try {
					Thread.sleep(200);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			} while (System.nanoTime() - lastLogEventTime < 400);
		}
	}

}
