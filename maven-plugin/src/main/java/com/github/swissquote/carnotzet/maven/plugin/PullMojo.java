package com.github.swissquote.carnotzet.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.swissquote.carnotzet.core.runtime.api.PullPolicy;

import lombok.Getter;

/**
 * Pulls all images in the carnotzet from the docker image registry. <br>
 * use -Dpull.policy=... to pull only under certain conditions. <br>
 * supported policies are (always|ifNotPresent|ifNewer)
 *
 */
@Mojo(name = "pull", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class PullMojo extends AbstractZetMojo {

	@Parameter(defaultValue = "always", property = "pull.policy")
	@Getter
	private String imagePullPolicy;

	@Override
	public void executeInternal() throws MojoExecutionException, MojoFailureException {
		PullPolicy policy = null;
		if (imagePullPolicy == null) {
			policy = PullPolicy.ALWAYS;
		} else {
			switch (imagePullPolicy) {
				case "always":
					policy = PullPolicy.ALWAYS;
					break;
				case "ifNotPresent":
					policy = PullPolicy.IF_LOCAL_IMAGE_ABSENT;
					break;
				case "ifNewer":
					policy = PullPolicy.IF_REGISTRY_IMAGE_NEWER;
					break;
				default:
					throw new MojoExecutionException("Unknown image pull policy : " + imagePullPolicy);
			}
		}

		getLog().info("Pulling images with policy : " + imagePullPolicy);
		getRuntime().pull(policy);
	}
}
