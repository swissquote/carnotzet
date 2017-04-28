package com.github.swissquote.carnotzet.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.swissquote.carnotzet.maven.plugin.impl.Logs;

/**
 * Output logs, by default, logs are followed with a tail of 200<br>
 * You can override this behavior using "follow" and "tail" system properties<br>
 * example to get the last 5 log events from each service and return : mvn zet:logs -Dfollow=false -Dtail=5
 */
@Mojo(name = "logs", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class LogsMojo extends AbstractZetMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Logs.execute(getRuntime(), getCarnotzet(), getService());
	}

}
