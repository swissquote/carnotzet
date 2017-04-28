package com.github.swissquote.carnotzet.maven.plugin.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;
import com.github.swissquote.carnotzet.core.runtime.api.Container;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntime;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

public final class Welcome {

	private Welcome() {
		// static function holder
	}

	public static void execute(ContainerOrchestrationRuntime runtime, Carnotzet carnotzet, Log log)
			throws MojoExecutionException, MojoFailureException {
		try {
			buildWelcomeHtmlFile(runtime, carnotzet);

			new ProcessBuilder("xdg-open", "file://" + getWelcomePagePath(carnotzet)).start();
			log.info("********************************************************");
			log.info("*                                                      *");
			log.info("* The WELCOME page was opened in your default browser  *");
			log.info("*                                                      *");
			log.info("********************************************************");
		}
		catch (IOException e) {
			throw new MojoExecutionException("Cannot start browser:" + e, e);
		}
	}

	private static void buildWelcomeHtmlFile(ContainerOrchestrationRuntime runtime, Carnotzet carnotzet)
			throws IOException, MojoExecutionException {
		StringBuilder welcomePage = new StringBuilder();
		welcomePage.append(
				Resources.toString(Thread.currentThread().getContextClassLoader().getResource("welcome/before.html"), Charsets.UTF_8));

		File[] children = carnotzet.getResourcesFolder().toFile().listFiles();
		if (children == null) {
			throw new CarnotzetDefinitionException(
					"Resources folder does not exist or is not a directory : [" + carnotzet.getResourcesFolder() + "]");
		}

		for (File child : children) {
			appendWelcomeFrom(child, welcomePage);
		}

		welcomePage.append(
				Resources.toString(Thread.currentThread().getContextClassLoader().getResource("welcome/after.html"), Charsets.UTF_8));

		String welcomePageStr = replaceIpPlaceholders(runtime, welcomePage.toString());

		Files.write(welcomePageStr, new File(getWelcomePagePath(carnotzet)), Charsets.UTF_8);
	}

	private static String replaceIpPlaceholders(ContainerOrchestrationRuntime runtime, String content) throws MojoExecutionException {
		String res = content;
		List<Container> containers = runtime.getContainers();
		for (Container container : containers) {
			res = res.replace("/${" + container.getServiceName() + ".ip}", "/" + container.getServiceName() + ".docker");
			res = res.replace("${" + container.getServiceName() + ".ip}",
					container.getIp() == null ? "No IP address, is container started ?" : container.getIp());
		}
		return res;
	}

	private static void appendWelcomeFrom(File child, StringBuilder welcomePage) throws IOException {
		File welcomeFile = new File(child, "welcome/welcome.html");
		if (welcomeFile.exists()) {
			welcomePage.append(Files.toString(welcomeFile, Charsets.UTF_8));
		}
	}

	private static String getWelcomePagePath(Carnotzet carnotzet) {
		return carnotzet.getResourcesFolder() + "/welcome.html";
	}

}
