package com.github.swissquote.carnotzet.maven.plugin.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntime;
import com.github.swissquote.carnotzet.core.welcome.IpPlaceholderResolver;
import com.github.swissquote.carnotzet.core.welcome.WelcomePageGenerator;

public final class Welcome {

	private Welcome() {
		// static function holder
	}

	public static void execute(ContainerOrchestrationRuntime runtime, Carnotzet carnotzet, Log log)
			throws MojoExecutionException, MojoFailureException {
		try {
			IpPlaceholderResolver ipPlaceholderResolver = new IpPlaceholderResolver(runtime);

			WelcomePageGenerator generator = new WelcomePageGenerator(Arrays.asList(ipPlaceholderResolver));

			Path moduleResources = carnotzet.getResourcesFolder().resolve("expanded-jars");
			Path welcomePagePath = carnotzet.getResourcesFolder().resolve("welcome.html");

			generator.buildWelcomeHtmlFile(moduleResources, welcomePagePath);

			new ProcessBuilder("xdg-open", "file://" + welcomePagePath).start();
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

}
