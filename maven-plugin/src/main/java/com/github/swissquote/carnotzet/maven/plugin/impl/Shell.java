package com.github.swissquote.carnotzet.maven.plugin.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

import com.github.swissquote.carnotzet.core.runtime.api.Container;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntime;

public final class Shell {

	private Shell() {
		// static function holder
	}

	public static void execute(ContainerOrchestrationRuntime runtime, Prompter prompter, Log log, String service)
			throws MojoExecutionException, MojoFailureException {
		List<Container> containers = runtime.getContainers();
		if (containers.isEmpty()) {
			log.info("There doesn't seem to be any containers created yet for this carnotzet, please make sure the carnotzet is started");
			return;
		}
		Container container = containers.stream().filter(c -> c.getServiceName().equals(service)).findFirst().orElse(null);
		if (container == null) {
			container = promptForContainer(containers, prompter, log);
		}

		runtime.shell(container);
	}

	/**
	 * Lists services and prompts the user to choose one
	 */
	private static Container promptForContainer(List<Container> containers, Prompter prompter, Log log) throws MojoExecutionException {

		log.info("");
		log.info("SERVICE");
		log.info("");
		Map<Integer, Container> options = new HashMap<>();
		Integer i = 1;

		for (Container container : containers) {
			options.put(i, container);
			log.info(String.format("%2d", i) + " : " + container.getServiceName());
			i++;
		}
		log.info("");
		try {
			String prompt = prompter.prompt("Choose a service");
			return options.get(Integer.valueOf(prompt));
		}
		catch (PrompterException e) {
			throw new MojoExecutionException("Prompter error" + e.getMessage());
		}
	}

}
