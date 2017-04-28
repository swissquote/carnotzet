package com.github.swissquote.carnotzet.maven.plugin.impl;

import java.util.List;

import org.apache.maven.plugin.logging.Log;

import com.github.swissquote.carnotzet.core.runtime.api.Container;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntime;

public final class Addresses {

	private Addresses() {
		// static function holder
	}

	public static void execute(ContainerOrchestrationRuntime runtime, Log log) {
		List<Container> containers = runtime.getContainers();
		if (containers.isEmpty()) {
			log.info("There doesn't seem to be any containers created yet for this carnotzet, please make sure the carnotzet is started");
			return;
		}

		log.info("");
		log.info(String.format("%-25s", "APPLICATION") + "   IP ADDRESS");
		log.info("");
		for (Container container : containers) {
			log.info(String.format("%-25s", container.getServiceName()) + " : "
					+ (container.getIp() == null ? "No address, is container started ?"
					: container.getIp() + " (" + container.getServiceName() + ".docker)"));
		}
		log.info("");
	}

}
