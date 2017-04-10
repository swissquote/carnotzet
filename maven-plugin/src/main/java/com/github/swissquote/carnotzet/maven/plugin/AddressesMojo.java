package com.github.swissquote.carnotzet.maven.plugin;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.swissquote.carnotzet.core.runtime.api.Container;

/**
 * Lists the IP addresses of running containers
 */
@Mojo(name = "addrs", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class AddressesMojo extends AbstractZetMojo {

	@Override
	public void executeGoal() throws MojoExecutionException, MojoFailureException {

		List<Container> containers = getRuntime().getContainers();
		if (containers.isEmpty()) {
			getLog().info("There doesn't seem to be any containers created yet for this carnotzet, please make sure the carnotzet is started");
			return;
		}

		getLog().info("");
		getLog().info(String.format("%-25s", "APPLICATION") + "   IP ADDRESS");
		getLog().info("");
		for (Container container : containers) {
			getLog().info(String.format("%-25s", container.getServiceName()) + " : "
					+ (container.getIp() == null ? "No address, is container started ?"
					: container.getIp() + " (" + container.getServiceName() + ".docker)"));
		}
		getLog().info("");
	}

}
