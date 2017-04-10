package com.github.swissquote.carnotzet.maven.plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

import com.github.swissquote.carnotzet.core.runtime.api.Container;

/**
 * Starts a shell in a running container and binds it's IO to the current process
 */
@Mojo(name = "shell", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ShellMojo extends AbstractZetMojo {

	@Component
	private Prompter prompter;

	@Override
	public void executeGoal() throws MojoExecutionException, MojoFailureException {
		List<Container> containers = getRuntime().getContainers();
		if (containers.isEmpty()) {
			getLog().info("There doesn't seem to be any containers created yet for this carnotzet, please make sure the carnotzet is started");
			return;
		}
		Container container = containers.stream().filter(c -> c.getServiceName().equals(service)).findFirst().orElse(null);
		if (container == null) {
			container = promptForContainer(containers);
		}

		getRuntime().shell(container);
	}

	/**
	 * Lists services and prompts the user to choose one
	 */
	private Container promptForContainer(List<Container> containers) throws MojoExecutionException {

		getLog().info("");
		getLog().info("SERVICE");
		getLog().info("");
		Map<Integer, Container> options = new HashMap<>();
		Integer i = 1;

		for (Container container : containers) {
			options.put(i, container);
			getLog().info(String.format("%2d", i) + " : " + container.getServiceName());
			i++;
		}
		getLog().info("");
		try {
			String prompt = prompter.prompt("Choose a service");
			return options.get(Integer.valueOf(prompt));
		}
		catch (PrompterException e) {
			throw new MojoExecutionException("Prompter error" + e.getMessage());
		}
	}

}
