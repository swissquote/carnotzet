package com.github.swissquote.cartnotzet.core.util;

import org.junit.Test;

import com.github.swissquote.carnotzet.core.runtime.DefaultCommandRunner;

// ensures that the wrapping doesn't break
public class ContainerStartupWrapperUtilsTest {

	@Test
	public void get_registry_entrypoint() {

	}

	@Test
	public void no_entrypoint_no_cmd() {

	}

	@Test
	public void no_entrypoint_exec_cmd() {

	}

	@Test
	public void no_entrypoint_shell_cmd() {

	}

	@Test
	public void shell_entrypoint_no_cmd() {

	}

	@Test
	public void shell_entrypoint_shell_cmd() {
		// CMD should be ignored
	}

	@Test
	public void shell_entrypoint_exec_cmd() {
		// CMD should be ignored
	}

	@Test
	public void exec_entrypoint_shell_cmd() {
		// This case rarely occurs in the wild because docker adds /bin/sh -c to arguments of the entrypoint)
		buildDockerImage("[\"echo\", \"\\\"'quoted from entrypoint'\\\"\", \"from entrypoint\"]",
				"[\"\\\"'yep yep'\\\"\", \"si\"]",
				"tests_carnotzet_exec_entrypoint_shell_cmd");

		String expected = runContainer("tests_carnotzet_exec_entrypoint_shell_cmd");

	}

	private String runContainer(String tag) {
		return null;
		//return DefaultCommandRunner.INSTANCE.runCommandAndCaptureOutput("docker", "run", "--rm", tag);
	}

	@Test
	public void exec_entrypoint_exec_cmd() {

	}

	private void buildDockerImage(String entrypoint, String cmd, String tag) {

	}

}
