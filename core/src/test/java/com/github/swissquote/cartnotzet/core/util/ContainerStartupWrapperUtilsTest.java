package com.github.swissquote.cartnotzet.core.util;

import static com.github.swissquote.carnotzet.core.runtime.DefaultCommandRunner.INSTANCE;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.util.ContainerStartupWrapperUtils;

public class ContainerStartupWrapperUtilsTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private void createDockerImage(String tag, String entrypoint, String cmd) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("FROM scratch\n");
		if (entrypoint != null) {
			sb.append("ENTRYPOINT ");
			sb.append(entrypoint);
			sb.append("\n");
		}
		if (cmd != null) {
			sb.append("CMD ");
			sb.append(cmd);
			sb.append("\n");
		}
		writeStringToFile(temp.newFile("Dockerfile"), sb.toString());
		INSTANCE.runCommand(temp.getRoot(), "docker", "build", "-t", tag, ".");
	}

	private void deleteDockerImage(String tag) {
		INSTANCE.runCommand(temp.getRoot(), "docker", "rmi", tag);
	}

	private CarnotzetModule getWrappedModule(String imageName) {
		CarnotzetModule module = CarnotzetModule.builder()
				.imageName(imageName)
				.topLevelModuleName("top-module")
				.dockerVolumes(new HashSet<>())
				.serviceId("my-service")
				.build();

		return ContainerStartupWrapperUtils.wrap(module)
				.withStartingScriptContent("echo hi!")
				.usingWrapperName("test-wrapper")
				.inResourceFolder(Paths.get("/tmp"))
				.build();
	}

	@Test
	public void exec_entrypoint_no_cmd() throws IOException {
		String imageName = "carnotzet-test-exec-entrypoint-no-cmd";
		createDockerImage(imageName, "[\"/container_entrypoint\"]", null);

		INSTANCE.runCommand(temp.getRoot(), "docker", "build", "-t", "carnotzet-test-exec-entrypoint-no-cmd", ".");

		CarnotzetModule wrapped = getWrappedModule(imageName);

		String wrapperContent = FileUtils.readFileToString(new File("/tmp/startup-wrappers/test-wrapper_my-service.sh"));
		assertThat(wrapperContent, is("#!/bin/sh\necho hi!\nexec \"$@\"\n"));
		assertTrue(wrapped.getDockerVolumes().contains("/tmp/startup-wrappers/test-wrapper_my-service.sh:/test-wrapper_my-service.sh"));
		assertThat(wrapped.getDockerEntrypoint(), is("[\"/test-wrapper_my-service.sh\",\"/container_entrypoint\"]"));
		assertNull(wrapped.getDockerCmd());

		deleteDockerImage(imageName);
	}

	@Test
	public void no_entrypoint_exec_cmd() throws IOException {
		String imageName = "carnotzet-test-no-entrypoint-exec-cmd";
		createDockerImage(imageName, null, "[\"/container_cmd\",\"arg1\"]");

		CarnotzetModule wrapped = getWrappedModule(imageName);

		assertThat(wrapped.getDockerEntrypoint(), is("[\"/test-wrapper_my-service.sh\"]"));
		assertThat(wrapped.getDockerCmd(), is("[\"/container_cmd\",\"arg1\"]"));
		deleteDockerImage(imageName);
	}

	@Test
	public void no_entrypoint_shell_cmd() throws IOException {
		String imageName = "carnotzet-test-no-entrypoint-shell-cmd";
		createDockerImage(imageName, null, "/container_cmd arg1");

		CarnotzetModule wrapped = getWrappedModule(imageName);

		assertThat(wrapped.getDockerEntrypoint(), is("[\"/test-wrapper_my-service.sh\"]"));
		assertThat(wrapped.getDockerCmd(), is("[\"/bin/sh\",\"-c\",\"/container_cmd arg1\"]"));
		deleteDockerImage(imageName);
	}

	@Test
	public void shell_entrypoint_no_cmd() throws IOException {
		String imageName = "carnotzet-test-shell-entrypoint-no-cmd";
		createDockerImage(imageName, "/container_entrypoint", null);

		CarnotzetModule wrapped = getWrappedModule(imageName);

		assertThat(wrapped.getDockerEntrypoint(), is("[\"/test-wrapper_my-service.sh\",\"/bin/sh\",\"-c\",\"/container_entrypoint\"]"));
		assertNull(wrapped.getDockerCmd());
		deleteDockerImage(imageName);
	}

	@Test
	public void shell_entrypoint_exec_cmd() throws IOException {
		String imageName = "carnotzet-test-shell-entrypoint-exec-cmd";
		createDockerImage(imageName, "/container_entrypoint", "[\"arg1\",\"arg2\"]");

		CarnotzetModule wrapped = getWrappedModule(imageName);

		assertThat(wrapped.getDockerEntrypoint(), is("[\"/test-wrapper_my-service.sh\",\"/bin/sh\",\"-c\",\"/container_entrypoint\"]"));
		// Note : This CMD has no effect in practice because /bin/sh -c only takes 1 argument
		assertThat(wrapped.getDockerCmd(), is("[\"arg1\",\"arg2\"]"));
		deleteDockerImage(imageName);
	}

	@Test
	public void exec_entrypoint_shell_cmd() throws IOException {
		String imageName = "carnotzet-test-exec-entrypoint-shell-cmd";
		createDockerImage(imageName, "[\"/container_entrypoint\"]", "/container_cmd arg1");

		CarnotzetModule wrapped = getWrappedModule(imageName);

		assertThat(wrapped.getDockerEntrypoint(), is("[\"/test-wrapper_my-service.sh\",\"/container_entrypoint\"]"));
		assertThat(wrapped.getDockerCmd(), is("[\"/bin/sh\",\"-c\",\"/container_cmd arg1\"]"));
		deleteDockerImage(imageName);
	}

	@Test
	public void exec_entrypoint_exec_cmd() throws IOException {
		String imageName = "carnotzet-test-exec-entrypoint-exec-cmd";
		createDockerImage(imageName, "[\"/container_entrypoint\",\"entrypoint_arg\"]", "[\"arg1\",\"arg2\"]");

		CarnotzetModule wrapped = getWrappedModule(imageName);

		assertThat(wrapped.getDockerEntrypoint(), is("[\"/test-wrapper_my-service.sh\",\"/container_entrypoint\",\"entrypoint_arg\"]"));
		assertThat(wrapped.getDockerCmd(), is("[\"arg1\",\"arg2\"]"));
		deleteDockerImage(imageName);
	}

}
