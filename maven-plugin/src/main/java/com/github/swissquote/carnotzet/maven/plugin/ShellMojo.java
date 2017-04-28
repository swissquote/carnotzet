package com.github.swissquote.carnotzet.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.components.interactivity.Prompter;

import com.github.swissquote.carnotzet.maven.plugin.impl.Shell;

/**
 * Starts a shell in a running container and binds it's IO to the current process
 */
@Mojo(name = "shell", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ShellMojo extends AbstractZetMojo {

	@Component
	private Prompter prompter;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Shell.execute(getRuntime(), prompter, getLog(), getService());
	}

}
