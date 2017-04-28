package com.github.swissquote.carnotzet.maven.plugin.impl;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntime;
import com.github.swissquote.carnotzet.core.runtime.log.LogEvent;
import com.github.swissquote.carnotzet.core.runtime.log.StdOutLogPrinter;

public final class Logs {

	private Logs() {
		// static function holder
	}

	private static volatile long lastLogEventTime = System.currentTimeMillis();

	public static void execute(ContainerOrchestrationRuntime runtime, Carnotzet carnotzet, String service) {

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

		StdOutLogPrinter printer = new StdOutLogPrinter(Utils.getServiceNames(carnotzet), tail, follow) {
			@Override
			public void acceptInternal(LogEvent event) {
				super.acceptInternal(event);
				lastLogEventTime = System.currentTimeMillis();
			}
		};

		if (service != null) {
			printer.setEventFilter((event) -> service.equals(event.getService()));
		}

		runtime.registerLogListener(printer);
		if (follow) {
			Utils.waitForUserInterrupt();
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
			}
			while (System.nanoTime() - lastLogEventTime < 400);
		}
	}

}
