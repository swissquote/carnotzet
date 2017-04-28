package com.github.swissquote.carnotzet.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.swissquote.carnotzet.maven.plugin.impl.Run;

/**
 * Start the environment, tail the log and wait for interrupt, stops the environment when you interrupt with CTRL+C
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class RunMojo extends AbstractZetMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Run.execute(getRuntime(), getCarnotzet(), getService());
	}

}
