package com.github.swissquote.carnotzet.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.swissquote.carnotzet.core.runtime.api.Container;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

/**
 * Generate and display a welcome page
 */
@Mojo(name = "welcome", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class WelcomeMojo extends AbstractZetMojo {

	@Override
	public void executeGoal() throws MojoExecutionException, MojoFailureException {
		try {
			buildWelcomeHtmlFile();

			new ProcessBuilder("xdg-open", "file://" + getWelcomePagePath()).start();
			getLog().info("********************************************************");
			getLog().info("*                                                      *");
			getLog().info("* The WELCOME page was opened in your default browser  *");
			getLog().info("*                                                      *");
			getLog().info("********************************************************");
		}
		catch (IOException e) {
			throw new MojoExecutionException("Cannot start browser:" + e, e);
		}
	}

	private void buildWelcomeHtmlFile() throws IOException, MojoExecutionException {
		StringBuilder welcomePage = new StringBuilder();
		welcomePage
				.append(Resources.toString(Thread.currentThread().getContextClassLoader().getResource("welcome/before.html"), Charsets.UTF_8));

		for (File child : carnotzet.getResourcesFolder().toFile().listFiles()) {
			appendWelcomeFrom(child, welcomePage);
		}

		welcomePage.append(Resources.toString(Thread.currentThread().getContextClassLoader().getResource("welcome/after.html"), Charsets.UTF_8));

		String welcomePageStr = replaceIpPlaceholders(welcomePage.toString());

		Files.write(welcomePageStr, new File(getWelcomePagePath()), Charsets.UTF_8);
	}

	private String replaceIpPlaceholders(String content) throws MojoExecutionException {
		String res = content;
		List<Container> containers = getRuntime().getContainers();
		for (Container container : containers) {
			res = res.replace("/${" + container.getServiceName() + ".ip}", "/" + container.getServiceName() + ".docker");
			res = res.replace("${" + container.getServiceName() + ".ip}",
					container.getIp() == null ? "No IP address, is container started ?" : container.getIp());
		}
		return res;
	}

	private void appendWelcomeFrom(File child, StringBuilder welcomePage) throws IOException {
		File welcomeFile = new File(child, "welcome/welcome.html");
		if (welcomeFile.exists()) {
			welcomePage.append(Files.toString(welcomeFile, Charsets.UTF_8));
		}
	}

	private String getWelcomePagePath() {
		return carnotzet.getResourcesFolder() + "/welcome.html";
	}

}
